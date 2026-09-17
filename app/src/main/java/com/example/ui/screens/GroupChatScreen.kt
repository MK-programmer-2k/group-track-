package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.MessageSyncStatus
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.GroupViewModel
import com.example.ui.viewmodel.LiveMapViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    groupId: String,
    chatViewModel: ChatViewModel,
    groupViewModel: GroupViewModel,
    liveMapViewModel: LiveMapViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val messages by chatViewModel.messages.collectAsState()
    val isSending by chatViewModel.isSending.collectAsState()
    val pendingChatCount by chatViewModel.pendingChatCount.collectAsState()
    val myLocation by liveMapViewModel.myCurrentLocation.collectAsState()
    val isOnline by chatViewModel.isOnline.collectAsState()
    val isOffline = !isOnline

    val groups by groupViewModel.cachedGroups.collectAsState()
    val currentGroupName = remember(groups, groupId) {
        groups.find { it.id == groupId }?.name ?: "Group Chat"
    }

    var textInput by remember { mutableStateOf("") }
    var showClearChatDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(groupId) {
        chatViewModel.setGroup(groupId)
    }

    LaunchedEffect(Unit) {
        chatViewModel.snackbarEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Scroll to bottom when messages count changes
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val timeFormatter = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault())
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(currentGroupName, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isOffline) Color(0xFFF43F5E) else EmeraldLive)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isOffline) "Offline (Local Queue Enabled)" else "Online & Synced",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pendingChatCount > 0) {
                        IconButton(
                            onClick = { chatViewModel.syncPending() },
                            modifier = Modifier.testTag("chat_sync_pending_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    Badge { Text("$pendingChatCount") }
                                }
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync Offline Messages")
                            }
                        }
                    }
                    IconButton(
                        onClick = onNavigateToMap,
                        modifier = Modifier.testTag("chat_open_map_button")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = "Open Map")
                    }
                    IconButton(
                        onClick = { showClearChatDialog = true },
                        modifier = Modifier.testTag("chat_clear_button")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Chat")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Offline Notification Banner
            AnimatedVisibility(visible = isOffline || pendingChatCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudQueue,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isOffline) "Offline Mode: Messages stored locally in SQLite" else "$pendingChatCount message(s) awaiting network sync",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        if (!isOffline && pendingChatCount > 0) {
                            TextButton(
                                onClick = { chatViewModel.syncPending() },
                                modifier = Modifier.testTag("sync_offline_now_btn")
                            ) {
                                Text("Sync Now", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Message List
            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = IndigoPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(32.dp))
                            }
                        }
                        Text("No messages yet", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Chat with members in $currentGroupName. Messages work offline and automatically sync when online.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.messageId }) { msg ->
                        ChatMessageBubble(
                            message = msg,
                            timeFormatter = timeFormatter,
                            onLocationClick = { lat, lng ->
                                val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Member Location)")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    onNavigateToMap()
                                }
                            }
                        )
                    }
                }
            }

            // Input Bar
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share Location Shortcut Button
                    IconButton(
                        onClick = {
                            myLocation?.let {
                                chatViewModel.shareCurrentLocation(it.latitude, it.longitude)
                            }
                        },
                        enabled = myLocation != null && !isSending,
                        modifier = Modifier.testTag("chat_share_location_btn")
                    ) {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = "Share Location",
                            tint = if (myLocation != null) EmeraldLive else MaterialTheme.colorScheme.outline
                        )
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Message ${currentGroupName}...", fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .testTag("chat_input_field"),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                chatViewModel.sendMessage(textInput)
                                textInput = ""
                            }
                        },
                        enabled = textInput.isNotBlank() && !isSending,
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (textInput.isNotBlank()) IndigoPrimary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    if (showClearChatDialog) {
        AlertDialog(
            onDismissRequest = { showClearChatDialog = false },
            title = { Text("Clear Chat History?") },
            text = { Text("This will remove all local messages stored for $currentGroupName.", fontSize = 13.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearChatDialog = false
                        chatViewModel.clearGroupChat()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessageEntity,
    timeFormatter: SimpleDateFormat,
    onLocationClick: (Double, Double) -> Unit
) {
    val isMe = message.isFromMe
    val alignment = if (isMe) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_bubble_${message.messageId}"),
        horizontalAlignment = alignment
    ) {
        if (!isMe) {
            Text(
                text = message.senderName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 12.dp, bottom = 2.dp)
            )
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Attached GPS Location Chip if present
                if (message.attachedLatitude != null && message.attachedLongitude != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isMe) Color(0x33FFFFFF) else MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLocationClick(message.attachedLatitude, message.attachedLongitude)
                            }
                            .testTag("chat_location_attachment_${message.messageId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = null,
                                tint = if (isMe) Color.White else IndigoPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = message.attachedLocationLabel ?: "Pinned Location",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = String.format(Locale.US, "%.4f, %.4f", message.attachedLatitude, message.attachedLongitude),
                                    fontSize = 10.sp,
                                    color = if (isMe) Color(0xCCFFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Text(
                    text = message.messageText,
                    fontSize = 14.sp,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = timeFormatter.format(Date(message.timestamp)),
                        fontSize = 10.sp,
                        color = if (isMe) Color(0xB3FFFFFF) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )

                    if (isMe) {
                        when (message.syncStatus) {
                            MessageSyncStatus.PENDING_SEND -> {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = "Pending Sync (Offline)",
                                    tint = Color(0xB3FFFFFF),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageSyncStatus.SENT -> {
                                Icon(
                                    Icons.Default.Done,
                                    contentDescription = "Sent",
                                    tint = Color(0xB3FFFFFF),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageSyncStatus.DELIVERED -> {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = "Delivered",
                                    tint = Color(0xFF93C5FD),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            MessageSyncStatus.FAILED -> {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = "Failed",
                                    tint = Color(0xFFFCA5A5),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
