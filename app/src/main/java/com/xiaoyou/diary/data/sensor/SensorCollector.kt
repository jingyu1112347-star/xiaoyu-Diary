package com.xiaoyou.diary.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * 传感器数据采集器
 * 采集加速度计（含重力分离）和陀螺仪数据，
 * 通过 StateFlow 实时推送给分析层
 */
class SensorCollector(context: Context) {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val gravitySensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

    /** 原始加速度（含重力） m/s² */
    private val _rawAccel = MutableStateFlow(FloatArray(3))
    val rawAccel: StateFlow<FloatArray> = _rawAccel.asStateFlow()

    /** 重力向量 */
    private val _gravity = MutableStateFlow(FloatArray(3))
    val gravity: StateFlow<FloatArray> = _gravity.asStateFlow()

    /** 用户加速度（去重力） m/s² */
    private val _userAccel = MutableStateFlow(FloatArray(3))
    val userAccel: StateFlow<FloatArray> = _userAccel.asStateFlow()

    /** 陀螺仪角速度 rad/s */
    private val _gyro = MutableStateFlow(FloatArray(3))
    val gyro: StateFlow<FloatArray> = _gyro.asStateFlow()

    /** 加速度合量（用于活动强度判断） */
    private val _accelMagnitude = MutableStateFlow(9.8f)
    val accelMagnitude: StateFlow<Float> = _accelMagnitude.asStateFlow()

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    _rawAccel.value = event.values.copyOf()
                    val mag = sqrt(
                        event.values[0] * event.values[0] +
                                event.values[1] * event.values[1] +
                                event.values[2] * event.values[2]
                    )
                    _accelMagnitude.value = mag
                    // 简单一阶低通滤波分离重力
                    val alpha = 0.8f
                    val g = _gravity.value
                    g[0] = alpha * g[0] + (1 - alpha) * event.values[0]
                    g[1] = alpha * g[1] + (1 - alpha) * event.values[1]
                    g[2] = alpha * g[2] + (1 - alpha) * event.values[2]
                    _gravity.value = g
                    // 用户加速度 = 原始 - 重力
                    val ua = FloatArray(3)
                    ua[0] = event.values[0] - g[0]
                    ua[1] = event.values[1] - g[1]
                    ua[2] = event.values[2] - g[2]
                    _userAccel.value = ua
                }
                Sensor.TYPE_GYROSCOPE -> {
                    _gyro.value = event.values.copyOf()
                }
                Sensor.TYPE_GRAVITY -> {
                    _gravity.value = event.values.copyOf()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    private var currentDelay = SensorManager.SENSOR_DELAY_GAME

    fun start(sampleRateHz: Int = 50) {
        currentDelay = hzToDelay(sampleRateHz)
        registerAll(currentDelay)
    }

    /** 统一注册三个传感器，任何单个失败都不影响其它传感器与整体流程 */
    private fun registerAll(delay: Int) {
        listOfNotNull(accelerometer, gyroscope, gravitySensor).forEach { sensor ->
            try {
                sensorManager.registerListener(listener, sensor, delay)
            } catch (_: SecurityException) {
                // 缺少 HIGH_SAMPLING_RATE_SENSORS 等权限时降级，避免闪退
            } catch (_: RuntimeException) {
                // 个别设备/传感器注册异常兜底
            }
        }
    }

    /** 动态更新采样率（热更新，无需重启） */
    fun updateSampleRate(sampleRateHz: Int) {
        val newDelay = hzToDelay(sampleRateHz)
        if (newDelay == currentDelay) return
        currentDelay = newDelay
        stop()
        registerAll(currentDelay)
    }

    /**
     * 将 Hz 转换为 SensorManager 的采样延迟（微秒）。
     * 注意：永不返回 SENSOR_DELAY_FASTEST(0)，否则在 Android 12+ 上
     * 未声明 HIGH_SAMPLING_RATE_SENSORS 权限会直接抛出 SecurityException 闪退，
     * 且 0us 会导致过高采样率带来耗电与性能开销。最高封顶到 GAME(约 50Hz)。
     */
    private fun hzToDelay(hz: Int): Int = when {
        hz >= 60 -> SensorManager.SENSOR_DELAY_GAME
        hz >= 30 -> SensorManager.SENSOR_DELAY_UI
        else -> SensorManager.SENSOR_DELAY_NORMAL
    }

    fun stop() {
        sensorManager.unregisterListener(listener)
    }
}
