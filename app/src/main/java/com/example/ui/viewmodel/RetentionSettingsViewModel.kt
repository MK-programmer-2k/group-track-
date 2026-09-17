package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.local.model.LocalStorageStats
import com.example.data.local.model.RetentionCleanupResult
import com.example.data.local.model.RetentionPeriodOption
import com.example.data.repository.GroupTrackRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class RetentionSettingsViewModel(
    private val repository: GroupTrackRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    val isAutoDeleteEnabled: StateFlow<Boolean> = sessionManager.autoDeleteEnabledFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val retentionDays: StateFlow<Int> = sessionManager.retentionDaysFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 14)

    val lastCleanupTime: StateFlow<Long> = sessionManager.lastCleanupTimeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0L)

    val lastDeletedCount: StateFlow<Int> = sessionManager.lastDeletedCountFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _storageStats = MutableStateFlow(LocalStorageStats())
    val storageStats: StateFlow<LocalStorageStats> = _storageStats.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        // Observe repository total trail count to keep stats up to date
        viewModelScope.launch {
            repository.totalTrailCountFlow.collect {
                refreshStats()
            }
        }
        viewModelScope.launch {
            combine(isAutoDeleteEnabled, retentionDays) { _, _ -> }
                .collect {
                    refreshStats()
                }
        }
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            val stats = repository.getLocalStorageStats()
            val cleanupTime = sessionManager.lastCleanupTimeFlow.first()
            val deletedCount = sessionManager.lastDeletedCountFlow.first()
            _storageStats.value = stats.copy(
                lastCleanupTime = cleanupTime,
                lastDeletedCount = deletedCount
            )
        }
    }

    fun setAutoDeleteEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setAutoDeleteEnabled(enabled)
            val msg = if (enabled) {
                "Auto-deletion policy activated (${retentionDays.value} days retention)"
            } else {
                "Auto-deletion policy paused"
            }
            _snackbarMessage.emit(msg)
            refreshStats()
        }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            sessionManager.setRetentionDays(days)
            val option = RetentionPeriodOption.fromDays(days)
            _snackbarMessage.emit("Retention policy updated to ${option.label} (${days} days)")
            refreshStats()
        }
    }

    fun executeManualCleanup() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val result: RetentionCleanupResult = repository.executeAutoDeletePolicy(
                    forceDays = retentionDays.value
                )
                refreshStats()
                _snackbarMessage.emit(result.message)
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to execute cleanup: ${e.localizedMessage ?: "Unknown error"}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                val count = repository.clearAllLocalHistory()
                refreshStats()
                _snackbarMessage.emit("Cleared all $count local location coordinates.")
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to clear local history: ${e.localizedMessage}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    class Factory(
        private val repository: GroupTrackRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(RetentionSettingsViewModel::class.java)) {
                return RetentionSettingsViewModel(repository, sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
