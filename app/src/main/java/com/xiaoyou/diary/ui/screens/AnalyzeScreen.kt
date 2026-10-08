package com.xiaoyou.diary.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.min
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.AnalyzeViewModel
import com.xiaoyou.diary.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(appVm: AppViewModel, analyzeVm: AnalyzeViewModel) {
    val colors = LocalAppColors.current
    var activeTrend by remember { mutableStateOf("行走") }

    // 监听数据库 Flow：records 变化时自动重组，统计结果实时刷新（Bug 4）
    val records by analyzeVm.allRecords.collectAsState()

    // 当前选中周的周一 0 点时间戳；默认定位到今天所在的自然周（Bug 1）
    var weekMonday by remember {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            minimalDaysInFirstWeek = 4
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            clear(Calendar.MINUTE); clear(Calendar.SECOND); clear(Calendar.MILLISECOND)
        }
        mutableStateOf(cal.timeInMillis)
    }
    val weekStartMs = weekMonday
    val weekEndMs = weekMonday + TimeUnit.DAYS.toMillis(7)
    val todayStartMs = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); clear(Calendar.MINUTE); clear(Calendar.SECOND); clear(Calendar.MILLISECOND)
        }.timeInMillis
    }
    val isCurrentWeek = todayStartMs in weekStartMs until weekEndMs

    // 选中周的统计区间过滤（切换周时自动重新统计，Bug 2）
    val weekRecords = records.filter { it.startTimeMs in weekStartMs until weekEndMs }

    // 周标签：年份 + ISO 周数 + 日期范围
    val weekCal = remember(weekMonday) {
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY; minimalDaysInFirstWeek = 4
            timeInMillis = weekMonday + TimeUnit.DAYS.toMillis(3) // 用周四确定所属年份/周
        }
    }
    val yearStr = weekCal.get(Calendar.YEAR)
    val weekNum = weekCal.get(Calendar.WEEK_OF_YEAR)
    val mdFmt = SimpleDateFormat("MM.dd", Locale.getDefault())
    val rangeStr = "${mdFmt.format(Date(weekStartMs))} - ${mdFmt.format(Date(weekEndMs - 1))}"

    // 日历选择器（Bug 3）
    var showCalendar by remember { mutableStateOf(false) }

    // 从本周记录实时计算指标
    val totalSec = weekRecords.sumOf { it.durationSec }
    val totalH = totalSec / 3600
    val totalM = (totalSec % 3600) / 60
    val totalDurationStr = "%02d:%02d".format(totalH, totalM)
    val detectCount = weekRecords.map { it.sessionId }.distinct().size
    val passRate = if (totalSec > 0) ((totalSec / 3600f) / 8f * 100).coerceAtMost(100f) else 0f
    val behaviorDurations = weekRecords.groupBy { it.behaviorName }
        .mapValues { (_, list) -> list.sumOf { it.durationSec } }
    val primaryName = behaviorDurations.maxByOrNull { it.value }?.key ?: BehaviorType.WALK.name
    val primaryBehavior = try { BehaviorType.valueOf(primaryName) } catch (_: Exception) { BehaviorType.WALK }
    val primaryBehaviorLabel = primaryBehavior.label

    // 环形图数据：从本周记录实时构建
    val donutData: List<Pair<Color, Float>> = if (behaviorDurations.isNotEmpty()) {
        val total = behaviorDurations.values.sum().coerceAtLeast(1)
        behaviorDurations.entries.sortedByDescending { it.value }.take(8).map { (name, dur) ->
            val type = try { BehaviorType.valueOf(name) } catch (_: Exception) { BehaviorType.WALK }
            type.color to (dur.toFloat() / total * 100f)
        }
    } else {
        listOf(
            BehaviorWalk to 38f, BehaviorSit to 27f, BehaviorStand to 15f,
            BehaviorJog to 9f, BehaviorRun to 5f, BehaviorTilt to 3f,
            BehaviorLie to 2f, BehaviorSideLie to 1f
        )
    }
    val primaryPct = donutData.firstOrNull()?.second ?: 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("数据分析", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { appVm.showToast("数据已导出到剪贴板") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = colors.navIcon, modifier = Modifier.size(17.dp))
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Week selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("‹", fontSize = 20.sp, color = if (weekMonday <= todayStartMs - TimeUnit.DAYS.toMillis(365)) colors.ink3.copy(alpha = 0.3f) else colors.ink3,
                        modifier = Modifier.clickable {
                            if (weekMonday > todayStartMs - TimeUnit.DAYS.toMillis(365)) {
                                weekMonday -= TimeUnit.DAYS.toMillis(7)
                            }
                        }.padding(horizontal = 12.dp, vertical = 6.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { showCalendar = true }
                    ) {
                        Text("$yearStr 年 · 第 $weekNum 周", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rangeStr, fontSize = 11.sp, color = colors.ink3)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.CalendarMonth, null, tint = if (isCurrentWeek) Primary else colors.ink3, modifier = Modifier.size(11.dp))
                            if (isCurrentWeek) {
                                Spacer(Modifier.width(3.dp))
                                Text("本周", fontSize = 10.sp, color = Primary)
                            }
                        }
                    }
                    Text("›", fontSize = 20.sp, color = if (weekMonday >= todayStartMs) colors.ink3.copy(alpha = 0.3f) else colors.ink3,
                        modifier = Modifier.clickable {
                            if (weekMonday < todayStartMs) {
                                weekMonday += TimeUnit.DAYS.toMillis(7)
                            }
                        }.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            // Summary 4 columns
            item {
                AppCard {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                        SummaryCell("总时长", if (totalSec > 0) totalDurationStr else "--:--")
                        VDivider()
                        SummaryCell("检测次数", "$detectCount")
                        VDivider()
                        SummaryCellColored("主要行为", primaryBehaviorLabel, Success)
                        VDivider()
                        SummaryCell("达标率", "%.1f%%".format(passRate))
                    }
                }
            }

            // Behavior structure - donut chart
            item {
                AppCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("行为结构", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(124.dp)) {
                                DonutChart(donutData)
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text("主要行为", fontSize = 10.sp, color = colors.ink3)
                                    Text(primaryBehaviorLabel, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
                                    Text("%.1f%%".format(primaryPct), fontSize = 11.sp, color = colors.ink2)
                                }
                            }
                            Spacer(Modifier.width(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val items = if (behaviorDurations.isNotEmpty()) {
                                    val total = behaviorDurations.values.sum().coerceAtLeast(1)
                                    behaviorDurations.entries.sortedByDescending { it.value }.take(8).map { (name, dur) ->
                                        val type = try { BehaviorType.valueOf(name) } catch (_: Exception) { BehaviorType.WALK }
                                        type to "%.1f%%".format(dur.toFloat() / total * 100f)
                                    }
                                } else {
                                    listOf(
                                        BehaviorType.WALK to "38.0%", BehaviorType.SIT to "27.0%",
                                        BehaviorType.STAND to "15.0%", BehaviorType.JOG to "9.0%",
                                        BehaviorType.RUN to "5.0%", BehaviorType.TILT to "3.0%",
                                        BehaviorType.LIE to "2.0%", BehaviorType.SIDE_LIE to "1.0%"
                                    )
                                }
                                items.chunked(2).forEach { rowItems ->
                                    Row {
                                        rowItems.forEachIndexed { idx, (type, pct) ->
                                            Row(
                                                modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(8.dp).background(type.color, RoundedCornerShape(2.dp)))
                                                Spacer(Modifier.width(6.dp))
                                                Text(type.label, fontSize = 11.sp, color = colors.ink2, modifier = Modifier.weight(1f))
                                                Text(pct, fontSize = 11.sp, color = colors.ink1)
                                            }
                                            if (idx == 0 && rowItems.size > 1) Spacer(Modifier.width(12.dp))
                                        }
                                        // Fill empty space for single-item rows
                                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 24h bar chart
            item {
                AppCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("24 小时分布", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                            Text("单位：分钟", fontSize = 11.sp, color = colors.ink3)
                        }
                        Spacer(Modifier.height(12.dp))
                        BarChart24h()
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("00", fontSize = 10.sp, color = colors.ink3)
                            Text("06", fontSize = 10.sp, color = colors.ink3)
                            Text("12", fontSize = 10.sp, color = colors.ink3)
                            Text("18", fontSize = 10.sp, color = colors.ink3)
                            Text("24", fontSize = 10.sp, color = colors.ink3)
                        }
                    }
                }
            }

            // Weekly trend line chart
            item {
                AppCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("周趋势", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            TrendChip("行走", activeTrend == "行走", Success) { activeTrend = "行走" }
                            Spacer(Modifier.width(8.dp))
                            TrendChip("静坐", activeTrend == "静坐", colors.soft) { activeTrend = "静坐" }
                            Spacer(Modifier.width(8.dp))
                            TrendChip("站立", activeTrend == "站立", colors.soft) { activeTrend = "站立" }
                        }
                        Spacer(Modifier.height(8.dp))
                        val lineColor = when (activeTrend) {
                            "行走" -> Success
                            "静坐" -> BehaviorSit
                            else -> BehaviorStand
                        }
                        TrendLineChart(lineColor)
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            listOf("一","二","三","四","五","六","日").forEach { Text(it, fontSize = 10.sp, color = colors.ink3) }
                        }
                    }
                }
            }

            // Health analysis
            item {
                AppCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("健康分析与周总结", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                        Spacer(Modifier.height(12.dp))
                        HealthRow(Success, "🚶", "运动量达标",
                            "本周累计行走 6:12，日均步态活动达标 5 天，优于上周。")
                        Spacer(Modifier.height(14.dp))
                        HealthRow(Warn, "⏰", "久坐风险提示",
                            "周四连续静坐 3:20 未中断，建议每 60 分钟起身活动 3 分钟。")
                        Spacer(Modifier.height(14.dp))
                        HealthRow(Primary, "📈", "行为切换频次",
                            "日均行为切换 42 次，活动节律正常，姿态分布均衡。")
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
        } // end inner Column

        // 日历选择弹层（Bug 3）：点击日期跳转到该日期所属周
        if (showCalendar) {
            val datePickerState = rememberDatePickerState()
            // 遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80101828))
                    .clickable { showCalendar = false }
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.sheetBg)
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("选择日期", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showCalendar = false }
                            .padding(6.dp)
                    ) { Text("取消", fontSize = 12.sp, color = colors.ink3) }
                }
                DatePicker(
                    state = datePickerState,
                    title = null,
                    headline = null,
                    colors = DatePickerDefaults.colors(
                        todayDateBorderColor = Primary,
                        selectedDayContainerColor = Primary,
                        selectedDayContentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Primary)
                            .clickable {
                                datePickerState.selectedDateMillis?.let { ms ->
                                    val cal = Calendar.getInstance().apply {
                                        firstDayOfWeek = Calendar.MONDAY; minimalDaysInFirstWeek = 4
                                        timeInMillis = ms
                                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                                        set(Calendar.HOUR_OF_DAY, 0); clear(Calendar.MINUTE); clear(Calendar.SECOND); clear(Calendar.MILLISECOND)
                                    }
                                    weekMonday = cal.timeInMillis
                                    showCalendar = false
                                    appVm.showToast("已跳转到所选日期所在周")
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 7.dp)
                    ) {
                        Text("确定", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SummaryCell(label: String, value: String) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = colors.ink3)
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
    }
}

