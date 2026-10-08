package com.xiaoyou.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyou.diary.ServiceLocator
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.screens.*
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.*
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appVm: AppViewModel = viewModel()
            val homeVm: HomeViewModel = viewModel()
            val settingsVm: SettingsViewModel = viewModel()
            val diaryVm: DiaryViewModel = viewModel()
            val analyzeVm: AnalyzeViewModel = viewModel()

            // 收集仓库的提醒事件（久坐提醒等），显示为 Toast
            val repository = remember { ServiceLocator.provideRepository(this) }
            LaunchedEffect(Unit) {
                repository.reminderEvent.collectLatest { msg ->
                    if (!msg.isNullOrEmpty()) {
                        appVm.showToast(msg)
                        repository.consumeReminder()
                    }
                }
            }

            XiaoYouTheme(darkTheme = appVm.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalAppColors.current.bg
                ) {
                    // 顶部避让系统状态栏，避免与系统时间/电量重叠
                    Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                        AppRoot(appVm, homeVm, settingsVm, diaryVm, analyzeVm)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    appVm: AppViewModel,
    homeVm: HomeViewModel,
    settingsVm: SettingsViewModel,
    diaryVm: DiaryViewModel,
    analyzeVm: AnalyzeViewModel
) {
    var showSplash by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showSplash) {
            SplashScreen(onEnter = { showSplash = false })
        } else {
            MainContent(appVm, homeVm, settingsVm, diaryVm, analyzeVm)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MainContent(
    appVm: AppViewModel,
    homeVm: HomeViewModel,
    settingsVm: SettingsViewModel,
    diaryVm: DiaryViewModel,
    analyzeVm: AnalyzeViewModel
) {
    val colors = LocalAppColors.current
    // 液态玻璃模糊源：页面内容作为被模糊的底层，供底栏与玻璃按钮采样
    val hazeState = remember { HazeState() }
    // 横向滑动 Pager：五个 Tab 页左右滑动切换，驱动底部液态胶囊选中框
    val pagerState = rememberPagerState(initialPage = appVm.currentTab.ordinal) {
        TabKey.entries.size
    }
    // Pager -> VM：滑动停稳后同步选中 Tab
    LaunchedEffect(pagerState.currentPage) {
        val tab = TabKey.entries[pagerState.currentPage]
        if (appVm.currentTab != tab) appVm.setTab(tab)
    }
    // VM -> Pager：外部导航（如校准完成返回首页）以弹簧动画滚动过去
    LaunchedEffect(appVm.currentTab) {
        val target = appVm.currentTab.ordinal
        if (pagerState.currentPage != target) {
            pagerState.animateScrollToPage(
                target,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        // 页面主内容：作为液态玻璃的模糊源铺满全屏；底部留白让内容可滚动到底栏后方
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState)
                .padding(bottom = 84.dp)
        ) {
            if (appVm.currentPage == "calibrate") {
                CalibrateScreen(appVm) {
                    settingsVm.markCalibrated()
                    appVm.showToast("校准完成 · 传感器零偏已更新")
                    appVm.setTab(TabKey.HOME)
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (TabKey.entries[page]) {
                        TabKey.HOME -> HomeScreen(appVm, homeVm, hazeState) { appVm.navigateTo("calibrate") }
                        TabKey.ANALYZE -> AnalyzeScreen(appVm, analyzeVm)
                        TabKey.DIARY -> DiaryScreen(appVm, diaryVm, analyzeVm)
                        TabKey.HISTORY -> HistoryScreen(appVm, analyzeVm)
                        TabKey.SETTINGS -> SettingsScreen(appVm, settingsVm, analyzeVm) { appVm.navigateTo("calibrate") }
                    }
                }
            }
        }

        // 液态玻璃底部导航：悬浮在内容上层，内容透过玻璃产生真实模糊
        if (appVm.currentPage != "calibrate") {
            TabBar(
                pagerState = pagerState,
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Toast overlay
        ToastHost(
            toast = appVm.toast,
            onDismiss = { appVm.dismissToast() }
        )

        // Confirm dialog overlay
        if (appVm.showConfirm) {
            ConfirmDialog(appVm)
        }

        // Diary bottom sheet overlay
        if (appVm.showDiarySheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80101828))
            )
            AnimatedVisibility(
                visible = appVm.showDiarySheet,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                DiarySheet(appVm, diaryVm)
            }
        }
    }
}
