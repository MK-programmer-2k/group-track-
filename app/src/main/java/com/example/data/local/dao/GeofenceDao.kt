package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.GeofenceEventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceDao {
    @Query("SELECT * FROM geofence_zones WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getGeofencesForGroup(groupId: String): Flow<List<GeofenceZoneEntity>>

    @Query("SELECT * FROM geofence_zones WHERE groupId = :groupId ORDER BY createdAt DESC")
    suspend fun getGeofencesForGroupSync(groupId: String): List<GeofenceZoneEntity>

    @Query("SELECT * FROM geofence_zones WHERE id = :id")
    suspend fun getGeofenceById(id: String): GeofenceZoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofence(geofence: GeofenceZoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGeofences(geofences: List<GeofenceZoneEntity>)

    @Query("DELETE FROM geofence_zones WHERE id = :id")
    suspend fun deleteGeofence(id: String)

    @Query("DELETE FROM geofence_zones WHERE groupId = :groupId")
    suspend fun deleteGeofencesForGroup(groupId: String)

    @Query("SELECT * FROM geofence_event_logs WHERE groupId = :groupId ORDER BY timestamp DESC LIMIT 100")
    fun getEventLogsForGroup(groupId: String): Flow<List<GeofenceEventLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventLog(eventLog: GeofenceEventLogEntity)

    @Query("DELETE FROM geofence_event_logs WHERE groupId = :groupId")
    suspend fun clearEventLogsForGroup(groupId: String)
}
