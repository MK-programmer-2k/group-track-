package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.InteractiveMapView
import com.example.ui.theme.EmeraldLive
import com.example.ui.viewmodel.GroupViewModel
import com.example.ui.viewmodel.LiveMapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen(
    groupViewModel: GroupViewModel,
    liveMapViewModel: LiveMapViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (groupId: String, userId: String?) -> Unit,
    onNavigateToGeofences: (groupId: String) -> Unit = {},
    onNavigateToChat: (groupId: String) -> Unit = {}
) {
    val activeGroupId by groupViewModel.activeGroupId.collectAsState()
    val activeGroupName by groupViewModel.activeGroupName.collectAsState()
    val myLocation by liveMapViewModel.myCurrentLocation.collectAsState()
    val members by liveMapViewModel.membersLocations.collectAsState()
    val geofences by liveMapViewModel.geofences.collectAsState()
    val selectedMember by liveMapViewModel.selectedMember.collectAsState()
    val isSharing by liveMapViewModel.isSharing.collectAsState()
    val isOffline by liveMapViewModel.isOffline.collectAsState()

    LaunchedEffect(activeGroupId) {
        activeGroupId?.let { liveMapViewModel.refreshLocations(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = activeGroupName ?: "Live Map",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isOffline) "Offline - Cached" else "${members.size} active sharers • ${geofences.size} zones",
                                fontSize = 12.sp,
                                color = if (isOffline) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            activeGroupId?.let { onNavigateToGeofences(it) }
                        },
                        modifier = Modifier.testTag("geofences_button")
                    ) {
                        Icon(Icons.Default.Adjust, contentDescription = "Geofence Zones", tint = Color(0xFF10B981))
                    }
                    IconButton(
                        onClick = {
                            activeGroupId?.let { onNavigateToChat(it) }
                        },
                        modifier = Modifier.testTag("live_map_chat_button")
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Group Chat")
                    }
                    IconButton(
                        onClick = { activeGroupId?.let { liveMapViewModel.refreshLocations(it) } },
                        modifier = Modifier.testTag("refresh_locations_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(
                        onClick = {
                            activeGroupId?.let { onNavigateToHistory(it, null) }
                        },
                        modifier = Modifier.testTag("group_history_button")
                    ) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Interactive Vector Map Visualizer
            InteractiveMapView(
                modifier = Modifier.fillMaxSize(),
                myLocation = myLocation,
                members = members,
                geofences = geofences,
                selectedMember = selectedMember,
                onMemberClick = { liveMapViewModel.selectMember(it) }
            )

            // Bottom Floating Members Tray
            if (members.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Group Members on Map",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap member to inspect",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            members.forEach { m ->
                                val isSelected = selectedMember?.userId == m.userId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { liveMapViewModel.selectMember(m) },
                                    label = { Text(m.userName) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.PersonPinCircle,
                                            contentDescription = null,
                                            tint = if (m.isOnline) EmeraldLive else Color.Gray,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    modifier = Modifier.testTag("member_chip_${m.userId}")
                                )
                            }
                        }
                    }
                }
            }

            // Member Details Bottom Sheet (when selected)
            selectedMember?.let { member ->
                MemberDetailsBottomSheet(
                    member = member,
                    myLocation = myLocation,
                    onDismiss = { liveMapViewModel.selectMember(null) },
                    onViewHistory = { userId ->
                        activeGroupId?.let { gId ->
                            onNavigateToHistory(gId, userId)
                        }
                    }
                )
            }
        }
    }
}
