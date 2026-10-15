package com.xiaoyou.diary.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorProbability
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.viewmodel.AppViewModel
import com.xiaoyou.diary.viewmodel.HomeViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

@Composable
fun HomeScreen(
    appVm: AppViewModel,
    homeVm: HomeViewModel,
    onCalibrate: () -> Unit,
    onSettings: () -> Unit
) {
    val c = LocalAppColors.current
    val dateStr = SimpleDateFormat("M月d日 EEEE", Locale.CHINESE).format(Date())

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        // ---------- 导航栏 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("概览", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = c.ink, letterSpacing = (-0.5f).sp)
                Text("$dateStr · 实时识别", fontSize = 12.5.sp, color = c.ink3)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillIconButton(Icons.Filled.GpsFixed, "传感器校准", onClick = onCalibrate)
                PillIconButton(Icons.Filled.Settings, "设置", onClick = onSettings)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            // 内容不足一屏时严格垂直居中；超出时自然顶部起滚
            verticalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 130.dp)
        ) {
            // ---------- 实时行为大卡 ----------
            item {
                AppSurface(modifier = Modifier.padding(bottom = 26.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (homeVm.isDetecting) {
                                PillBadge("检测中", BadgeTone.PURPLE, pulseDot = true)
                            } else {
                                PillBadge("已停止", BadgeTone.GREEN)
                            }
                            Text(
                                "${homeVm.sampleRate}Hz · 加速度/陀螺仪/重力",
                                fontSize = 11.sp,
                                color = c.ink3
                            )
                        }

                        if (homeVm.isDetecting) {
                            LiveHeroContent(homeVm)
                            Spacer(Modifier.height(14.dp))
                            LiveWave(active = true)
                        } else {
                            StoppedHero()
                        }
                    }
                }
            }

            // ---------- 姿态参数 ----------
            item {
                GroupHeader("姿态参数 · ATTITUDE", modifier = Modifier.padding(bottom = 11.dp))
                AppSurface(modifier = Modifier.padding(bottom = 26.dp)) {
                    Row(modifier = Modifier.padding(vertical = 16.dp)) {
                        PoseCell("PITCH", if (homeVm.isDetecting) homeVm.attitude.pitch else null)
                        VerticalRule()
                        PoseCell("ROLL", if (homeVm.isDetecting) homeVm.attitude.roll else null)
                        VerticalRule()
                        PoseCell("YAW", if (homeVm.isDetecting) homeVm.attitude.yaw else null)
                    }
                }
            }

            // ---------- 8 分类行为概率 ----------
            item {
                GroupHeader(
                    "8 分类行为概率",
                    modifier = Modifier.padding(bottom = 11.dp),
                    accent = true,
                    trailing = { Text("实时", fontSize = 12.sp, color = c.ink3) }
                )
                AppSurface(modifier = Modifier.padding(bottom = 26.dp)) {
                    homeVm.probabilities.forEachIndexed { index, prob ->
                        ProbabilityRow(prob, index == 0 && homeVm.isDetecting, homeVm.isDetecting)
                        if (index < homeVm.probabilities.lastIndex) Hairline(start = 18.dp)
                    }
                }
            }

            // ---------- 快捷操作 ----------
            item {
                GroupHeader(
                    "快捷操作",
                    modifier = Modifier.padding(bottom = 11.dp),
                    trailing = { HeaderMore("更多") }
                )
                AppSurface {
                    ActionRow(
                        icon = Icons.Filled.PlayArrow,
                        title = "开始检测",
                        subtitle = "持续识别并每 30 秒记录一条",
                        tone = c.blue,
                        soft = c.blueSoft,
                        onClick = {
                            if (!homeVm.isDetecting) {
                                homeVm.toggleDetection { appVm.showToast(it, ToastType.INFO) }
                            }
                        }
                    )
                    Hairline()
                    ActionRow(
                        icon = Icons.Filled.Stop,
                        title = "停止检测",
                        subtitle = "结束本次识别会话",
                        tone = c.red,
                        soft = c.redSoft,
                        enabled = homeVm.isDetecting,
                        onClick = {
                            if (homeVm.isDetecting) {
                                homeVm.toggleDetection { appVm.showToast(it) }
                            }
                        }
                    )
                    Hairline()
                    ActionRow(
                        icon = Icons.Filled.Bolt,
                        title = "单次检测",
                        subtitle = "快速采样一次（即将推出）",
                        tone = c.ink2,
                        soft = c.surface2,
                        enabled = false,
                        onClick = { }
                    )
                    Hairline()
                    ActionRow(
                        icon = Icons.Filled.GpsFixed,
                        title = "传感器校准",
                        subtitle = "静置 / 水平 / 六面校准",
                        tone = c.ink2,
                        soft = c.surface2,
                        onClick = onCalibrate
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveHeroContent(homeVm: HomeViewModel) {
    val c = LocalAppColors.current

    // 本次会话内行为切换次数（VM 未提供，本地按主行为变化统计）。
    // 首个行为不算切换：仅在已有上一行为且行为发生变化时 +1。
    var switches by remember { mutableStateOf(0) }
    var lastBehavior by remember { mutableStateOf<BehaviorType?>(null) }
    LaunchedEffect(homeVm.isDetecting) {
        if (homeVm.isDetecting) {
            switches = 0
            lastBehavior = null
        }
    }
    LaunchedEffect(homeVm.currentBehavior) {
        if (homeVm.isDetecting) {
            val prev = lastBehavior
            if (prev != null && prev != homeVm.currentBehavior) switches++
            lastBehavior = homeVm.currentBehavior
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            homeVm.currentBehavior.label,
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color = c.yellow,
            letterSpacing = (-1f).sp
        )
        Column(horizontalAlignment = Alignment.End) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.0f".format(homeVm.confidence),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = c.blue,
                    letterSpacing = (-1f).sp
                )
                Text("%", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.blue)
            }
            Text("置信度", fontSize = 11.sp, color = c.ink3)
        }
    }
    Row(
        modifier = Modifier.padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(buildString {
            append("持续 ")
            append(formatElapsed(homeVm.elapsedSeconds))
        }, fontSize = 12.5.sp, color = c.ink2)
        Text("本次切换 $switches 次", fontSize = 12.5.sp, color = c.ink2)
    }
}

