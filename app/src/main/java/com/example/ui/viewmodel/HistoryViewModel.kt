package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.UserLocationTrailEntity
import com.example.data.remote.model.LocationHistoryItem
import com.example.data.repository.GroupTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: GroupTrackRepository
) : ViewModel() {

    private val _historyPoints = MutableStateFlow<List<LocationHistoryItem>>(emptyList())
    val historyPoints: StateFlow<List<LocationHistoryItem>> = _historyPoints.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedRange = MutableStateFlow("today")
    val selectedRange: StateFlow<String> = _selectedRange.asStateFlow()

    // 24-hour local Room trail
    val local24hTrail: StateFlow<List<UserLocationTrailEntity>> = repository
        .getPersonalTrail24HoursFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun loadHistory(groupId: String, userId: String?, range: String = "today") {
        _selectedRange.value = range
        viewModelScope.launch {
            _isLoading.value = true

            if (range == "local_24h") {
                loadPersonalLocalTrail(groupId)
                _isLoading.value = false
                return@launch
            }

            val result = repository.fetchLocationHistory(groupId, userId, range)
            result.onSuccess { points ->
                if (points.isNotEmpty() || userId != null) {
                    _historyPoints.value = points
                } else {
                    // Fallback to local 24-hour Room trail for personal review
                    loadPersonalLocalTrail(groupId)
                }
            }.onFailure {
                // Offline fallback to local 24-hour Room trail
                loadPersonalLocalTrail(groupId)
            }
            _isLoading.value = false
        }
    }

    private suspend fun loadPersonalLocalTrail(groupId: String) {
        val localPoints = repository.getPersonalTrail24Hours()
        val mapped = localPoints.map {
            LocationHistoryItem(
                id = it.id.toString(),
                userId = it.userId,
                groupId = groupId,
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                speed = it.speed,
                bearing = it.bearing,
                recordedAt = it.recordedAt.ifBlank {
                    java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(it.timestamp))
                }
            )
        }
        _historyPoints.value = mapped
    }

    fun clearLocalTrail() {
        viewModelScope.launch {
            repository.clearPersonalTrail()
            if (_selectedRange.value == "local_24h") {
                _historyPoints.value = emptyList()
            }
        }
    }

    class Factory(private val repository: GroupTrackRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HistoryViewModel(repository) as T
        }
    }
}
