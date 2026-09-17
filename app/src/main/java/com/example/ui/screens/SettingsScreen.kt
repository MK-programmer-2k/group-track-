package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SessionManager
import com.example.data.local.model.RetentionPeriodOption
import com.example.data.remote.api.ApiClient
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseOffline
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.RetentionSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    sessionManager: SessionManager,
    retentionSettingsViewModel: RetentionSettingsViewModel? = null,
    onNavigateToRetentionSettings: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val context = LocalContext.current
    val userName by sessionManager.userNameFlow.collectAsState(initial = null)
    val userEmail by sessionManager.userEmailFlow.collectAsState(initial = null)
    val isAutoDeleteEnabled by sessionManager.autoDeleteEnabledFlow.collectAsState(initial = true)
    val retentionDays by sessionManager.retentionDaysFlow.collectAsState(initial = 14)
    val storageStats by retentionSettingsViewModel?.storageStats?.collectAsState() ?: remember {
        mutableStateOf(com.example.data.local.model.LocalStorageStats())
    }

    var serverUrl by remember { mutableStateOf(ApiClient.getBaseUrl()) }
    var showServerUrlDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Card
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = IndigoPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = IndigoPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(userName ?: "GroupTrack User", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(userEmail ?: "Active Account", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Text("System & Privacy Permissions", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

            // Permissions item
            OutlinedCard(
                onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = IndigoPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Android App Permissions", fontWeight = FontWeight.SemiBold)
                            Text("Manage Location & Notification access", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            // Backend Server Configuration
            OutlinedCard(
                onClick = { showServerUrlDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Dns, contentDescription = null, tint = IndigoPrimary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Backend API Server", fontWeight = FontWeight.SemiBold)
                            Text(serverUrl, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }

            Text("Local Database & Data Privacy", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)

            // Interactive Auto-Deletion Policy Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("auto_deletion_policy_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAutoDeleteEnabled) EmeraldLive.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.AutoDelete,
                                        contentDescription = null,
                                        tint = if (isAutoDeleteEnabled) EmeraldLive else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Auto-Delete Location History", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    if (isAutoDeleteEnabled) "Active (${retentionDays} days retention)" else "Policy Paused",
                                    fontSize = 12.sp,
                                    color = if (isAutoDeleteEnabled) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isAutoDeleteEnabled,
                            onCheckedChange = { checked ->
                                retentionSettingsViewModel?.setAutoDeleteEnabled(checked) ?: run {
                                    kotlinx.coroutines.runBlocking {
                                        sessionManager.setAutoDeleteEnabled(checked)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("settings_auto_delete_switch")
                        )
                    }

                    AnimatedVisibility(visible = isAutoDeleteEnabled) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "Auto-clear Room database coordinates older than:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(7 to "7 Days", 14 to "14 Days", 30 to "30 Days").forEach { (days, label) ->
                                    FilterChip(
                                        selected = retentionDays == days,
                                        onClick = {
                                            retentionSettingsViewModel?.setRetentionDays(days) ?: run {
                                                kotlinx.coroutines.runBlocking {
                                                    sessionManager.setRetentionDays(days)
                                                }
                                            }
                                        },
                                        label = { Text(label, fontSize = 12.sp, fontWeight = if (retentionDays == days) FontWeight.Bold else FontWeight.Normal) },
                                        modifier = Modifier.testTag("settings_retention_chip_${days}d")
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "${storageStats.totalLocationCount} points recorded",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                "Est. Size: ${storageStats.estimatedSizeFormatted}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        FilledTonalButton(
                            onClick = onNavigateToRetentionSettings,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("configure_retention_policy_button")
                        ) {
                            Text("Configure Policy", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Logout Button
            OutlinedButton(
                onClick = {
                    authViewModel.logout()
                    onLoggedOut()
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = RoseOffline),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }

    if (showServerUrlDialog) {
        var tempUrl by remember { mutableStateOf(serverUrl) }
        AlertDialog(
            onDismissRequest = { showServerUrlDialog = false },
            title = { Text("Configure Backend API") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter base API URL (e.g., http://10.0.2.2:3000/ or cloud endpoint):", fontSize = 13.sp)
                    OutlinedTextField(
                        value = tempUrl,
                        onValueChange = { tempUrl = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        serverUrl = tempUrl
                        ApiClient.updateBaseUrl(tempUrl, context, sessionManager)
                        showServerUrlDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
