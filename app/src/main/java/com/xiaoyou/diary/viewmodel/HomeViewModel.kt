package com.xiaoyou.diary.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
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

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository: BehaviorRepository = ServiceLocator.provideRepository(app)

    var isDetecting by mutableStateOf(false)
        private set

    var elapsedSeconds by mutableStateOf(0)
        private set

    var confidence by mutableStateOf(92.4f)
        private set

    var currentBehavior by mutableStateOf(BehaviorType.WALK)
        private set

    var attitude by mutableStateOf(AttitudeParams())
        private set

    var probabilities by mutableStateOf(
        BehaviorType.entries.map { type ->
            BehaviorProbability(type, if (type == BehaviorType.WALK) 92.4f else (0.1f + kotlin.random.Random.nextFloat() * 2.9f))
        }.sortedByDescending { it.probability }
            .mapIndexed { i, bp -> if (i == 0) bp.copy(highlighted = true) else bp }
    )
        private set

    private var timerJob: Job? = null
    private var collectJob: Job? = null

    fun toggleDetection(onToast: (String) -> Unit) {
        isDetecting = !isDetecting
        if (isDetecting) {
            elapsedSeconds = 0
            onToast("传感器检测已启动")
            repository.startDetection()

            // 计时器
            timerJob = viewModelScope.launch {
                while (isActive && isDetecting) {
                    delay(1000)
                    elapsedSeconds++
                }
            }

            // 收集实时分析结果
            collectJob = viewModelScope.launch {
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
            viewModelScope.launch {
                repository.stopDetection()
            }
            onToast("检测已停止，数据已保存")
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
        super.onCleared()
    }
}
