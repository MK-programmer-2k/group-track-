package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "grouptrack_prefs")

class SessionManager(private val context: Context) {

    companion object {
        val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_ACTIVE_GROUP_ID = stringPreferencesKey("active_group_id")
        val KEY_ACTIVE_GROUP_NAME = stringPreferencesKey("active_group_name")
        val KEY_IS_SHARING = booleanPreferencesKey("is_sharing_active")
        val KEY_SHARING_DURATION = stringPreferencesKey("sharing_duration")
        val KEY_SHARING_EXPIRY = longPreferencesKey("sharing_expiry_time")
        val KEY_LAST_UPDATE_TIME = longPreferencesKey("last_location_update_time")
        val KEY_BATTERY_SAVER_WARN_DISMISSED = booleanPreferencesKey("battery_warn_dismissed")

        // Auto-deletion policy preferences for Room local location history
        val KEY_AUTO_DELETE_ENABLED = booleanPreferencesKey("auto_delete_history_enabled")
        val KEY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
        val KEY_LAST_CLEANUP_TIME = longPreferencesKey("history_last_cleanup_time")
        val KEY_LAST_DELETED_COUNT = intPreferencesKey("history_last_deleted_count")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val userNameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_NAME] }
    val userEmailFlow: Flow<String?> = context.dataStore.data.map { it[KEY_USER_EMAIL] }
    val activeGroupIdFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ACTIVE_GROUP_ID] }
    val activeGroupNameFlow: Flow<String?> = context.dataStore.data.map { it[KEY_ACTIVE_GROUP_NAME] }
    val isSharingFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_SHARING] ?: false }
    val lastUpdateTimeFlow: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_UPDATE_TIME] ?: 0L }

    val autoDeleteEnabledFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_DELETE_ENABLED] ?: true }
    val retentionDaysFlow: Flow<Int> = context.dataStore.data.map { it[KEY_RETENTION_DAYS] ?: 14 }
    val lastCleanupTimeFlow: Flow<Long> = context.dataStore.data.map { it[KEY_LAST_CLEANUP_TIME] ?: 0L }
    val lastDeletedCountFlow: Flow<Int> = context.dataStore.data.map { it[KEY_LAST_DELETED_COUNT] ?: 0 }

    suspend fun saveAuth(token: String, userId: String, name: String, email: String) {
        context.dataStore.edit {
            it[KEY_ACCESS_TOKEN] = token
            it[KEY_USER_ID] = userId
            it[KEY_USER_NAME] = name
            it[KEY_USER_EMAIL] = email
        }
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.map { it[KEY_ACCESS_TOKEN] }.first()
    }

    suspend fun getUserId(): String? {
        return context.dataStore.data.map { it[KEY_USER_ID] }.first()
    }

    suspend fun getUserName(): String? {
        return context.dataStore.data.map { it[KEY_USER_NAME] }.first()
    }

    suspend fun getActiveGroupId(): String? {
        return context.dataStore.data.map { it[KEY_ACTIVE_GROUP_ID] }.first()
    }

    suspend fun getActiveGroupName(): String? {
        return context.dataStore.data.map { it[KEY_ACTIVE_GROUP_NAME] }.first()
    }

    suspend fun setActiveGroup(groupId: String, groupName: String) {
        context.dataStore.edit {
            it[KEY_ACTIVE_GROUP_ID] = groupId
            it[KEY_ACTIVE_GROUP_NAME] = groupName
        }
    }

    suspend fun setSharingState(isSharing: Boolean, duration: String = "UNTIL_STOP", expiryMillis: Long? = null) {
        context.dataStore.edit {
            it[KEY_IS_SHARING] = isSharing
            it[KEY_SHARING_DURATION] = duration
            if (expiryMillis != null) {
                it[KEY_SHARING_EXPIRY] = expiryMillis
            } else {
                it.remove(KEY_SHARING_EXPIRY)
            }
        }
    }

    suspend fun recordLocationUpdate(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit {
            it[KEY_LAST_UPDATE_TIME] = timestamp
        }
    }

    suspend fun setAutoDeleteEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[KEY_AUTO_DELETE_ENABLED] = enabled
        }
    }

    suspend fun setRetentionDays(days: Int) {
        context.dataStore.edit {
            it[KEY_RETENTION_DAYS] = days
        }
    }

    suspend fun recordCleanup(timestamp: Long = System.currentTimeMillis(), deletedCount: Int = 0) {
        context.dataStore.edit {
            it[KEY_LAST_CLEANUP_TIME] = timestamp
            it[KEY_LAST_DELETED_COUNT] = deletedCount
        }
    }

    suspend fun isAutoDeleteEnabled(): Boolean {
        return context.dataStore.data.map { it[KEY_AUTO_DELETE_ENABLED] ?: true }.first()
    }

    suspend fun getRetentionDays(): Int {
        return context.dataStore.data.map { it[KEY_RETENTION_DAYS] ?: 14 }.first()
    }

    suspend fun clearSession() {
        context.dataStore.edit {
            it.clear()
        }
    }
}
