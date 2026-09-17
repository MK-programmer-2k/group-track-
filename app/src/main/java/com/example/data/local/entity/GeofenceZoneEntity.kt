package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a custom geofence boundary defined for a tracking group.
 */
@Entity(tableName = "geofence_zones")
data class GeofenceZoneEntity(
    @PrimaryKey
    val id: String,
    val groupId: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val zoneType: String = "CUSTOM", // HOME, OFFICE, CAMPUS, GYM, CUSTOM
    val colorHex: String = "#10B981",
    val notifyOnEnter: Boolean = true,
    val notifyOnExit: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
