package com.xiaoyou.diary.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyou.diary.ServiceLocator
import com.xiaoyou.diary.data.storage.SettingsStorage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {

    private val settings: SettingsStorage = ServiceLocator.provideSettings(app)

    // 以 Compose State 镜像 StateFlow，供 UI 直接读取
    var sampleRate by mutableStateOf(settings.sampleRate.value)
        private set
    var sensitivityValue by mutableStateOf(settings.sensitivity.value)
        private set
    var thresholdValue by mutableStateOf(settings.threshold.value)
        private set
    var sedentaryReminder by mutableStateOf(settings.sedentaryReminder.value)
        private set
    var vibrationFeedback by mutableStateOf(settings.vibration.value)
        private set
    var backgroundMonitoring by mutableStateOf(settings.backgroundMonitoring.value)
        private set
    var reminderInterval by mutableStateOf(settings.reminderInterval.value)
        private set
    var isCalibrated by mutableStateOf(settings.calibrated.value)
        private set
    var lastCalibrateTime by mutableStateOf(settings.lastCalibrateTime.value)
        private set
    var storageLimitDays by mutableStateOf(settings.storageLimitDays.value)
        private set

    private val sensitivitySteps = listOf(25, 50, 68, 85, 100)
    private val sampleRateSteps = listOf(25, 50, 100, 200)
    private val thresholdSteps = listOf(0.5f, 0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.9f)
    private val intervalSteps = listOf(15, 30, 45, 60, 90, 120)

    init {
        // 同步外部变更（如其它页面修改）到 UI State
        viewModelScope.launch {
            settings.sensitivity.collectLatest { sensitivityValue = it }
        }
        viewModelScope.launch {
            settings.sampleRate.collectLatest { sampleRate = it }
        }
        viewModelScope.launch {
            settings.threshold.collectLatest { thresholdValue = it }
        }
        viewModelScope.launch {
            settings.sedentaryReminder.collectLatest { sedentaryReminder = it }
        }
        viewModelScope.launch {
            settings.vibration.collectLatest { vibrationFeedback = it }
        }
        viewModelScope.launch {
            settings.backgroundMonitoring.collectLatest { backgroundMonitoring = it }
        }
        viewModelScope.launch {
            settings.reminderInterval.collectLatest { reminderInterval = it }
        }
        viewModelScope.launch {
            settings.calibrated.collectLatest { isCalibrated = it }
        }
        viewModelScope.launch {
            settings.lastCalibrateTime.collectLatest { lastCalibrateTime = it }
        }
        viewModelScope.launch {
            settings.storageLimitDays.collectLatest { storageLimitDays = it }
        }
    }

    fun adjustSensitivity(onToast: (String) -> Unit) {
        val idx = sensitivitySteps.indexOf(sensitivityValue)
        val next = sensitivitySteps[(idx + 1) % sensitivitySteps.size]
        settings.setSensitivity(next)
        onToast("检测灵敏度：$next%")
    }

    fun cycleSampleRate(onToast: (String) -> Unit) {
        val idx = sampleRateSteps.indexOf(sampleRate)
        val next = sampleRateSteps[(idx + 1) % sampleRateSteps.size]
        settings.setSampleRate(next)
        onToast("采样率已切换为：$next Hz")
    }

    fun cycleThreshold(onToast: (String) -> Unit) {
        val idx = thresholdSteps.indexOf(thresholdValue)
        val next = thresholdSteps[(idx + 1) % thresholdSteps.size]
        settings.setThreshold(next)
        onToast("判定阈值已设置为：%.2f".format(next))
    }

    fun cycleInterval(onToast: (String) -> Unit) {
        val idx = intervalSteps.indexOf(reminderInterval)
        val next = intervalSteps[(idx + 1) % intervalSteps.size]
        settings.setReminderInterval(next)
        onToast("久坐提醒间隔：每 $next 分钟")
    }

    fun toggleSedentary(onToast: (String) -> Unit) {
        settings.setSedentaryReminder(!sedentaryReminder)
        onToast("久坐提醒 已" + if (!sedentaryReminder) "开启" else "关闭")
    }

    fun toggleVibration(onToast: (String) -> Unit) {
        settings.setVibration(!vibrationFeedback)
        onToast("震动反馈 已" + if (!vibrationFeedback) "开启" else "关闭")
    }

    fun toggleBackground(onToast: (String) -> Unit) {
        settings.setBackgroundMonitoring(!backgroundMonitoring)
        onToast("后台持续监测 已" + if (!backgroundMonitoring) "开启" else "关闭")
    }

    fun markCalibrated() {
        settings.setCalibrated(true)
        settings.setLastCalibrateTime(System.currentTimeMillis())
    }
}
