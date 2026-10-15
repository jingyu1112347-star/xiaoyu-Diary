package com.xiaoyou.diary.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.model.HistorySession
import com.xiaoyou.diary.ui.components.*
import com.xiaoyou.diary.ui.theme.LocalAppColors
import com.xiaoyou.diary.viewmodel.AnalyzeViewModel
import com.xiaoyou.diary.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 从原始记录补全会话的时间戳信息（sessionId 是 UUID，无法直接取日期） */
private data class SessionMeta(
    val startMs: Long,
    val endMs: Long,
    val totalSec: Int,
    val date: String,
    val isToday: Boolean
)

private fun durationParts(totalSec: Int): Pair<String, String> {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return when {
        h > 0 -> "%d:%02d".format(h, m) to "小时"
        m > 0 -> "$m" to "分钟"
        else -> "$totalSec" to "秒"
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(appVm: AppViewModel, analyzeVm: AnalyzeViewModel) {
    val c = LocalAppColors.current
    val context = LocalContext.current
    val sessions by analyzeVm.sessions.collectAsState()
    val records by analyzeVm.allRecords.collectAsState()

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // 会话元数据：sessionId -> 起止时间戳/总时长/日期。
    // 日期以会话最早分段的真实开始时间重新格式化，不依赖列表顺序。
    val dateFmt = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val metaMap = remember(records, todayStr) {
        records.groupBy { it.sessionId }.mapValues { (_, list) ->
            val startMs = list.minOf { it.startTimeMs }
            val date = dateFmt.format(Date(startMs))
            SessionMeta(
                startMs = startMs,
                endMs = list.maxOf { it.endTimeMs },
                totalSec = list.sumOf { it.durationSec },
                date = date,
                isToday = date == todayStr
            )
        }
    }

    // 按真实开始时间倒序
    val sortedSessions = remember(sessions, metaMap) {
        sessions.sortedByDescending { metaMap[it.id]?.startMs ?: 0L }
    }

    var showSheet by remember { mutableStateOf(false) }
    var sheetMode by remember { mutableStateOf<SheetMode>(SheetMode.Filter) }
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var batchMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<String>() }
    var detailSession by remember { mutableStateOf<HistorySession?>(null) }
    var menuSession by remember { mutableStateOf<HistorySession?>(null) }

    val filtered = if (selectedFilter != null)
        sortedSessions.filter { it.primaryBehavior.label == selectedFilter } else sortedSessions
    val todayList = filtered.filter { metaMap[it.id]?.isToday == true }
    val earlierList = filtered.filter { metaMap[it.id]?.isToday != true }

    val hmFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val mdFmt = SimpleDateFormat("M月d日 HH:mm", Locale.getDefault())

    fun timeRange(s: HistorySession): String {
        val m = metaMap[s.id] ?: return "${s.startTime} – ${s.endTime}"
        return if (m.isToday) "${hmFmt.format(Date(m.startMs))} – ${hmFmt.format(Date(m.endMs))}"
        else "${mdFmt.format(Date(m.startMs))} – ${hmFmt.format(Date(m.endMs))}"
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
            if (batchMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleButton(Icons.Filled.Close, "退出批量管理") {
                        batchMode = false; selectedIds.clear()
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("已选 ${selectedIds.size}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = c.ink)
                }
            } else {
                Column {
                    Text("历史记录", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = c.ink, letterSpacing = (-0.5f).sp)
                    Text("共 ${sortedSessions.size} 次检测会话", fontSize = 12.5.sp, color = c.ink3)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (batchMode) {
                    Box(
                        modifier = Modifier
                            .height(44.dp)
                            .clip(CircleShape)
                            .background(if (selectedIds.isNotEmpty()) c.redSoft else c.surface2)
                            .tapScale(enabled = selectedIds.isNotEmpty()) {
                                val toDelete = selectedIds.toSet()
                                // 批量删除同样需要二次确认，避免误触丢失多条会话
                                appVm.showConfirmDialog(
                                    "删除选中记录",
                                    "将永久删除选中的 ${toDelete.size} 条检测会话及其全部明细，不可恢复，确定删除吗？"
                                ) {
                                    analyzeVm.deleteSessions(toDelete) {
                                        appVm.showToast("已删除 ${toDelete.size} 条记录")
                                    }
                                    selectedIds.clear(); batchMode = false
                                }
                            }
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("删除", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                            color = if (selectedIds.isNotEmpty()) c.red else c.ink3)
                    }
                } else {
                    PillIconButton(Icons.Filled.Search, "筛选与管理") {
                        sheetMode = SheetMode.Filter; showSheet = true
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 130.dp)
        ) {
            if (sortedSessions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Filled.Search,
                        title = "暂无检测记录",
                        description = "完成第一次检测后，会话会按日期\n自动归档到这里，支持分段明细查看。",
                        modifier = Modifier.padding(bottom = 40.dp),
                        action = {
                            PrimaryButton(
                                text = "开始第一次检测",
                                icon = Icons.Filled.PlayArrow,
                                modifier = Modifier.padding(horizontal = 40.dp),
                                onClick = { appVm.setTab(TabKey.HOME) }
                            )
                        }
                    )
                }
                return@LazyColumn
            }

            // ---------- 今天 ----------
            item {
                GroupHeader("今天", accent = true, modifier = Modifier.padding(bottom = 9.dp),
                    trailing = { Text("${todayList.size} 次", fontSize = 12.sp, color = c.ink3) })
            }
            if (todayList.isNotEmpty()) {
                item {
                    AppSurface(modifier = Modifier.padding(bottom = 24.dp)) {
                        todayList.forEachIndexed { idx, s ->
                            SessionRow(
                                session = s,
                                timeText = timeRange(s),
                                today = true,
                                batchMode = batchMode,
                                selected = s.id in selectedIds,
                                onToggle = {
                                    if (s.id in selectedIds) selectedIds.remove(s.id) else selectedIds.add(s.id)
                                },
                                onClick = { detailSession = s },
                                onLongClick = { menuSession = s },
                                totalSec = metaMap[s.id]?.totalSec ?: 0
                            )
                            if (idx < todayList.lastIndex) Hairline(start = 14.dp)
                        }
                    }
                }
            }

            // ---------- 更早 ----------
            if (earlierList.isNotEmpty()) {
                item {
                    GroupHeader("更早", modifier = Modifier.padding(bottom = 9.dp),
                        trailing = { Text("${earlierList.size} 次", fontSize = 12.sp, color = c.ink3) })
                }
                item {
                    AppSurface {
                        earlierList.forEachIndexed { idx, s ->
                            SessionRow(
                                session = s,
                                timeText = timeRange(s),
                                today = false,
                                batchMode = batchMode,
                                selected = s.id in selectedIds,
                                onToggle = {
                                    if (s.id in selectedIds) selectedIds.remove(s.id) else selectedIds.add(s.id)
                                },
                                onClick = { detailSession = s },
                                onLongClick = { menuSession = s },
                                totalSec = metaMap[s.id]?.totalSec ?: 0
                            )
                            if (idx < earlierList.lastIndex) Hairline(start = 14.dp)
                        }
                    }
                }
            }

            item {
                Text(
                    "长按会话可呼出 查看 / 复制 / 导出 / 删除 菜单",
                    fontSize = 11.5.sp, color = c.ink3,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
      }

        // ---------- 筛选 / 管理 面板 ----------
        if (showSheet) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(c.scrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showSheet = false }
                    )
            )
        }
        AnimatedVisibility(
            visible = showSheet,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FilterSheet(
                selectedFilter = selectedFilter,
                onSelect = { selectedFilter = it; showSheet = false },
                onBatch = { batchMode = true; showSheet = false; selectedIds.clear() }
            )
        }

        // ---------- 会话明细时间线 ----------
        detailSession?.let { s ->
            SessionDetailSheet(
                session = s,
                totalSec = metaMap[s.id]?.totalSec ?: 0,
                onDismiss = { detailSession = null }
            )
        }

        // ---------- 长按上下文菜单（动作面板） ----------
        menuSession?.let { s ->
            SessionActionSheet(
                onDismiss = { menuSession = null },
                onView = { menuSession = null; detailSession = s },
                onCopy = {
                    val clip = android.content.ClipData.newPlainText(
                        "session",
                        "检测会话 ${timeRange(s)} · 主导行为 ${s.primaryBehavior.label} · 切换 ${s.switchCount} 次"
                    )
                    context.getSystemService(android.content.ClipboardManager::class.java).setPrimaryClip(clip)
                    appVm.showToast("会话信息已复制")
                    menuSession = null
                },
                onExport = {
                    val id = s.id
                    menuSession = null
                    analyzeVm.exportSession(context, id) { ok, msg ->
                        appVm.showToast(if (ok) "会话已导出" else "导出失败：$msg", if (ok) ToastType.OK else ToastType.ERROR)
                    }
                },
                onDelete = {
                    val id = s.id
                    menuSession = null
                    appVm.showConfirmDialog("删除该记录", "将删除这条检测会话及其全部明细，不可恢复。") {
                        analyzeVm.deleteSessions(setOf(id)) { appVm.showToast("记录已删除") }
                    }
                }
            )
        }
    }
}

private enum class SheetMode { Filter }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionRow(
    session: HistorySession,
    timeText: String,
    today: Boolean,
    batchMode: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    totalSec: Int
) {
    val c = LocalAppColors.current
    val color = c.behavior(session.primaryBehavior)
    val (durValue, durUnit) = durationParts(totalSec)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (today) Modifier.background(c.blueSoft) else Modifier)
            .then(
                if (batchMode) Modifier.clickable { onToggle() }
                else Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
            )
            .padding(start = 0.dp, end = 14.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 4px 彩色竖条
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(topEnd = 3.dp, bottomEnd = 3.dp))
                .background(color)
        )
        if (batchMode) {
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (selected) c.blue else Color.Transparent)
                    .border(1.5.dp, if (selected) c.blue else c.rule2, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(timeText, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("主导行为 · ${session.primaryBehavior.label}", fontSize = 12.sp, color = c.ink2)
                Text(" | ", fontSize = 12.sp, color = c.ink3)
                Text("切换 ${session.switchCount} 次", fontSize = 12.sp, color = c.ink2)
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
            Text(durValue, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = c.ink)
            Text(durUnit, fontSize = 10.5.sp, color = c.ink3, modifier = Modifier.padding(top = 2.dp))
        }
        Text("›", fontSize = 18.sp, color = c.ink3)
    }
}

