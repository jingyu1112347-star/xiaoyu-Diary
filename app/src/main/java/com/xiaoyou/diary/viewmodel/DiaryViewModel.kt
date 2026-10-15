package com.xiaoyou.diary.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyou.diary.ServiceLocator
import com.xiaoyou.diary.model.BehaviorType
import com.xiaoyou.diary.model.DiaryEntry
import com.xiaoyou.diary.data.storage.DiaryStorage
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DiaryViewModel(app: Application) : AndroidViewModel(app) {

    private val storage: DiaryStorage = ServiceLocator.provideDiary(app)

    var selectedMood by mutableStateOf("开心")
        private set

    var diaryText by mutableStateOf("")
        private set

    val moods = listOf(
        "开心" to "😊",
        "平静" to "😐",
        "疲惫" to "😔",
        "兴奋" to "🤩",
        "低落" to "😢"
    )

    /** 真实日记列表（从持久化存储读取），无数据时为空 */
    val entries = storage.entries
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectMood(mood: String) {
        selectedMood = mood
    }

    fun updateText(text: String) {
        diaryText = text
    }

    /**
     * 保存日记。
     * @param tagTypes 用户在弹窗中勾选的行为标签（多选），如实落盘
     */
    fun saveDiary(tagTypes: Set<BehaviorType>, onToast: (String) -> Unit): Boolean {
        val text = diaryText.trim()
        if (text.isEmpty()) {
            onToast("请写点内容再保存")
            return false
        }
        val now = Date()
        val dateLabel = dateLabel(now)
        val newEntry = DiaryEntry(
            id = System.currentTimeMillis().toString(),
            date = SimpleDateFormat("M 月 d 日", Locale.getDefault()).format(now),
            sortDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(now),
            dateLabel = dateLabel,
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now),
            mood = selectedMood,
            moodEmoji = moods.first { it.first == selectedMood }.second,
            content = text,
            tags = tagTypes.map { it to it.label }
        )
        viewModelScope.launch { storage.insert(newEntry) }
        diaryText = ""
        selectedMood = "开心"
        onToast("日记已保存")
        return true
    }

    fun deleteDiary(id: String) {
        viewModelScope.launch { storage.delete(id) }
    }

    fun resetSheet() {
        diaryText = ""
        selectedMood = "开心"
    }

    private fun dateLabel(date: Date): String {
        // 先把两个时刻都归一化到当地零点，再算自然日差值，
        // 避免昨晚 23:30 与今天 00:30 因不足 24 小时被算成同一天
        val today = Calendar.getInstance().apply { truncateToMidnight() }
        val that = Calendar.getInstance().apply {
            time = date
            truncateToMidnight()
        }
        val diffDays = (today.timeInMillis - that.timeInMillis) / (24L * 60 * 60 * 1000)
        return when (diffDays) {
            0L -> "今天"
            1L -> "昨天"
            else -> SimpleDateFormat("EEEE", Locale.CHINESE).format(date)
        }
    }

    private fun Calendar.truncateToMidnight() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
