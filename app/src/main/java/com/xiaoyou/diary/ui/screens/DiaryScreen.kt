package com.xiaoyou.diary.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.AnalyzeViewModel
import com.xiaoyou.diary.viewmodel.AppViewModel
import com.xiaoyou.diary.viewmodel.DiaryViewModel

@Composable
fun DiaryScreen(appVm: AppViewModel, diaryVm: DiaryViewModel, analyzeVm: AnalyzeViewModel) {
    val colors = LocalAppColors.current
    val records by analyzeVm.allRecords.collectAsState()
    val entries by diaryVm.entries.collectAsState()
    var showMoodPicker by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<String?>(null) }

    // 今日行为数据：从记录中统计
    val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    val todayRecords = records.filter { it.date == today }
    val behaviorDurations = todayRecords.groupBy { it.behaviorName }
        .mapValues { (_, list) -> list.sumOf { it.durationSec } }
    val topBehaviors = behaviorDurations.entries
        .sortedByDescending { it.value }
        .take(3)
        .map { (name, sec) ->
            val type = try { BehaviorType.valueOf(name) } catch (_: Exception) { BehaviorType.SIT }
            val m = sec / 60
            val s = sec % 60
            type to "%02d:%02d".format(m, s)
        }

    // 获取当前心情的 emoji
    val currentMoodEmoji = diaryVm.moods.firstOrNull { it.first == diaryVm.selectedMood }?.second ?: "😊"

    // 真实日期显示
    val realDate = java.text.SimpleDateFormat("M 月 d 日", java.util.Locale.getDefault()).format(java.util.Date())
    val weekDay = java.text.SimpleDateFormat("EEEE", java.util.Locale.CHINESE).format(java.util.Date())
    val weekOfYear = java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_YEAR)

    // 过滤后的日记列表（搜索 + 日期筛选）
    val filteredEntries = entries.filter { e ->
        (selectedDate == null || e.date == selectedDate) &&
            (searchQuery.isEmpty() || e.content.contains(searchQuery) || e.mood.contains(searchQuery))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("行为日记", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (showSearch || searchQuery.isNotEmpty()) Primary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { showSearch = !showSearch; if (!showSearch) searchQuery = "" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, null, tint = if (searchQuery.isNotEmpty()) Primary else colors.navIcon, modifier = Modifier.size(17.dp))
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        .background(if (showCalendar || selectedDate != null) Primary.copy(alpha = 0.12f) else Color.Transparent)
                        .clickable { showCalendar = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.CalendarMonth, null, tint = if (selectedDate != null) Primary else colors.navIcon, modifier = Modifier.size(17.dp))
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(realDate, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
                        Text("$weekDay · 第 $weekOfYear 周", fontSize = 12.sp, color = colors.ink3)
                    }
                    Text("已记录 ${entries.size} 篇", fontSize = 12.sp, color = colors.ink3)
                }
            }

            // 搜索栏（展开时显示）
            if (showSearch) {
                item {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索日记内容或心情…", fontSize = 12.sp, color = colors.ink3) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = colors.card,
                            unfocusedContainerColor = colors.card,
                            focusedIndicatorColor = Primary,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    )
                }
            }

            // 日期筛选标签（选中后显示）
            if (selectedDate != null) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(Primary.copy(alpha = 0.1f), RoundedCornerShape(999.dp))
                                .clickable { selectedDate = null }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("📅 $selectedDate  ✕", fontSize = 11.sp, color = Primary)
                        }
                    }
                }
            }

            // Today behavior summary
            item {
                AppCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("今日行为摘要", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                            Text(if (todayRecords.isNotEmpty()) "自动同步" else "暂无数据", fontSize = 11.sp, color = colors.ink3)
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (topBehaviors.isNotEmpty()) {
                                topBehaviors.forEach { (type, dur) ->
                                    BehaviorSummaryCell(type, dur) {
                                        appVm.showToast("${type.label}：今日累计 $dur")
                                    }
                                }
                            } else {
                                BehaviorSummaryCell(BehaviorType.WALK, "00:00") {}
                                BehaviorSummaryCell(BehaviorType.SIT, "00:00") {}
                                BehaviorSummaryCell(BehaviorType.STAND, "00:00") {}
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.line))
                        Spacer(Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showMoodPicker = true }
                                .padding(vertical = 4.dp)
                        ) {
                            Text("今日心情", fontSize = 12.sp, color = colors.ink2)
                            Spacer(Modifier.width(8.dp))
                            Text(currentMoodEmoji, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(diaryVm.selectedMood, fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.width(4.dp))
                            Text("点击切换", fontSize = 10.sp, color = colors.ink3)
                            Spacer(Modifier.weight(1f))
                            Text("写日记 ›", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clickable {
                                    diaryVm.resetSheet()
                                    appVm.openDiarySheet()
                                })
                        }
                    }
                }
            }

            // Past diaries header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("往期日记", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                    Text("+ 写一篇", fontSize = 12.sp, color = Primary, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            diaryVm.resetSheet()
                            appVm.openDiarySheet()
                        })
                }
            }

            // Diary list
            if (filteredEntries.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📔", fontSize = 36.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (entries.isEmpty()) "还没有日记，点击「写日记」开始记录吧" else "没有匹配的日记",
                            fontSize = 12.sp, color = colors.ink3
                        )
                    }
                }
            } else {
                items(filteredEntries.size) { idx ->
                    DiaryCard(filteredEntries[idx], onDelete = { id -> diaryVm.deleteDiary(id) })
                }
            }

            item {
                Box(
                    modifier = Modifier.fillMaxWidth().clickable { appVm.showToast("加载更早的日记...") }.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⋯ 查看更早的日记", fontSize = 12.sp, color = colors.ink3)
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // 心情选择遮罩
    if (showMoodPicker) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80101828))
                .clickable { showMoodPicker = false }
        )
    }

    // 心情选择底部弹窗
    androidx.compose.animation.AnimatedVisibility(
        visible = showMoodPicker,
        enter = androidx.compose.animation.slideInVertically { it },
        exit = androidx.compose.animation.slideOutVertically { it },
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(colors.sheetBg)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier.width(40.dp).height(4.dp)
                    .background(Color(0xFFDCE0E6), RoundedCornerShape(999.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
            Text("选择今日心情", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                diaryVm.moods.forEach { (mood, emoji) ->
                    val active = diaryVm.selectedMood == mood
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) Primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable {
                                diaryVm.selectMood(mood)
                                showMoodPicker = false
                                appVm.showToast("心情已更新：$emoji $mood")
                            }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(emoji, fontSize = 26.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(mood, fontSize = 10.sp,
                            color = if (active) Primary else colors.ink3,
                            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    // 日历选择遮罩
    if (showCalendar) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80101828))
                .clickable { showCalendar = false }
        )
    }

    // 日历选择底部弹窗：按日期筛选日记
    androidx.compose.animation.AnimatedVisibility(
        visible = showCalendar,
        enter = androidx.compose.animation.slideInVertically { it },
        exit = androidx.compose.animation.slideOutVertically { it },
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        val dates = entries.map { it.date }.distinct().sortedDescending()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(colors.sheetBg)
                .padding(20.dp)
        ) {
            Box(
                modifier = Modifier.width(40.dp).height(4.dp)
                    .background(Color(0xFFDCE0E6), RoundedCornerShape(999.dp))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
            Text("按日期查看", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selectedDate == null) Primary.copy(alpha = 0.1f) else Color.Transparent)
                    .clickable { selectedDate = null; showCalendar = false }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AllInclusive, null, tint = colors.ink3, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text("全部日记", fontSize = 14.sp, color = if (selectedDate == null) Primary else colors.ink1)
            }
            if (dates.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("暂无日记记录", fontSize = 12.sp, color = colors.ink3, modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                dates.forEach { d ->
                    val cnt = entries.count { it.date == d }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedDate == d) Primary.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { selectedDate = d; showCalendar = false }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarMonth, null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(d, fontSize = 14.sp, color = if (selectedDate == d) Primary else colors.ink1,
                                fontWeight = if (selectedDate == d) FontWeight.Medium else FontWeight.Normal)
                        }
                        Text("$cnt 篇", fontSize = 11.sp, color = colors.ink3)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    }
}

@Composable
private fun RowScope.BehaviorSummaryCell(type: BehaviorType, duration: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(type.color.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(type.emoji, fontSize = 16.sp)
        Spacer(Modifier.height(6.dp))
        Text(duration, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
        Text(type.label, fontSize = 10.sp, color = colors.ink2)
    }
}

@Composable
private fun DiaryCard(entry: com.xiaoyou.diary.model.DiaryEntry, onDelete: (String) -> Unit = {}) {
    val colors = LocalAppColors.current
    AppCard {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(entry.moodEmoji, fontSize = 24.sp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("${entry.date} · ${entry.dateLabel}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                    Text(entry.time, fontSize = 11.sp, color = colors.ink3)
                }
                Spacer(Modifier.height(6.dp))
                Text(entry.content, fontSize = 12.sp, color = colors.ink2, lineHeight = 18.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        entry.tags.forEach { (type, dur) ->
                            Box(
                                modifier = Modifier
                                    .background(type.color.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("${type.emoji} ${type.label} $dur", fontSize = 10.sp, color = type.color)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onDelete(entry.id) }
                            .padding(4.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, null, tint = colors.ink3, modifier = Modifier.size(15.dp))
                    }
                }
            }
        }
    }
}