@Composable
private fun FilterSheet(
    selectedFilter: String?,
    onSelect: (String?) -> Unit,
    onBatch: () -> Unit
) {
    val c = LocalAppColors.current
    Column(
        modifier = Modifier
            // 底部留出悬浮 Dock 的高度，避免“批量管理会话”入口被导航胶囊遮挡
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 104.dp)
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
        Text("筛选行为类型", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink,
            modifier = Modifier.padding(start = 18.dp, top = 12.dp, bottom = 6.dp))
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            FilterRow("全部行为", null == selectedFilter) { onSelect(null) }
            BehaviorType.entries.forEach { t ->
                FilterRow("${t.emoji}  ${t.label}", selectedFilter == t.label) { onSelect(t.label) }
            }
            Hairline()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .tapScale(onClick = onBatch)
                    .padding(horizontal = 12.dp, vertical = 13.dp)
            ) {
                Icon(Icons.Filled.Check, null, tint = c.blue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(10.dp))
                Text("批量管理会话", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.blue)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FilterRow(label: String, active: Boolean, onClick: () -> Unit) {
    val c = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) c.blueSoft else Color.Transparent)
            .tapScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) c.blue else c.ink)
        if (active) Icon(Icons.Filled.Check, null, tint = c.blue, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SessionDetailSheet(
    session: HistorySession,
    totalSec: Int,
    onDismiss: () -> Unit
) {
    val c = LocalAppColors.current
    Box(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier
        .matchParentSize()
        .background(c.scrim)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, onClick = onDismiss
        ))
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SheetRoundX(onDismiss)
            Text("会话明细", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.ink)
            Spacer(Modifier.size(34.dp))
        }
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            SheetDescription(
                "主导行为 ${session.primaryBehavior.label} · 切换 ${session.switchCount} 次 · " +
                    durationParts(totalSec).let { "${it.first} ${it.second}" }
            )
            session.segments.forEachIndexed { idx, (type, pct) ->
                TimelineRow(
                    type = type,
                    pct = pct,
                    minutes = (totalSec * pct / 100f / 60f),
                    last = idx == session.segments.lastIndex
                )
            }
        }
    }
    }
}

