package com.xiaoyou.diary.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.viewmodel.AnalyzeViewModel
import com.xiaoyou.diary.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzeScreen(appVm: AppViewModel, analyzeVm: AnalyzeViewModel) {
    val c = LocalAppColors.current
    val records by analyzeVm.allRecords.collectAsState()

    // 日 / 周 分段
    var rangeMode by remember { mutableStateOf(1) } // 0=日 1=周

    // 锚点：日模式为选中日 0 点；周模式为选中周的周一 0 点（ISO，周一、minimalDays=4）
    fun mondayOf(ms: Long): Long = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
        timeInMillis = ms
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        clear(Calendar.MINUTE); clear(Calendar.SECOND); clear(Calendar.MILLISECOND)
    }.timeInMillis

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); clear(Calendar.MINUTE); clear(Calendar.SECOND); clear(Calendar.MILLISECOND)
        }.timeInMillis
    }
    var anchorDay by remember { mutableStateOf(todayStart) }
    var weekMonday by remember { mutableStateOf(mondayOf(todayStart)) }

    val rangeStart = if (rangeMode == 0) anchorDay else weekMonday
    val rangeSpan = if (rangeMode == 0) TimeUnit.DAYS.toMillis(1) else TimeUnit.DAYS.toMillis(7)
    val rangeEnd = rangeStart + rangeSpan
    val isCurrentRange = todayStart in rangeStart until rangeEnd

    val rangeRecords = records.filter { it.startTimeMs in rangeStart until rangeEnd }

    // 周标题（年份 + ISO 周数 + 周一至周日）
    val weekCal = remember(weekMonday) {
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY; minimalDaysInFirstWeek = 4
            timeInMillis = weekMonday + TimeUnit.DAYS.toMillis(3)
        }
    }
    val mdFmt = SimpleDateFormat("MM.dd E", Locale.CHINESE)
    val mdyFmt = SimpleDateFormat("M月d日 E", Locale.CHINESE)
    val centerTitle = if (rangeMode == 0) mdyFmt.format(Date(anchorDay))
    else "${weekCal.get(Calendar.YEAR)} · 第 ${weekCal.get(Calendar.WEEK_OF_YEAR)} 周"
    val centerSub = if (rangeMode == 0) "当日统计"
    else "${mdFmt.format(Date(rangeStart))} – ${mdFmt.format(Date(rangeEnd - 1))}"

    var showCalendar by remember { mutableStateOf(false) }

    // ---------- 统计指标（实时订阅 Flow） ----------
    val totalSec = rangeRecords.sumOf { it.durationSec }
    val totalH = totalSec / 3600
    val totalM = (totalSec % 3600) / 60
    val detectCount = rangeRecords.map { it.sessionId }.distinct().size
    val passRate = if (totalSec > 0) ((totalSec / 3600f) / 8f * 100).coerceAtMost(100f) else 0f
    val behaviorDurations = rangeRecords.groupBy { it.behaviorName }
        .mapValues { (_, list) -> list.sumOf { it.durationSec } }
    val primaryType = behaviorDurations.maxByOrNull { it.value }?.key
        ?.let { runCatching { BehaviorType.valueOf(it) }.getOrNull() }
        ?: BehaviorType.WALK

    // 环形图分段
    val donutSegments: List<Pair<BehaviorType, Float>> = if (behaviorDurations.isNotEmpty()) {
        val total = behaviorDurations.values.sum().coerceAtLeast(1)
        behaviorDurations.entries.sortedByDescending { it.value }
            .map { (name, dur) ->
                (runCatching { BehaviorType.valueOf(name) }.getOrNull() ?: BehaviorType.WALK) to
                    (dur.toFloat() / total * 100f)
            }
    } else emptyList()

    // 24 小时分布（区间内按时段汇总）。先按秒累加，最后四舍五入到分钟，
    // 避免不足 1 分钟的尾段（如停止时补录的 1~29 秒）被整除直接清零。
    val hourlyMinutes = remember(rangeRecords) {
        val secArr = IntArray(24)
        val cal = Calendar.getInstance()
        rangeRecords.forEach { r ->
            cal.timeInMillis = r.startTimeMs
            secArr[cal.get(Calendar.HOUR_OF_DAY)] += r.durationSec
        }
        IntArray(24) { (secArr[it] + 30) / 60 }
    }

    // 周趋势：区间所在 7 天（或日模式下最近 7 个锚点日）的活动分钟。
    // 单次遍历归桶，不再对每一天做一次全量 filter。
    val trendMinutes = remember(records, rangeRecords, rangeStart, anchorDay, rangeMode) {
        val dayMs = TimeUnit.DAYS.toMillis(1)
        val base = if (rangeMode == 1) rangeStart else anchorDay - 6 * dayMs
        val source = if (rangeMode == 1) rangeRecords else records
        val secArr = IntArray(7)
        val cal = Calendar.getInstance()
        source.forEach { r ->
            cal.timeInMillis = r.startTimeMs
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.clear(Calendar.MINUTE); cal.clear(Calendar.SECOND); cal.clear(Calendar.MILLISECOND)
            val idx = ((cal.timeInMillis - base) / dayMs).toInt()
            if (idx in 0..6) secArr[idx] += r.durationSec
        }
        IntArray(7) { (secArr[it] + 30) / 60 }
    }

    Box(modifier = Modifier.fillMaxSize().background(c.bg)) {
     Column(modifier = Modifier.fillMaxSize()) {
        // ---------- 导航栏 ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("数据分析", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = c.ink, letterSpacing = (-0.5f).sp)
                Text("行为统计可视化", fontSize = 12.5.sp, color = c.ink3)
            }
            val context = androidx.compose.ui.platform.LocalContext.current
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillIconButton(Icons.Filled.CalendarMonth, "选择日期", onClick = { showCalendar = true })
                PillIconButton(Icons.AutoMirrored.Filled.OpenInNew, "导出", onClick = {
                    // 接线到真实导出：生成 JSON 文件并调起系统分享
                    analyzeVm.exportData(context) { ok, _ ->
                        appVm.showToast(
                            if (ok) "数据已导出，正在打开分享" else "导出失败，请重试",
                            if (ok) ToastType.OK else ToastType.ERROR
                        )
                    }
                })
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            // 内容不足一屏时严格垂直居中；超出时自然顶部起滚
            verticalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 130.dp)
        ) {
            // ---------- 日 / 周 分段控件 ----------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.surface2)
                        .padding(3.dp)
                ) {
                    listOf("日", "周").forEachIndexed { i, label ->
                        val on = rangeMode == i
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (on) c.elevated else Color.Transparent)
                                .tapScale { rangeMode = i },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (on) c.ink else c.ink3
                            )
                        }
                    }
                }
            }

            // ---------- 区间导航 ----------
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val canBack = rangeStart > todayStart - TimeUnit.DAYS.toMillis(365)
                    CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上一${if (rangeMode == 0) "天" else "周"}",
                        enabled = canBack,
                        onClick = {
                            if (rangeMode == 0) anchorDay -= TimeUnit.DAYS.toMillis(1)
                            else weekMonday -= TimeUnit.DAYS.toMillis(7)
                        })
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(centerTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = c.ink)
                            if (isCurrentRange) {
                                Spacer(Modifier.width(7.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(c.greenSoft)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                ) {
                                    Text(if (rangeMode == 0) "今天" else "本周",
                                        fontSize = 10.sp, fontWeight = FontWeight.Bold, color = c.green)
                                }
                            }
                        }
                        Text(centerSub, fontSize = 11.5.sp, color = c.ink3, modifier = Modifier.padding(top = 3.dp))
                    }
                    CircleButton(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下一${if (rangeMode == 0) "天" else "周"}",
                        enabled = !isCurrentRange,
                        onClick = {
                            if (isCurrentRange) return@CircleButton
                            if (rangeMode == 0) anchorDay += TimeUnit.DAYS.toMillis(1)
                            else weekMonday += TimeUnit.DAYS.toMillis(7)
                        })
                }
            }

            // ---------- 2×2 统计格 ----------
            item {
                AppSurface(modifier = Modifier.padding(top = 10.dp, bottom = 26.dp)) {
                    Column {
                        Row {
                            StatCell("总时长", if (totalSec > 0) "${totalH}h ${totalM}m" else "--",
                                tone = c.yellow)
                            Box(Modifier.width(1.dp).height(72.dp).background(c.rule))
                            StatCell("检测次数", "$detectCount", suffix = "次", tone = c.blue)
                        }
                        Box(Modifier.fillMaxWidth().height(1.dp).background(c.rule))
                        Row {
                            StatCell("主要行为", primaryType.label, tone = c.purple, big = false)
                            Box(Modifier.width(1.dp).height(72.dp).background(c.rule))
                            StatCell("达标率", "%.0f".format(passRate), suffix = "%", tone = c.green)
                        }
                    }
                }
            }

            // ---------- 行为占比 ----------
            item {
                GroupHeader("行为占比", accent = true, modifier = Modifier.padding(bottom = 11.dp),
                    trailing = { Text(if (rangeMode == 1) "本周" else "当日", fontSize = 12.sp, color = c.ink3) })
                AppSurface(modifier = Modifier.padding(bottom = 26.dp)) {
                    if (donutSegments.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 34.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("暂无行为数据", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
                            Text(
                                "完成一次检测后生成占比分析",
                                fontSize = 12.sp, color = c.ink3,
                                modifier = Modifier.padding(top = 5.dp)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DonutChart(donutSegments, "%.0f%%".format(passRate))
                            Spacer(Modifier.width(20.dp))
                            DonutLegend(donutSegments, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // ---------- 24 小时分布 ----------
            item {
                GroupHeader("24 小时分布", modifier = Modifier.padding(bottom = 11.dp),
                    trailing = { Text("← 横向滑动 →", fontSize = 12.sp, color = c.ink3) })
                AppSurface(modifier = Modifier.padding(bottom = 26.dp)) {
                    val maxMin = (hourlyMinutes.maxOrNull() ?: 0).coerceAtLeast(1)
                    val peakHour = hourlyMinutes.indexOf(hourlyMinutes.maxOrNull() ?: 0)
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.width(560.dp).height(120.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            hourlyMinutes.forEachIndexed { h, mins ->
                                Column(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    verticalArrangement = Arrangement.Bottom,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    if (h % 6 == 0) Text("$h", fontSize = 9.sp, color = c.ink3)
                                    Spacer(Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(if (mins > 0) (20 + 80f * mins / maxMin).dp else 3.dp)
                                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomEnd = 2.dp, bottomStart = 2.dp))
                                            .background(if (h == peakHour && mins > 0) c.yellow else c.blue.copy(alpha = 0.85f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ---------- 周趋势 ----------
            item {
                GroupHeader("周趋势 · 活动时长", accent = true, modifier = Modifier.padding(bottom = 11.dp),
                    trailing = { Text("分钟/天", fontSize = 12.sp, color = c.ink3) })
                AppSurface(modifier = Modifier.padding(bottom = 8.dp)) {
                    TrendChart(trendMinutes, modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 22.dp, end = 22.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val labels = if (rangeMode == 1) listOf("一", "二", "三", "四", "五", "六", "日")
                        else listOf("6天前", "5天前", "4天前", "3天前", "前天", "昨天", "今天")
                        labels.forEach { Text(it, fontSize = 10.5.sp, color = c.ink3) }
                    }
                }
            }
        }
     } // end inner Column

    // ---------- 日期选择 ----------
    if (showCalendar) {
        // 禁止选择未来日期（今天结束之前的时间戳均可选）
        val todayEndMs = todayStart + TimeUnit.DAYS.toMillis(1) - 1
        val dpState = rememberDatePickerState(
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= todayEndMs
            }
        )
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(c.scrim)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { showCalendar = false }
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(22.dp))
                .background(c.elevated)
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("选择日期", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
                Box(modifier = Modifier.tapScale { showCalendar = false }.padding(6.dp)) {
                    Text("取消", fontSize = 12.sp, color = c.ink3)
                }
            }
            DatePicker(
                state = dpState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = c.elevated,
                    todayDateBorderColor = c.blue,
                    selectedDayContainerColor = c.blue,
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.blue)
                        .tapScale {
                            dpState.selectedDateMillis?.let { ms ->
                                if (rangeMode == 0) {
                                    anchorDay = Calendar.getInstance().apply {
                                        timeInMillis = ms
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        clear(Calendar.MINUTE); clear(Calendar.SECOND); clear(Calendar.MILLISECOND)
                                    }.timeInMillis
                                } else weekMonday = mondayOf(ms)
                                showCalendar = false
                                appVm.showToast("已跳转到所选日期所在${if (rangeMode == 0) "日" else "周"}")
                            }
                        }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text("确定", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
            }
        }
        } // end centered overlay Box
    }
    } // end outer Box
}

@Composable
private fun RowScope.StatCell(
    label: String,
    value: String,
    suffix: String? = null,
    tone: Color,
    big: Boolean = true
) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier.weight(1f).padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Text(label, fontSize = 11.5.sp, color = c.ink3)
        Spacer(Modifier.height(7.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                fontSize = if (big) 23.sp else 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = tone,
                letterSpacing = (-0.4f).sp
            )
            if (suffix != null) {
                Text(suffix, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.ink3,
                    modifier = Modifier.padding(start = 5.dp, bottom = 2.dp))
            }
        }
    }
}

@Composable
private fun DonutChart(segments: List<Pair<BehaviorType, Float>>, centerText: String) {
    val c = LocalAppColors.current
    Box(modifier = Modifier.size(132.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 15.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawCircle(
                color = c.surface2,
                radius = diameter / 2f,
                center = Offset(size.width / 2f, size.height / 2f),
                style = Stroke(width = strokeWidth)
            )
            var start = -90f
            segments.take(8).forEach { (type, pct) ->
                val sweep = pct / 100f * 360f
                drawArc(
                    color = c.behavior(type),
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
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(centerText, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = c.green)
            Text("活动达标率", fontSize = 10.5.sp, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun DonutLegend(segments: List<Pair<BehaviorType, Float>>, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    // 前 4 类 + 其他
    val top = segments.take(4)
    val otherPct = segments.drop(4).sumOf { it.second.toDouble() }.toFloat()
    val rows = top.toMutableList()
    if (otherPct > 0.5f) rows.add(BehaviorType.SIT to otherPct) // 仅占位，颜色单独处理
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        rows.forEachIndexed { idx, (type, pct) ->
            val isOther = idx >= 4
            val dotColor = if (isOther) c.ink3 else c.behavior(type)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(3.dp)).background(dotColor))
                Spacer(Modifier.width(8.dp))
                Text(if (isOther) "其他" else type.label, fontSize = 12.5.sp, color = c.ink2)
                Text(
                    "%.0f%%".format(pct),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.ink,
                    modifier = Modifier.weight(1f).padding(end = 2.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun TrendChart(minutes: IntArray, modifier: Modifier = Modifier) {
    val c = LocalAppColors.current
    val maxV = (minutes.maxOrNull() ?: 0).coerceAtLeast(1)
    val peakIdx = minutes.indexOf(minutes.maxOrNull() ?: 0)
    Canvas(modifier = modifier.fillMaxWidth().height(150.dp)) {
        val w = size.width
        val h = size.height
        val baseY = h - 30f
        val topY = 12f
        val n = minutes.size
        val stepX = w / (n - 1)
        fun px(i: Int) = if (n == 1) w / 2f else i * stepX
        fun py(v: Int) = baseY - (v.toFloat() / maxV) * (baseY - topY)
        val pts = minutes.indices.map { Offset(px(it), py(minutes[it])) }

        // 底线
        drawLine(c.rule, Offset(0f, baseY), Offset(w, baseY), strokeWidth = 1.dp.toPx())

        // 渐变填充
        val fill = Path().apply {
            moveTo(pts.first().x, baseY)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(pts.last().x, baseY)
            close()
        }
        drawPath(
            fill,
            brush = Brush.verticalGradient(
                colors = listOf(c.blue.copy(alpha = 0.35f), c.blue.copy(alpha = 0f)),
                startY = topY, endY = baseY
            )
        )
        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(line, color = c.blue, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        pts.forEachIndexed { i, p ->
            val peak = i == peakIdx && minutes[i] > 0
            drawCircle(if (peak) c.yellow else c.blue, radius = if (peak) 4.5.dp.toPx() else 3.5.dp.toPx(), center = p)
        }
    }
}
