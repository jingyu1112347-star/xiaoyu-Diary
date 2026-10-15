package com.xiaoyou.diary.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import com.xiaoyou.diary.ui.theme.LocalAppColors

/** 4 个核心 Tab（设置 / 校准为二级页，不进主 Tab） */
enum class TabKey(val label: String, val icon: ImageVector) {
    HOME("概览", Icons.Filled.Home),
    ANALYZE("分析", Icons.Filled.BarChart),
    DIARY("日记", Icons.Filled.Book),
    HISTORY("历史", Icons.Filled.History);
}

/**
 * 透明液态玻璃底部导航 + 圆形 FAB：
 * - 胶囊为高透液态玻璃：真实折射后方画面、通透棱边高光、顶部反光（非磨砂、非纯色）
 * - 蓝色/红色 FAB 按下后向上凸起：放大悬浮、外圈透明玻璃气泡、棱边强高光、底部轻阴影，
 *   气泡与胶囊边缘自然交叠融合
 */
@Composable
fun Dock(
    current: TabKey,
    detecting: Boolean,
    onTab: (TabKey) -> Unit,
    onFab: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backdrop = rememberWindowBackdrop()

    val pillStyle = glassStyle(strong = false)
    val bubbleStyle = glassStyle(strong = true)

    // 锚点真实宽度（px）。不能让 Popup 内容用 fillMaxWidth：Compose 1.6 的 Popup
    // 测量上限取自窗口 context 的 Configuration.screenWidthDp*density，在部分国产
    // ROM（显示大小/全面屏兼容配置）上会比物理屏窄 20~40dp，导致整个 Dock 右移、
    // 胶囊被挤窄。这里以锚点实测宽度强制 Popup 等宽（requiredWidth 可突破 Popup
    // 的 AT_MOST 测量上限，PopupLayout 会按实测宽回写窗口宽度），任何设备几何一致。
    var anchorWidthPx by remember { mutableStateOf(0) }
    val anchorWidthDp = with(LocalDensity.current) { anchorWidthPx.toDp() }

    // 零尺寸锚点（贴着父布局底部居中）。Dock 本体在下面的独立 Popup 窗口（第二个
    // Surface）中渲染：主窗口 PixelCopy 永远抓不到玻璃与图标文字，从物理上消除
    // 白雾自反馈与文字重影；抓帧也不再需要任何闸门，玻璃窗口一帧都不闪。
    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { anchorWidthPx = it.size.width }
    ) {
        Popup(
            popupPositionProvider = DockPopupPositionProvider,
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                clippingEnabled = false
            )
        ) {
            // Surface 只覆盖窗口自身尺寸：胶囊 12dp 投影、FAB 按下气泡的上浮放大
            // 都画在 Row 边界之外，四周必须预留白边，否则会被窗口边缘硬裁切
            Box(
                modifier = Modifier
                    .then(
                        if (anchorWidthPx > 0) Modifier.requiredWidth(anchorWidthDp)
                        else Modifier.fillMaxWidth()
                    )
                    .padding(start = 6.dp, end = 6.dp, top = 16.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp) // 外留白 6 + 内边距 12 = 原 18dp
                        .padding(bottom = 12.dp),   // 外留白 12 + 内边距 12 = 原 24dp
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ---------- 透明液态玻璃胶囊 ----------
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .shadow(elevation = 12.dp, shape = RoundedCornerShape(999.dp))
                            .liquidGlass(backdrop, GlassShape.PILL, pillStyle)
                            .padding(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabKey.entries.forEach { tab ->
                            TabItem(
                                tab = tab,
                                selected = tab == current,
                                modifier = Modifier.weight(1f),
                                onClick = { onTab(tab) }
                            )
                        }
                    }

                    // ---------- 蓝色 FAB：按下凸起玻璃球 ----------
                    GlassFab(
                        detecting = detecting,
                        backdrop = backdrop,
                        bubbleStyle = bubbleStyle,
                        onFab = onFab
                    )
                }
            }
        }
    }
}

/**
 * Dock 悬浮窗口定位：以零尺寸锚点（贴在全屏父布局 BottomCenter）为准对齐。
 * 不能用 windowSize 贴底——它是窗口可视帧尺寸，部分 ROM 上不含完整系统栏区域，
 * 会把 Dock 顶高/顶偏；anchorBounds 与 Popup 坐标同属主窗口坐标系。
 * Popup 内容已被强制为锚点等宽，故 x 恒等于 anchorBounds.left；保留居中公式，
 * 使宽度测量在任何 ROM 上出现差异时窗口也只会对称校正，不会单侧漂移。
 */
private object DockPopupPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset = IntOffset(
        x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2,
        y = anchorBounds.bottom - popupContentSize.height
    )
}

