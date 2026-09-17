package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a simplified trail coordinate of the user's own personal movement
 * over the last 24 hours for personal review and offline retrospective.
 */
@Entity(
    tableName = "user_location_trail",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["userId", "timestamp"])
    ]
)
data class UserLocationTrailEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "me",
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val speed: Float? = null,
    val altitude: Double? = null,
    val bearing: Float? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val recordedAt: String = ""
)
