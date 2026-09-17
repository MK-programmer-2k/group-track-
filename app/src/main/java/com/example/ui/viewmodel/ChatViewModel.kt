package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SessionManager
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MessageSyncStatus
import com.example.data.repository.GroupTrackRepository
import com.example.util.NetworkConnectivityObserver
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ChatViewModel(
    private val context: Context,
    private val repository: GroupTrackRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _activeGroupId = MutableStateFlow<String?>(null)
    val activeGroupId: StateFlow<String?> = _activeGroupId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    val isOnline: StateFlow<Boolean> = NetworkConnectivityObserver.observeConnectivity(context)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val pendingChatCount: StateFlow<Int> = repository.pendingChatCountFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    init {
        // Automatically sync queued Room database messages as soon as connectivity returns
        viewModelScope.launch {
            isOnline.collect { online ->
                if (online) {
                    val synced = repository.syncPendingChatMessages()
                    if (synced > 0) {
                        _snackbarEvent.emit("Network restored: Synced $synced offline message(s) via Retrofit")
                    }
                }
            }
        }
    }

    fun setGroup(groupId: String) {
        if (_activeGroupId.value == groupId) return
        _activeGroupId.value = groupId

        viewModelScope.launch {
            repository.getChatMessagesFlow(groupId).collect { list ->
                _messages.value = list
            }
        }

        // Try syncing pending messages whenever group opens
        viewModelScope.launch {
            val synced = repository.syncPendingChatMessages()
            if (synced > 0) {
                _snackbarEvent.emit("Synced $synced offline chat message(s)")
            }
        }
    }

    fun sendMessage(text: String, lat: Double? = null, lng: Double? = null, label: String? = null) {
        val groupId = _activeGroupId.value ?: return
        if (text.isBlank() && lat == null) return

        viewModelScope.launch {
            _isSending.value = true
            try {
                val sent = repository.sendChatMessage(
                    groupId = groupId,
                    messageText = text,
                    attachedLatitude = lat,
                    attachedLongitude = lng,
                    attachedLocationLabel = label
                )
                if (sent.syncStatus == MessageSyncStatus.PENDING_SEND) {
                    _snackbarEvent.emit("Saved offline. Will deliver when network is restored.")
                }
            } catch (e: Exception) {
                _snackbarEvent.emit("Failed to save message: ${e.localizedMessage}")
            } finally {
                _isSending.value = false
            }
        }
    }

    fun shareCurrentLocation(lat: Double, lng: Double, label: String = "My Current Location") {
        sendMessage(
            text = "📍 Shared location: $label",
            lat = lat,
            lng = lng,
            label = label
        )
    }

    fun syncPending() {
        viewModelScope.launch {
            val count = repository.syncPendingChatMessages()
            if (count > 0) {
                _snackbarEvent.emit("Delivered $count pending message(s)")
            } else {
                _snackbarEvent.emit("All messages are up-to-date")
            }
        }
    }

    fun clearGroupChat() {
        val groupId = _activeGroupId.value ?: return
        viewModelScope.launch {
            repository.clearChatForGroup(groupId)
            _snackbarEvent.emit("Cleared conversation history")
        }
    }

    class Factory(
        private val context: Context,
        private val repository: GroupTrackRepository,
        private val sessionManager: SessionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                return ChatViewModel(context, repository, sessionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
