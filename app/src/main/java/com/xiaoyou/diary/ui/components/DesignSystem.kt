package com.xiaoyou.diary.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.xiaoyou.diary.ui.theme.LocalAppColors

/* 圆角常量（对齐参考稿） */
object Dimens {
    val Card = 22.dp
    val Sheet = 30.dp
    val Button = 16.dp
    val Field = 18.dp
    val Pill = 999.dp
}

/** 按下时缩放到 0.96 的点击反馈（参考 :active scale(.96)），无涟漪干扰 */
@Composable
fun Modifier.tapScale(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "tapScale")
    return this
        .scale(scale)
        .clickable(
            interactionSource = interaction,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}

/** 单层表面卡片：比背景亮一级 + 细分隔线描边，禁止嵌套 */
@Composable
fun AppSurface(
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(Dimens.Card),
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.rule, shape),
        content = content
    )
}

/** 行内细分隔线 */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    start: Dp = 18.dp,
    end: Dp = 18.dp
) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .padding(start = start, end = end)
            .fillMaxWidth()
            .height(1.dp)
            .background(c.rule)
    )
}

/**
 * 分组标题：
 * @param accent true = 强调色小型大写；false = 灰色小型大写
 * @param trailing 右侧操作区（更多/右箭头等）
 */
@Composable
fun GroupHeader(
    text: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.6.sp,
            color = if (accent) c.blue else c.ink3
        )
        trailing?.invoke()
    }
}

@Composable
fun HeaderMore(text: String = "更多", onClick: (() -> Unit)? = null) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .height(32.dp)
            .then(if (onClick != null) Modifier.tapScale(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = 12.sp, color = c.ink3)
        Text("›", fontSize = 15.sp, color = c.ink3)
    }
}

/** 胶囊状态徽章：紫色=状态，绿色=计数/达标，蓝色=运行 */
@Composable
fun PillBadge(
    text: String,
    tone: BadgeTone,
    modifier: Modifier = Modifier,
    pulseDot: Boolean = false
) {
    val c = LocalAppColors.current
    val (fg, bg) = when (tone) {
        BadgeTone.PURPLE -> c.purple to c.purpleSoft
        BadgeTone.GREEN -> c.green to c.greenSoft
        BadgeTone.BLUE -> c.blue to c.blueSoft
        BadgeTone.RED -> c.red to c.redSoft
        BadgeTone.YELLOW -> c.yellow to c.yellowSoft
    }
    Row(
        modifier = modifier
            .height(26.dp)
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (pulseDot) PulseDot(color = fg, size = 6.dp)
        else Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(fg))
        Text(text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = fg)
    }
}

enum class BadgeTone { PURPLE, GREEN, BLUE, RED, YELLOW }

/** 44dp 独立圆形图标按钮 */
@Composable
fun CircleButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.surface)
            .border(1.dp, c.rule, CircleShape)
            .then(if (enabled) Modifier.tapScale(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, contentDescription,
            tint = if (enabled) c.ink else c.ink3.copy(alpha = 0.4f),
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 44dp 高的胶囊图标按钮（导航栏右上操作组） */
@Composable
fun PillIconButton(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Box(
        modifier = modifier
            .height(44.dp)
            .width(44.dp)
            .clip(CircleShape)
            .background(c.surface)
            .border(1.dp, c.rule, CircleShape)
            .tapScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = c.ink2, modifier = Modifier.size(18.dp))
    }
}

/** 全宽实心主按钮（蓝/红） */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    tone: Color = LocalAppColors.current.blue,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(Dimens.Button))
            .background(if (enabled) tone else c.surface2)
            .tapScale(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(17.dp))
        }
        Text(
            text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) Color.White else c.ink3
        )
    }
}

/* ---------- 底部弹窗骨架（圆角面板 + 抓手 + 左 X 右确认） ---------- */

@Composable
fun AppModalSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String,
    showConfirm: Boolean = false,
    onConfirm: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalAppColors.current
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(c.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentHeight(Alignment.Bottom)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.Sheet))
                    .background(c.surface)
                    .border(1.dp, c.rule2, RoundedCornerShape(Dimens.Sheet))
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 9.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(c.rule2)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SheetRoundX(onDismiss)
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink)
                    if (showConfirm) {
                        SheetRoundCheck(onConfirm ?: onDismiss)
                    } else {
                        Spacer(Modifier.size(34.dp))
                    }
                }
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = 18.dp, end = 18.dp, bottom = 28.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
fun SheetRoundX(onClick: () -> Unit) {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(c.surface2)
            .tapScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("✕", fontSize = 12.sp, color = c.ink2)
    }
}

@Composable
fun SheetRoundCheck(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(LocalAppColors.current.blue)
            .tapScale(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("✓", fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

/** 弹窗内说明文字 */
@Composable
fun SheetDescription(text: String, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    Text(
        text,
        modifier = modifier.padding(top = 2.dp, bottom = 12.dp),
        fontSize = 12.5.sp,
        color = c.ink3,
        lineHeight = 20.sp
    )
}

/** 呼吸脉冲小圆点（检测中徽章） */
@Composable
fun PulseDot(color: Color, size: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    Box(modifier = Modifier.size(size).clip(CircleShape).background(color.copy(alpha = alpha)))
}

/** 空状态：圆形插画 + 标题 + 描述 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    val c = LocalAppColors.current
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(c.surface)
                .border(1.dp, c.rule, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = c.ink3, modifier = Modifier.size(44.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = c.ink, letterSpacing = (-0.3).sp)
        Spacer(Modifier.height(10.dp))
        Text(
            description,
            fontSize = 13.5.sp,
            color = c.ink3,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(26.dp))
            action()
        }
    }
}