@Composable
private fun RowScope.SummaryCellColored(label: String, value: String, valueColor: Color) {
    val colors = LocalAppColors.current
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 11.sp, color = colors.ink3)
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}

@Composable
private fun RowScope.VDivider() {
    val colors = LocalAppColors.current
    Box(Modifier.width(1.dp).height(30.dp).background(colors.line))
}

@Composable
private fun DonutChart(segments: List<Pair<Color, Float>>) {
    val trackColor = Color(0xFFF0F2F5)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 16.dp.toPx()
        val diameter = min(size.width, size.height) - strokeWidth
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
        val arcSize = Size(diameter, diameter)
        // Background track
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth)
        )
        // Segments (rotate -90 to start at top)
        var start = -90f
        segments.forEach { (color, pct) ->
            val sweep = pct / 100f * 360f
            drawArc(
                color = color,
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
            start += sweep
        }
    }
}

@Composable
private fun BarChart24h() {
    val data = listOf(
        10f to BehaviorLie, 8f to BehaviorLie, 6f to BehaviorLie, 5f to BehaviorLie,
        6f to BehaviorLie, 8f to BehaviorLie, 25f to BehaviorSit, 45f to BehaviorWalk,
        62f to BehaviorWalk, 78f to BehaviorSit, 85f to BehaviorSit, 72f to BehaviorSit,
        60f to BehaviorWalk, 55f to BehaviorWalk, 68f to BehaviorSit, 80f to BehaviorSit,
        88f to BehaviorSit, 76f to BehaviorSit, 70f to BehaviorWalk, 82f to BehaviorWalk,
        90f to BehaviorWalk, 75f to BehaviorSit, 45f to BehaviorLie, 25f to BehaviorLie
    )
    Row(
        modifier = Modifier.fillMaxWidth().height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { (heightPct, color) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightPct / 100f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun TrendChip(label: String, active: Boolean, activeColor: Color, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val bg = if (active) activeColor else colors.soft
    val textColor = if (active) Color.White else colors.ink2
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(label, fontSize = 11.sp, color = textColor)
    }
}

@Composable
private fun TrendLineChart(lineColor: Color) {
    val points = listOf(
        0.50f, 0.64f, 0.43f, 0.75f, 0.59f, 0.80f, 0.55f
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(108.dp)) {
        val w = size.width
        val h = size.height
        val n = points.size
        val stepX = w / (n - 1)
        val pts = points.mapIndexed { i, p ->
            Offset(i * stepX, h - p * h * 0.8f - h * 0.1f)
        }
        // Fill area
        val fillPath = Path().apply {
            moveTo(pts[0].x, h)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, h)
            close()
        }
        drawPath(fillPath, color = lineColor.copy(alpha = 0.22f))
        // Line
        val linePath = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            linePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
        // Points
        pts.forEachIndexed { i, p ->
            val r = if (i == 5) 4.5f else 3f
            drawCircle(Color.White, radius = r + 1f, center = p)
            drawCircle(lineColor, radius = r, center = p, style = Stroke(width = 2f))
        }
    }
}

@Composable
private fun HealthRow(iconBgColor: Color, emoji: String, title: String, desc: String) {
    val colors = LocalAppColors.current
    Row {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(iconBgColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 12.sp, color = colors.ink2, lineHeight = 18.sp)
        }
    }
}
