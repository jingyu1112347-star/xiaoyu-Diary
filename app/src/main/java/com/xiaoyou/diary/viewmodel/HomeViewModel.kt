package com.xiaoyou.diary.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyou.diary.ServiceLocator
import com.xiaoyou.diary.data.repository.BehaviorRepository
import com.xiaoyou.diary.model.AttitudeParams
import com.xiaoyou.diary.model.BehaviorProbability
import com.xiaoyou.diary.model.BehaviorType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: BehaviorRepository = ServiceLocator.provideRepository(app)

    var isDetecting by mutableStateOf(false)
        private set

    var elapsedSeconds by mutableIntStateOf(0)
        private set

    // 未检测时为中性占位，避免向用户展示伪造的置信度/姿态数据
    var confidence by mutableStateOf(0f)
        private set

    var currentBehavior by mutableStateOf(BehaviorType.WALK)
        private set

    var attitude by mutableStateOf(AttitudeParams(0f, 0f, 0f))
        private set

    // 未检测时概率为均匀分布的中性占位，UI 层在停止态显示“--”
    var probabilities by mutableStateOf(
        BehaviorType.entries.map { BehaviorProbability(it, 100f / BehaviorType.entries.size) }
    )
        private set

    /** 当前设置的采样率（Hz），供 UI 如实展示 */
    var sampleRate by mutableIntStateOf(repository.sampleRate.value)
        private set

    private var timerJob: Job? = null
    private var collectJob: Job? = null
    private var sampleRateJob: Job? = null
    // 启停互斥：快速连点时拒绝重入，避免启动/停止竞态
    private val toggleMutex = Mutex()

    init {
        sampleRateJob = viewModelScope.launch {
            repository.sampleRate.collectLatest { sampleRate = it }
        }
    }

    fun toggleDetection(onToast: (String) -> Unit) {
        // 上一次启停尚未处理完时直接忽略，防止快速连点造成状态错乱
        if (!toggleMutex.tryLock()) {
            onToast("操作处理中，请稍候")
            return
        }
        val turningOn = !isDetecting
        isDetecting = turningOn
        viewModelScope.launch {
            try {
                if (turningOn) {
                    elapsedSeconds = 0
                    repository.startDetection()
                    onToast("传感器检测已启动")

                    // 计时器
                    timerJob = launch {
                        while (isActive && isDetecting) {
                            delay(1000)
                            elapsedSeconds++
                        }
                    }

                    // 收集实时分析结果
                    collectJob = launch {
                        repository.latestResult.collectLatest { result ->
                            result ?: return@collectLatest
                            currentBehavior = result.primaryBehavior
                            confidence = result.confidence
                            attitude = AttitudeParams(
                                pitch = result.pitch,
                                roll = result.roll,
                                yaw = result.yaw
                            )
                            probabilities = result.probabilities
                        }
                    }
                } else {
                    timerJob?.cancel()
                    collectJob?.cancel()
                    val saved = repository.stopDetection()
                    // 恢复中性占位，停止后不再残留/展示上一次检测的数值
                    confidence = 0f
                    attitude = AttitudeParams(0f, 0f, 0f)
                    probabilities = BehaviorType.entries.map {
                        BehaviorProbability(it, 100f / BehaviorType.entries.size)
                    }
                    onToast(if (saved) "检测已停止，数据已保存" else "检测已停止（时长过短或置信度不足，本次未记录）")
                }
            } finally {
                toggleMutex.unlock()
            }
        }
    }

    fun formatTime(): String {
        val h = elapsedSeconds / 3600
        val m = (elapsedSeconds % 3600) / 60
        val s = elapsedSeconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }

    override fun onCleared() {
        timerJob?.cancel()
        collectJob?.cancel()
        sampleRateJob?.cancel()
        super.onCleared()
    }
}
