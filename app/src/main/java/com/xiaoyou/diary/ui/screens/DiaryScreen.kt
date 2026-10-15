package com.xiaoyou.diary.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.model.DiaryEntry
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.viewmodel.AnalyzeViewModel
import com.xiaoyou.diary.viewmodel.AppViewModel
import com.xiaoyou.diary.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val diaryMoods = listOf("😞" to "低落", "😕" to "疲惫", "😐" to "平静", "🙂" to "开心", "😄" to "兴奋")

@Composable
fun DiaryScreen(appVm: AppViewModel, diaryVm: DiaryViewModel, analyzeVm: AnalyzeViewModel) {
    val c = LocalAppColors.current
    val records by analyzeVm.allRecords.collectAsState()
    val entries by diaryVm.entries.collectAsState()
    var showCalendar by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<String?>(null) }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val dateTitle = SimpleDateFormat("M月d日", Locale.getDefault()).format(Date())
    val todayRecords = records.filter { it.date == today }

    // 今日三类行为摘要（行走 / 静坐 / 站立）
    val durSec: (BehaviorType) -> Int = { t ->
        todayRecords.filter { it.behaviorName == t.name }.sumOf { it.durationSec }
    }
    val summary = listOf(
        Triple(BehaviorType.WALK, Icons.AutoMirrored.Filled.DirectionsWalk, durSec(BehaviorType.WALK)),
        Triple(BehaviorType.SIT, Icons.Filled.Chair, durSec(BehaviorType.SIT)),
        Triple(BehaviorType.STAND, Icons.Filled.Accessibility, durSec(BehaviorType.STAND))
    )
    val maxDur = (summary.maxOf { it.third }.coerceAtLeast(60))
    val achieved = durSec(BehaviorType.WALK) >= 30 * 60

    val filteredEntries = entries.filter { selectedDate == null || it.sortDate == selectedDate }

    // 日期筛选项：按 yyyy-MM-dd 机器键去重倒序（展示文案仍为“M月d日”）
    val dateOptions = entries
        .filter { it.sortDate.isNotEmpty() }
        .distinctBy { it.sortDate }
        .sortedByDescending { it.sortDate }
        .map { it.sortDate to it.date }

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
                Text("日记", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = c.ink, letterSpacing = (-0.5f).sp)
                Text("$dateTitle · 记录此刻状态", fontSize = 12.5.sp, color = c.ink3)
            }
            PillIconButton(Icons.Filled.CalendarMonth, "按日期查看", onClick = { showCalendar = true })
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 130.dp)
        ) {
            // ---------- 当日行为摘要 ----------
            item {
                GroupHeader(
                    "当日行为摘要",
                    accent = true,
                    modifier = Modifier.padding(bottom = 11.dp),
                    trailing = {
                        if (todayRecords.isNotEmpty()) {
                            PillBadge(if (achieved) "已达标" else "待达标", if (achieved) BadgeTone.GREEN else BadgeTone.YELLOW)
                        }
                    }
                )
                AppSurface(modifier = Modifier.padding(bottom = 26.dp)) {
                    summary.forEachIndexed { idx, (type, icon, sec) ->
                        DaySummaryRow(type, icon, sec, maxDur)
                        if (idx < summary.lastIndex) Hairline()
                    }
                }
            }

            // ---------- 今天心情 ----------
            item {
                GroupHeader("今天心情如何", modifier = Modifier.padding(bottom = 11.dp))
                AppSurface(modifier = Modifier.padding(bottom = 26.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        diaryMoods.forEach { (emoji, mood) ->
                            val active = diaryVm.selectedMood == mood
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(if (active) c.yellowSoft else c.surface2)
                                    .border(2.dp, if (active) c.yellow else Color.Transparent, CircleShape)
                                    .tapScale { diaryVm.selectMood(mood) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }

            // ---------- 今日日记 ----------
            item {
                GroupHeader(
                    if (selectedDate == null) "今日日记" else "日记回顾",
                    accent = true,
                    modifier = Modifier.padding(bottom = 11.dp),
                    trailing = {
                        HeaderMore("写一篇", onClick = {
                            diaryVm.resetSheet()
                            appVm.openDiarySheet()
                        })
                    }
                )
                if (filteredEntries.isEmpty()) {
                    AppSurface(modifier = Modifier.padding(bottom = 14.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📔", fontSize = 32.sp)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (entries.isEmpty()) "还没有日记，点击下方按钮开始记录吧" else "该日期暂无日记",
                                fontSize = 12.5.sp, color = c.ink3
                            )
                        }
                    }
                } else {
                    filteredEntries.forEach { entry ->
                        DiaryItemCard(
                            entry = entry,
                            onRequestDelete = {
                                // 删除前二次确认，避免误触永久丢失日记
                                appVm.showConfirmDialog("删除这篇日记", "删除后不可恢复，确定删除吗？") {
                                    diaryVm.deleteDiary(entry.id)
                                    appVm.showToast("日记已删除")
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                PrimaryButton(
                    text = "写日记",
                    icon = Icons.Filled.Edit,
                    modifier = Modifier.padding(bottom = 10.dp),
                    onClick = {
                        diaryVm.resetSheet()
                        appVm.openDiarySheet()
                    }
                )
            }
        }
     }

        // ---------- 日期筛选底部弹窗 ----------
        if (showCalendar) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(c.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showCalendar = false }
                    )
            )
        }
        AnimatedVisibility(
            visible = showCalendar,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            DateFilterSheet(
                dates = dateOptions,
                countOf = { d -> entries.count { it.sortDate == d } },
                selected = selectedDate,
                onPick = { selectedDate = it; showCalendar = false }
            )
        }
    }
}

/** 时长格式：不足 1 分钟显示秒，X 分钟 / X 小时 Y 分 */
private fun formatMinutes(sec: Int): String {
    val m = sec / 60
    return when {
        m >= 60 -> "${m / 60} 小时 ${m % 60} 分"
        m > 0 -> "$m 分钟"
        else -> "$sec 秒"
    }
}

@Composable
private fun DaySummaryRow(type: BehaviorType, icon: ImageVector, sec: Int, maxSec: Int) {
    val c = LocalAppColors.current
    val color = c.behavior(type)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(c.behaviorSoft(type)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(type.label, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            Spacer(Modifier.height(7.dp))
            Box(
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(c.surface2)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((sec.toFloat() / maxSec).coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Text(formatMinutes(sec), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color,
            modifier = Modifier.width(74.dp))
    }
}

@Composable
private fun DiaryItemCard(entry: DiaryEntry, onRequestDelete: () -> Unit) {
    val c = LocalAppColors.current
    // 按行切分：标题取首个非空行，正文为该行之后的内容；
    // 不能用 removePrefix(title)，内容以空行开头时会匹配失败导致标题在正文里重复
    val rawLines = entry.content.split("\n")
    val titleIndex = rawLines.indexOfFirst { it.isNotBlank() }
    val title = rawLines.getOrNull(titleIndex)?.takeIf { it.isNotBlank() } ?: "今日记录"
    val body = if (titleIndex >= 0) {
        rawLines.drop(titleIndex + 1).joinToString("\n").trim()
    } else entry.content
    AppSurface {
        Column(modifier = Modifier.padding(16.dp, end = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(entry.time, fontSize = 11.sp, color = c.ink3)
                Text(" · 检测绑定 ${entry.tags.size} 个行为标签", fontSize = 11.sp, color = c.ink3)
            }
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink,
                modifier = Modifier.padding(top = 7.dp, bottom = 5.dp))
            Text(body, fontSize = 13.sp, color = c.ink2, lineHeight = 21.sp, maxLines = 3)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    entry.tags.take(3).forEach { (type, _) ->
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(c.behaviorSoft(type))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("#${type.label}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c.behavior(type))
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .tapScale { onRequestDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.DeleteOutline, "删除", tint = c.ink3, modifier = Modifier.size(15.dp))
                }
            }
        }
    }
}

@Composable
private fun DateFilterSheet(
    /** 机器日期键(yyyy-MM-dd) 到展示文案(M月d日) 的有序列表 */
    dates: List<Pair<String, String>>,
    countOf: (String) -> Int,
    selected: String?,
    onPick: (String?) -> Unit
) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Sheet))
            .background(c.surface)
            .border(1.dp, c.rule2, RoundedCornerShape(Dimens.Sheet))
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 9.dp)
                .align(Alignment.CenterHorizontally)
                .width(40.dp).height(5.dp)
                .clip(CircleShape).background(c.rule2)
        )
        Text(
            "按日期查看",
            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink,
            modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 8.dp)
        )
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
            SheetDateRow("全部日记", null, selected == null) { onPick(null) }
            dates.forEach { (sortDate, label) ->
                SheetDateRow(label, "${countOf(sortDate)} 篇", selected == sortDate) { onPick(sortDate) }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SheetDateRow(label: String, trailing: String?, active: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) c.blueSoft else Color.Transparent)
            .tapScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) c.blue else c.ink
        )
        if (trailing != null) Text(trailing, fontSize = 11.sp, color = c.ink3)
    }
}
