package com.xiaoyou.diary.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.ui.theme.Success

private val TRACK_WIDTH = 38.dp
private val TRACK_HEIGHT = 22.dp
private val KNOB_SIZE = 18.dp
private val KNOB_PADDING = 2.dp
private val KNOB_TRAVEL = TRACK_WIDTH - KNOB_SIZE - KNOB_PADDING * 2 // 16.dp

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    // 背景颜色动画
    val bgColor = remember { Animatable(colors.switchOffBg) }
    // 滑块横向位移动画（单位：dp，用 Float 驱动再转 dp）
    val knobOffset = remember { Animatable(0f) }

    // 监听 checked 变化，执行位移动画
    LaunchedEffect(checked) {
        val targetOffset = if (checked) KNOB_TRAVEL.value else 0f
        knobOffset.animateTo(targetOffset, animationSpec = tween(durationMillis = 220))
    }

    // 监听 checked 变化，执行背景色动画
    val targetBg = if (checked) Success else colors.switchOffBg
    LaunchedEffect(targetBg) {
        bgColor.animateTo(targetBg, animationSpec = tween(durationMillis = 220))
    }

    Box(
        modifier = modifier
            .width(TRACK_WIDTH)
            .height(TRACK_HEIGHT)
            .clip(CircleShape)
            .background(bgColor.value)
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .clickable { onCheckedChange(!checked) }
    ) {
        // 滑块：由 knobOffset (Float) 驱动横向位移，确保 animateTo 平滑滑动
        Box(
            modifier = Modifier
                .size(KNOB_SIZE)
                .clip(CircleShape)
                .background(Color.White)
                .align(Alignment.CenterStart)
                .offset(x = (KNOB_PADDING.value + knobOffset.value).dp)
        )
    }
}

private val RING_SIZE = 30.dp

/**
 * 同心圆环形开关：
 *  - 外圈：浅色细圆环边框
 *  - 中心与外圈之间的环形区域：开启为明亮绿色，关闭为中性灰
 *  - 中心小圆：开启为纯白色，关闭为浅灰
 * 整体为正圆，与深色/浅色界面协调，保持与原开关相近的占位与对齐。
 */
@Composable
fun RingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val dark = colors.bg.isDark()

    val ringColor by animateColorAsState(
        targetValue = if (checked) Success else colors.switchOffBg,
        animationSpec = tween(220), label = "ringColor"
    )
    val centerColor by animateColorAsState(
        targetValue = if (checked) Color.White else if (dark) Color(0xFF8A93A0) else Color(0xFFB7BDC6),
        animationSpec = tween(220), label = "centerColor"
    )
    // 外圈细边框：深色界面用低透明白，浅色界面用低透明灰
    val borderColor = if (dark) Color(0x33FFFFFF) else Color(0x2E000000)

    Canvas(
        modifier = modifier
            .size(RING_SIZE)
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .clickable { onCheckedChange(!checked) }
    ) {
        val stroke = 1.2.dp.toPx()
        val outer = size.minDimension / 2f - stroke / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        // 环形填充（中心之后会被小圆覆盖，形成环形区域）
        drawCircle(color = ringColor, radius = outer, center = center)
        // 外圈浅色细边框
        drawCircle(
            color = borderColor,
            radius = outer,
            center = center,
            style = Stroke(width = stroke)
        )
        // 中心小圆
        drawCircle(color = centerColor, radius = outer * 0.40f, center = center)
    }
}
