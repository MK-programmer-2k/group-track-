package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PendingLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingLocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: PendingLocationEntity): Long

    @Query("SELECT * FROM pending_locations ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingBatch(limit: Int = 50): List<PendingLocationEntity>

    @Query("DELETE FROM pending_locations WHERE id IN (:ids)")
    suspend fun deleteLocationsByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM pending_locations")
    fun getPendingCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_locations")
    suspend fun getPendingCount(): Int

    // Limit queue size to sensible maximum (1000)
    @Query("DELETE FROM pending_locations WHERE id NOT IN (SELECT id FROM pending_locations ORDER BY createdAt DESC LIMIT 1000)")
    suspend fun purgeOldPendingLocations()
}
