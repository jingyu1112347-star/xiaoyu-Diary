package com.xiaoyou.diary.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiaoyou.diary.ServiceLocator
import com.xiaoyou.diary.ui.components.TabKey
import com.xiaoyou.diary.ui.components.ToastState
import com.xiaoyou.diary.ui.components.ToastType
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = ServiceLocator.provideSettings(app)

    var isDarkMode by mutableStateOf(settings.darkMode.value)
        private set

    var currentTab by mutableStateOf(TabKey.HOME)
        private set

    var toast by mutableStateOf<ToastState?>(null)
        private set

    var showConfirm by mutableStateOf(false)
        private set

    var confirmTitle by mutableStateOf("")
        private set

    var confirmDesc by mutableStateOf("")
        private set

    var showDiarySheet by mutableStateOf(false)
        private set

    var currentPage by mutableStateOf("home")
        private set

    private var pendingConfirmAction: (() -> Unit)? = null

    init {
        viewModelScope.launch {
            settings.darkMode.collectLatest { isDarkMode = it }
        }
    }

    fun toggleDarkMode() {
        settings.setDarkMode(!isDarkMode)
    }

    fun setTab(tab: TabKey) {
        currentTab = tab
        currentPage = tab.name.lowercase()
    }

    fun navigateTo(page: String) {
        currentPage = page
    }

    fun showToast(text: String, type: ToastType = ToastType.OK) {
        toast = ToastState(text = text, type = type)
    }

    fun dismissToast() {
        toast = null
    }

    fun showConfirmDialog(title: String, desc: String, onConfirm: () -> Unit) {
        confirmTitle = title
        confirmDesc = desc
        pendingConfirmAction = onConfirm
        showConfirm = true
    }

    fun confirmAction() {
        showConfirm = false
        pendingConfirmAction?.invoke()
        pendingConfirmAction = null
    }

    fun cancelConfirm() {
        showConfirm = false
        pendingConfirmAction = null
    }

    fun openDiarySheet() {
        showDiarySheet = true
    }

    fun closeDiarySheet() {
        showDiarySheet = false
    }
}
