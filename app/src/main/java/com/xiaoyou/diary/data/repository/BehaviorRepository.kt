package com.xiaoyou.diary.data.repository

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.xiaoyou.diary.ServiceLocator
import com.xiaoyou.diary.data.analyzer.AnalysisResult
import com.xiaoyou.diary.data.analyzer.BehaviorAnalyzer
import com.xiaoyou.diary.data.db.BehaviorRecordEntity
import com.xiaoyou.diary.data.sensor.SensorCollector
import com.xiaoyou.diary.data.storage.BehaviorStorage
import com.xiaoyou.diary.data.storage.SettingsStorage
import com.xiaoyou.diary.model.BehaviorType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * 行为检测仓库：串联 传感器采集 → 行为分析 → 本地存储
 * 同时消费设置变更，实现提醒/震动等业务逻辑
 */
class BehaviorRepository(context: Context) {

    private val appContext = context.applicationContext
    // 复用 ServiceLocator 的全局单例，保证与 UI 层共享同一份设置状态
    private val settings: SettingsStorage = ServiceLocator.provideSettings(appContext)
    private val sensorCollector = SensorCollector(appContext)
    private val analyzer = BehaviorAnalyzer(
        sensitivity = settings.sensitivity.value,
        threshold = settings.threshold.value
    )
    private val storage = BehaviorStorage(appContext)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var analysisJob: Job? = null
    private var recordJob: Job? = null
    private var reminderJob: Job? = null
    private var sampleRateJob: Job? = null
    private var sensitivityJob: Job? = null
    private var thresholdJob: Job? = null

    private var sessionId: String = ""
    private var sessionStartMs: Long = 0L

    private val _latestResult = MutableStateFlow<AnalysisResult?>(null)
    val latestResult: StateFlow<AnalysisResult?> = _latestResult.asStateFlow()

    /** 提醒事件（久坐提醒等），UI 层 collect 后显示 Toast */
    private val _reminderEvent = MutableStateFlow<String?>(null)
    val reminderEvent: StateFlow<String?> = _reminderEvent.asStateFlow()
    fun consumeReminder() { _reminderEvent.value = null }

    val allRecords = storage.records

    fun getRecordsByDate(date: String) = storage.getRecordsByDate(date)
    fun getRecordsSince(sinceMs: Long) = storage.getRecordsSince(sinceMs)

    suspend fun getBehaviorStatsSince(sinceMs: Long) = storage.getBehaviorStatsSince(sinceMs)
    suspend fun getHourlyDistribution(date: String) = storage.getHourlyDistribution(date)
    suspend fun clearAllData() = storage.clearAll()
    suspend fun deleteBySessionIds(ids: Set<String>) = storage.deleteBySessionIds(ids)
    fun exportJson(): String = storage.exportJson()
    suspend fun importJson(json: String): Int = storage.importJson(json)

    /** 应用上下文（供 UI 层拿 Application 做文件分享） */
    val applicationContext: Context get() = appContext

    fun startDetection() {
        sessionId = UUID.randomUUID().toString()
        sessionStartMs = System.currentTimeMillis()
        analyzer.reset()
        sensorCollector.start(settings.sampleRate.value)

        // 监听设置变更，热更新传感器与分析器。三个监听分别持有 Job，
        // 停止/重启时统一取消，避免重复启动检测导致协程无限累积泄漏。
        sampleRateJob?.cancel()
        sampleRateJob = scope.launch {
            settings.sampleRate.collectLatest { rate ->
                if (analysisJob?.isActive == true) sensorCollector.updateSampleRate(rate)
            }
        }
        sensitivityJob?.cancel()
        sensitivityJob = scope.launch {
            settings.sensitivity.collectLatest { s ->
                analyzer.updateParams(s, settings.threshold.value)
            }
        }
        thresholdJob?.cancel()
        thresholdJob = scope.launch {
            settings.threshold.collectLatest { t ->
                analyzer.updateParams(settings.sensitivity.value, t)
            }
        }

        analysisJob = scope.launch {
            while (true) {
                val result = analyzer.update(
                    userAccel = sensorCollector.userAccel.value,
                    gyro = sensorCollector.gyro.value,
                    gravity = sensorCollector.gravity.value,
                    accelMagnitude = sensorCollector.accelMagnitude.value
                )
                _latestResult.value = result
                delay(100)
            }
        }

        recordJob = scope.launch {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            while (true) {
                delay(30_000)
                val result = _latestResult.value ?: continue
                // 仅当置信度达到阈值才记录
                if (result.confidence / 100f < settings.threshold.value) continue
                val now = System.currentTimeMillis()
                storage.insert(
                    BehaviorRecordEntity(
                        behaviorName = result.primaryBehavior.name,
                        behaviorLabel = result.primaryBehavior.label,
                        confidence = result.confidence,
                        startTimeMs = now - 30_000,
                        endTimeMs = now,
                        durationSec = 30,
                        pitch = result.pitch,
                        roll = result.roll,
                        yaw = result.yaw,
                        sessionId = sessionId,
                        date = dateFormat.format(Date(now))
                    )
                )
            }
        }

        startSedentaryReminder()
    }

    /** 久坐提醒：每隔 reminderInterval 分钟检查是否久坐，是则提醒 + 震动 */
    private fun startSedentaryReminder() {
        reminderJob?.cancel()
        reminderJob = scope.launch {
            while (true) {
                val intervalMin = settings.reminderInterval.value
                delay(intervalMin * 60_000L)
                if (!settings.sedentaryReminder.value) continue
                val result = _latestResult.value ?: continue
                if (result.primaryBehavior == BehaviorType.SIT || result.primaryBehavior == BehaviorType.LIE) {
                    _reminderEvent.value = "久坐提醒：已保持 ${result.primaryBehavior.label} 超过 ${intervalMin} 分钟，起来活动一下吧！"
                    if (settings.vibration.value) vibrate()
                }
            }
        }
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(400)
                }
            }
        } catch (_: Exception) { }
    }

    /** 提供给 UI 的震动反馈（如点击） */
    fun hapticFeedback() {
        if (settings.vibration.value) vibrate()
    }

    suspend fun stopDetection() {
        analysisJob?.cancel()
        recordJob?.cancel()
        reminderJob?.cancel()
        sampleRateJob?.cancel()
        sensitivityJob?.cancel()
        thresholdJob?.cancel()
        sensorCollector.stop()

        val result = _latestResult.value
        if (result != null && result.confidence / 100f >= settings.threshold.value) {
            val now = System.currentTimeMillis()
            val elapsed = ((now - sessionStartMs) / 1000).toInt()
            if (elapsed > 0) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                storage.insert(
                    BehaviorRecordEntity(
                        behaviorName = result.primaryBehavior.name,
                        behaviorLabel = result.primaryBehavior.label,
                        confidence = result.confidence,
                        startTimeMs = sessionStartMs,
                        endTimeMs = now,
                        durationSec = elapsed.coerceAtMost(30),
                        pitch = result.pitch,
                        roll = result.roll,
                        yaw = result.yaw,
                        sessionId = sessionId,
                        date = dateFormat.format(Date(now))
                    )
                )
            }
        }
        _latestResult.value = null
    }
}
