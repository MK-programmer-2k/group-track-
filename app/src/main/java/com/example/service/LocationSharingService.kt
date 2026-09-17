package com.example.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.GroupTrackApplication
import com.example.MainActivity
import com.example.R
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class LocationSharingService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_GROUP_ID = "EXTRA_GROUP_ID"
        const val EXTRA_GROUP_NAME = "EXTRA_GROUP_NAME"
        const val EXTRA_DURATION = "EXTRA_DURATION"

        const val CHANNEL_ID = "grouptrack_location_channel"
        const val NOTIFICATION_ID = 1001

        private val _currentLocation = kotlinx.coroutines.flow.MutableStateFlow<Location?>(null)
        val currentLocation = _currentLocation
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var activeGroupId: String = ""
    private var activeGroupName: String = "Group"
    private var sharingDuration: String = "UNTIL_STOP"
    private var expiryTimeMillis: Long? = null

    private var lastReportedLocation: Location? = null
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (loc in result.locations) {
                    handleNewLocation(loc)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                activeGroupId = intent.getStringExtra(EXTRA_GROUP_ID) ?: ""
                activeGroupName = intent.getStringExtra(EXTRA_GROUP_NAME) ?: "Group"
                sharingDuration = intent.getStringExtra(EXTRA_DURATION) ?: "UNTIL_STOP"
                computeExpiryTime(sharingDuration)

                startForegroundServiceNotification()
                startLocationUpdates()
                startExpiryTimerIfNeeded()
            }
            ACTION_STOP -> {
                stopLocationSharing()
            }
        }
        return START_STICKY
    }

    private fun computeExpiryTime(duration: String) {
        val now = System.currentTimeMillis()
        expiryTimeMillis = when (duration) {
            "ONE_HOUR" -> now + 3600_000L
            "FOUR_HOURS" -> now + 4 * 3600_000L
            "END_OF_DAY" -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                }
                cal.timeInMillis
            }
            else -> null
        }
    }

    private fun startExpiryTimerIfNeeded() {
        expiryTimeMillis?.let { expiry ->
            serviceScope.launch {
                val delayTime = expiry - System.currentTimeMillis()
                if (delayTime > 0) {
                    delay(delayTime)
                    Log.i("LocationService", "Sharing duration expired. Stopping sharing.")
                    postExpirationNotification()
                    stopLocationSharing()
                }
            }
        }
    }

    private fun startForegroundServiceNotification() {
        val stopIntent = Intent(this, LocationSharingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activityIntent = Intent(this, MainActivity::class.java)
        val activityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("GroupTrack Live Sharing Active")
            .setContentText("Sharing live location with $activeGroupName")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setContentIntent(activityPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Sharing", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            LocationUpdateStrategy.DESIRED_INTERVAL_MILLIS
        ).apply {
            setMinUpdateIntervalMillis(LocationUpdateStrategy.MIN_INTERVAL_MILLIS)
            setMinUpdateDistanceMeters(LocationUpdateStrategy.MIN_DISPLACEMENT_METERS)
            setWaitForAccurateLocation(false)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("LocationService", "Requested FusedLocationProviderClient updates")
        } catch (e: SecurityException) {
            Log.e("LocationService", "Missing location permissions: ${e.message}")
            stopSelf()
        }
    }

    private fun handleNewLocation(location: Location) {
        if (!LocationUpdateStrategy.isValidLocation(location)) {
            Log.d("LocationService", "Rejected invalid location point")
            return
        }

        if (!LocationUpdateStrategy.isSignificantMove(lastReportedLocation, location)) {
            Log.d("LocationService", "Suppressed stationary non-significant update")
            return
        }

        lastReportedLocation = location
        _currentLocation.value = location

        val app = application as? GroupTrackApplication ?: return
        val repo = app.repository
        val recordedAtStr = isoDateFormat.format(Date(location.time))

        serviceScope.launch {
            repo.submitLocation(
                groupId = activeGroupId,
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = if (location.hasAccuracy()) location.accuracy else null,
                altitude = if (location.hasAltitude()) location.altitude else null,
                speed = if (location.hasSpeed()) location.speed else null,
                bearing = if (location.hasBearing()) location.bearing else null,
                recordedAt = recordedAtStr
            )

            // Record simplified personal location trail in local Room DB
            repo.recordUserTrailPoint(
                latitude = location.latitude,
                longitude = location.longitude,
                accuracy = if (location.hasAccuracy()) location.accuracy else null,
                speed = if (location.hasSpeed()) location.speed else null,
                altitude = if (location.hasAltitude()) location.altitude else null,
                bearing = if (location.hasBearing()) location.bearing else null,
                recordedAt = recordedAtStr
            )
            // Execute user-defined auto-deletion policy (e.g., 7, 14, or 30 days)
            repo.executeAutoDeletePolicy()

            // Geofence boundary evaluation during live location sharing
            if (activeGroupId.isNotEmpty()) {
                val zones = repo.geofenceDao.getGeofencesForGroupSync(activeGroupId)
                if (zones.isNotEmpty()) {
                    val uid = app.sessionManager.getUserId() ?: "me"
                    val uname = app.sessionManager.getUserName() ?: "You"
                    GeofenceTransitionEngine.evaluateMember(
                        context = this@LocationSharingService,
                        scope = serviceScope,
                        geofenceDao = repo.geofenceDao,
                        userId = uid,
                        userName = uname,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        zones = zones
                    )
                }
            }
        }
    }

    private fun postExpirationNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Sharing Ended")
            .setContentText("Your scheduled sharing period has concluded.")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()
        manager.notify(1002, notif)
    }

    private fun stopLocationSharing() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        val app = application as? GroupTrackApplication
        app?.let {
            serviceScope.launch {
                it.repository.stopSharing(activeGroupId)
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Location Sharing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing notification displayed while live group location sharing is enabled"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
