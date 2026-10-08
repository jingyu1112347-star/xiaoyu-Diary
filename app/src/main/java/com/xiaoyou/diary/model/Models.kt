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
    val pitch: Float = -12.4f,
    val roll: Float = 3.8f,
    val yaw: Float = 87.2f
)

data class HistorySession(
    val id: String,
    val startTime: String,
    val endTime: String,
    val primaryBehavior: BehaviorType,
    val switchCount: Int,
    val totalDuration: String,
    val segments: List<Pair<BehaviorType, Float>>
)

data class DiaryEntry(
    val id: String,
    val date: String,
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
