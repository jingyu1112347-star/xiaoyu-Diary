package com.xiaoyou.diary.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.ui.theme.Primary
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

enum class TabKey(val label: String, val icon: ImageVector) {
    HOME("首页", Icons.Default.Waves),
    ANALYZE("分析", Icons.Default.PieChart),
    DIARY("监测", Icons.Default.MonitorHeart),
    HISTORY("历史", Icons.Default.History),
    SETTINGS("设置", Icons.Default.Settings);

    val isCenter: Boolean get() = this == DIARY
}

/**
 * 悬浮液态玻璃 Tab 栏：
 * - 液态胶囊选中框：跟随横向滑动/切页连续移动（spring 弹性回弹）
 * - 图标选中时弹性放大（MediumBouncy spring）
 * - 点击 Tab 以弹性动画滚动 Pager；内容区支持左右滑动切换页面
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TabBar(
    pagerState: PagerState,
    hazeState: HazeState,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val tabs = TabKey.entries
    val dark = colors.bg.isDark()
    val scope = rememberCoroutineScope()
    val current = pagerState.currentPage

    val stripShape = RoundedCornerShape(36.dp)
    val glassTint = if (dark) Color(0x99202530) else Color(0x99FFFFFF)
    val rimColor = if (dark) Color(0x40A9C9FF) else Color(0x66A9CCF2)

    // 点击/外部导航切页时的弹簧动画参数
    val pageSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
    ) {
        // 液态玻璃条：真实背景模糊 + 高光反射 + 渐变边缘 + 描边 + 柔和投影
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                .fillMaxWidth()
                .height(72.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = stripShape,
                    ambientColor = Color(0x1A1B3A66),
                    spotColor = Color(0x331677FF)
                )
                .frostedGlass(
                    hazeState = hazeState,
                    shape = stripShape,
                    tint = glassTint,
                    blurRadius = 20.dp,
                    borderColor = rimColor,
                    borderWidth = 1.dp,
                    sheen = if (dark) Color(0x2EFFFFFF) else Color(0x59FFFFFF),
                    edgeShade = if (dark) Color(0x40000000) else Color(0x0F1B2B4D)
                )
        ) {
            // 液态胶囊选中框：位置直接由 Pager 的连续偏移驱动，滑动时跟手，点击时随弹簧动画滑动
            BoxWithConstraints(modifier = Modifier.matchParentSize()) {
                val slot = maxWidth / tabs.size
                val fraction = pagerState.currentPage + pagerState.currentPageOffsetFraction
                val capsuleAlpha by animateFloatAsState(
                    targetValue = if (current == TabKey.DIARY.ordinal) 0f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "capsuleAlpha"
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = slot * fraction + 6.dp)
                        .width(slot - 12.dp)
                        .height(56.dp)
                        .alpha(capsuleAlpha)
                        .glassSurface(
                            shape = RoundedCornerShape(28.dp),
                            tint = if (dark) Color(0x2EFFFFFF) else Color(0xC2FFFFFF),
                            borderColor = if (dark) Color(0x4DFFFFFF) else Color(0x8CFFFFFF),
                            borderWidth = 1.dp,
                            sheen = if (dark) Color(0x33FFFFFF) else Color(0x66FFFFFF),
                            edgeShade = if (dark) Color(0x1F000000) else Color(0x0F1B2B4D)
                        )
                )
            }

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val selected = tab.ordinal == current
                    // 选中图标弹性放大
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.18f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = 900f
                        ),
                        label = "iconScale"
                    )
                    if (tab.isCenter) {
                        // 中央凸起按钮（监测）
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.fillMaxHeight()
                            ) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .shadow(
                                            elevation = 12.dp,
                                            shape = CircleShape,
                                            ambientColor = Color(0x661677FF),
                                            spotColor = Color(0x661677FF)
                                        )
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = if (selected)
                                                    listOf(Color(0xFF0E5BD6), Color(0xFF0A3E96))
                                                else
                                                    listOf(Color(0xFF1677FF), Color(0xFF0E5BD6))
                                            )
                                        )
                                        .border(3.dp, colors.card, CircleShape)
                                        .clickable {
                                            scope.launch {
                                                pagerState.animateScrollToPage(
                                                    tab.ordinal,
                                                    animationSpec = pageSpring
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.label,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .scale(iconScale)
                                    )
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = tab.label,
                                    fontSize = 11.sp,
                                    color = if (selected) Primary else colors.tabInactive,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    } else {
                        // 普通标签
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable {
                                    scope.launch {
                                        pagerState.animateScrollToPage(
                                            tab.ordinal,
                                            animationSpec = pageSpring
                                        )
                                    }
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.label,
                                tint = if (selected) Primary else colors.tabInactive,
                                modifier = Modifier
                                    .size(19.dp)
                                    .scale(iconScale)
                            )
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = tab.label,
                                fontSize = 11.sp,
                                color = if (selected) Primary else colors.tabInactive
                            )
                        }
                    }
                }
            }
        }
    }
}
