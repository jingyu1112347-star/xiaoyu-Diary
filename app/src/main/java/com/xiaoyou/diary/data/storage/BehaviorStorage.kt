package com.xiaoyou.diary.data.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xiaoyou.diary.data.db.BehaviorRecordEntity
import com.xiaoyou.diary.data.db.BehaviorStat
import com.xiaoyou.diary.data.db.HourlyStat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 基于 JSON 文件的本地存储（替代 Room，避免 KSP/kapt 与 Gradle 9.x 的兼容问题）
 * 所有记录保存在 app 内部存储 behavior_records.json
 */
class BehaviorStorage(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val mutex = Mutex()
    private val file = java.io.File(appContext.filesDir, "behavior_records.json")

    private val _records = MutableStateFlow<List<BehaviorRecordEntity>>(emptyList())
    val records: StateFlow<List<BehaviorRecordEntity>> = _records.asStateFlow()

    private var nextId = 1L

    init {
        loadFromDisk()
    }

    private fun loadFromDisk() {
        if (!file.exists()) {
            _records.value = emptyList()
            return
        }
        try {
            val json = file.readText()
            val type = object : TypeToken<List<BehaviorRecordEntity>>() {}.type
            val list: List<BehaviorRecordEntity> = gson.fromJson(json, type) ?: emptyList()
            _records.value = list
            nextId = (list.maxOfOrNull { it.id } ?: 0L) + 1
        } catch (_: Exception) {
            _records.value = emptyList()
        }
    }

    private suspend fun saveToDisk() = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                file.writeText(gson.toJson(_records.value))
            } catch (_: Exception) { }
        }
    }

    suspend fun insert(record: BehaviorRecordEntity) {
        val withId = record.copy(id = nextId++)
        _records.value = _records.value + withId
        saveToDisk()
    }

    suspend fun insertAll(records: List<BehaviorRecordEntity>) {
        val withIds = records.map { it.copy(id = nextId++) }
        _records.value = _records.value + withIds
        saveToDisk()
    }

    fun getRecordsByDate(date: String): StateFlow<List<BehaviorRecordEntity>> {
        val flow = MutableStateFlow(_records.value.filter { it.date == date })
        return flow.asStateFlow()
    }

    fun getRecordsSince(sinceMs: Long): StateFlow<List<BehaviorRecordEntity>> {
        val flow = MutableStateFlow(_records.value.filter { it.startTimeMs >= sinceMs })
        return flow.asStateFlow()
    }

    fun getAllRecords(): StateFlow<List<BehaviorRecordEntity>> = _records.asStateFlow()

    suspend fun getBehaviorStatsSince(sinceMs: Long): List<BehaviorStat> {
        val filtered = _records.value.filter { it.startTimeMs >= sinceMs }
        return filtered.groupBy { it.behaviorName }
            .map { (name, list) ->
                BehaviorStat(
                    behaviorName = name,
                    behaviorLabel = list.first().behaviorLabel,
                    totalDuration = list.sumOf { it.durationSec.toLong() }
                )
            }
            .sortedByDescending { it.totalDuration }
    }

    suspend fun getHourlyDistribution(date: String): List<HourlyStat> {
        val sdf = SimpleDateFormat("HH", Locale.getDefault())
        val filtered = _records.value.filter { it.date == date }
        return filtered.groupBy { "${sdf.format(Date(it.startTimeMs))}|${it.behaviorName}" }
            .map { (key, list) ->
                val parts = key.split("|")
                HourlyStat(parts[0], parts[1], list.size)
            }
    }

    suspend fun clearAll() {
        _records.value = emptyList()
        nextId = 1
        saveToDisk()
    }

    suspend fun deleteById(id: Long) {
        _records.value = _records.value.filterNot { it.id == id }
        saveToDisk()
    }

    suspend fun deleteBySessionId(sessionId: String) {
        _records.value = _records.value.filterNot { it.sessionId == sessionId }
        saveToDisk()
    }

    suspend fun deleteBySessionIds(sessionIds: Set<String>) {
        _records.value = _records.value.filterNot { it.sessionId in sessionIds }
        saveToDisk()
    }

    suspend fun deleteByDate(date: String) {
        _records.value = _records.value.filterNot { it.date == date }
        saveToDisk()
    }

    /** 导出全部记录为 JSON 字符串 */
    fun exportJson(): String = gson.toJson(_records.value)

    /** 从 JSON 字符串导入记录（追加） */
    suspend fun importJson(json: String): Int {
        return try {
            val type = object : TypeToken<List<BehaviorRecordEntity>>() {}.type
            val list: List<BehaviorRecordEntity> = gson.fromJson(json, type) ?: emptyList()
            val withIds = list.map { it.copy(id = nextId++) }
            _records.value = _records.value + withIds
            saveToDisk()
            withIds.size
        } catch (_: Exception) { 0 }
    }
}
