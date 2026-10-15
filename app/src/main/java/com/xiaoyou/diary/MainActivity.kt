package com.xiaoyou.diary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoyou.diary.ServiceLocator
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.screens.*
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
                        appVm.showToast(msg, ToastType.INFO)
                        repository.consumeReminder()
                    }
                }
            }

            XiaoYouTheme(darkTheme = appVm.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalAppColors.current.bg
                ) {
                    Box(modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
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
    val scope = rememberCoroutineScope()
    // 记录进入校准页前的页面，系统返回键按来源回退（设置页→设置，主页→主页）
    var pageBeforeCalibrate by remember { mutableStateOf("home") }

    // 系统返回键：二级页/日记弹窗消费返回事件回退，而不是直接退出 App
    BackHandler(enabled = appVm.showDiarySheet) {
        appVm.closeDiarySheet()
    }
    BackHandler(enabled = appVm.currentPage == "calibrate") {
        appVm.navigateTo(pageBeforeCalibrate)
    }
    BackHandler(enabled = appVm.currentPage == "settings") {
        appVm.setTab(appVm.currentTab)
    }

    val pagerState = rememberPagerState(initialPage = appVm.currentTab.ordinal) {
        TabKey.entries.size
    }

    // Pager -> VM：滑动停稳后同步选中 Tab
    LaunchedEffect(pagerState.currentPage) {
        val tab = TabKey.entries[pagerState.currentPage]
        if (appVm.currentTab != tab) appVm.setTab(tab)
    }
    // VM -> Pager：外部导航以弹簧动画滚动（背景位移回弹纵深）
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

    // 弹窗弹出时背景缩小后推（scale .93）
    val sheetOpen = appVm.showDiarySheet
    val contentScale by animateFloatAsState(if (sheetOpen) 0.93f else 1f, label = "rootScale")
    val contentRound by animateFloatAsState(if (sheetOpen) 38f else 0f, label = "rootRound")

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {

        // ---------- 主页面（4 Tab Pager） ----------
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = contentScale
                    scaleY = contentScale
                    shape = RoundedCornerShape(contentRound.dp.toPx())
                    clip = contentRound > 0.5f
                }
        ) {
            if (appVm.currentPage == "calibrate") {
                CalibrateScreen(appVm) {
                    settingsVm.markCalibrated()
                    appVm.showToast("校准完成 · 传感器零偏已更新", ToastType.OK)
                    appVm.navigateTo(pageBeforeCalibrate)
                }
            } else if (appVm.currentPage == "settings") {
                SettingsScreen(
                    appVm = appVm,
                    settingsVm = settingsVm,
                    analyzeVm = analyzeVm,
                    onCalibrate = {
                        pageBeforeCalibrate = "settings"
                        appVm.navigateTo("calibrate")
                    },
                    onBack = { appVm.setTab(appVm.currentTab) }
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (TabKey.entries[page]) {
                        TabKey.HOME -> HomeScreen(
                            appVm = appVm,
                            homeVm = homeVm,
                            onCalibrate = {
                                pageBeforeCalibrate = "home"
                                appVm.navigateTo("calibrate")
                            },
                            onSettings = { appVm.navigateTo("settings") }
                        )
                        TabKey.ANALYZE -> AnalyzeScreen(appVm, analyzeVm)
                        TabKey.DIARY -> DiaryScreen(appVm, diaryVm, analyzeVm)
                        TabKey.HISTORY -> HistoryScreen(appVm, analyzeVm)
                    }
                }

                // 悬浮胶囊导航 + FAB（二级页隐藏）
                Dock(
                    current = appVm.currentTab,
                    detecting = homeVm.isDetecting,
                    onTab = { tab ->
                        scope.launch {
                            pagerState.animateScrollToPage(
                                tab.ordinal,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    },
                    onFab = {
                        // 非主页只切回主页，不在同一次点击里顺带启停检测，避免交互混乱
                        if (appVm.currentTab != TabKey.HOME) {
                            appVm.setTab(TabKey.HOME)
                        } else {
                            homeVm.toggleDetection { msg ->
                                appVm.showToast(
                                    msg,
                                    if (homeVm.isDetecting) ToastType.INFO else ToastType.OK
                                )
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // ---------- Toast ----------
        ToastHost(toast = appVm.toast, onDismiss = { appVm.dismissToast() })

        // ---------- 确认对话框 ----------
        if (appVm.showConfirm) {
            ConfirmDialog(appVm)
        }

        // ---------- 日记底部弹窗（背景缩放 + 底部滑入） ----------
        AnimatedVisibility(
            visible = appVm.showDiarySheet,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x73000000))
            )
        }
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
