package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MessageSyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getMessagesForGroupFlow(groupId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE groupId = :groupId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(groupId: String, limit: Int = 50): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE syncStatus = :status ORDER BY timestamp ASC")
    suspend fun getPendingMessages(status: MessageSyncStatus = MessageSyncStatus.PENDING_SEND): List<ChatMessageEntity>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE syncStatus = :status")
    fun getPendingCountFlow(status: MessageSyncStatus = MessageSyncStatus.PENDING_SEND): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)

    @Query("UPDATE chat_messages SET syncStatus = :newStatus WHERE messageId = :messageId")
    suspend fun updateSyncStatus(messageId: String, newStatus: MessageSyncStatus)

    @Query("DELETE FROM chat_messages WHERE groupId = :groupId")
    suspend fun deleteMessagesForGroup(groupId: String)

    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneMessagesOlderThan(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM chat_messages WHERE groupId = :groupId")
    suspend fun getMessageCount(groupId: String): Int
}
