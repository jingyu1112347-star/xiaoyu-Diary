package com.xiaoyou.diary.data.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xiaoyou.diary.model.DiaryEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 日记条目本地存储（JSON 文件）
 */
class DiaryStorage(context: Context) {

    private val appContext = context.applicationContext
    private val gson = Gson()
    private val mutex = Mutex()
    private val file = java.io.File(appContext.filesDir, "diary_entries.json")
    private val tmpFile = java.io.File(appContext.filesDir, "diary_entries.json.tmp")

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries: StateFlow<List<DiaryEntry>> = _entries.asStateFlow()

    init { loadFromDisk() }

    private fun loadFromDisk() {
        if (!file.exists()) { _entries.value = emptyList(); return }
        try {
            val type = object : TypeToken<List<DiaryEntry>>() {}.type
            val list: List<DiaryEntry> = gson.fromJson(file.readText(), type) ?: emptyList()
            _entries.value = list
        } catch (_: Exception) {
            // 备份损坏文件后再置空，避免后续保存把原数据永久覆盖
            runCatching {
                val bak = java.io.File(appContext.filesDir, "diary_entries.json.bak")
                if (bak.exists()) bak.delete()
                file.renameTo(bak)
            }
            _entries.value = emptyList()
        }
    }

    /** 原子写盘：先写临时文件再 rename，避免写入过程中被杀导致文件损坏 */
    private suspend fun saveToDisk() = mutex.withLock {
        withContext(Dispatchers.IO) {
            try {
                tmpFile.writeText(gson.toJson(_entries.value))
                if (!tmpFile.renameTo(file)) {
                    tmpFile.copyTo(file, overwrite = true)
                    tmpFile.delete()
                }
            } catch (_: Exception) { }
        }
    }

    suspend fun insert(entry: DiaryEntry) {
        _entries.value = listOf(entry) + _entries.value
        saveToDisk()
    }

    suspend fun delete(id: String) {
        _entries.value = _entries.value.filterNot { it.id == id }
        saveToDisk()
    }

    suspend fun clearAll() {
        _entries.value = emptyList()
        saveToDisk()
    }
}