/** 液态玻璃外观参数（像素在本地按 Density 换算） */
@Composable
private fun glassStyle(strong: Boolean): GlassStyle {
    val dark = LocalAppColors.current.isDark
    val density = androidx.compose.ui.platform.LocalDensity.current
    return with(density) {
        if (strong) {
            // 凸起玻璃球气泡：棱边更亮、折射幅度更大
            GlassStyle(
                tintTop = Color.White.copy(alpha = if (dark) 0.06f else 0.16f),
                tintMid = Color.White.copy(alpha = if (dark) 0.02f else 0.06f),
                tintBottom = Color.White.copy(alpha = if (dark) 0.05f else 0.14f),
                rimBright = Color.White.copy(alpha = if (dark) 1.0f else 0.95f),
                rimDim = Color.White.copy(alpha = if (dark) 0.22f else 0.5f),
                sheen = Color.White.copy(alpha = if (dark) 0.42f else 0.3f),
                refractionHeight = 4.dp.toPx(),
                refractionAmount = 4.dp.toPx(),
                rimWidth = 2.dp.toPx()
            )
        } else {
            GlassStyle(
                // tint 与折射量保持克制：仅边缘一圈薄透镜，内部始终清晰通透
                tintTop = Color.White.copy(alpha = if (dark) 0.04f else 0.20f),
                tintMid = Color.White.copy(alpha = if (dark) 0.015f else 0.08f),
                tintBottom = Color.White.copy(alpha = if (dark) 0.03f else 0.18f),
                rimBright = Color.White.copy(alpha = if (dark) 0.55f else 0.9f),
                rimDim = Color.White.copy(alpha = if (dark) 0.16f else 0.42f),
                sheen = Color.White.copy(alpha = if (dark) 0.06f else 0.24f),
                refractionHeight = 4.dp.toPx(),
                refractionAmount = 2.5.dp.toPx(),
                rimWidth = 1.2.dp.toPx()
            )
        }
    }
}

@Composable
private fun GlassFab(
    detecting: Boolean,
    backdrop: WindowBackdrop,
    bubbleStyle: GlassStyle,
    onFab: () -> Unit
) {
    val c = LocalAppColors.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    // 按下凸起 / 检测状态切换时，背景短时间高速刷新，保证玻璃球折射实时
    LaunchedEffect(pressed) { if (pressed) backdrop.kick() }
    LaunchedEffect(detecting) { backdrop.kick() }

    // 凸起进度：按下后弹簧鼓起，松手回落
    val bulge by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "fabBulge"
    )
    val fabColor = if (detecting) c.red else c.blue
    val fabTop = lerp(fabColor, Color.White, if (detecting) 0.14f else 0.18f)
    val fabBottom = lerp(fabColor, Color.Black, 0.12f)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .width(104.dp)
                .height(108.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // 底部接触阴影（鼓起时变淡变小，产生悬浮感）
            Box(
                modifier = Modifier
                    .padding(bottom = 2.dp)
                    .graphicsLayer {
                        alpha = 0.22f * (1f - 0.5f * bulge)
                        scaleX = 1f - 0.18f * bulge
                    }
                    .width(58.dp)
                    .height(11.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.Black, Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            // FAB 品牌色环境辉光（按下时随玻璃气泡增强）
            Box(
                modifier = Modifier
                    .padding(bottom = 0.dp)
                    .size(74.dp)
                    .offset {
                        IntOffset(
                            (-6.dp.toPx() * bulge).roundToInt(),
                            (-10.dp.toPx() * bulge).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        alpha = 0.20f + 0.16f * bulge
                        val s = 1f + 0.22f * bulge
                        scaleX = s
                        scaleY = s
                    }
                    .background(
                        Brush.radialGradient(
                            0.55f to fabColor.copy(alpha = 0.42f),
                            1f to Color.Transparent
                        ),
                        CircleShape
                    )
            )

            // 透明玻璃气泡：静止时与 FAB 完全重合（不可见），按下后放大上浮，与胶囊边缘交叠融合
            Box(
                modifier = Modifier
                    .padding(bottom = 0.dp)
                    .size(64.dp)
                    .offset {
                        IntOffset(
                            (-6.dp.toPx() * bulge).roundToInt(),
                            (-16.dp.toPx() * bulge).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        alpha = bulge
                        val s = 1f + 0.375f * bulge   // 64dp → 88dp
                        scaleX = s
                        scaleY = s
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.62f)
                    }
                    .liquidGlass(backdrop, GlassShape.CIRCLE, bubbleStyle),
                content = {
                    // 玻璃球左上高光斑（镜面反射）
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                val cx = size.width * 0.36f
                                val cy = size.height * 0.28f
                                val r = size.width * 0.5f
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = 0.55f),
                                            Color.Transparent
                                        ),
                                        center = Offset(cx, cy),
                                        radius = r
                                    ),
                                    radius = r,
                                    center = Offset(cx, cy)
                                )
                            }
                    )
                }
            )

            // 蓝色/红色圆形 FAB 本体
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .offset {
                        IntOffset(
                            (-6.dp.toPx() * bulge).roundToInt(),
                            (-16.dp.toPx() * bulge).roundToInt()
                        )
                    }
                    .graphicsLayer {
                        val s = 1f + 0.1f * bulge
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(fabTop, fabBottom)))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onFab
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 顶部玻璃反光
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.White.copy(alpha = 0.28f),
                                0.42f to Color.Transparent,
                                1f to Color.White.copy(alpha = 0.08f)
                            )
                        )
                )
                // 高光棱边
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                )
                Icon(
                    imageVector = if (detecting) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (detecting) "停止检测" else "开始检测",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            if (detecting) "停止检测" else "开始检测",
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = c.ink3
        )
    }
}

@Composable
private fun TabItem(
    tab: TabKey,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 900f),
        label = "tabIconScale"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            if (c.isDark) Color.White else c.ink
        } else {
            c.ink3
        },
        label = "tabColor"
    )
    Column(
        modifier = modifier
            .height(50.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(Color.White.copy(alpha = if (c.isDark) 0.14f else 0.72f))
                } else {
                    Modifier
                }
            )
            .tapScale(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = contentColor,
            modifier = Modifier
                .size(20.dp)
                .scale(iconScale)
        )
        Spacer(Modifier.height(3.dp))
        Text(tab.label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = contentColor)
    }
}
