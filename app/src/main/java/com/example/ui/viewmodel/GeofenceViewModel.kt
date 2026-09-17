package com.example.ui.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.GroupTrackApplication
import com.example.data.local.entity.GeofenceEventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.service.GeofenceTransitionEngine
import com.example.service.LocationSharingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class GeofenceViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as GroupTrackApplication
    private val repository = app.repository
    private val sessionManager = app.sessionManager
    private val geofenceDao = repository.geofenceDao

    val activeGroupId: StateFlow<String?> = sessionManager.activeGroupIdFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val activeGroupName: StateFlow<String?> = sessionManager.activeGroupNameFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val myCurrentLocation: StateFlow<Location?> = LocationSharingService.currentLocation

    val geofences: StateFlow<List<GeofenceZoneEntity>> = activeGroupId
        .flatMapLatest { gId ->
            if (gId != null) geofenceDao.getGeofencesForGroup(gId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val eventLogs: StateFlow<List<GeofenceEventLogEntity>> = activeGroupId
        .flatMapLatest { gId ->
            if (gId != null) geofenceDao.getEventLogsForGroup(gId) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createGeofence(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        zoneType: String = "CUSTOM",
        colorHex: String = "#10B981",
        notifyOnEnter: Boolean = true,
        notifyOnExit: Boolean = true
    ) {
        val gId = activeGroupId.value ?: return
        val zone = GeofenceZoneEntity(
            id = "geo_${UUID.randomUUID().toString().take(8)}",
            groupId = gId,
            name = name.trim(),
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            zoneType = zoneType,
            colorHex = colorHex,
            notifyOnEnter = notifyOnEnter,
            notifyOnExit = notifyOnExit
        )

        viewModelScope.launch(Dispatchers.IO) {
            repository.addGeofence(zone)
        }
    }

    fun deleteGeofence(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGeofence(id)
        }
    }

    fun clearHistory() {
        val gId = activeGroupId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearGeofenceEvents(gId)
        }
    }

    fun simulateAlert(zone: GeofenceZoneEntity, memberName: String, eventType: String) {
        GeofenceTransitionEngine.triggerSimulatedEvent(
            context = app.applicationContext,
            scope = viewModelScope,
            geofenceDao = geofenceDao,
            zone = zone,
            memberName = memberName,
            eventType = eventType
        )
    }
}
