package com.example.service

import android.location.Location

object LocationUpdateStrategy {
    const val DESIRED_INTERVAL_MILLIS = 30_000L // 30 seconds
    const val MIN_INTERVAL_MILLIS = 15_000L     // 15 seconds
    const val MIN_DISPLACEMENT_METERS = 25.0f   // 25 meters
    const val MAX_ALLOWED_ACCURACY_METERS = 150.0f // Reject points worse than 150m accuracy
    const val MAX_STALE_TIMESTAMP_AGE_MILLIS = 5 * 60 * 1000L // 5 minutes

    fun isValidLocation(location: Location): Boolean {
        // 1. Coordinate range check
        if (location.latitude < -90.0 || location.latitude > 90.0) return false
        if (location.longitude < -180.0 || location.longitude > 180.0) return false
        if (location.latitude == 0.0 && location.longitude == 0.0) return false

        // 2. Accuracy check (reject impossible/coarse accuracy)
        if (location.hasAccuracy() && location.accuracy > MAX_ALLOWED_ACCURACY_METERS) {
            return false
        }

        // 3. Stale timestamp check
        val age = System.currentTimeMillis() - location.time
        if (age > MAX_STALE_TIMESTAMP_AGE_MILLIS) {
            return false
        }

        return true
    }

    fun isSignificantMove(lastLocation: Location?, newLocation: Location): Boolean {
        if (lastLocation == null) return true

        val distance = lastLocation.distanceTo(newLocation)
        // If moved beyond threshold, it is significant
        if (distance >= MIN_DISPLACEMENT_METERS) return true

        // If stationary (speed < 0.5 m/s) and moved less than 15m, don't ping server repeatedly
        if (newLocation.hasSpeed() && newLocation.speed < 0.5f && distance < 15.0f) {
            val timeDiff = newLocation.time - lastLocation.time
            // Only report stationary ping at most once every 2 minutes for heartbeat
            return timeDiff >= 120_000L
        }

        return distance >= MIN_DISPLACEMENT_METERS
    }
}
