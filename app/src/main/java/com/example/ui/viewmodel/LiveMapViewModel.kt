package com.example.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.remote.model.MemberLatestLocation
import com.example.data.remote.socket.SocketManager
import com.example.data.repository.GroupTrackRepository
import com.example.service.GeofenceTransitionEngine
import com.example.service.LocationSharingService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class LiveMapViewModel(
    private val context: Context,
    private val repository: GroupTrackRepository,
    private val sessionManager: SessionManager,
    private val socketManager: SocketManager
) : ViewModel() {

    val isSharing = sessionManager.isSharingFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeGroupId = sessionManager.activeGroupIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeGroupName = sessionManager.activeGroupNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val myCurrentLocation: StateFlow<Location?> = LocationSharingService.currentLocation

    val geofences: StateFlow<List<GeofenceZoneEntity>> = activeGroupId
        .flatMapLatest { gId ->
            if (gId != null) repository.getGeofencesFlow(gId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _membersLocations = MutableStateFlow<List<MemberLatestLocation>>(emptyList())
    val membersLocations: StateFlow<List<MemberLatestLocation>> = _membersLocations.asStateFlow()

    private val _selectedMember = MutableStateFlow<MemberLatestLocation?>(null)
    val selectedMember: StateFlow<MemberLatestLocation?> = _selectedMember.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    val pendingQueueCount = repository.pendingCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        // Observe realtime updates from WebSocket
        viewModelScope.launch {
            socketManager.locationUpdates.collect { incoming ->
                val current = _membersLocations.value.toMutableList()
                val idx = current.indexOfFirst { it.userId == incoming.userId }
                if (idx >= 0) {
                    current[idx] = incoming
                } else {
                    current.add(incoming)
                }
                _membersLocations.value = current

                // Evaluate geofence boundaries for incoming member update
                checkMemberGeofence(incoming.userId, incoming.userName, incoming.latitude, incoming.longitude)
            }
        }

        // Also check geofences for current user when device GPS updates
        viewModelScope.launch {
            myCurrentLocation.collect { loc ->
                if (loc != null) {
                    val uid = sessionManager.getUserId() ?: "me"
                    val uname = sessionManager.getUserName() ?: "You"
                    checkMemberGeofence(uid, uname, loc.latitude, loc.longitude)
                }
            }
        }
    }

    private fun checkMemberGeofence(userId: String, userName: String, lat: Double, lng: Double) {
        val zones = geofences.value
        if (zones.isNotEmpty()) {
            GeofenceTransitionEngine.evaluateMember(
                context = context,
                scope = viewModelScope,
                geofenceDao = repository.geofenceDao,
                userId = userId,
                userName = userName,
                latitude = lat,
                longitude = lng,
                zones = zones
            )
        }
    }

    fun startSharingLocation(groupId: String, groupName: String, duration: String) {
        viewModelScope.launch {
            repository.startSharing(groupId, duration)
            sessionManager.setActiveGroup(groupId, groupName)

            // Start Android Foreground Service
            val intent = Intent(context, LocationSharingService::class.java).apply {
                action = LocationSharingService.ACTION_START
                putExtra(LocationSharingService.EXTRA_GROUP_ID, groupId)
                putExtra(LocationSharingService.EXTRA_GROUP_NAME, groupName)
                putExtra(LocationSharingService.EXTRA_DURATION, duration)
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            // Connect WebSocket for group
            socketManager.connect(groupId)
            refreshLocations(groupId)
        }
    }

    fun stopSharingLocation(groupId: String) {
        viewModelScope.launch {
            repository.stopSharing(groupId)
            val intent = Intent(context, LocationSharingService::class.java).apply {
                action = LocationSharingService.ACTION_STOP
            }
            context.startService(intent)
            socketManager.disconnect()
            refreshLocations(groupId)
        }
    }

    fun refreshLocations(groupId: String) {
        viewModelScope.launch {
            val result = repository.fetchLatestLocations(groupId)
            result.onSuccess {
                _membersLocations.value = it
                _isOffline.value = false
            }.onFailure {
                _isOffline.value = true
                // Fallback to local Room cached member locations
                repository.getMemberLocationsFlow(groupId).firstOrNull()?.let { cachedList ->
                    _membersLocations.value = cachedList.map { c ->
                        MemberLatestLocation(
                            userId = c.userId,
                            userName = c.userName,
                            groupId = c.groupId,
                            latitude = c.latitude,
                            longitude = c.longitude,
                            accuracy = c.accuracy,
                            speed = c.speed,
                            bearing = c.bearing,
                            recordedAt = c.recordedAt,
                            isLive = c.isLive,
                            isOnline = c.isOnline,
                            diffSeconds = 999L
                        )
                    }
                }
            }
        }
    }

    fun selectMember(member: MemberLatestLocation?) {
        _selectedMember.value = member
    }

    override fun onCleared() {
        super.onCleared()
        socketManager.disconnect()
    }

    class Factory(
        private val context: Context,
        private val repository: GroupTrackRepository,
        private val sessionManager: SessionManager,
        private val socketManager: SocketManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LiveMapViewModel(context, repository, sessionManager, socketManager) as T
        }
    }
}
