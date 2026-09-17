package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CachedMemberLocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedMemberLocationDao {
    @Query("SELECT * FROM cached_member_locations WHERE groupId = :groupId ORDER BY userName ASC")
    fun getLocationsForGroup(groupId: String): Flow<List<CachedMemberLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<CachedMemberLocationEntity>)

    @Query("DELETE FROM cached_member_locations WHERE groupId = :groupId")
    suspend fun clearGroupLocations(groupId: String)

    @Query("DELETE FROM cached_member_locations")
    suspend fun clearAll()
}
