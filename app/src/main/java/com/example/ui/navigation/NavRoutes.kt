package com.example.ui.navigation

object NavRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val LIVE_MAP = "live_map"
    const val GROUPS = "groups"
    const val CREATE_GROUP = "create_group"
    const val JOIN_GROUP = "join_group"
    const val GROUP_MEMBERS = "group_members/{groupId}"
    const val LOCATION_HISTORY = "location_history/{groupId}?userId={userId}"
    const val SHARING_SETTINGS = "sharing_settings"
    const val SETTINGS = "settings"
    const val GEOFENCES = "geofences"
    const val DATA_RETENTION = "data_retention"
    const val GROUP_CHAT = "group_chat/{groupId}"

    fun groupMembers(groupId: String) = "group_members/$groupId"
    fun groupChat(groupId: String) = "group_chat/$groupId"
    fun locationHistory(groupId: String, userId: String? = null) =
        if (userId != null) "location_history/$groupId?userId=$userId" else "location_history/$groupId"
}
