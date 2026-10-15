package com.xiaoyou.diary.model

import androidx.compose.ui.graphics.Color
import com.xiaoyou.diary.ui.theme.*

enum class BehaviorType(
    val label: String,
    val emoji: String,
    val color: Color,
    val iconName: String
) {
    WALK("行走", "🚶", BehaviorWalk, "person_walking"),
    JOG("慢跑", "🏃", BehaviorJog, "person_running"),
    RUN("快跑", "⚡", BehaviorRun, "bolt"),
    SIT("静坐", "🪑", BehaviorSit, "chair"),
    STAND("站立", "🧍", BehaviorStand, "person"),
    TILT("倾斜", "📱", BehaviorTilt, "phone_iphone"),
    LIE("平躺", "🛏️", BehaviorLie, "bed"),
    SIDE_LIE("侧躺", "😴", BehaviorSideLie, "bed_side");

    companion object {
        val all = entries.toList()
    }
}

data class BehaviorProbability(
    val type: BehaviorType,
    val probability: Float,
    val highlighted: Boolean = false
)

data class AttitudeParams(
    // 默认 0 为中性占位，真实数值仅在检测时由分析结果填入
    val pitch: Float = 0f,
    val roll: Float = 0f,
    val yaw: Float = 0f
)

data class HistorySession(
    val id: String,
    val startTime: String,
    val endTime: String,
    val primaryBehavior: BehaviorType,
    val switchCount: Int,
    val totalDuration: String,
    val segments: List<Pair<BehaviorType, Float>>,
    /** 会话起始时间戳，供列表排序使用（id 为随机 UUID 不可排序） */
    val startMs: Long = 0L
)

data class DiaryEntry(
    val id: String,
    val date: String,
    /** yyyy-MM-dd 机器可读日期，用于筛选与排序（date 为展示文案，不可字典序排序） */
    val sortDate: String = "",
    val dateLabel: String,
    val time: String,
    val mood: String,
    val moodEmoji: String,
    val content: String,
    val tags: List<Pair<BehaviorType, String>>
)

data class ToastMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String
)