private fun formatElapsed(total: Int): String {
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

@Composable
private fun StoppedHero() {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(58.dp).clip(CircleShape).background(c.surface2),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Bolt, null, tint = c.ink3, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("暂无实时采集数据", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink)
        Spacer(Modifier.height(5.dp))
        Text(
            "检测已停止，点击下方 FAB 或快捷操作\n即可重新开始行为识别",
            fontSize = 12.5.sp,
            color = c.ink3,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun LiveWave(active: Boolean) {
    val c = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "wavePhase"
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
        val path = Path()
        val amp = if (active) size.height * 0.32f else size.height * 0.07f
        for (x in 0..size.width.toInt() step 4) {
            val y = size.height / 2f +
                sin(x / 22f + phase) * amp +
                sin(x / 9f + phase * 1.7f) * amp * 0.25f
            if (x == 0) path.moveTo(0f, y) else path.lineTo(x.toFloat(), y)
        }
        drawPath(
            path = path,
            color = c.blue.copy(alpha = 0.85f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun RowScope.PoseCell(tag: String, value: Float?) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(tag, fontSize = 11.sp, color = c.ink3, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            // 未检测时无真实姿态数据，统一显示“--”占位
            Text(
                value?.let { "%.1f".format(it) } ?: "--",
                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                color = if (value != null) c.ink else c.ink3
            )
            Text("°", fontSize = 12.sp, color = c.ink3, modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(9.dp))
        PoseMeter(value ?: 0f)
    }
}

@Composable
private fun RowScope.VerticalRule() {
    val c = LocalAppColors.current
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(62.dp)
            .align(Alignment.CenterVertically)
            .background(c.rule)
    )
}

@Composable
private fun PoseMeter(value: Float) {
    val c = LocalAppColors.current
    val fraction = ((value + 180f) / 360f).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(c.surface2)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(CircleShape)
                .background(c.blue)
        )
    }
}

@Composable
private fun ProbabilityRow(prob: BehaviorProbability, isTop: Boolean, live: Boolean) {
    val c = LocalAppColors.current
    val color = c.behavior(prob.type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(start = 14.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧 4px 彩色竖条
        Box(modifier = Modifier.width(4.dp).height(30.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Spacer(Modifier.width(12.dp))
        Text(
            prob.type.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isTop) c.yellow else c.ink,
            modifier = Modifier.width(42.dp)
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.weight(1f).height(7.dp).clip(CircleShape).background(c.surface2)
        ) {
            // 停止态不展示概率条，避免把占位/残留数据当作实时结果
            if (live) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((prob.probability / 100f).coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            if (live) "%.0f%%".format(prob.probability) else "--",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isTop) c.blue else c.ink2,
            modifier = Modifier.width(44.dp)
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    tone: Color,
    soft: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 18.dp)
            .then(if (enabled) Modifier.tapScale(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(soft),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (enabled) tone else c.ink3, modifier = Modifier.size(17.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) c.ink else c.ink3
            )
            Text(subtitle, fontSize = 11.5.sp, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", fontSize = 18.sp, color = c.ink3)
    }
}
