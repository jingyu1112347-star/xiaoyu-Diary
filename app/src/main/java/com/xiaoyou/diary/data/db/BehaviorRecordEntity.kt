package com.xiaoyou.diary.data.db

/**
 * 行为检测记录：每条记录代表一个时间段内检测到的行为
 */
data class BehaviorRecordEntity(
    val id: Long = 0,
    val behaviorName: String,
    val behaviorLabel: String,
    val confidence: Float,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationSec: Int,
    val pitch: Float,
    val roll: Float,
    val yaw: Float,
    val sessionId: String,
    val date: String
)

data class BehaviorStat(
    val behaviorName: String,
    val behaviorLabel: String,
    val totalDuration: Long
)

data class HourlyStat(
    val hour: String,
    val behaviorName: String,
    val cnt: Int
)
