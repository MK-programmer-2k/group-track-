package com.example.data.remote.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroupDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String?,
    @Json(name = "inviteCode") val inviteCode: String,
    @Json(name = "myRole") val myRole: String?,
    @Json(name = "memberCount") val memberCount: Int?,
    @Json(name = "activeSharersCount") val activeSharersCount: Int?
)

@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String?
)

@JsonClass(generateAdapter = true)
data class JoinGroupRequest(
    @Json(name = "inviteCode") val inviteCode: String
)

@JsonClass(generateAdapter = true)
data class MemberDto(
    @Json(name = "id") val id: String,
    @Json(name = "userId") val userId: String,
    @Json(name = "name") val name: String,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: String,
    @Json(name = "status") val status: String,
    @Json(name = "joinedAt") val joinedAt: String?,
    @Json(name = "isSharing") val isSharing: Boolean,
    @Json(name = "lastSeenAt") val lastSeenAt: String?
)

@JsonClass(generateAdapter = true)
data class GroupDetailsResponse(
    @Json(name = "group") val group: GroupDto,
    @Json(name = "myRole") val myRole: String,
    @Json(name = "members") val members: List<MemberDto>
)

@JsonClass(generateAdapter = true)
data class ApproveMemberRequest(
    @Json(name = "userId") val userId: String
)

@JsonClass(generateAdapter = true)
data class AdminDashboardStats(
    @Json(name = "groupName") val groupName: String,
    @Json(name = "inviteCode") val inviteCode: String,
    @Json(name = "totalMembers") val totalMembers: Int,
    @Json(name = "pendingRequests") val pendingRequests: Int,
    @Json(name = "activeSharers") val activeSharers: Int,
    @Json(name = "myRole") val myRole: String
)
