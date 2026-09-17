package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Message sync status for offline/online capability
 */
enum class MessageSyncStatus {
    PENDING_SEND, // Queued locally while offline or awaiting server ack
    SENT,         // Dispatched to server or received from network
    DELIVERED,    // Delivered / acknowledged
    FAILED        // Delivery failure
}

/**
 * Room database entity storing chat messages for groups and individual members.
 * Works fully offline and synchronizes when network is available.
 */
@Entity(
    tableName = "chat_messages",
    indices = [
        Index(value = ["groupId"]),
        Index(value = ["timestamp"]),
        Index(value = ["syncStatus"])
    ]
)
data class ChatMessageEntity(
    @PrimaryKey
    val messageId: String,
    val groupId: String,
    val senderId: String,
    val senderName: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean = false,
    val syncStatus: MessageSyncStatus = MessageSyncStatus.SENT,
    val attachedLatitude: Double? = null,
    val attachedLongitude: Double? = null,
    val attachedLocationLabel: String? = null
)
