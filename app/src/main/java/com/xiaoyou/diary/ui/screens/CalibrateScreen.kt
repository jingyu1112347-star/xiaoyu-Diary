package com.xiaoyou.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.AppViewModel

@Composable
fun CalibrateScreen(appVm: AppViewModel, onFinish: () -> Unit) {
    val colors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                    .clickable { appVm.setTab(com.xiaoyou.diary.ui.components.TabKey.HOME) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ChevronLeft, null, tint = colors.ink1, modifier = Modifier.size(18.dp))
            }
            Text("传感器校准", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
            Spacer(Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(72.dp).background(Warn.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🧭", fontSize = 32.sp)
                }
                Spacer(Modifier.height(16.dp))
                Text("校准前请按步骤操作", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
                Spacer(Modifier.height(6.dp))
                Text(
                    "校准可消除传感器零偏，提升姿态解算与行为识别精度",
                    fontSize = 12.sp, color = colors.ink3, textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(4.dp))

            // Step 1
            AppCard {
                Row(modifier = Modifier.padding(16.dp)) {
                    StepNumber(1)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("水平静置设备", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                        Spacer(Modifier.height(4.dp))
                        Text("将手机屏幕朝上，平放在稳固的水平桌面，避免任何震动与触碰。",
                            fontSize = 12.sp, color = colors.ink2, lineHeight = 18.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✓", fontSize = 12.sp, color = Success)
                            Spacer(Modifier.width(8.dp))
                            Text("检测到水平度 0.6°（合格）", fontSize = 11.sp, color = colors.ink3)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Step 2
            AppCard {
                Row(modifier = Modifier.padding(16.dp)) {
                    StepNumber(2)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("保持静止 3 秒", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                        Spacer(Modifier.height(4.dp))
                        Text("采样过程中请勿移动设备，系统将自动采集 150 组静态样本。",
                            fontSize = 12.sp, color = colors.ink2, lineHeight = 18.sp)
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(colors.line)) {
                            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Primary))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("150 / 150 组 · 已完成", fontSize = 11.sp, color = colors.ink3)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Step 3
            AppCard {
                Row(modifier = Modifier.padding(16.dp)) {
                    StepNumber(3)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("旋转设备一周", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                        Spacer(Modifier.height(4.dp))
                        Text("将设备沿三个轴向各缓慢旋转 360°，用于标定磁力计与陀螺仪。",
                            fontSize = 12.sp, color = colors.ink2, lineHeight = 18.sp)
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AxisCell("俯仰", "完成", Success)
                            AxisCell("横滚", "完成", Success)
                            AxisCell("偏航", "进行中", Warn)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Warning
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Warn.copy(alpha = 0.09f), RoundedCornerShape(8.dp))
                    .padding(12.dp, 16.dp)
            ) {
                Text("⚠", fontSize = 13.sp)
                Spacer(Modifier.width(10.dp))
                Text("校准期间请远离磁性物体与金属桌面，否则可能导致偏航角漂移。",
                    fontSize = 12.sp, color = Color(0xFFB36A0E), lineHeight = 18.sp)
            }
        }

        // Bottom button
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Primary)
                    .clickable {
                        appVm.showToast("校准完成 · 传感器零偏已更新")
                        onFinish()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("完成校准", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Text("上次校准：3 月 18 日 14:22", fontSize = 12.sp, color = colors.ink3)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StepNumber(n: Int) {
    Box(
        modifier = Modifier.size(32.dp).background(Primary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("$n", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun RowScope.AxisCell(label: String, value: String, valueColor: Color) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier.weight(1f).background(colors.soft, RoundedCornerShape(6.dp)).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 10.sp, color = colors.ink3)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = valueColor)
    }
}
