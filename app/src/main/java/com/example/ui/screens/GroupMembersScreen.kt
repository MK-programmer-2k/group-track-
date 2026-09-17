package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.model.MemberDto
import com.example.ui.theme.EmeraldLive
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.RoseOffline
import com.example.ui.theme.SlateInactive
import com.example.ui.viewmodel.GroupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupMembersScreen(
    groupId: String,
    groupViewModel: GroupViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val details by groupViewModel.selectedGroupDetails.collectAsState()
    val adminStats by groupViewModel.adminStats.collectAsState()

    LaunchedEffect(groupId) {
        groupViewModel.loadGroupDetails(groupId)
        groupViewModel.loadAdminStats(groupId)
    }

    var memberToRemove by remember { mutableStateOf<MemberDto?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.group?.name ?: "Group Members") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { onNavigateToChat(groupId) },
                        modifier = Modifier.testTag("members_chat_button")
                    ) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Open Chat")
                    }
                    IconButton(onClick = {
                        groupViewModel.loadGroupDetails(groupId)
                        groupViewModel.loadAdminStats(groupId)
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group Invite Code Card
            details?.group?.let { group ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Group Invite Code",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = group.inviteCode,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp
                                )
                                FilledTonalButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Group Invite Code", group.inviteCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Invite code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("copy_invite_code_button")
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy")
                                }
                            }
                            Text(
                                text = "Share this code with friends so they can join ${group.name}.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }

            // Admin Dashboard Card (if user is ADMIN/OWNER)
            adminStats?.let { stats ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = IndigoPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Admin Overview", fontWeight = FontWeight.Bold)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Members: ${stats.totalMembers}", fontSize = 13.sp)
                                Text("Active Sharers: ${stats.activeSharers}", fontSize = 13.sp, color = EmeraldLive, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Members Header
            item {
                Text(
                    text = "Members (${details?.members?.size ?: 0})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Members List
            details?.members?.let { members ->
                items(members) { m ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = IndigoPrimary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            m.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            color = IndigoPrimary
                                        )
                                    }
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(m.name, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (m.role == "OWNER") Color(0xFFE0E7FF) else Color(0xFFF1F5F9)
                                        ) {
                                            Text(
                                                m.role,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (m.role == "OWNER") IndigoPrimary else Color.DarkGray,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (m.isSharing) EmeraldLive else SlateInactive)
                                        )
                                        Text(
                                            text = if (m.isSharing) "Location Sharing ON" else "Sharing Inactive",
                                            fontSize = 11.sp,
                                            color = if (m.isSharing) EmeraldLive else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Pending Approval or Remove Member
                            val myRole = details?.myRole
                            val canManage = myRole == "OWNER" || myRole == "ADMIN"

                            if (m.status == "PENDING" && canManage) {
                                Button(
                                    onClick = { groupViewModel.approveMember(groupId, m.userId) },
                                    modifier = Modifier.testTag("approve_btn_${m.userId}")
                                ) {
                                    Text("Approve", fontSize = 12.sp)
                                }
                            } else if (canManage && m.role != "OWNER") {
                                IconButton(
                                    onClick = { memberToRemove = m },
                                    modifier = Modifier.testTag("remove_btn_${m.userId}")
                                ) {
                                    Icon(Icons.Default.PersonRemove, contentDescription = "Remove member", tint = RoseOffline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for removing member
    memberToRemove?.let { m ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text("Remove Member") },
            text = { Text("Are you sure you want to remove ${m.name} from this group? Their location access will be revoked immediately.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        groupViewModel.removeMember(groupId, m.userId)
                        memberToRemove = null
                    }
                ) {
                    Text("Remove", color = RoseOffline)
                }
            },
            dismissButton = {
                TextButton(onClick = { memberToRemove = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
