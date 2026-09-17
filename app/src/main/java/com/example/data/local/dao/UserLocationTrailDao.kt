package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.UserLocationTrailEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for managing the user's personal 24-hour location trail history.
 */
@Dao
interface UserLocationTrailDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrailPoint(point: UserLocationTrailEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrailPoints(points: List<UserLocationTrailEntity>)

    /**
     * Retrieves the trail coordinates recorded over the requested window (e.g. past 24 hours),
     * sorted chronologically for path rendering.
     */
    @Query("SELECT * FROM user_location_trail WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getTrailLast24HoursFlow(sinceTimestamp: Long): Flow<List<UserLocationTrailEntity>>

    /**
     * Synchronous fetch of trail points within the time window.
     */
    @Query("SELECT * FROM user_location_trail WHERE timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getTrailLast24Hours(sinceTimestamp: Long): List<UserLocationTrailEntity>

    /**
     * Query trail for a specific userId within the time window.
     */
    @Query("SELECT * FROM user_location_trail WHERE userId = :userId AND timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    fun getUserTrailFlow(userId: String, sinceTimestamp: Long): Flow<List<UserLocationTrailEntity>>

    @Query("SELECT * FROM user_location_trail WHERE userId = :userId AND timestamp >= :sinceTimestamp ORDER BY timestamp ASC")
    suspend fun getUserTrail(userId: String, sinceTimestamp: Long): List<UserLocationTrailEntity>

    /**
     * Retrieves recent trail entries limited to latest N points.
     */
    @Query("SELECT * FROM user_location_trail ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTrailFlow(limit: Int = 300): Flow<List<UserLocationTrailEntity>>

    /**
     * Prunes coordinates older than the specified cutoff timestamp (e.g., now - 24 hours).
     */
    @Query("DELETE FROM user_location_trail WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneTrailOlderThan(cutoffTimestamp: Long): Int

    /**
     * Clears all trail records for privacy reset.
     */
    @Query("DELETE FROM user_location_trail WHERE userId = :userId")
    suspend fun clearTrailForUser(userId: String)

    @Query("DELETE FROM user_location_trail")
    suspend fun clearAll()

    /**
     * Counts the number of recorded trail points in the last 24 hours.
     */
    @Query("SELECT COUNT(*) FROM user_location_trail WHERE timestamp >= :sinceTimestamp")
    fun getTrailCountSinceFlow(sinceTimestamp: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_location_trail WHERE timestamp >= :sinceTimestamp")
    suspend fun getTrailCountSince(sinceTimestamp: Long): Int

    /**
     * Total number of trail records currently stored in Room database.
     */
    @Query("SELECT COUNT(*) FROM user_location_trail")
    fun getTotalTrailCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_location_trail")
    suspend fun getTotalTrailCount(): Int

    @Query("SELECT MIN(timestamp) FROM user_location_trail")
    suspend fun getOldestTrailTimestamp(): Long?

    @Query("SELECT MAX(timestamp) FROM user_location_trail")
    suspend fun getNewestTrailTimestamp(): Long?

    /**
     * Counts the number of points that are older than the cutoff timestamp (eligible for auto-deletion).
     */
    @Query("SELECT COUNT(*) FROM user_location_trail WHERE timestamp < :cutoffTimestamp")
    suspend fun getCountOlderThan(cutoffTimestamp: Long): Int

    /**
     * Fetches the most recent trail coordinate recorded.
     */
    @Query("SELECT * FROM user_location_trail ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestTrailPoint(): UserLocationTrailEntity?
}
