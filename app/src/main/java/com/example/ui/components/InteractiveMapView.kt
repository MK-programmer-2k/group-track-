package com.example.ui.components

import android.location.Location
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GeofenceZoneEntity
import com.example.data.remote.model.LocationHistoryItem
import com.example.data.remote.model.MemberLatestLocation
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun InteractiveMapView(
    modifier: Modifier = Modifier,
    myLocation: Location?,
    members: List<MemberLatestLocation>,
    geofences: List<GeofenceZoneEntity> = emptyList(),
    historyPoints: List<LocationHistoryItem> = emptyList(),
    selectedMember: MemberLatestLocation? = null,
    onMemberClick: (MemberLatestLocation) -> Unit = {}
) {
    // Center point anchor (default to my location, or first member, or default coordinates 13.0827, 80.2707)
    val anchorLat = remember(myLocation, members) {
        myLocation?.latitude ?: members.firstOrNull()?.latitude ?: 13.0827
    }
    val anchorLng = remember(myLocation, members) {
        myLocation?.longitude ?: members.firstOrNull()?.longitude ?: 80.2707
    }

    var zoom by remember { mutableStateOf(16f) }
    var panOffsetX by remember { mutableStateOf(0f) }
    var panOffsetY by remember { mutableStateOf(0f) }

    // Pulsing radar animation for user's live location beacon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 15f,
        targetValue = 65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF131D2D))
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(10f, 22f)
                    panOffsetX += pan.x
                    panOffsetY += pan.y
                }
            }
            .testTag("interactive_map_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f + panOffsetX, size.height / 2f + panOffsetY)

            // Conversion scale factor: degrees to canvas pixels based on zoom level
            val metersPerPixel = (156543.03392 * cos(Math.toRadians(anchorLat)) / 2.0.pow(zoom.toDouble())).toFloat()
            val scaleLat = (111320f / metersPerPixel)
            val scaleLng = (111320f * cos(Math.toRadians(anchorLat)).toFloat() / metersPerPixel)

            fun toCanvasOffset(lat: Double, lng: Double): Offset {
                val dx = (lng - anchorLng).toFloat() * scaleLng
                val dy = -(lat - anchorLat).toFloat() * scaleLat // Invert Y because canvas Y is downwards
                return Offset(center.x + dx, center.y + dy)
            }

            // 1. Draw subtle radar map grid
            val gridSize = 80f
            val startX = (panOffsetX % gridSize)
            val startY = (panOffsetY % gridSize)
            var x = startX
            while (x < size.width) {
                drawLine(
                    color = Color(0x1F38BDF8),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridSize
            }
            var y = startY
            while (y < size.height) {
                drawLine(
                    color = Color(0x1F38BDF8),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }

            // 2. Draw History Polyline Trail (if present)
            if (historyPoints.size > 1) {
                val path = Path()
                val firstPt = toCanvasOffset(historyPoints[0].latitude, historyPoints[0].longitude)
                path.moveTo(firstPt.x, firstPt.y)
                for (i in 1 until historyPoints.size) {
                    val pt = toCanvasOffset(historyPoints[i].latitude, historyPoints[i].longitude)
                    path.lineTo(pt.x, pt.y)
                }
                // Polyline shadow & stroke
                drawPath(
                    path = path,
                    color = Color(0xFF60A5FA).copy(alpha = 0.8f),
                    style = Stroke(width = 6f)
                )

                // Draw points along history
                for (pt in historyPoints) {
                    val c = toCanvasOffset(pt.latitude, pt.longitude)
                    drawCircle(color = Color(0xFF2563EB), radius = 4f, center = c)
                }
            }

            // 3. Draw Defined Geofence Zones
            geofences.forEach { zone ->
                val zoneCenter = toCanvasOffset(zone.latitude, zone.longitude)
                val zoneRadiusPx = (zone.radiusMeters / metersPerPixel).coerceAtLeast(15f)

                val zoneColor = try {
                    Color(android.graphics.Color.parseColor(zone.colorHex))
                } catch (_: Exception) {
                    Color(0xFF10B981)
                }

                // Shaded zone area
                drawCircle(
                    color = zoneColor.copy(alpha = 0.16f),
                    radius = zoneRadiusPx,
                    center = zoneCenter
                )

                // Outer boundary stroke ring
                drawCircle(
                    color = zoneColor.copy(alpha = 0.85f),
                    radius = zoneRadiusPx,
                    center = zoneCenter,
                    style = Stroke(width = 3.5f)
                )

                // Center anchor marker
                drawCircle(
                    color = zoneColor,
                    radius = 7f,
                    center = zoneCenter
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = zoneCenter
                )

                // Zone label badge above circle
                val zoneEmoji = when (zone.zoneType.uppercase()) {
                    "HOME" -> "🏠 "
                    "OFFICE" -> "🏢 "
                    "CAMPUS" -> "🎓 "
                    "GYM" -> "🏋️ "
                    else -> "📍 "
                }
                val label = "$zoneEmoji${zone.name} (${zone.radiusMeters.toInt()}m)"
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    zoneCenter.x,
                    zoneCenter.y - zoneRadiusPx - 12f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                        setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
                    }
                )
            }

            // 4. Draw Group Members Pins
            members.forEach { member ->
                val memberPos = toCanvasOffset(member.latitude, member.longitude)
                val isSelected = selectedMember?.userId == member.userId

                // Accuracy circle
                val accuracyPx = (member.accuracy ?: 20f) / metersPerPixel
                drawCircle(
                    color = if (member.isOnline) Color(0x2210B981) else Color(0x2264748B),
                    radius = accuracyPx.coerceIn(15f, 120f),
                    center = memberPos
                )

                // Selection highlight ring
                if (isSelected) {
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = 28f,
                        center = memberPos,
                        style = Stroke(width = 3f)
                    )
                }

                // Member Pin Outer circle
                val pinColor = when {
                    !member.isLive -> SlateInactive
                    member.isOnline -> EmeraldLive
                    else -> AmberStale
                }

                drawCircle(color = Color.White, radius = 20f, center = memberPos)
                drawCircle(color = pinColor, radius = 16f, center = memberPos)

                // Online indicator badge
                drawCircle(
                    color = if (member.isOnline) Color(0xFF00E676) else Color(0xFF757575),
                    radius = 5f,
                    center = Offset(memberPos.x + 12f, memberPos.y - 12f)
                )

                // Render Name Label natively on Canvas
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 32f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    member.userName,
                    memberPos.x,
                    memberPos.y + 45f,
                    paint
                )
            }

            // 4. Draw Current User's Location (Blue Beacon with pulsing radar halo)
            if (myLocation != null) {
                val myPos = toCanvasOffset(myLocation.latitude, myLocation.longitude)

                // Pulsing wave
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = pulseAlpha),
                    radius = pulseRadius,
                    center = myPos
                )

                // GPS Accuracy halo
                val myAccuracyPx = (myLocation.accuracy) / metersPerPixel
                drawCircle(
                    color = Color(0x2238BDF8),
                    radius = myAccuracyPx.coerceIn(12f, 100f),
                    center = myPos
                )

                // User central dot
                drawCircle(color = Color.White, radius = 16f, center = myPos)
                drawCircle(color = IndigoPrimary, radius = 12f, center = myPos)

                // "You" label
                val myPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = 30f
                    isFakeBoldText = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "You",
                    myPos.x,
                    myPos.y + 38f,
                    myPaint
                )
            }
        }

        // Floating Map Controls: Compass, Zoom (+ / -), Re-center Me, Re-center Group
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Re-center on Me
            FilledTonalIconButton(
                onClick = {
                    panOffsetX = 0f
                    panOffsetY = 0f
                    zoom = 16.5f
                },
                modifier = Modifier.testTag("recenter_me_button")
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Center on my location")
            }

            // Re-center on Group members centroid
            FilledTonalIconButton(
                onClick = {
                    if (members.isNotEmpty()) {
                        val avgLat = members.map { it.latitude }.average()
                        val avgLng = members.map { it.longitude }.average()
                        panOffsetX = 0f
                        panOffsetY = 0f
                        zoom = 15f
                    }
                },
                modifier = Modifier.testTag("recenter_group_button")
            ) {
                Icon(Icons.Default.Group, contentDescription = "Center on group")
            }

            // Zoom In
            FilledTonalIconButton(
                onClick = { zoom = (zoom + 1f).coerceAtMost(22f) },
                modifier = Modifier.testTag("zoom_in_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Zoom In")
            }

            // Zoom Out
            FilledTonalIconButton(
                onClick = { zoom = (zoom - 1f).coerceAtLeast(10f) },
                modifier = Modifier.testTag("zoom_out_button")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
            }
        }

        // Live Radar Compass Overlay in top left
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xBB0F172A))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(EmeraldLive)
                )
                Text(
                    text = "${members.count { it.isOnline }} online / ${members.size} active",
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * Calculates distance in meters between two lat/lng pairs using the Haversine formula
 */
fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val earthRadius = 6371000.0 // meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2.0)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (earthRadius * c).toFloat()
}

fun formatDistance(meters: Float): String {
    return if (meters < 1000) {
        "${meters.roundToInt()} m away"
    } else {
        String.format("%.1f km away", meters / 1000f)
    }
}
