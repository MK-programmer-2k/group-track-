package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SendChatMessageRequest(
    @Json(name = "messageId") val messageId: String,
    @Json(name = "groupId") val groupId: String,
    @Json(name = "messageText") val messageText: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "attachedLatitude") val attachedLatitude: Double? = null,
    @Json(name = "attachedLongitude") val attachedLongitude: Double? = null,
    @Json(name = "attachedLocationLabel") val attachedLocationLabel: String? = null
)

@JsonClass(generateAdapter = true)
data class ChatMessageResponseDto(
    @Json(name = "messageId") val messageId: String,
    @Json(name = "groupId") val groupId: String,
    @Json(name = "senderId") val senderId: String,
    @Json(name = "senderName") val senderName: String,
    @Json(name = "messageText") val messageText: String,
    @Json(name = "timestamp") val timestamp: Long,
    @Json(name = "attachedLatitude") val attachedLatitude: Double? = null,
    @Json(name = "attachedLongitude") val attachedLongitude: Double? = null,
    @Json(name = "attachedLocationLabel") val attachedLocationLabel: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncChatBatchRequest(
    @Json(name = "messages") val messages: List<SendChatMessageRequest>
)

@JsonClass(generateAdapter = true)
data class SyncChatBatchResponse(
    @Json(name = "syncedMessageIds") val syncedMessageIds: List<String>,
    @Json(name = "failedMessageIds") val failedMessageIds: List<String> = emptyList()
)
