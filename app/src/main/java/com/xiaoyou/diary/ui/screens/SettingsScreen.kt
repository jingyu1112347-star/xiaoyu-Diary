package com.xiaoyou.diary.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.viewmodel.AnalyzeViewModel
import com.xiaoyou.diary.viewmodel.AppViewModel
import com.xiaoyou.diary.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    appVm: AppViewModel,
    settingsVm: SettingsViewModel,
    analyzeVm: AnalyzeViewModel,
    onCalibrate: () -> Unit,
    onBack: () -> Unit
) {
    val c = LocalAppColors.current
    val context = LocalContext.current

    // 导入文件选择器（业务保留）
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

    Column(modifier = Modifier.fillMaxSize().background(c.bg)) {
        // ---------- 导航栏：左返回 · 居中标题 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "返回", onClick = onBack)
            Text(
                "设置",
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = c.ink,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.size(44.dp))
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 30.dp)
        ) {
            // ---------- 检测参数 ----------
            item {
                GroupHeader("检测参数", accent = true, modifier = Modifier.padding(bottom = 11.dp))
                AppSurface {
                    // 采样率 10~50Hz、5Hz 步进：受 Android 12+ 采样率权限与耗电限制，
                    // SensorCollector 实际最高约 50Hz，滑条范围需与实际能力一致
                    val srGrid = ((settingsVm.sampleRate / 5) * 5).coerceIn(10, 50).toFloat()
                    SliderRow(
                        title = "采样率（Hz）",
                        valueText = "${settingsVm.sampleRate.coerceIn(10, 50)}",
                        value = srGrid,
                        range = 10f..50f,
                        steps = 7 // 5Hz 步进：10..50 共 9 档 → steps = 7
                    ) { settingsVm.setSampleRateDirect(((it.roundToInt() / 5) * 5).coerceIn(10, 50)) }
                    Hairline()
                    SliderRow(
                        title = "识别阈值",
                        valueText = "%.2f".format(settingsVm.thresholdValue),
                        value = settingsVm.thresholdValue,
                        range = 0.30f..0.95f,
                        steps = 12 // 0.05 步进
                    ) { settingsVm.setThresholdDirect(it) }
                    Hairline()
                    val sensSteps = settingsVm.sensitivitySteps
                    val sensIndex = sensSteps.indexOf(settingsVm.sensitivityValue).coerceAtLeast(0)
                    SliderRow(
                        title = "检测灵敏度",
                        valueText = settingsVm.sensitivityLabel(),
                        value = sensIndex.toFloat(),
                        range = 0f..(sensSteps.lastIndex).toFloat(),
                        steps = sensSteps.size - 2
                    ) { settingsVm.setSensitivityDirect(sensSteps[it.roundToInt()]) }
                    Hairline()
                    SliderRow(
                        title = "久坐提醒间隔",
                        valueText = "${settingsVm.reminderInterval} 分钟",
                        value = settingsVm.reminderInterval.toFloat(),
                        range = 15f..120f,
                        steps = 6 // 15 分钟步进
                    ) { settingsVm.setIntervalDirect(it.roundToInt()) }
                }
            }

            // ---------- 提醒与运行 ----------
            item {
                GroupHeader("提醒与运行", accent = true, modifier = Modifier.padding(top = 26.dp, bottom = 11.dp))
                AppSurface {
                    ToggleRow(
                        icon = Icons.Filled.Notifications, soft = c.yellowSoft, tone = c.yellow,
                        title = "久坐提醒", sub = "每 ${settingsVm.reminderInterval} 分钟提醒起身活动",
                        checked = settingsVm.sedentaryReminder
                    ) { settingsVm.toggleSedentary { appVm.showToast(it) } }
                    Hairline()
                    ToggleRow(
                        icon = Icons.Filled.Vibration, soft = c.purpleSoft, tone = c.purple,
                        title = "震动反馈", sub = "状态变更与提醒时震动",
                        checked = settingsVm.vibrationFeedback
                    ) { settingsVm.toggleVibration { appVm.showToast(it) } }
                    Hairline()
                    ToggleRow(
                        icon = Icons.Filled.Apps, soft = c.blueSoft, tone = c.blue,
                        title = "后台持续监测", sub = "即将支持：切到后台后继续采样（当前仅前台检测）",
                        checked = settingsVm.backgroundMonitoring
                    ) { settingsVm.toggleBackground { appVm.showToast(it) } }
                    Hairline()
                    ToggleRow(
                        icon = Icons.Filled.DarkMode, soft = c.surface2, tone = c.ink2,
                        title = "深色模式", sub = "深色优先，可手动切换浅色",
                        checked = appVm.isDarkMode
                    ) { appVm.toggleDarkMode() }
                }
            }

            // ---------- 其他 ----------
            item {
                GroupHeader("其他", modifier = Modifier.padding(top = 26.dp, bottom = 11.dp))
                AppSurface {
                    NavRow(
                        icon = Icons.Filled.GpsFixed, soft = c.greenSoft, tone = c.green,
                        title = "传感器校准",
                        sub = if (settingsVm.isCalibrated && settingsVm.lastCalibrateTime > 0)
                            "上次校准 " + SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(settingsVm.lastCalibrateTime))
                        else "建议每周校准一次",
                        value = "去校准",
                        onClick = onCalibrate
                    )
                    Hairline()
                    NavRow(
                        icon = Icons.Filled.Shield, soft = c.surface2, tone = c.ink2,
                        title = "隐私与数据",
                        sub = "全部本地运算 · 不上传数据",
                        onClick = { appVm.showToast("小幽日记 · 数据仅保存在本机", ToastType.INFO) }
                    )
                    Hairline()
                    NavRow(
                        icon = Icons.Filled.FileUpload, soft = c.blueSoft, tone = c.blue,
                        title = "导出数据",
                        sub = "导出 JSON 并通过系统分享",
                        onClick = {
                            analyzeVm.exportData(context) { ok, path ->
                                appVm.showToast(if (ok) "数据已导出，正在分享…" else "导出失败：$path")
                            }
                        }
                    )
                    Hairline()
                    NavRow(
                        icon = Icons.Filled.FileDownload, soft = c.blueSoft, tone = c.blue,
                        title = "导入数据",
                        sub = "从 JSON 文件恢复检测记录",
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
                    )
                }
            }

            // ---------- 关于：版本信息与本版更新内容 ----------
            item {
                GroupHeader("关于", modifier = Modifier.padding(top = 26.dp, bottom = 11.dp))
                AppSurface {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingIcon(Icons.Filled.NewReleases, c.blueSoft, c.blue)
                        Spacer(Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("小幽日记 v1.1.0", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink)
                            Text("2026年10月15日 · 最新版本", fontSize = 11.5.sp, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
                        }
                        Text("最新", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = c.blue)
                    }
                    Hairline()
                    // 本版更新内容（与仓库 CHANGELOG.md 保持一致）
                    Column(modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 14.dp)) {
                        listOf(
                            "全新液态玻璃底部导航：真实背景折射，滑动实时跟手",
                            "整体视觉焕新：深色优先主题，卡片、按钮与动效统一重做",
                            "概览页新增实时检测波形、姿态角与 8 类行为概率",
                            "分析页支持行为占比、周趋势与当日统计",
                            "历史记录支持筛选、批量管理与会话导出",
                            "全部数据本地运算，绝不上传"
                        ).forEach { note ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text("·", fontSize = 12.5.sp, color = c.blue)
                                Spacer(Modifier.width(8.dp))
                                Text(note, fontSize = 12.5.sp, color = c.ink2)
                            }
                        }
                    }
                }
            }

            // ---------- 危险操作 ----------
            item {
                GroupHeader("危险操作", modifier = Modifier.padding(top = 26.dp, bottom = 11.dp))
                AppSurface {
                    DangerRow(
                        icon = Icons.Filled.DeleteOutline,
                        title = "清除全部记录",
                        sub = "不可恢复，请谨慎操作"
                    ) {
                        appVm.showConfirmDialog("清除全部记录", "将删除所有已保存的检测记录，此操作不可恢复。") {
                            analyzeVm.clearAll { appVm.showToast("全部记录已清除") }
                        }
                    }
                }
                Text(
                    "小幽日记 v1.1.0 · 本地离线识别",
                    fontSize = 11.sp, color = c.ink3,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onCommit: (Float) -> Unit
) {
    val c = LocalAppColors.current
    // 拖动中使用本地临时值，停止拖动再提交存储，避免高频写偏好
    var dragValue by remember { mutableStateOf<Float?>(null) }
    Column(modifier = Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, fontSize = 12.5.sp, color = c.ink2)
            Text(valueText, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = c.ink)
        }
        Slider(
            value = dragValue ?: value,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onCommit(it) }
                dragValue = null
            },
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = c.blue,
                inactiveTrackColor = c.surface2,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    soft: Color,
    tone: Color,
    title: String,
    sub: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon, soft, tone)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = c.ink)
            Text(sub, fontSize = 11.5.sp, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
        }
        RingSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    soft: Color,
    tone: Color,
    title: String,
    sub: String,
    value: String? = null,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick = onClick)
            .padding(horizontal = 18.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon, soft, tone)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = c.ink)
            Text(sub, fontSize = 11.5.sp, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
        }
        Text(value ?: "", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = c.blue)
        if (value != null) Spacer(Modifier.width(4.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            tint = c.ink3, modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun DangerRow(
    icon: ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit
) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick = onClick)
            .padding(horizontal = 18.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingIcon(icon, c.redSoft, c.red)
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = c.red)
            Text(sub, fontSize = 11.5.sp, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector, soft: Color, tone: Color) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .background(soft, RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tone, modifier = Modifier.size(16.dp))
    }
}
