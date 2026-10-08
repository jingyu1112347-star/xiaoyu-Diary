package com.xiaoyou.diary.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.model.HistorySession
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.*
import com.xiaoyou.diary.viewmodel.AnalyzeViewModel
import com.xiaoyou.diary.viewmodel.AppViewModel

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(appVm: AppViewModel, analyzeVm: AnalyzeViewModel) {
    val colors = LocalAppColors.current
    val sessions by analyzeVm.sessions.collectAsState()

    // 筛选面板 & 批量管理状态
    var showFilterSheet by remember { mutableStateOf(false) }
    var batchMode by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val selectedIds = remember { mutableStateListOf<String>() }

    // 按日期分组
    val todaySessions = sessions.take(3)
    val earlierSessions = sessions.drop(3).take(2)

    // 筛选过滤
    val filteredToday = if (selectedFilter != null) {
        todaySessions.filter { it.primaryBehavior.label == selectedFilter }
    } else todaySessions
    val filteredEarlier = if (selectedFilter != null) {
        earlierSessions.filter { it.primaryBehavior.label == selectedFilter }
    } else earlierSessions

    Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (batchMode) {
                        Icon(Icons.Default.Close, null, tint = colors.ink1, modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                batchMode = false
                                selectedIds.clear()
                            })
                        Spacer(Modifier.width(8.dp))
                        Text("已选 ${selectedIds.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
                    } else {
                        Text("历史记录", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (showFilterSheet || selectedFilter != null) Primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { showFilterSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.FilterList, null,
                            tint = if (selectedFilter != null) Primary else colors.navIcon,
                            modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (batchMode) Primary.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable {
                                batchMode = !batchMode
                                if (!batchMode) selectedIds.clear()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(if (batchMode) Icons.Default.Check else Icons.Default.Checklist, null,
                            tint = if (batchMode) Primary else colors.navIcon,
                            modifier = Modifier.size(17.dp))
                    }
                    if (batchMode) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedIds.isNotEmpty()) Danger.copy(alpha = 0.1f) else colors.soft)
                                .clickable(enabled = selectedIds.isNotEmpty()) {
                                    val toDelete = selectedIds.toSet()
                                    analyzeVm.deleteSessions(toDelete) {
                                        appVm.showToast("已删除 ${toDelete.size} 条记录")
                                    }
                                    selectedIds.clear()
                                    batchMode = false
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("删除", fontSize = 12.sp,
                                color = if (selectedIds.isNotEmpty()) Danger else colors.ink3,
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp)
            ) {
                stickyHeader {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(colors.bg).padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("今天", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                        Text("${filteredToday.size} 次", fontSize = 11.sp, color = colors.ink3)
                    }
                }
                items(filteredToday.size) { idx ->
                    SessionCard(
                        session = filteredToday[idx],
                        batchMode = batchMode,
                        selected = selectedIds.contains(filteredToday[idx].id),
                        onToggleSelect = {
                            if (selectedIds.contains(it)) selectedIds.remove(it) else selectedIds.add(it)
                        },
                        onDelete = { id ->
                            appVm.showConfirmDialog("删除该记录", "将删除这条检测记录，不可恢复。") {
                                analyzeVm.deleteSessions(setOf(id)) { appVm.showToast("记录已删除") }
                            }
                        }
                    )
                    if (idx < filteredToday.size - 1) Spacer(Modifier.height(8.dp))
                }
                if (filteredToday.isEmpty()) {
                    item {
                        Text(
                            if (sessions.isEmpty()) "暂无检测记录，去首页开始检测吧" else "无匹配记录",
                            fontSize = 13.sp, color = colors.ink3,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }

                stickyHeader {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(colors.bg).padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("更早", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                        Text("${filteredEarlier.size} 次", fontSize = 11.sp, color = colors.ink3)
                    }
                }
                items(filteredEarlier.size) { idx ->
                    SessionCard(
                        session = filteredEarlier[idx],
                        batchMode = batchMode,
                        selected = selectedIds.contains(filteredEarlier[idx].id),
                        onToggleSelect = {
                            if (selectedIds.contains(it)) selectedIds.remove(it) else selectedIds.add(it)
                        },
                        onDelete = { id ->
                            appVm.showConfirmDialog("删除该记录", "将删除这条检测记录，不可恢复。") {
                                analyzeVm.deleteSessions(setOf(id)) { appVm.showToast("记录已删除") }
                            }
                        }
                    )
                    if (idx < filteredEarlier.size - 1) Spacer(Modifier.height(8.dp))
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }

        // 筛选底部面板
        AnimatedVisibility(
            visible = showFilterSheet,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FilterSheet(
                selectedFilter = selectedFilter,
                onSelect = { selectedFilter = it; showFilterSheet = false },
                onDismiss = { showFilterSheet = false }
            )
        }

        // 筛选遮罩
        if (showFilterSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80101828))
                    .clickable { showFilterSheet = false }
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: HistorySession,
    batchMode: Boolean = false,
    selected: Boolean = false,
    onToggleSelect: (String) -> Unit = {},
    onDelete: (String) -> Unit = {}
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (batchMode) Modifier.clickable { onToggleSelect(session.id) } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (batchMode) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (selected) Primary else Color.Transparent)
                    .border(1.5.dp, if (selected) Primary else colors.line, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(Modifier.width(10.dp))
        }
        AppCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(12.dp, 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(28.dp).background(session.primaryBehavior.color.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(session.primaryBehavior.emoji, fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("${session.startTime} - ${session.endTime}", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.ink1)
                            Text("${session.primaryBehavior.label}为主 · 行为切换 ${session.switchCount} 次", fontSize = 11.sp, color = colors.ink3)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!batchMode) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onDelete(session.id) }
                                    .padding(4.dp)
                            ) {
                                Icon(Icons.Default.DeleteOutline, null, tint = colors.ink3, modifier = Modifier.size(15.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(session.totalDuration, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.ink1)
                            Text("总时长", fontSize = 10.sp, color = colors.ink3)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(999.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    session.segments.forEach { (type, pct) ->
                        Box(Modifier.fillMaxWidth(pct / 100f).height(6.dp).background(type.color))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSheet(
    selectedFilter: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current
    val filters = listOf(null) + BehaviorType.entries.map { it.label }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(colors.sheetBg)
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier.width(40.dp).height(4.dp).background(Color(0xFFDCE0E6), RoundedCornerShape(999.dp))
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))
        Text("筛选行为类型", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.ink1)
        Spacer(Modifier.height(12.dp))
        filters.forEach { label ->
            val isSelected = (selectedFilter == label)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) Primary.copy(alpha = 0.08f) else Color.Transparent)
                    .clickable { onSelect(label) }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (label == null) {
                    Icon(Icons.Default.AllInclusive, null, tint = colors.ink3, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("全部行为", fontSize = 14.sp, color = if (isSelected) Primary else colors.ink1, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
                } else {
                    val type = BehaviorType.entries.first { it.label == label }
                    Text(type.emoji, fontSize = 14.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(label, fontSize = 14.sp, color = if (isSelected) Primary else colors.ink1, fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal)
                }
                Spacer(Modifier.weight(1f))
                if (isSelected) {
                    Icon(Icons.Default.Check, null, tint = Primary, modifier = Modifier.size(16.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
