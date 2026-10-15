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
    // 保留期跟随设置变化，存储层在加载/写入时清理超期记录
    private val storage = BehaviorStorage(appContext) { settings.storageLimitDays.value }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var analysisJob: Job? = null
    private var recordJob: Job? = null
    private var sampleRateJob: Job? = null
    private var sensitivityJob: Job? = null
    private var thresholdJob: Job? = null

    // 重入保护：防止重复 startDetection 导致旧协程泄漏与重复写库
    @Volatile
    private var detecting = false

    private var sessionId: String = ""
    private var sessionStartMs: Long = 0L

    private val _latestResult = MutableStateFlow<AnalysisResult?>(null)
    val latestResult: StateFlow<AnalysisResult?> = _latestResult.asStateFlow()

    /** 当前采样率设置，供 UI 如实展示 */
    val sampleRate: StateFlow<Int> = settings.sampleRate

    /** 提醒事件（久坐提醒等），UI 层 collect 后显示 Toast */
    private val _reminderEvent = MutableStateFlow<String?>(null)
    val reminderEvent: StateFlow<String?> = _reminderEvent.asStateFlow()
    fun consumeReminder() { _reminderEvent.value = null }

    val allRecords = storage.records

    suspend fun clearAllData() = storage.clearAll()
    suspend fun deleteBySessionIds(ids: Set<String>) = storage.deleteBySessionIds(ids)
    suspend fun exportJson(): String = storage.exportJson()
    suspend fun importJson(json: String): Int = storage.importJson(json)

    /** 应用上下文（供 UI 层拿 Application 做文件分享） */
    val applicationContext: Context get() = appContext

    /**
     * 开始一次检测会话。幂等：重复调用直接忽略，避免旧协程泄漏和重复写库。
     */
    fun startDetection() {
        if (detecting) return
        detecting = true

        sessionId = UUID.randomUUID().toString()
        sessionStartMs = System.currentTimeMillis()
        // 重置久坐连续状态，保证新会话从头累计
        sedentaryType = null
        sedentarySinceMs = 0L
        lastSedentaryReminderMs = 0L
        analyzer.reset()
        sensorCollector.start(settings.sampleRate.value)

        // 监听设置变更，热更新传感器与分析器。三个监听分别持有 Job，
        // 停止/重启时统一取消，避免重复启动检测导致协程无限累积泄漏。
        sampleRateJob?.cancel()
        sampleRateJob = scope.launch {
            settings.sampleRate.collectLatest { rate ->
                if (detecting) sensorCollector.updateSampleRate(rate)
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
                updateSedentaryState(result.primaryBehavior, System.currentTimeMillis())
                maybeFireSedentaryReminder(System.currentTimeMillis())
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
    }

    // ---- 久坐提醒：基于连续坐/躺时长累计，而非到点抽样一帧 ----
    /** 当前连续久坐姿态（SIT/LIE），非久坐时为 null */
    private var sedentaryType: BehaviorType? = null
    /** 当前连续久坐姿态的起始时间戳 */
    private var sedentarySinceMs: Long = 0L
    /** 上一次发出久坐提醒的时间戳，用于按间隔重复提醒 */
    private var lastSedentaryReminderMs: Long = 0L

    private fun isSedentary(type: BehaviorType) =
        type == BehaviorType.SIT || type == BehaviorType.LIE

    /** 按每帧结果推进连续久坐状态；姿态切换或起身时重新累计 */
    private fun updateSedentaryState(type: BehaviorType, nowMs: Long) {
        if (!isSedentary(type)) {
            sedentaryType = null
            sedentarySinceMs = 0L
            lastSedentaryReminderMs = 0L
            return
        }
        if (sedentaryType != type) {
            sedentaryType = type
            sedentarySinceMs = nowMs
            lastSedentaryReminderMs = 0L
        }
    }

    /** 连续坐/躺达到设定间隔才提醒；之后每过一个间隔重复提醒一次 */
    private fun maybeFireSedentaryReminder(nowMs: Long) {
        val type = sedentaryType ?: return
        val since = sedentarySinceMs
        if (since <= 0L || !settings.sedentaryReminder.value) return
        val intervalMs = settings.reminderInterval.value * 60_000L
        if (nowMs - since < intervalMs) return
        if (lastSedentaryReminderMs != 0L && nowMs - lastSedentaryReminderMs < intervalMs) return
        lastSedentaryReminderMs = nowMs
        _reminderEvent.value = "久坐提醒：已连续保持 ${type.label} 超过 ${settings.reminderInterval.value} 分钟，起来活动一下吧！"
        if (settings.vibration.value) vibrate()
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

    /**
     * 结束检测会话。
     * 仅补录最后一个 30 秒周期之后的“零头段”，不再整段重复写入；
     * @return 本次会话是否实际落盘了补录记录，供 UI 给出准确提示
     */
    suspend fun stopDetection(): Boolean {
        analysisJob?.cancel()
        recordJob?.cancel()
        sampleRateJob?.cancel()
        sensitivityJob?.cancel()
        thresholdJob?.cancel()
        sensorCollector.stop()
        detecting = false

        var saved = false
        val result = _latestResult.value
        val startMs = sessionStartMs
        if (result != null && result.confidence / 100f >= settings.threshold.value && startMs > 0L) {
            val now = System.currentTimeMillis()
            // 最后一个完整 30 秒周期的边界时间
            val lastBoundary = startMs + (now - startMs) / 30_000L * 30_000L
            val remainderSec = ((now - lastBoundary) / 1000L).toInt()
            // 只补零头（0 表示恰好停在整周期上，无需补录）
            if (remainderSec > 0) {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                storage.insert(
                    BehaviorRecordEntity(
                        behaviorName = result.primaryBehavior.name,
                        behaviorLabel = result.primaryBehavior.label,
                        confidence = result.confidence,
                        startTimeMs = lastBoundary,
                        endTimeMs = now,
                        durationSec = remainderSec,
                        pitch = result.pitch,
                        roll = result.roll,
                        yaw = result.yaw,
                        sessionId = sessionId,
                        date = dateFormat.format(Date(now))
                    )
                )
                saved = true
            }
        }
        _latestResult.value = null
        sessionStartMs = 0L
        sedentaryType = null
        sedentarySinceMs = 0L
        lastSedentaryReminderMs = 0L
        return saved
    }
}
