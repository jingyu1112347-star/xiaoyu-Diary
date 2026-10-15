package com.xiaoyou.diary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyou.diary.ServiceLocator
import com.xiaoyou.diary.data.repository.BehaviorRepository
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.model.HistorySession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalyzeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: BehaviorRepository = ServiceLocator.provideRepository(app)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    /** 全部记录（响应式，存储层变化自动更新） */
    val allRecords = repository.allRecords
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    /** 历史会话（用于History页），按会话开始时间倒序 */
    val sessions: StateFlow<List<HistorySession>> = repository.allRecords
        .map { records -> buildSessions(records) }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun clearAll(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.clearAllData()
            onDone()
        }
    }

    fun deleteSessions(sessionIds: Set<String>, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteBySessionIds(sessionIds)
            onDone()
        }
    }

    /** 导出数据到外部存储并通过系统分享。序列化与写盘放在 IO 线程，避免主线程卡顿 */
    fun exportData(context: android.content.Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val json = repository.exportJson()
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val target = java.io.File(dir, "xiaoyou_records_${System.currentTimeMillis()}.json")
                    // 原子写：先写临时文件再改名
                    val tmp = java.io.File(dir, "xiaoyou_records.tmp")
                    tmp.writeText(json)
                    if (!tmp.renameTo(target)) {
                        tmp.copyTo(target, overwrite = true)
                        tmp.delete()
                    }
                    target
                }
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

    /** 导出单个会话的记录并通过系统分享；写盘放在 IO 线程，避免主线程卡顿 */
    fun exportSession(context: android.content.Context, sessionId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    val records = repository.allRecords.value.filter { it.sessionId == sessionId }
                    val json = com.google.gson.Gson().toJson(records)
                    val dir = context.getExternalFilesDir(null) ?: context.filesDir
                    val target = java.io.File(dir, "xiaoyou_session_${System.currentTimeMillis()}.json")
                    // 原子写：先写临时文件再改名
                    val tmp = java.io.File(dir, "xiaoyou_session.tmp")
                    tmp.writeText(json)
                    if (!tmp.renameTo(target)) {
                        tmp.copyTo(target, overwrite = true)
                        tmp.delete()
                    }
                    target
                }
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "导出会话数据").addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
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

            // 真实切换次数：按时间排序后相邻分段行为发生变化的次数
            var switches = 0
            for (i in 1 until sorted.size) {
                if (sorted[i].behaviorName != sorted[i - 1].behaviorName) switches++
            }

            HistorySession(
                id = sessionId,
                startTime = timeFormat.format(Date(first.startTimeMs)),
                endTime = timeFormat.format(Date(last.endTimeMs)),
                primaryBehavior = try { BehaviorType.valueOf(primaryName) } catch (_: Exception) { BehaviorType.SIT },
                switchCount = switches,
                totalDuration = formatDuration(totalSec),
                segments = segments,
                startMs = first.startTimeMs
            )
        }.sortedByDescending { session ->
            // 按会话真实开始时间倒序（id 是随机 UUID，不能用于排序）
            session.startMs
        }
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
