package com.xiaoyou.diary.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xiaoyou.diary.ui.theme.LocalAppColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild

/** 判断颜色是否为深色（用于液态玻璃明暗自适应） */
internal fun Color.isDark(): Boolean {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return luminance < 0.5f
}

/**
 * 强化液态玻璃：基于 Haze 的真实背景模糊 + 渐变边缘 + 顶部高光反射 + 描边。
 * 即使浅色主题下背景接近白色，也通过 tint + 渐变 + 描边保证玻璃面板清晰可见，
 * 不会退化为普通不透明面板。children（文字/图标）绘制在玻璃层之上，不受影响。
 */
fun Modifier.frostedGlass(
    hazeState: HazeState,
    shape: Shape,
    tint: Color,
    blurRadius: Dp = 20.dp,
    borderColor: Color = Color(0x40FFFFFF),
    borderWidth: Dp = 1.dp,
    sheen: Color = Color(0x33FFFFFF),
    edgeShade: Color = Color(0x14000000)
): Modifier {
    return this
        // 1) 真实背景模糊（被底层 .haze(hazeState) 的内容采样）
        .hazeChild(
            state = hazeState,
            shape = shape,
            style = HazeStyle(
                tint = tint,
                blurRadius = blurRadius,
                noiseFactor = 0.15f
            )
        )
        // 2) 液态渐变边缘 + 轻微反光（保证明暗主题下都清晰可见）
        .clip(shape)
        .drawBehind {
            // 顶部高光反射带
            drawRect(
                brush = Brush.verticalGradient(
                    0f to sheen,
                    0.4f to Color.Transparent
                )
            )
            // 底部轻微阴影，形成液态边缘厚度
            drawRect(
                brush = Brush.verticalGradient(
                    0.6f to Color.Transparent,
                    1f to edgeShade
                )
            )
        }
        // 3) 细描边勾勒玻璃轮廓
        .border(BorderStroke(borderWidth, borderColor), shape)
}

/**
 * 仿真液态玻璃表面（不依赖 Haze）：
 * 半透明磨砂色调 + 顶部高光反射 + 渐变边缘 + 描边。
 * 适用于位于模糊源内部的面板/按钮（如内容卡片、操作按钮）——
 * 若对其使用 hazeChild 会把自身文字一并模糊，故此处仅做光学拟态，
 * 文字始终清晰锐利，深浅主题下均保持玻璃质感不退化为实体面板。
 */
fun Modifier.glassSurface(
    shape: Shape,
    tint: Color,
    borderColor: Color,
    borderWidth: Dp = 1.dp,
    sheen: Color = Color(0x40FFFFFF),
    edgeShade: Color = Color(0x14000000)
): Modifier {
    return this
        .clip(shape)
        .drawBehind {
            // 半透明磨砂底
            drawRect(tint)
            // 顶部高光反射带
            drawRect(
                brush = Brush.verticalGradient(
                    0f to sheen,
                    0.4f to Color.Transparent
                )
            )
            // 底部渐变阴影形成液态边缘厚度
            drawRect(
                brush = Brush.verticalGradient(
                    0.6f to Color.Transparent,
                    1f to edgeShade
                )
            )
        }
        .border(BorderStroke(borderWidth, borderColor), shape)
}

/**
 * 液态玻璃（LiquidGlass）修饰符：
 * 半透明磨砂底 + 顶部高光反射 + 细白描边 + 柔和投影，自动适配深色/浅色主题。
 * 与 Compose 1.6 / Kotlin 1.9 工具链完全兼容。
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    elevation: Dp = 14.dp
): Modifier {
    val colors = LocalAppColors.current
    val dark = colors.bg.isDark()

    // 磨砂底：浅色用白色透明，深色用深灰透明，透出底层背景形成玻璃感
    val frostedBase = if (dark) Color(0x991B1E25) else Color(0xB3FFFFFF)
    // 顶部高光反射（玻璃镜面感）
    val sheenTop = if (dark) Color(0x1FFFFFFF) else Color(0x70FFFFFF)
    val sheenBottom = Color.Transparent
    // 细描边：浅色偏白，深色偏白低透明
    val borderColor = if (dark) Color(0x26FFFFFF) else Color(0x80FFFFFF)
    // 柔和投影
    val shadowColor = if (dark) Color(0x55000000) else Color(0x14101828)

    return this
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
        .clip(shape)
        .drawBehind {
            // 磨砂底
            drawRect(color = frostedBase)
            // 顶部高光渐变（上亮下透）
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(sheenTop, sheenBottom),
                    startY = 0f,
                    endY = size.height * 0.5f
                )
            )
        }
        .border(BorderStroke(1.dp, borderColor), shape)
}

/**
 * 通栏液态玻璃（用于底部导航栏等全屏宽度容器）：
 * 半透明磨砂底 + 顶部高光线，无圆角、无外扩阴影，深浅主题自适应。
 */
@Composable
fun Modifier.glassBar(): Modifier {
    val colors = LocalAppColors.current
    val dark = colors.bg.isDark()
    val frostedBase = if (dark) Color(0xE616181C) else Color(0xE6FFFFFF)
    val rimColor = if (dark) Color(0x26FFFFFF) else Color(0x66FFFFFF)
    return this
        .drawBehind {
            drawRect(color = frostedBase)
            // 顶部玻璃高光线
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(rimColor, Color.Transparent),
                    startY = 0f,
                    endY = 2.dp.toPx()
                )
            )
        }
}

/**
 * 液态玻璃卡片：替代普通卡片，所有页面统一套用玻璃质感。
 * 内容放在磨砂玻璃之上，文字保持原有可读性。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape)
    ) {
        content()
    }
}
