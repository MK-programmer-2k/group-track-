package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.GroupViewModel
import com.example.ui.viewmodel.LiveMapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSharingSettingsScreen(
    groupViewModel: GroupViewModel,
    liveMapViewModel: LiveMapViewModel,
    onNavigateBack: () -> Unit
) {
    val activeGroupId by groupViewModel.activeGroupId.collectAsState()
    val activeGroupName by groupViewModel.activeGroupName.collectAsState()
    val isSharing by liveMapViewModel.isSharing.collectAsState()

    var selectedDuration by remember { mutableStateOf("UNTIL_STOP") }

    val durationOptions = listOf(
        Triple("UNTIL_STOP", "Share until I stop", "Continuous sharing until you explicitly tap stop"),
        Triple("ONE_HOUR", "Share for 1 hour", "Automatically stops sharing after 60 minutes"),
        Triple("FOUR_HOURS", "Share for 4 hours", "Stops sharing after 4 hours"),
        Triple("END_OF_DAY", "Share until end of day", "Automatically concludes at 11:59 PM tonight")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sharing Preferences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Privacy & Consent Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Explicit Consent Policy",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Your live GPS position is only shared with authorized members of '${activeGroupName ?: "your active group"}' while sharing is active. You can stop sharing at any second.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Duration Selection
            Text(
                text = "Location Sharing Duration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    durationOptions.forEach { (key, title, subtitle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedDuration == key,
                                    onClick = { selectedDuration = key }
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDuration == key,
                                onClick = { selectedDuration = key },
                                modifier = Modifier.testTag("duration_radio_$key")
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Battery Optimization Note
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = EmeraldLive,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Smart Battery Saver", fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "GroupTrack automatically pauses frequent GPS pings when you are stationary and throttles updates based on displacement (25m minimum).",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action: Start or Update
            Button(
                onClick = {
                    val gId = activeGroupId ?: return@Button
                    val gName = activeGroupName ?: "Group"
                    liveMapViewModel.startSharingLocation(gId, gName, selectedDuration)
                    onNavigateBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_sharing_preferences_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSharing) "Update Duration & Continue Sharing" else "Start Sharing with Duration")
            }
        }
    }
}
