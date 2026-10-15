package com.xiaoyou.diary.data.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xiaoyou.diary.data.db.BehaviorRecordEntity
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 基于 JSON 文件的本地存储（替代 Room，避免 KSP/kapt 与 Gradle 9.x 的兼容问题）
 * 所有记录保存在 app 内部存储 behavior_records.json
 *
 * 注意：
 * - 文件在后台线程异步加载，所有写操作等待加载完成后再执行，避免主线程读盘卡顿
 * - 所有“读改写”均在同一把互斥锁内完成，避免并发插入丢记录
 * - 写盘走临时文件 + rename 原子落盘，避免进程被杀时写坏数据文件
 */
class BehaviorStorage(
    context: Context,
    /** 数据保留天数，动态读取设置；超出保留期的历史记录在加载/写入时清理 */
    private val retentionDays: () -> Int = { 90 }
) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val mutex = Mutex()
    private val file = java.io.File(appContext.filesDir, "behavior_records.json")
    private val tmpFile = java.io.File(appContext.filesDir, "behavior_records.json.tmp")

    // 独立 IO 作用域：仅负责启动时加载，生命周期与存储单例一致
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // 加载完成信号：写操作必须等待，避免把还没读到的旧数据覆盖掉
    private val loaded = CompletableDeferred<Unit>()

    private val _records = MutableStateFlow<List<BehaviorRecordEntity>>(emptyList())
    val records: StateFlow<List<BehaviorRecordEntity>> = _records.asStateFlow()

    private var nextId = 1L

    init {
        ioScope.launch {
            try {
                loadFromDisk()
            } finally {
                // 即使读盘异常也要放行，避免所有写操作永久挂起
                loaded.complete(Unit)
            }
        }
    }

    /** 后台线程读盘；解析失败时把坏文件备份为 .bak，避免后续写空列表覆盖原始数据 */
    private fun loadFromDisk() {
        if (!file.exists()) {
            _records.value = emptyList()
            return
        }
        val list: List<BehaviorRecordEntity> = try {
            val json = file.readText()
            val type = object : TypeToken<List<BehaviorRecordEntity>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            // 备份损坏文件后再置空，给用户保留排查/恢复的可能
            runCatching {
                val bak = java.io.File(appContext.filesDir, "behavior_records.json.bak")
                if (bak.exists()) bak.delete()
                file.renameTo(bak)
            }
            emptyList()
        }
        nextId = (list.maxOfOrNull { it.id } ?: 0L) + 1
        val pruned = prune(list)
        _records.value = pruned
        if (pruned.size != list.size) persistLocked()
    }

    /** 按保留期过滤旧记录 */
    private fun prune(list: List<BehaviorRecordEntity>): List<BehaviorRecordEntity> {
        val days = retentionDays().coerceAtLeast(1)
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        return list.filter { it.startTimeMs >= cutoff }
    }

    /** 原子写盘：先写临时文件，再 rename 覆盖正式文件。调用方必须已持有 mutex */
    private fun persistLocked() {
        try {
            tmpFile.writeText(gson.toJson(_records.value))
            // renameTo 在同一目录下为原子操作；个别机型失败时退化为覆盖拷贝
            if (!tmpFile.renameTo(file)) {
                tmpFile.copyTo(file, overwrite = true)
                tmpFile.delete()
            }
        } catch (_: Exception) { }
    }

    /** 在锁内完成“读改写 + 落盘”的统一入口，避免并发丢数据 */
    private suspend fun mutate(block: (MutableList<BehaviorRecordEntity>) -> Unit) {
        loaded.await()
        mutex.withLock {
            val copy = _records.value.toMutableList()
            block(copy)
            _records.value = copy
            withContext(Dispatchers.IO) { persistLocked() }
        }
    }

    suspend fun insert(record: BehaviorRecordEntity) {
        loaded.await()
        mutex.withLock {
            val copy = _records.value.toMutableList()
            copy.add(record.copy(id = nextId++))
            // 记录量较大时顺带按保留期清理一次，避免无限增长
            val pruned = if (copy.size % 50 == 0) prune(copy) else copy
            _records.value = pruned
            withContext(Dispatchers.IO) { persistLocked() }
        }
    }

    suspend fun insertAll(records: List<BehaviorRecordEntity>) {
        loaded.await()
        mutex.withLock {
            val copy = _records.value.toMutableList()
            records.forEach { copy.add(it.copy(id = nextId++)) }
            _records.value = copy
            withContext(Dispatchers.IO) { persistLocked() }
        }
    }

    fun getAllRecords(): StateFlow<List<BehaviorRecordEntity>> = _records.asStateFlow()

    suspend fun clearAll() = mutate { list ->
        list.clear()
        nextId = 1
    }

    suspend fun deleteById(id: Long) = mutate { list ->
        list.removeAll { it.id == id }
    }

    suspend fun deleteBySessionId(sessionId: String) = mutate { list ->
        list.removeAll { it.sessionId == sessionId }
    }

    suspend fun deleteBySessionIds(sessionIds: Set<String>) = mutate { list ->
        list.removeAll { it.sessionId in sessionIds }
    }

    suspend fun deleteByDate(date: String) = mutate { list ->
        list.removeAll { it.date == date }
    }

    /** 导出全部记录为 JSON 字符串（等待加载完成，确保导出内容完整） */
    suspend fun exportJson(): String {
        loaded.await()
        return mutex.withLock { gson.toJson(_records.value) }
    }

    /** 从 JSON 字符串导入记录（追加） */
    suspend fun importJson(json: String): Int {
        return try {
            val type = object : TypeToken<List<BehaviorRecordEntity>>() {}.type
            val list: List<BehaviorRecordEntity> = gson.fromJson(json, type) ?: emptyList()
            loaded.await()
            mutex.withLock {
                val copy = _records.value.toMutableList()
                list.forEach { copy.add(it.copy(id = nextId++)) }
                _records.value = copy
                withContext(Dispatchers.IO) { persistLocked() }
                list.size
            }
        } catch (_: Exception) { 0 }
    }
}
