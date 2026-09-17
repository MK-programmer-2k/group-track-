package com.example.ui.screens

import android.location.Location
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GeofenceEventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.GeofenceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeofenceManagementScreen(
    viewModel: GeofenceViewModel,
    onNavigateBack: () -> Unit
) {
    val geofences by viewModel.geofences.collectAsState()
    val eventLogs by viewModel.eventLogs.collectAsState()
    val activeGroupName by viewModel.activeGroupName.collectAsState()
    val myLocation by viewModel.myCurrentLocation.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var simulateTargetZone by remember { mutableStateOf<GeofenceZoneEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Geofence Zones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = activeGroupName ?: "Active Group",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedTabIndex == 1 && eventLogs.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.testTag("clear_geofence_logs_button")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null) },
                    text = { Text("Add Zone") },
                    containerColor = EmeraldLive,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_geofence_fab")
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs: Zones list vs Activity logs
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Adjust, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Zones (${geofences.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_zones")
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Alert Logs (${eventLogs.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_alerts")
                )
            }

            if (selectedTabIndex == 0) {
                // Zones Tab
                if (geofences.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.ShareLocation,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "No Geofence Zones Defined",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Create virtual boundaries like Home, Office, or Campus to receive instant entry and exit alerts for group members.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.testTag("empty_add_zone_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create First Zone")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Sensors,
                                        contentDescription = null,
                                        tint = EmeraldLive,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Active Geofence Monitoring",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Local notifications trigger automatically whenever group members enter or leave these zones.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        items(geofences, key = { it.id }) { zone ->
                            GeofenceZoneCard(
                                zone = zone,
                                onDelete = { viewModel.deleteGeofence(zone.id) },
                                onTestAlert = { simulateTargetZone = zone }
                            )
                        }

                        // Bottom padding for FAB
                        item { Spacer(modifier = Modifier.height(72.dp)) }
                    }
                }
            } else {
                // Event Logs Tab
                if (eventLogs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.NotificationsNone,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "No Transition Events Yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "When members enter or leave designated zones, activity alerts will be recorded here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(eventLogs, key = { it.id }) { log ->
                            GeofenceEventLogCard(log = log)
                        }
                    }
                }
            }
        }
    }

    // Create Geofence Dialog
    if (showCreateDialog) {
        CreateGeofenceDialog(
            myLocation = myLocation,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, lat, lng, radius, type, color, notifyEnter, notifyExit ->
                viewModel.createGeofence(
                    name = name,
                    latitude = lat,
                    longitude = lng,
                    radiusMeters = radius,
                    zoneType = type,
                    colorHex = color,
                    notifyOnEnter = notifyEnter,
                    notifyOnExit = notifyExit
                )
                showCreateDialog = false
            }
        )
    }

    // Simulate Alert Dialog
    simulateTargetZone?.let { targetZone ->
        SimulateAlertDialog(
            zone = targetZone,
            onDismiss = { simulateTargetZone = null },
            onTrigger = { memberName, eventType ->
                viewModel.simulateAlert(targetZone, memberName, eventType)
                simulateTargetZone = null
            }
        )
    }
}

