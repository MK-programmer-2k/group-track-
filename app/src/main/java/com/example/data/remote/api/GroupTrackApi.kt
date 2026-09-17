package com.example.data.remote.api

import com.example.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

interface GroupTrackApi {

    // Auth
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<BaseApiResponse<AuthResponseData>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<BaseApiResponse<AuthResponseData>>

    @POST("auth/logout")
    suspend fun logout(): Response<BaseApiResponse<Any>>

    // Groups
    @GET("groups")
    suspend fun getGroups(): Response<BaseApiResponse<List<GroupDto>>>

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<BaseApiResponse<GroupDto>>

    @GET("groups/{id}")
    suspend fun getGroupDetails(@Path("id") groupId: String): Response<BaseApiResponse<GroupDetailsResponse>>

    @POST("groups/join")
    suspend fun joinGroup(@Body request: JoinGroupRequest): Response<BaseApiResponse<Any>>

    @POST("groups/{id}/approve")
    suspend fun approveMember(
        @Path("id") groupId: String,
        @Body request: ApproveMemberRequest
    ): Response<BaseApiResponse<Any>>

    @DELETE("groups/{id}/members/{userId}")
    suspend fun removeMember(
        @Path("id") groupId: String,
        @Path("userId") userId: String
    ): Response<BaseApiResponse<Any>>

    // Location Sharing
    @POST("location-sharing/start")
    suspend fun startSharing(@Body request: StartSharingRequest): Response<BaseApiResponse<Any>>

    @POST("location-sharing/stop")
    suspend fun stopSharing(@Body request: StopSharingRequest): Response<BaseApiResponse<Any>>

    // Locations
    @POST("locations")
    suspend fun sendLocation(@Body request: LocationPointRequest): Response<BaseApiResponse<Any>>

    @POST("locations/batch")
    suspend fun sendBatchLocations(@Body request: BatchLocationsRequest): Response<BaseApiResponse<Any>>

    @GET("locations/latest")
    suspend fun getLatestLocations(@Query("groupId") groupId: String): Response<BaseApiResponse<List<MemberLatestLocation>>>

    @GET("locations/history")
    suspend fun getLocationHistory(
        @Query("groupId") groupId: String,
        @Query("userId") userId: String? = null,
        @Query("range") range: String = "today"
    ): Response<BaseApiResponse<List<LocationHistoryItem>>>

    // Admin
    @GET("admin/groups/{id}/dashboard")
    suspend fun getAdminDashboard(@Path("id") groupId: String): Response<BaseApiResponse<AdminDashboardStats>>

    // Chat
    @POST("chat/send")
    suspend fun sendChatMessage(@Body request: SendChatMessageRequest): Response<BaseApiResponse<ChatMessageResponseDto>>

    @POST("chat/sync-batch")
    suspend fun syncChatBatch(@Body request: SyncChatBatchRequest): Response<BaseApiResponse<SyncChatBatchResponse>>

    @GET("chat/messages")
    suspend fun getChatMessages(
        @Query("groupId") groupId: String,
        @Query("since") sinceTimestamp: Long? = null
    ): Response<BaseApiResponse<List<ChatMessageResponseDto>>>
}

