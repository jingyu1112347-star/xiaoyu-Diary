package com.xiaoyou.diary.data.analyzer

import com.xiaoyou.diary.model.BehaviorProbability
import com.xiaoyou.diary.model.BehaviorType
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 行为分析模型
 * 基于传感器特征的规则分类器：
 *  - 加速度合量 & 方差 → 判断活动强度（静坐/站立/行走/跑步）
 *  - 重力向量方向 → 判断姿态（平躺/侧躺/倾斜）
 *  - 陀螺仪方差 → 判断是否在频繁转动
 *
 * 输出：8种行为的概率分布 + 最可能行为 + 姿态角
 */
class BehaviorAnalyzer(
    private var sensitivity: Int = 68,
    private var threshold: Float = 0.75f
) {

    /** 灵敏度倍率：灵敏度越高，检测阈值越低（越灵敏） */
    private fun sensFactor(): Float {
        // 68 -> 1.0; 25 -> ~0.6 (更迟钝); 100 -> 1.4 (更灵敏)
        return 0.4f + (sensitivity / 100f) * 1.0f
    }

    fun updateParams(sensitivity: Int, threshold: Float) {
        this.sensitivity = sensitivity
        this.threshold = threshold
    }

    // 滑动窗口，存储最近 N 帧的加速度合量
    private val magnitudeWindow = ArrayDeque<Float>()
    private val maxWindowSize = 50

    fun update(
        userAccel: FloatArray,
        gyro: FloatArray,
        gravity: FloatArray,
        accelMagnitude: Float
    ): AnalysisResult {
        // 更新滑动窗口
        magnitudeWindow.addLast(accelMagnitude)
        if (magnitudeWindow.size > maxWindowSize) magnitudeWindow.removeFirst()

        // 计算特征
        val meanMag = magnitudeWindow.average().toFloat()
        val varianceMag = computeVariance(magnitudeWindow, meanMag)
        val stdMag = sqrt(varianceMag)
        val gyroMag = sqrt(gyro[0] * gyro[0] + gyro[1] * gyro[1] + gyro[2] * gyro[2])

        // 计算姿态角（从重力向量）
        val pitch = Math.toDegrees(atan2(gravity[0], sqrt(gravity[1] * gravity[1] + gravity[2] * gravity[2])).toDouble()).toFloat()
        val roll = Math.toDegrees(atan2(gravity[1], gravity[2]).toDouble()).toFloat()
        val yaw = 0f // 简化：无磁力计时不计算绝对偏航

        // 分类决策
        val probabilities = classify(meanMag, stdMag, gyroMag, gravity)
        val best = probabilities.maxByOrNull { it.probability } ?: probabilities.first()

        return AnalysisResult(
            probabilities = probabilities,
            primaryBehavior = best.type,
            confidence = best.probability,
            pitch = pitch,
            roll = roll,
            yaw = yaw
        )
    }

    private fun classify(
        meanMag: Float,
        stdMag: Float,
        gyroMag: Float,
        gravity: FloatArray
    ): List<BehaviorProbability> {
        // 灵敏度因子：越高则阈值越低
        val f = sensFactor()
        val tRun = 4.0f / f
        val tJog = 1.5f / f
        val tWalkLow = 0.6f / f
        val tWalkHigh = 2.0f * f
        val tStatic = 0.3f * f
        val tSlightLow = 0.3f * f
        val tSlightHigh = 0.6f * f

        // 重力向量 z 分量判断竖直程度
        val gNorm = sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2]).coerceAtLeast(0.01f)
        val gx = gravity[0] / gNorm
        val gy = gravity[1] / gNorm
        val gz = gravity[2] / gNorm

        // 各行为得分（后转概率）
        val scores = HashMap<BehaviorType, Float>()
        BehaviorType.entries.forEach { scores[it] = 0.01f }

        // === 姿态类 ===
        // 平躺：gz 接近 1 或 -1（屏幕朝上或朝下），几乎无运动
        if (gz > 0.85f && stdMag < 0.5f * f) {
            scores[BehaviorType.LIE] = 80f
            scores[BehaviorType.SIDE_LIE] = 10f
        }
        // 侧躺：gx 或 gy 主导
        if ((kotlin.math.abs(gx) > 0.8f || kotlin.math.abs(gy) > 0.8f) && stdMag < 0.5f * f) {
            scores[BehaviorType.SIDE_LIE] = 75f
            scores[BehaviorType.LIE] = 15f
        }
        // 倾斜：明显偏离竖直但未到平躺
        val tiltScore = (1f - kotlin.math.abs(gz)).coerceIn(0f, 1f) * 50f
        if (tiltScore > 25f && stdMag < 1.0f * f) {
            scores[BehaviorType.TILT] = tiltScore
        }

        // === 活动强度类 ===
        when {
            // 剧烈运动：加速度方差大 + 陀螺仪大
            stdMag > tRun && gyroMag > 2.0f / f -> {
                scores[BehaviorType.RUN] = 85f
                scores[BehaviorType.JOG] = 30f
            }
            // 中等运动
            stdMag > tJog && meanMag > 11f / f -> {
                scores[BehaviorType.JOG] = 70f
                scores[BehaviorType.RUN] = 25f
                scores[BehaviorType.WALK] = 30f
            }
            // 行走：周期性中等幅度
            stdMag in tWalkLow..tWalkHigh && meanMag in 9.5f..12.5f -> {
                scores[BehaviorType.WALK] = 75f
                scores[BehaviorType.JOG] = 20f
            }
            // 静止类
            stdMag < tStatic -> {
                // 判断是坐还是站：看 gz 接近 1（正常握持）→ 站/坐
                if (gz > 0.7f) {
                    // 手机屏幕朝前 → 可能坐着看手机
                    scores[BehaviorType.SIT] = 60f
                    scores[BehaviorType.STAND] = 35f
                } else {
                    scores[BehaviorType.STAND] = 50f
                    scores[BehaviorType.SIT] = 40f
                }
            }
            // 微弱运动（站着微动）
            stdMag in tSlightLow..tSlightHigh -> {
                scores[BehaviorType.STAND] = 55f
                scores[BehaviorType.SIT] = 35f
            }
        }

        // 归一化为概率（softmax 简化版）
        val total = scores.values.sum()
        val result = BehaviorType.entries.map { type ->
            val prob = (scores[type]!! / total * 100f).coerceIn(0.1f, 99f)
            BehaviorProbability(type, prob, highlighted = false)
        }.sortedByDescending { it.probability }

        // 标记最高概率项
        val maxIdx = result.indexOfFirst { it.probability == result.maxOf { p -> p.probability } }
        return result.mapIndexed { i, bp ->
            if (i == maxIdx) bp.copy(highlighted = true) else bp
        }
    }

    private fun computeVariance(window: List<Float>, mean: Float): Float {
        if (window.size < 2) return 0f
        return window.sumOf { ((it - mean) * (it - mean)).toDouble() }.toFloat() / window.size
    }

    fun reset() {
        magnitudeWindow.clear()
    }
}

data class AnalysisResult(
    val probabilities: List<BehaviorProbability>,
    val primaryBehavior: BehaviorType,
    val confidence: Float,
    val pitch: Float,
    val roll: Float,
    val yaw: Float
)