@Composable
private fun GeofenceZoneCard(
    zone: GeofenceZoneEntity,
    onDelete: () -> Unit,
    onTestAlert: () -> Unit
) {
    val zoneColor = remember(zone.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(zone.colorHex))
        } catch (_: Exception) {
            Color(0xFF10B981)
        }
    }

    val typeIcon = when (zone.zoneType.uppercase()) {
        "HOME" -> Icons.Default.Home
        "OFFICE" -> Icons.Default.Business
        "CAMPUS" -> Icons.Default.School
        "GYM" -> Icons.Default.FitnessCenter
        else -> Icons.Default.Place
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("geofence_card_${zone.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(zoneColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = zoneColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = zone.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${zone.zoneType} • Radius: ${zone.radiusMeters.toInt()} meters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_zone_${zone.id}")
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Delete zone",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Coordinates Row
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GPS: ${String.format("%.4f, %.4f", zone.latitude, zone.longitude)}",
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (zone.notifyOnEnter) {
                            Text("🟢 Enter Alert", fontSize = 11.sp, color = EmeraldLive, fontWeight = FontWeight.Medium)
                        }
                        if (zone.notifyOnExit) {
                            Text("🔴 Exit Alert", fontSize = 11.sp, color = RoseOffline, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Test simulation CTA
            OutlinedButton(
                onClick = onTestAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .testTag("test_alert_button_${zone.id}"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Icon(Icons.Default.NotificationAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simulate Entry/Exit Alert", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GeofenceEventLogCard(log: GeofenceEventLogEntity) {
    val isEnter = log.eventType.equals("ENTER", ignoreCase = true)
    val statusColor = if (isEnter) EmeraldLive else RoseOffline
    val timeFormat = remember { SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEnter) Icons.Default.Login else Icons.Default.Logout,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.userName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = timeFormat.format(Date(log.timestamp)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = if (isEnter) "Entered zone: ${log.geofenceName}" else "Left zone: ${log.geofenceName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun CreateGeofenceDialog(
    myLocation: Location?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, lat: Double, lng: Double, radius: Float, type: String, color: String, notifyEnter: Boolean, notifyExit: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("HOME") }
    var latText by remember { mutableStateOf(myLocation?.latitude?.toString() ?: "13.0827") }
    var lngText by remember { mutableStateOf(myLocation?.longitude?.toString() ?: "80.2707") }
    var radiusMeters by remember { mutableFloatStateOf(150f) }
    var selectedColor by remember { mutableStateOf("#10B981") }
    var notifyOnEnter by remember { mutableStateOf(true) }
    var notifyOnExit by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val zoneTypes = listOf("HOME" to "Home 🏠", "OFFICE" to "Office 🏢", "CAMPUS" to "Campus 🎓", "GYM" to "Gym 🏋️", "CUSTOM" to "Custom 📍")
    val colorPresets = listOf(
        "#10B981" to "Green",
        "#3B82F6" to "Blue",
        "#8B5CF6" to "Purple",
        "#EC4899" to "Pink",
        "#F59E0B" to "Amber"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Define Custom Geofence Zone", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Zone Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("Zone Name (e.g. Home, HQ)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("geofence_name_input")
                )

                // Category selection
                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    zoneTypes.take(4).forEach { (typeKey, label) ->
                        FilterChip(
                            selected = selectedType == typeKey,
                            onClick = {
                                selectedType = typeKey
                                if (name.isBlank() || name in zoneTypes.map { it.first }) {
                                    name = typeKey.lowercase().replaceFirstChar { it.uppercase() }
                                }
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Coordinates with Current Location autofill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Boundary Center", style = MaterialTheme.typography.labelMedium)
                    if (myLocation != null) {
                        TextButton(
                            onClick = {
                                latText = myLocation.latitude.toString()
                                lngText = myLocation.longitude.toString()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Use Current GPS", fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = latText,
                        onValueChange = { latText = it },
                        label = { Text("Latitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = lngText,
                        onValueChange = { lngText = it },
                        label = { Text("Longitude") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Radius selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Radius: ${radiusMeters.toInt()} meters", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(50f, 150f, 300f, 500f).forEach { r ->
                            AssistChip(
                                onClick = { radiusMeters = r },
                                label = { Text("${r.toInt()}m", fontSize = 10.sp) }
                            )
                        }
                    }
                }
                Slider(
                    value = radiusMeters,
                    onValueChange = { radiusMeters = it },
                    valueRange = 50f..1000f,
                    steps = 18
                )

                // Color choices
                Text("Color Accent", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colorPresets.forEach { (hex, _) ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Notification toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify when member arrives (Enter)")
                    Switch(checked = notifyOnEnter, onCheckedChange = { notifyOnEnter = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Notify when member leaves (Exit)")
                    Switch(checked = notifyOnExit, onCheckedChange = { notifyOnExit = it })
                }

                errorMsg?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Please enter a zone name"
                        return@Button
                    }
                    val lat = latText.toDoubleOrNull()
                    val lng = lngText.toDoubleOrNull()
                    if (lat == null || lng == null) {
                        errorMsg = "Please enter valid coordinates"
                        return@Button
                    }
                    onConfirm(name, lat, lng, radiusMeters, selectedType, selectedColor, notifyOnEnter, notifyOnExit)
                },
                modifier = Modifier.testTag("confirm_create_geofence_button")
            ) {
                Text("Create Zone")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SimulateAlertDialog(
    zone: GeofenceZoneEntity,
    onDismiss: () -> Unit,
    onTrigger: (memberName: String, eventType: String) -> Unit
) {
    var memberName by remember { mutableStateOf("Manikandan") }
    var eventType by remember { mutableStateOf("ENTER") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Simulate Zone Transition", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Test local notifications and event logging for zone '${zone.name}' without waiting for physical movement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = { Text("Member Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Transition Type", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = eventType == "ENTER",
                        onClick = { eventType = "ENTER" },
                        label = { Text("🟢 Arrive (Enter)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = eventType == "EXIT",
                        onClick = { eventType = "EXIT" },
                        label = { Text("🔴 Leave (Exit)") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onTrigger(memberName, eventType) },
                modifier = Modifier.testTag("trigger_simulation_button")
            ) {
                Text("Fire Notification")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
