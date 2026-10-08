package com.xiaoyou.diary.data.storage

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 全局设置持久化存储（基于 SharedPreferences）
 * 所有设置项通过 StateFlow 暴露，修改后立即写入磁盘并通知订阅者
 */
class SettingsStorage(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("xiaoyou_settings", Context.MODE_PRIVATE)

    // ---- 各设置项的 StateFlow ----
    private val _sampleRate = MutableStateFlow(prefs.getInt(KEY_SAMPLE_RATE, 50))
    val sampleRate: StateFlow<Int> = _sampleRate.asStateFlow()

    private val _sensitivity = MutableStateFlow(prefs.getInt(KEY_SENSITIVITY, 68))
    val sensitivity: StateFlow<Int> = _sensitivity.asStateFlow()

    private val _threshold = MutableStateFlow(prefs.getFloat(KEY_THRESHOLD, 0.75f))
    val threshold: StateFlow<Float> = _threshold.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _sedentaryReminder = MutableStateFlow(prefs.getBoolean(KEY_SEDENTARY, true))
    val sedentaryReminder: StateFlow<Boolean> = _sedentaryReminder.asStateFlow()

    private val _vibration = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION, true))
    val vibration: StateFlow<Boolean> = _vibration.asStateFlow()

    private val _backgroundMonitoring = MutableStateFlow(prefs.getBoolean(KEY_BACKGROUND, false))
    val backgroundMonitoring: StateFlow<Boolean> = _backgroundMonitoring.asStateFlow()

    private val _reminderInterval = MutableStateFlow(prefs.getInt(KEY_REMINDER_INTERVAL, 60))
    val reminderInterval: StateFlow<Int> = _reminderInterval.asStateFlow()

    private val _calibrated = MutableStateFlow(prefs.getBoolean(KEY_CALIBRATED, false))
    val calibrated: StateFlow<Boolean> = _calibrated.asStateFlow()

    private val _lastCalibrateTime = MutableStateFlow(prefs.getLong(KEY_LAST_CALIBRATE, 0L))
    val lastCalibrateTime: StateFlow<Long> = _lastCalibrateTime.asStateFlow()

    private val _storageLimitDays = MutableStateFlow(prefs.getInt(KEY_STORAGE_LIMIT, 90))
    val storageLimitDays: StateFlow<Int> = _storageLimitDays.asStateFlow()

    // ---- setter（更新内存 + 持久化）----
    fun setSampleRate(value: Int) { _sampleRate.value = value; prefs.edit().putInt(KEY_SAMPLE_RATE, value).apply() }
    fun setSensitivity(value: Int) { _sensitivity.value = value; prefs.edit().putInt(KEY_SENSITIVITY, value).apply() }
    fun setThreshold(value: Float) { _threshold.value = value; prefs.edit().putFloat(KEY_THRESHOLD, value).apply() }
    fun setDarkMode(value: Boolean) { _darkMode.value = value; prefs.edit().putBoolean(KEY_DARK_MODE, value).apply() }
    fun setSedentaryReminder(value: Boolean) { _sedentaryReminder.value = value; prefs.edit().putBoolean(KEY_SEDENTARY, value).apply() }
    fun setVibration(value: Boolean) { _vibration.value = value; prefs.edit().putBoolean(KEY_VIBRATION, value).apply() }
    fun setBackgroundMonitoring(value: Boolean) { _backgroundMonitoring.value = value; prefs.edit().putBoolean(KEY_BACKGROUND, value).apply() }
    fun setReminderInterval(value: Int) { _reminderInterval.value = value; prefs.edit().putInt(KEY_REMINDER_INTERVAL, value).apply() }
    fun setCalibrated(value: Boolean) { _calibrated.value = value; prefs.edit().putBoolean(KEY_CALIBRATED, value).apply() }
    fun setLastCalibrateTime(value: Long) { _lastCalibrateTime.value = value; prefs.edit().putLong(KEY_LAST_CALIBRATE, value).apply() }
    fun setStorageLimitDays(value: Int) { _storageLimitDays.value = value; prefs.edit().putInt(KEY_STORAGE_LIMIT, value).apply() }

    companion object {
        private const val KEY_SAMPLE_RATE = "sample_rate"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_THRESHOLD = "threshold"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_SEDENTARY = "sedentary_reminder"
        private const val KEY_VIBRATION = "vibration"
        private const val KEY_BACKGROUND = "background_monitoring"
        private const val KEY_REMINDER_INTERVAL = "reminder_interval"
        private const val KEY_CALIBRATED = "calibrated"
        private const val KEY_LAST_CALIBRATE = "last_calibrate"
        private const val KEY_STORAGE_LIMIT = "storage_limit"
    }
}