@Composable
private fun TimelineRow(type: BehaviorType, pct: Float, minutes: Float, last: Boolean) {
    val c = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Column(
            modifier = Modifier.width(14.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(c.behavior(type))
            )
            if (!last) Box(Modifier.width(2.dp).weight(1f).background(c.rule))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(type.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            Text("占比 %.1f%%".format(pct), fontSize = 11.5.sp, color = c.ink3, modifier = Modifier.padding(top = 3.dp))
        }
        Text("%.0f 分钟".format(minutes), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = c.ink2)
    }
}

@Composable
private fun SessionActionSheet(
    onDismiss: () -> Unit,
    onView: () -> Unit,
    onCopy: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val c = LocalAppColors.current
    Box(modifier = Modifier.fillMaxSize()) {
    Box(modifier = Modifier
        .matchParentSize()
        .background(c.scrim)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, onClick = onDismiss
        ))
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            // 底部留出悬浮 Dock 的高度，避免最后一行被导航胶囊遮挡
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 104.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.Sheet))
            .background(c.surface)
            .border(1.dp, c.rule2, RoundedCornerShape(Dimens.Sheet))
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 8.dp)
    ) {
        ActionSheetRow(Icons.Filled.Visibility, "查看明细", c.ink2, onView)
        ActionSheetRow(Icons.Filled.ContentCopy, "复制信息", c.ink2, onCopy)
        ActionSheetRow(Icons.Filled.FileUpload, "导出会话", c.ink2, onExport)
        ActionSheetRow(Icons.Filled.DeleteOutline, "删除记录", c.red, onDelete)
        Spacer(Modifier.height(6.dp))
    }
    }
}

@Composable
private fun ActionSheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tone: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .tapScale(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = tone, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = tone)
    }
}
