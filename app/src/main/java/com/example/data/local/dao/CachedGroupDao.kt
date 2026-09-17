package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.CachedGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedGroupDao {
    @Query("SELECT * FROM cached_groups ORDER BY updatedAt DESC")
    fun getAllGroupsFlow(): Flow<List<CachedGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<CachedGroupEntity>)

    @Query("SELECT * FROM cached_groups WHERE id = :groupId LIMIT 1")
    suspend fun getGroupById(groupId: String): CachedGroupEntity?

    @Query("DELETE FROM cached_groups")
    suspend fun clearAll()
}
