package com.xiaoyou.diary.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyou.diary.ServiceLocator
import com.xiaoyou.diary.data.db.BehaviorStat
import com.xiaoyou.diary.data.db.HourlyStat
import com.xiaoyou.diary.data.repository.BehaviorRepository
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.model.HistorySession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class AnalyzeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: BehaviorRepository = ServiceLocator.provideRepository(app)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /** 全部记录（响应式，数据库变化自动更新） */
    val allRecords = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** 行为统计（最近7天） */
    var behaviorStats by mutableStateOf<List<BehaviorStat>>(emptyList())
        private set

    /** 24小时分布 */
    var hourlyStats by mutableStateOf<List<HourlyStat>>(emptyList())
        private set

    /** 历史会话（用于History页） */
    val sessions: StateFlow<List<HistorySession>> = repository.allRecords
        .map { records -> buildSessions(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
            behaviorStats = repository.getBehaviorStatsSince(sevenDaysAgo)
            hourlyStats = repository.getHourlyDistribution(dateFormat.format(Date()))
        }
    }

    fun clearAll(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            refresh()
            onDone()
        }
    }

    fun deleteSessions(sessionIds: Set<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteBySessionIds(sessionIds)
            refresh()
            onDone()
        }
    }

    /** 导出数据到外部存储并通过系统分享 */
    fun exportData(context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = repository.exportJson()
                val dir = context.getExternalFilesDir(null) ?: context.filesDir
                val file = java.io.File(dir, "xiaoyou_records_${System.currentTimeMillis()}.json")
                file.writeText(json)
                // 分享文件
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "导出行为数据").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                onResult(true, file.absolutePath)
            } catch (e: Exception) {
                onResult(false, e.message ?: "导出失败")
            }
        }
    }

    /** 导入 JSON 数据 */
    fun importJson(json: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importJson(json)
            refresh()
            onResult(count)
        }
    }

    /**
     * 将扁平记录按 sessionId 聚合为会话列表
     */
    private fun buildSessions(records: List<com.xiaoyou.diary.data.db.BehaviorRecordEntity>): List<HistorySession> {
        if (records.isEmpty()) return emptyList()
        val bySession = records.groupBy { it.sessionId }
        return bySession.map { (sessionId, recs) ->
            val sorted = recs.sortedBy { it.startTimeMs }
            val first = sorted.first()
            val last = sorted.last()
            val totalSec = sorted.sumOf { it.durationSec }
            // 统计每种行为的时长
            val behaviorDurations = sorted.groupBy { it.behaviorName }
                .mapValues { (_, list) -> list.sumOf { it.durationSec } }
            val primaryName = behaviorDurations.maxByOrNull { it.value }?.key ?: BehaviorType.SIT.name
            val segments = behaviorDurations.map { (name, dur) ->
                val type = try { BehaviorType.valueOf(name) } catch (_: Exception) { BehaviorType.SIT }
                type to (dur.toFloat() / totalSec.coerceAtLeast(1) * 100f)
            }.sortedByDescending { it.second }

            HistorySession(
                id = sessionId,
                startTime = timeFormat.format(Date(first.startTimeMs)),
                endTime = timeFormat.format(Date(last.endTimeMs)),
                primaryBehavior = try { BehaviorType.valueOf(primaryName) } catch (_: Exception) { BehaviorType.SIT },
                switchCount = sorted.map { it.behaviorName }.distinct().size,
                totalDuration = formatDuration(totalSec),
                segments = segments
            )
        }.sortedByDescending { it.id }
    }

    private fun formatDuration(totalSec: Int): String {
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return when {
            h > 0 -> "${h}h${m}m"
            m > 0 -> "${m}m${s}s"
            else -> "${s}s"
        }
    }
}
