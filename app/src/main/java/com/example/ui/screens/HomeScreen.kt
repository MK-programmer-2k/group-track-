package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.core.content.ContextCompat
import com.example.ui.theme.*
import com.example.ui.viewmodel.GroupViewModel
import com.example.ui.viewmodel.LiveMapViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    groupViewModel: GroupViewModel,
    liveMapViewModel: LiveMapViewModel,
    onNavigateToMap: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToMembers: (String) -> Unit,
    onNavigateToSharingSettings: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToGeofences: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val isSharing by liveMapViewModel.isSharing.collectAsState()
    val activeGroupId by groupViewModel.activeGroupId.collectAsState()
    val activeGroupName by groupViewModel.activeGroupName.collectAsState()
    val cachedGroups by groupViewModel.cachedGroups.collectAsState()
    val myLocation by liveMapViewModel.myCurrentLocation.collectAsState()
    val isOffline by liveMapViewModel.isOffline.collectAsState()
    val pendingCount by liveMapViewModel.pendingQueueCount.collectAsState()
    val memberLocations by liveMapViewModel.membersLocations.collectAsState()

    // Pick first group if none active
    LaunchedEffect(cachedGroups, activeGroupId) {
        if (activeGroupId == null && cachedGroups.isNotEmpty()) {
            groupViewModel.selectActiveGroup(cachedGroups[0].id, cachedGroups[0].name)
        }
    }

    LaunchedEffect(activeGroupId) {
        activeGroupId?.let { liveMapViewModel.refreshLocations(it) }
    }

    // Runtime Permission Launcher
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    var showGroupSelectorDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("group_title_row")
                    ) {
                        Column {
                            Text(
                                text = activeGroupName ?: "Select Group",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "GroupTrack Live",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (activeGroupId != null) {
                        IconButton(
                            onClick = { activeGroupId?.let { onNavigateToChat(it) } },
                            modifier = Modifier.testTag("home_chat_icon_button")
                        ) {
                            Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Group Chat")
                        }
                    }
                    IconButton(
                        onClick = { showGroupSelectorDialog = true },
                        modifier = Modifier.testTag("switch_group_button")
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Group")
                    }
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Banner (if not granted)
            AnimatedVisibility(visible = !hasLocationPermission) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.GpsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Location Permission Required",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "GroupTrack requires location access to share live GPS coordinates when you choose to enable sharing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Button(
                            onClick = {
                                val perms = mutableListOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    perms.add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                permissionLauncher.launch(perms.toTypedArray())
                            },
                            modifier = Modifier.testTag("grant_permission_button")
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            // Offline & Sync Banner
            if (isOffline || pendingCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isOffline) "Offline Mode Active" else "Syncing Queued Updates",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF92400E),
                                fontSize = 13.sp
                            )
                            Text(
                                text = if (pendingCount > 0) "$pendingCount updates queued for upload" else "Showing cached member data",
                                color = Color(0xFFB45309),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // PRIMARY HERO CARD: Location Sharing Status
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isSharing) Color(0xFF064E3B) else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sharing_status_card")
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isSharing) EmeraldLight else SlateInactive)
                            )
                            Text(
                                text = if (isSharing) "SHARING LIVE LOCATION" else "LOCATION SHARING OFF",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSharing) EmeraldLight else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = onNavigateToSharingSettings,
                            modifier = Modifier.testTag("sharing_settings_icon")
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Sharing duration and settings",
                                tint = if (isSharing) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Column {
                        Text(
                            text = if (isSharing) "Sharing with ${activeGroupName ?: "group"}" else "You are not sharing your location",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSharing) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isSharing)
                                "Members of ${activeGroupName ?: "this group"} can see your live position on the map."
                            else
                                "Your live position is completely private. Tap below to share with your group.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSharing) Color(0xFFD1FAE5) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // GPS info coordinates
                    if (myLocation != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSharing) Color(0x33000000) else MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.MyLocation,
                                        contentDescription = null,
                                        tint = if (isSharing) EmeraldLight else IndigoPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = String.format("%.4f, %.4f", myLocation!!.latitude, myLocation!!.longitude),
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isSharing) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "±${myLocation!!.accuracy.toInt()}m accuracy",
                                    fontSize = 12.sp,
                                    color = if (isSharing) Color.LightGray else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Main Action Button: Start / Stop
                    Button(
                        onClick = {
                            if (!hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                                return@Button
                            }

                            val gId = activeGroupId ?: return@Button
                            val gName = activeGroupName ?: "Group"

                            if (isSharing) {
                                liveMapViewModel.stopSharingLocation(gId)
                            } else {
                                liveMapViewModel.startSharingLocation(gId, gName, "UNTIL_STOP")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSharing) RoseOffline else IndigoPrimary,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("toggle_sharing_button")
                    ) {
                        Icon(
                            imageVector = if (isSharing) Icons.Default.LocationOff else Icons.Default.LocationOn,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSharing) "STOP SHARING" else "START SHARING LIVE LOCATION",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Group Members Quick Summary & Open Map CTA
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Group Live Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            onClick = { activeGroupId?.let { onNavigateToMembers(it) } },
                            modifier = Modifier.testTag("view_members_link")
                        ) {
                            Text("Manage Members")
                        }
                    }

                    val onlineCount = memberLocations.count { it.isOnline }
                    val activeCount = memberLocations.size

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "$onlineCount",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Online Now",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "$activeCount",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "Sharing Active",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Open Live Map & Chat buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onNavigateToMap,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_live_map_button")
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Live Map", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { activeGroupId?.let { onNavigateToChat(it) } },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("open_group_chat_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary)
                        ) {
                            Icon(Icons.Default.ChatBubble, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Group Chat", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // GEOFENCE ZONES & ALERTS CARD
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nav_geofences_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x1A10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Adjust,
                                    contentDescription = null,
                                    tint = EmeraldLive,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Geofence Zones",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Home, Office & Custom Boundaries",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TextButton(
                            onClick = onNavigateToGeofences,
                            modifier = Modifier.testTag("manage_geofences_link")
                        ) {
                            Text("Manage")
                        }
                    }

                    Text(
                        text = "Set up virtual geofence boundaries for your group to trigger local push notifications whenever members enter or leave designated zones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    FilledTonalButton(
                        onClick = onNavigateToGeofences,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("open_geofences_button")
                    ) {
                        Icon(Icons.Default.ShareLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Configure Zones & Alert Rules", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Navigation Actions Grid (Groups, Create, Join)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    onClick = onNavigateToGroups,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_groups_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("My Groups", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                OutlinedCard(
                    onClick = onNavigateToSharingSettings,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("nav_sharing_timer_card")
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = EmeraldLive,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Duration Timer", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    // Group Switcher Dialog
    if (showGroupSelectorDialog) {
        AlertDialog(
            onDismissRequest = { showGroupSelectorDialog = false },
            title = { Text("Select Active Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cachedGroups.isEmpty()) {
                        Text("No groups found. Please create or join a group.")
                    } else {
                        cachedGroups.forEach { group ->
                            val isSelected = group.id == activeGroupId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                onClick = {
                                    groupViewModel.selectActiveGroup(group.id, group.name)
                                    showGroupSelectorDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(group.name, fontWeight = FontWeight.SemiBold)
                                        Text("${group.memberCount} members", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGroupSelectorDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
