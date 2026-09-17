package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_member_locations")
data class CachedMemberLocationEntity(
    @PrimaryKey
    val id: String, // composite: groupId_userId
    val groupId: String,
    val userId: String,
    val userName: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?,
    val speed: Float?,
    val bearing: Float?,
    val recordedAt: String,
    val isLive: Boolean,
    val isOnline: Boolean,
    val updatedAt: Long = System.currentTimeMillis()
)
