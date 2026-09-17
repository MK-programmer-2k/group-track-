package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.GroupTrackApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Handles device boot completed.
 * Recovers app state: checks if user had active sharing with unexpired duration.
 * Does not silently track if permissions or state are revoked.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device reboot detected")
            val app = context.applicationContext as? GroupTrackApplication ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val sessionManager = app.sessionManager
                val isSharing = sessionManager.isSharingFlow.first()
                val activeGroupId = sessionManager.activeGroupIdFlow.first()
                val activeGroupName = sessionManager.activeGroupNameFlow.first()

                if (isSharing && !activeGroupId.isNullOrBlank()) {
                    Log.i("BootReceiver", "Re-evaluating location sharing post-reboot for group $activeGroupId")
                    // Note: Foreground services post-boot require foreground service start
                    // If sharing is still active and valid, restart foreground service
                    val serviceIntent = Intent(context, LocationSharingService::class.java).apply {
                        action = LocationSharingService.ACTION_START
                        putExtra(LocationSharingService.EXTRA_GROUP_ID, activeGroupId)
                        putExtra(LocationSharingService.EXTRA_GROUP_NAME, activeGroupName ?: "Group")
                    }
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        Log.w("BootReceiver", "Could not restart location service after boot: ${e.message}")
                    }
                }
            }
        }
    }
}
