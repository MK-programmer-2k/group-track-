package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.ui.components.InteractiveMapView
import com.example.ui.components.calculateDistanceMeters
import com.example.ui.theme.IndigoPrimary
import com.example.ui.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationHistoryScreen(
    groupId: String,
    userId: String?,
    historyViewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToRetentionSettings: (() -> Unit)? = null
) {
    val historyPoints by historyViewModel.historyPoints.collectAsState()
    val isLoading by historyViewModel.isLoading.collectAsState()
    val selectedRange by historyViewModel.selectedRange.collectAsState()

    LaunchedEffect(groupId, userId) {
        historyViewModel.loadHistory(groupId, userId, "today")
    }

    // Compute total distance traveled
    val totalDistanceMeters = remember(historyPoints) {
        if (historyPoints.size < 2) 0f
        else {
            var sum = 0f
            for (i in 0 until historyPoints.size - 1) {
                sum += calculateDistanceMeters(
                    historyPoints[i].latitude,
                    historyPoints[i].longitude,
                    historyPoints[i + 1].latitude,
                    historyPoints[i + 1].longitude
                )
            }
            sum
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Location History") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (onNavigateToRetentionSettings != null) {
                        IconButton(
                            onClick = onNavigateToRetentionSettings,
                            modifier = Modifier.testTag("retention_settings_button")
                        ) {
                            Icon(Icons.Default.AutoDelete, contentDescription = "Auto-Deletion Policy")
                        }
                    }
                    if (selectedRange == "local_24h" && historyPoints.isNotEmpty()) {
                        IconButton(
                            onClick = { historyViewModel.clearLocalTrail() },
                            modifier = Modifier.testTag("clear_personal_trail_button")
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Clear Personal Trail")
                        }
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
            // Time Range Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "today" to "Today",
                    "local_24h" to "24h Personal Trail",
                    "yesterday" to "Yesterday",
                    "7days" to "Last 7 Days"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedRange == key,
                        onClick = { historyViewModel.loadHistory(groupId, userId, key) },
                        label = { Text(label) },
                        modifier = Modifier.testTag("history_range_$key")
                    )
                }
            }

            // Stats summary card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Distance", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = String.format("%.2f km", totalDistanceMeters / 1000f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Divider(modifier = Modifier.height(32.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Points", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${historyPoints.size}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Divider(modifier = Modifier.height(32.dp).width(1.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = if (isLoading) "Loading..." else if (historyPoints.isEmpty()) "No data" else "Recorded",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Visual Map route canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                InteractiveMapView(
                    modifier = Modifier.fillMaxSize(),
                    myLocation = null,
                    members = emptyList(),
                    historyPoints = historyPoints
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Route breadcrumb checkpoints
            Text(
                text = "Recorded Timeline Checkpoints",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(horizontal = 16.dp)
            ) {
                if (historyPoints.isEmpty() && !isLoading) {
                    item {
                        Text(
                            text = "No history points recorded for the selected time range.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(historyPoints.takeLast(15).reversed()) { pt ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Navigation,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "${pt.latitude}, ${pt.longitude}",
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = pt.recordedAt.take(19).replace("T", " "),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
