package com.example.service

import android.content.Context
import android.util.Log
import com.example.data.local.dao.GeofenceDao
import com.example.data.local.entity.GeofenceEventLogEntity
import com.example.data.local.entity.GeofenceZoneEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

object GeofenceTransitionEngine {

    // Tracks last known inside/outside status: key = "userId_geofenceId" -> true (inside) or false (outside)
    private val memberZoneStates = ConcurrentHashMap<String, Boolean>()

    /**
     * Calculates geodesic distance in meters between two coordinate pairs using Haversine formula.
     */
    fun calculateDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val earthRadius = 6371000.0 // meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return (earthRadius * c).toFloat()
    }

    /**
     * Evaluates a member's coordinates against active geofence zones.
     * Fires local notifications and records event logs upon boundary crossing.
     */
    fun evaluateMember(
        context: Context,
        scope: CoroutineScope,
        geofenceDao: GeofenceDao,
        userId: String,
        userName: String,
        latitude: Double,
        longitude: Double,
        zones: List<GeofenceZoneEntity>
    ) {
        for (zone in zones) {
            val distance = calculateDistanceMeters(latitude, longitude, zone.latitude, zone.longitude)
            val isInside = distance <= zone.radiusMeters
            val stateKey = "${userId}_${zone.id}"
            val previousState = memberZoneStates[stateKey]

            if (previousState != null && previousState != isInside) {
                val eventType = if (isInside) "ENTER" else "EXIT"
                val shouldNotify = if (isInside) zone.notifyOnEnter else zone.notifyOnExit

                Log.i(
                    "GeofenceEngine",
                    "Member $userName ($userId) $eventType zone '${zone.name}' (distance: ${distance.toInt()}m, radius: ${zone.radiusMeters}m)"
                )

                if (shouldNotify) {
                    GeofenceNotificationHelper.showTransitionNotification(
                        context = context,
                        geofenceName = zone.name,
                        memberName = userName,
                        eventType = eventType,
                        distanceMeters = distance
                    )
                }

                // Persist event log to database
                scope.launch(Dispatchers.IO) {
                    geofenceDao.insertEventLog(
                        GeofenceEventLogEntity(
                            geofenceId = zone.id,
                            geofenceName = zone.name,
                            groupId = zone.groupId,
                            userId = userId,
                            userName = userName,
                            eventType = eventType,
                            timestamp = System.currentTimeMillis(),
                            distanceMeters = distance
                        )
                    )
                }
            }

            memberZoneStates[stateKey] = isInside
        }
    }

    /**
     * Helper to manually trigger an entry/exit alert simulation for testing & verification.
     */
    fun triggerSimulatedEvent(
        context: Context,
        scope: CoroutineScope,
        geofenceDao: GeofenceDao,
        zone: GeofenceZoneEntity,
        memberName: String,
        eventType: String
    ) {
        GeofenceNotificationHelper.showTransitionNotification(
            context = context,
            geofenceName = zone.name,
            memberName = memberName,
            eventType = eventType,
            distanceMeters = if (eventType == "ENTER") 25f else zone.radiusMeters + 50f
        )

        scope.launch(Dispatchers.IO) {
            geofenceDao.insertEventLog(
                GeofenceEventLogEntity(
                    geofenceId = zone.id,
                    geofenceName = zone.name,
                    groupId = zone.groupId,
                    userId = "simulated_${memberName.lowercase().replace(" ", "_")}",
                    userName = memberName,
                    eventType = eventType,
                    timestamp = System.currentTimeMillis(),
                    distanceMeters = if (eventType == "ENTER") 25f else zone.radiusMeters + 50f
                )
            )
        }
    }

    /**
     * Checks if a given coordinate is within a zone boundary.
     */
    fun isInsideZone(latitude: Double, longitude: Double, zone: GeofenceZoneEntity): Boolean {
        return calculateDistanceMeters(latitude, longitude, zone.latitude, zone.longitude) <= zone.radiusMeters
    }

    fun clearState() {
        memberZoneStates.clear()
    }
}
