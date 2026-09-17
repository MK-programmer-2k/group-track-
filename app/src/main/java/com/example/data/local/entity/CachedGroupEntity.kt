package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_groups")
data class CachedGroupEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val description: String?,
    val inviteCode: String,
    val myRole: String,
    val memberCount: Int,
    val activeSharersCount: Int,
    val updatedAt: Long = System.currentTimeMillis()
)
