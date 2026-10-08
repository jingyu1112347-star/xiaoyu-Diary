package com.xiaoyou.diary.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.AnalyzeViewModel
import com.xiaoyou.diary.viewmodel.AppViewModel
import com.xiaoyou.diary.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    appVm: AppViewModel,
    settingsVm: SettingsViewModel,
    analyzeVm: AnalyzeViewModel,
    onCalibrate: () -> Unit
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    // 导入文件选择器
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                analyzeVm.importJson(json) { count ->
                    appVm.showToast(if (count > 0) "成功导入 $count 条记录" else "导入失败：文件格式无效")
                }
            } catch (_: Exception) {
                appVm.showToast("导入失败：无法读取文件")
            }
        }
    }

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
            Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
            Spacer(Modifier.width(24.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Detection settings
            item {
                Column {
                    Text("检测设置", fontSize = 12.sp, color = colors.ink3, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    SettingsGroup {
                        SettingRow("采样率", right = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${settingsVm.sampleRate} Hz", fontSize = 13.sp, color = colors.ink2)
                                Icon(Icons.Default.ChevronRight, null, tint = colors.ink3, modifier = Modifier.size(12.dp))
                            }
                        }) { settingsVm.cycleSampleRate { appVm.showToast(it) } }
                        DividerLine()
                        SettingRow("检测灵敏度", right = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("低", fontSize = 12.sp, color = colors.ink3)
                                Spacer(Modifier.width(8.dp))
                                // 滑块轨道：点击整行循环切换档位，旋钮位置按百分比计算
                                val trackW = 100.dp
                                val knobSize = 14.dp
                                val travelPx = with(LocalDensity.current) { (trackW - knobSize).toPx() }
                                val knobFraction = settingsVm.sensitivityValue / 100f
                                Box(
                                    modifier = Modifier
                                        .width(trackW)
                                        .height(22.dp)
                                        .clickable { settingsVm.adjustSensitivity { appVm.showToast(it) } },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    // 轨道背景
                                    Box(
                                        Modifier.fillMaxWidth().height(4.dp).align(Alignment.Center)
                                            .clip(CircleShape).background(colors.line)
                                    )
                                    // 已选进度
                                    Box(
                                        Modifier.fillMaxWidth(knobFraction).height(4.dp).align(Alignment.Center)
                                            .clip(CircleShape).background(Primary)
                                    )
                                    // 旋钮：位置 = 百分比 * 可移动距离
                                    Box(
                                        Modifier
                                            .size(knobSize)
                                            .align(Alignment.CenterStart)
                                            .offset(x = with(LocalDensity.current) { (knobFraction * travelPx).toDp() })
                                            .background(Color.White, CircleShape)
                                            .border(width = 2.dp, color = Primary, shape = CircleShape)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Text("高", fontSize = 12.sp, color = colors.ink3)
                            }
                        })
                        DividerLine()
                        SettingRow("判定阈值", right = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("%.2f".format(settingsVm.thresholdValue), fontSize = 13.sp, color = colors.ink2)
                                Icon(Icons.Default.ChevronRight, null, tint = colors.ink3, modifier = Modifier.size(12.dp))
                            }
                        }) { settingsVm.cycleThreshold { appVm.showToast(it) } }
                        DividerLine()
                        SettingRow("深色模式", right = {
                            RingSwitch(
                                checked = appVm.isDarkMode,
                                onCheckedChange = { appVm.toggleDarkMode() }
                            )
                        })
                    }
                }
            }

            // Reminders
            item {
                Column {
                    Text("提醒与反馈", fontSize = 12.sp, color = colors.ink3, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    SettingsGroup {
                        SettingRow("久坐提醒", right = {
                            RingSwitch(checked = settingsVm.sedentaryReminder, onCheckedChange = { settingsVm.toggleSedentary { appVm.showToast(it) } })
                        })
                        DividerLine()
                        SettingRow("震动反馈", right = {
                            RingSwitch(checked = settingsVm.vibrationFeedback, onCheckedChange = { settingsVm.toggleVibration { appVm.showToast(it) } })
                        })
                        DividerLine()
                        SettingRow("后台持续监测", right = {
                            RingSwitch(checked = settingsVm.backgroundMonitoring, onCheckedChange = { settingsVm.toggleBackground { appVm.showToast(it) } })
                        })
                        DividerLine()
                        SettingRow("久坐提醒间隔", right = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("每 ${settingsVm.reminderInterval} 分钟", fontSize = 13.sp, color = colors.ink2)
                                Icon(Icons.Default.ChevronRight, null, tint = colors.ink3, modifier = Modifier.size(12.dp))
                            }
                        }) { settingsVm.cycleInterval { appVm.showToast(it) } }
                    }
                }
            }

            // Device & calibration
            item {
                Column {
                    Text("设备与校准", fontSize = 12.sp, color = colors.ink3, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    SettingsGroup {
                        SettingRow("传感器状态", right = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(6.dp).background(Success, CircleShape))
                                Spacer(Modifier.width(6.dp))
                                Text("已连接", fontSize = 13.sp, color = Success)
                            }
                        }) { appVm.showToast("传感器已连接 · 信号良好") }
                        DividerLine()
                        SettingRow("传感器校准", right = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (settingsVm.isCalibrated) {
                                    Text("校准完成", fontSize = 13.sp, color = Success)
                                } else {
                                    Text("建议校准", fontSize = 13.sp, color = Warn)
                                }
                                Icon(Icons.Default.ChevronRight, null,
                                    tint = if (settingsVm.isCalibrated) Success else Warn,
                                    modifier = Modifier.size(12.dp))
                            }
                        }) { onCalibrate() }
                        if (settingsVm.isCalibrated && settingsVm.lastCalibrateTime > 0) {
                            DividerLine()
                            SettingRow("上次校准", right = {
                                Text(
                                    SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(settingsVm.lastCalibrateTime)),
                                    fontSize = 12.sp, color = colors.ink3
                                )
                            })
                        }
                    }
                }
            }

            // Data management
            item {
                Column {
                    Text("数据管理", fontSize = 12.sp, color = colors.ink3, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    SettingsGroup {
                        SettingRow("导出数据", right = {
                            Icon(Icons.Default.ChevronRight, null, tint = colors.ink3, modifier = Modifier.size(12.dp))
                        }) {
                            analyzeVm.exportData(context) { ok, path ->
                                appVm.showToast(if (ok) "数据已导出，正在分享…" else "导出失败：$path")
                            }
                        }
                        DividerLine()
                        SettingRow("导入数据", right = {
                            Icon(Icons.Default.ChevronRight, null, tint = colors.ink3, modifier = Modifier.size(12.dp))
                        }) { importLauncher.launch(arrayOf("application/json", "*/*")) }
                        DividerLine()
                        SettingRow("数据存储上限", right = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${settingsVm.storageLimitDays} 天", fontSize = 13.sp, color = colors.ink2)
                                Icon(Icons.Default.ChevronRight, null, tint = colors.ink3, modifier = Modifier.size(12.dp))
                            }
                        }) { appVm.showToast("存储上限：${settingsVm.storageLimitDays} 天") }
                        DividerLine()
                        SettingRow("清空历史记录", textColor = Danger, right = {
                            Icon(Icons.Default.ChevronRight, null, tint = Danger.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        }) {
                            appVm.showConfirmDialog(
                                "清空历史记录",
                                "将删除所有已保存的检测记录，此操作不可恢复。"
                            ) {
                                analyzeVm.clearAll { appVm.showToast("历史记录已清空") }
                            }
                        }
                    }
                }
            }

            // About
            item {
                Column {
                    Text("关于", fontSize = 12.sp, color = colors.ink3, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
                    SettingsGroup {
                        SettingRow("隐私政策", right = {
                            Icon(Icons.Default.ChevronRight, null, tint = colors.ink3, modifier = Modifier.size(12.dp))
                        }) { appVm.showToast("隐私政策 · 小幽日记") }
                        DividerLine()
                        SettingRow("版本", right = {
                            Text("v1.0.0", fontSize = 13.sp, color = colors.ink3)
                        })
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }
    }
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(12.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingRow(
    label: String,
    textColor: Color? = null,
    right: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 16.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = textColor ?: colors.ink1)
        right()
    }
}
