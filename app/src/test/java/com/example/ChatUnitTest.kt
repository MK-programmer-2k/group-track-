package com.example

import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MessageSyncStatus
import com.example.data.remote.model.SendChatMessageRequest
import com.example.data.remote.model.SyncChatBatchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUnitTest {

    @Test
    fun testChatMessageOfflineEntityCreation() {
        val now = System.currentTimeMillis()
        val offlineMsg = ChatMessageEntity(
            messageId = "msg_test_1",
            groupId = "group_alpha",
            senderId = "user_me",
            senderName = "Alex",
            messageText = "Heading to campsite, see you soon!",
            timestamp = now,
            isFromMe = true,
            syncStatus = MessageSyncStatus.PENDING_SEND,
            attachedLatitude = 37.7749,
            attachedLongitude = -122.4194,
            attachedLocationLabel = "Campsite Base"
        )

        assertEquals("group_alpha", offlineMsg.groupId)
        assertEquals(MessageSyncStatus.PENDING_SEND, offlineMsg.syncStatus)
        assertTrue(offlineMsg.isFromMe)
        assertNotNull(offlineMsg.attachedLatitude)
        assertEquals(37.7749, offlineMsg.attachedLatitude!!, 0.0001)
    }

    @Test
    fun testChatMessageDeliveredStatus() {
        val msg = ChatMessageEntity(
            messageId = "msg_test_2",
            groupId = "group_alpha",
            senderId = "user_bob",
            senderName = "Bob",
            messageText = "Acknowledged! Tracking you on map.",
            syncStatus = MessageSyncStatus.DELIVERED
        )

        assertEquals(MessageSyncStatus.DELIVERED, msg.syncStatus)
        assertEquals("Bob", msg.senderName)
    }

    @Test
    fun testOfflineQueuedMessageToBatchPayloadConversion() {
        val pending = listOf(
            ChatMessageEntity(
                messageId = "msg_offline_1",
                groupId = "group_101",
                senderId = "user_1",
                senderName = "Alex",
                messageText = "Message offline 1",
                timestamp = 1000L,
                syncStatus = MessageSyncStatus.PENDING_SEND
            ),
            ChatMessageEntity(
                messageId = "msg_offline_2",
                groupId = "group_101",
                senderId = "user_1",
                senderName = "Alex",
                messageText = "Message offline 2 with GPS",
                timestamp = 2000L,
                syncStatus = MessageSyncStatus.PENDING_SEND,
                attachedLatitude = 40.7128,
                attachedLongitude = -74.0060,
                attachedLocationLabel = "NYC"
            )
        )

        val batchRequests = pending.map {
            SendChatMessageRequest(
                messageId = it.messageId,
                groupId = it.groupId,
                messageText = it.messageText,
                timestamp = it.timestamp,
                attachedLatitude = it.attachedLatitude,
                attachedLongitude = it.attachedLongitude,
                attachedLocationLabel = it.attachedLocationLabel
            )
        }
        val syncBatch = SyncChatBatchRequest(batchRequests)

        assertEquals(2, syncBatch.messages.size)
        assertEquals("msg_offline_1", syncBatch.messages[0].messageId)
        assertEquals("msg_offline_2", syncBatch.messages[1].messageId)
        assertEquals(40.7128, syncBatch.messages[1].attachedLatitude!!, 0.0001)
    }
}
