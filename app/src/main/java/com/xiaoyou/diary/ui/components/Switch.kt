package com.xiaoyou.diary.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

/**
 * 同心圆环形开关（规范：占位 30dp 正圆，44dp 点击热区，220ms 颜色过渡）：
 *  开启：环形区域明亮绿色 + 外圈浅色细环 + 中心纯白小圆
 *  关闭：环形区域中性灰 + 中心浅灰小圆
 */
@Composable
fun RingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalAppColors.current

    val ringColor by animateColorAsState(
        targetValue = if (checked) c.green else Color.Transparent,
        animationSpec = tween(220), label = "ringColor"
    )
    val centerColor by animateColorAsState(
        targetValue = if (checked) Color.White else c.ink3,
        animationSpec = tween(220), label = "centerColor"
    )

    Box(
        modifier = modifier
            .size(44.dp)
            .semantics {
                role = Role.Switch
                toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
            }
            .tapScale { onCheckedChange(!checked) },
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val stroke = 1.dp.toPx()
            val outer = size.minDimension / 2f - stroke / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            // 环形区域填充
            drawCircle(color = ringColor, radius = outer, center = center)
            // 外圈浅色细圆环边框
            drawCircle(
                color = c.rule2,
                radius = outer,
                center = center,
                style = Stroke(width = stroke)
            )
            // 中心小圆（半径比例 0.40 ≈ 设计稿 inset 8/30）
            drawCircle(color = centerColor, radius = outer * 0.40f, center = center)
        }
    }
}
