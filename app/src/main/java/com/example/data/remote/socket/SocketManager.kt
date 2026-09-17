package com.example.data.remote.socket

import android.util.Log
import com.example.data.local.SessionManager
import com.example.data.remote.api.ApiClient
import com.example.data.remote.model.MemberLatestLocation
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SocketManager(private val sessionManager: SessionManager) {

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val locationAdapter = moshi.adapter(MemberLatestLocation::class.java)

    private val _locationUpdates = MutableSharedFlow<MemberLatestLocation>(extraBufferCapacity = 64)
    val locationUpdates: SharedFlow<MemberLatestLocation> = _locationUpdates.asSharedFlow()

    private val _connectionState = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val connectionState: SharedFlow<Boolean> = _connectionState.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var currentGroupId: String? = null

    fun connect(groupId: String) {
        currentGroupId = groupId
        disconnect()

        scope.launch {
            val token = sessionManager.getAccessToken() ?: ""
            val httpUrl = ApiClient.getBaseUrl()
            val wsUrl = httpUrl.replace("http://", "ws://").replace("https://", "wss://") + "socket.io/?EIO=4&transport=websocket"

            val request = Request.Builder()
                .url(wsUrl)
                .header("Authorization", "Bearer $token")
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d("SocketManager", "Connected to realtime location stream")
                    _connectionState.tryEmit(true)
                    // Join group room message
                    val joinMsg = JSONObject().apply {
                        put("event", "join_group")
                        put("data", groupId)
                    }.toString()
                    ws.send(joinMsg)
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleSocketMessage(text)
                }

                override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                    Log.d("SocketManager", "Realtime stream closing: $reason")
                    _connectionState.tryEmit(false)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.w("SocketManager", "Realtime stream failure: ${t.message}")
                    _connectionState.tryEmit(false)
                }
            })
        }
    }

    private fun handleSocketMessage(text: String) {
        try {
            if (text.startsWith("42")) { // Socket.io event frame
                val arrayStr = text.substring(2)
                val jsonArray = org.json.JSONArray(arrayStr)
                if (jsonArray.length() >= 2) {
                    val eventName = jsonArray.getString(0)
                    if (eventName == "location_update") {
                        val payload = jsonArray.getJSONObject(1).toString()
                        val memberLoc = locationAdapter.fromJson(payload)
                        if (memberLoc != null) {
                            _locationUpdates.tryEmit(memberLoc)
                        }
                    }
                }
            } else if (text.contains("latitude") && text.contains("longitude")) {
                val memberLoc = locationAdapter.fromJson(text)
                if (memberLoc != null) {
                    _locationUpdates.tryEmit(memberLoc)
                }
            }
        } catch (e: Exception) {
            Log.e("SocketManager", "Failed to parse websocket frame: ${e.message}")
        }
    }

    fun disconnect() {
        try {
            webSocket?.close(1000, "User left group or stopped sharing")
            webSocket = null
            _connectionState.tryEmit(false)
        } catch (e: Exception) {
            Log.e("SocketManager", "Error closing socket: ${e.message}")
        }
    }
}
