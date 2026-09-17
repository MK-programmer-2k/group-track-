package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class StartSharingRequest(
    @Json(name = "groupId") val groupId: String,
    @Json(name = "duration") val duration: String = "UNTIL_STOP"
)

@JsonClass(generateAdapter = true)
data class StopSharingRequest(
    @Json(name = "groupId") val groupId: String
)

@JsonClass(generateAdapter = true)
data class LocationPointRequest(
    @Json(name = "groupId") val groupId: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "accuracy") val accuracy: Float?,
    @Json(name = "altitude") val altitude: Double?,
    @Json(name = "speed") val speed: Float?,
    @Json(name = "bearing") val bearing: Float?,
    @Json(name = "recordedAt") val recordedAt: String
)

@JsonClass(generateAdapter = true)
data class BatchLocationsRequest(
    @Json(name = "locations") val locations: List<LocationPointRequest>
)

@JsonClass(generateAdapter = true)
data class MemberLatestLocation(
    @Json(name = "userId") val userId: String,
    @Json(name = "userName") val userName: String,
    @Json(name = "groupId") val groupId: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "accuracy") val accuracy: Float?,
    @Json(name = "speed") val speed: Float?,
    @Json(name = "bearing") val bearing: Float?,
    @Json(name = "recordedAt") val recordedAt: String?,
    @Json(name = "isLive") val isLive: Boolean,
    @Json(name = "isOnline") val isOnline: Boolean,
    @Json(name = "diffSeconds") val diffSeconds: Long?
)

@JsonClass(generateAdapter = true)
data class LocationHistoryItem(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "groupId") val groupId: String,
    @Json(name = "latitude") val latitude: Double,
    @Json(name = "longitude") val longitude: Double,
    @Json(name = "accuracy") val accuracy: Float?,
    @Json(name = "speed") val speed: Float?,
    @Json(name = "bearing") val bearing: Float?,
    @Json(name = "recordedAt") val recordedAt: String
)
