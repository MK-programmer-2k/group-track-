package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.CachedGroupDao
import com.example.data.local.dao.CachedMemberLocationDao
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.GeofenceDao
import com.example.data.local.dao.PendingLocationDao
import com.example.data.local.dao.UserLocationTrailDao
import com.example.data.local.entity.CachedGroupEntity
import com.example.data.local.entity.CachedMemberLocationEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.GeofenceEventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.local.entity.PendingLocationEntity
import com.example.data.local.entity.UserLocationTrailEntity

@Database(
    entities = [
        PendingLocationEntity::class,
        CachedGroupEntity::class,
        CachedMemberLocationEntity::class,
        GeofenceZoneEntity::class,
        GeofenceEventLogEntity::class,
        UserLocationTrailEntity::class,
        ChatMessageEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingLocationDao(): PendingLocationDao
    abstract fun cachedGroupDao(): CachedGroupDao
    abstract fun cachedMemberLocationDao(): CachedMemberLocationDao
    abstract fun geofenceDao(): GeofenceDao
    abstract fun userLocationTrailDao(): UserLocationTrailDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "grouptrack.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
