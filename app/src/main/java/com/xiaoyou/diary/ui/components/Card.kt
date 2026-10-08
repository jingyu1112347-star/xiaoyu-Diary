package com.xiaoyou.diary.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.theme.LocalAppColors

/**
 * 小面积圆润液态玻璃卡片（参考 Liquid Glass Backdrop）：
 * 半透明磨砂 + 轻微反光 + 淡蓝渐变边缘 + 柔和投影，
 * 文字清晰锐利，深浅主题都不退化为实体面板。
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = LocalAppColors.current.bg.isDark()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0x1A1B3A66),
                spotColor = Color(0x261677FF)
            )
            .glassSurface(
                shape = RoundedCornerShape(22.dp),
                tint = if (dark) Color(0x8F161B22) else Color(0x8CEAF3FF),
                borderColor = if (dark) Color(0x3D9FC5E8) else Color(0x73A9CCF2),
                borderWidth = 1.dp,
                sheen = if (dark) Color(0x2EFFFFFF) else Color(0x59FFFFFF),
                edgeShade = if (dark) Color(0x2E000000) else Color(0x0A1B2B4D)
            )
    ) {
        content()
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = RoundedCornerShape(14.dp))
    ) {
        content()
    }
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.line)
    )
}
