package com.xiaoyou.diary.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ModeNight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.AppViewModel
import com.xiaoyou.diary.viewmodel.HomeViewModel
import dev.chrisbanes.haze.HazeState

@Composable
fun HomeScreen(
    appVm: AppViewModel,
    homeVm: HomeViewModel,
    hazeState: HazeState,
    onCalibrate: () -> Unit
) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        // NavBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "小幽日记",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.ink1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { appVm.toggleDarkMode() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (appVm.isDarkMode) Icons.Default.LightMode else Icons.Outlined.ModeNight,
                        contentDescription = "深色模式",
                        tint = colors.navIcon,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { appVm.showToast("更多功能开发中") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = null,
                        tint = colors.navIcon,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // Content
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top core area
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(Color(0x1F52C41A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(homeVm.currentBehavior.emoji, fontSize = 32.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(homeVm.currentBehavior.label, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("置信度 ", fontSize = 13.sp, color = colors.ink2)
                        Text(
                            "%.1f".format(homeVm.confidence),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.ink1
                        )
                        Text("%", fontSize = 13.sp, color = colors.ink2)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(6.dp).background(
                                if (homeVm.isDetecting) Success else colors.ink3,
                                CircleShape
                            )
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (homeVm.isDetecting) "检测中 · 已运行 " else "已停止 · 已运行 ",
                            fontSize = 11.sp,
                            color = colors.ink3
                        )
                        Text(
                            homeVm.formatTime(),
                            fontSize = 11.sp,
                            color = if (homeVm.isDetecting) colors.ink1 else colors.ink3,
                            fontWeight = if (homeVm.isDetecting) FontWeight.Normal else FontWeight.Normal
                        )
                        Text(" · 采样 50Hz", fontSize = 11.sp, color = colors.ink3)
                    }
                }
            }

            // Attitude params
            item {
                GlassCard(modifier = Modifier.padding(vertical = 0.dp)) {
                    Column(modifier = Modifier.padding(12.dp, 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("姿态参数", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, null, tint = colors.ink3, modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("实时更新", fontSize = 11.sp, color = colors.ink3)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("俯仰角", fontSize = 11.sp, color = colors.ink3)
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    Text("%.1f".format(homeVm.attitude.pitch), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                                    Text("°", fontSize = 11.sp, color = colors.ink3)
                                }
                            }
                            Column(
                                Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("横滚角", fontSize = 11.sp, color = colors.ink3)
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    Text("%.1f".format(homeVm.attitude.roll), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                                    Text("°", fontSize = 11.sp, color = colors.ink3)
                                }
                            }
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("偏航角", fontSize = 11.sp, color = colors.ink3)
                                Spacer(Modifier.height(4.dp))
                                Row {
                                    Text("%.1f".format(homeVm.attitude.yaw), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                                    Text("°", fontSize = 11.sp, color = colors.ink3)
                                }
                            }
                        }
                    }
                }
            }

            // Behavior probabilities
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(12.dp, 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("行为概率", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                            Text("8 类 · 实时刷新", fontSize = 11.sp, color = colors.ink3)
                        }
                        Spacer(Modifier.height(10.dp))
                        homeVm.probabilities.forEach { prob ->
                            ProbabilityRow(prob.type, prob.probability, prob.highlighted, colors.ink1, colors.ink2, colors.line)
                            if (prob != homeVm.probabilities.last()) Spacer(Modifier.height(5.dp))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }

        // Bottom action area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val dark = colors.bg.isDark()
            // 胶囊形液态玻璃按钮（小圆角胶囊，参考 Liquid Glass Backdrop）
            val btnShape = RoundedCornerShape(26.dp)
            val smallShape = RoundedCornerShape(18.dp)

            // 主按钮：着色液态玻璃（蓝/红磨砂玻璃，透出底层内容模糊）
            val detectTint = if (homeVm.isDetecting) Color(0xA6E5484D) else Color(0xA61677FF)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(52.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = btnShape,
                        ambientColor = detectTint,
                        spotColor = detectTint
                    )
                    .glassSurface(
                        shape = btnShape,
                        tint = detectTint,
                        borderColor = Color.White.copy(alpha = 0.45f),
                        borderWidth = 1.dp,
                        sheen = Color(0x4DFFFFFF),
                        edgeShade = Color(0x33000000)
                    )
                    .clickable { homeVm.toggleDetection { appVm.showToast(it) } },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (homeVm.isDetecting) {
                        PulseDot()
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        if (homeVm.isDetecting) "停止检测" else "开始检测",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // 次按钮：中性液态玻璃（浅色白磨砂 / 深色淡白磨砂），描边保证可见
            val ghostTint = if (dark) Color(0x59FFFFFF) else Color(0x99FFFFFF)
            val ghostBorder = if (dark) Color(0x40FFFFFF) else Color(0x66000000)
            Row(
                modifier = Modifier.fillMaxWidth(0.7f),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .glassSurface(
                            shape = smallShape,
                            tint = ghostTint,
                            borderColor = ghostBorder,
                            borderWidth = 1.dp,
                            sheen = if (dark) Color(0x33FFFFFF) else Color(0x55FFFFFF),
                            edgeShade = if (dark) Color(0x20000000) else Color(0x14000000)
                        )
                        .clickable { appVm.showToast("已启动单次检测") },
                    contentAlignment = Alignment.Center
                ) {
                    Text("单次检测", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (dark) Color.White else Color(0xFF101820))
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .glassSurface(
                            shape = smallShape,
                            tint = ghostTint,
                            borderColor = ghostBorder,
                            borderWidth = 1.dp,
                            sheen = if (dark) Color(0x33FFFFFF) else Color(0x55FFFFFF),
                            edgeShade = if (dark) Color(0x20000000) else Color(0x14000000)
                        )
                        .clickable { onCalibrate() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("传感器校准", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = if (dark) Color.White else Color(0xFF101820))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ProbabilityRow(
    type: BehaviorType,
    probability: Float,
    highlighted: Boolean,
    ink1: Color,
    ink2: Color,
    line: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .background(
                if (highlighted) type.color.copy(alpha = 0.09f) else Color.Transparent,
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(type.emoji, fontSize = 11.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            type.label,
            fontSize = 12.sp,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            color = ink1,
            modifier = Modifier.width(28.dp)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(CircleShape)
                .background(line)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(probability / 100f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(type.color)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "%.1f%%".format(probability),
            fontSize = 12.sp,
            fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
            color = if (highlighted) ink1 else ink2,
            modifier = Modifier.width(44.dp)
        )
    }
}

@Composable
private fun PulseDot() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = androidx.compose.animation.core.CubicBezierEasing(0.24f, 0.72f, 0.34f, 1f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = androidx.compose.animation.core.CubicBezierEasing(0.24f, 0.72f, 0.34f, 1f)),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    Box(Modifier.size(8.dp)) {
        Box(
            Modifier
                .size(8.dp)
                .scale(scale)
                .background(Color.White.copy(alpha = alpha), CircleShape)
        )
        Box(Modifier.size(8.dp).background(Color.White, CircleShape))
    }
}
