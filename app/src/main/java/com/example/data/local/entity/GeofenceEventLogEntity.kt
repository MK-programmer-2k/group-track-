package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity logging geofence boundary crossings (entry or exit) by group members.
 */
@Entity(tableName = "geofence_event_logs")
data class GeofenceEventLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val geofenceId: String,
    val geofenceName: String,
    val groupId: String,
    val userId: String,
    val userName: String,
    val eventType: String, // "ENTER" or "EXIT"
    val timestamp: Long = System.currentTimeMillis(),
    val distanceMeters: Float = 0f
)
