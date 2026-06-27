package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SunsetOrange
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveWeatherMap(
    cityName: String,
    lat: Double,
    lon: Double,
    modifier: Modifier = Modifier
) {
    var mapLayer by remember { mutableStateOf(MapLayer.RAIN_RADAR) }
    var isSatelliteView by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var scanRadius by remember { mutableStateOf(200f) }

    // Touch feedback state
    var clickPoint by remember { mutableStateOf<Offset?>(null) }

    // Pulse animation for radar
    val transition = rememberInfiniteTransition(label = "radarScan")
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )

    // Moving weather cells (wind drift)
    val driftProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "driftProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1C2833))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
    ) {
        // Map Canvas Screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            clickPoint = offset
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2f, h / 2f)

                // 1. Draw Map Base (stylized topography or dark ocean grid)
                if (isSatelliteView) {
                    // Sat View: Blended deep green & brown terrain masses
                    drawRect(color = Color(0xFF112211)) // Base forest
                    // Draw landmasses
                    drawCircle(Color(0xFF223311), radius = w * 0.4f, center = Offset(w * 0.3f, h * 0.3f))
                    drawCircle(Color(0xFF2E401A), radius = w * 0.3f, center = Offset(w * 0.7f, h * 0.6f))
                } else {
                    // Standard: Cool slate tech theme with coordinate lines
                    drawRect(color = Color(0xFF15222E))
                    // Draw grid coordinate lines
                    val colWidth = w / 6f
                    val rowHeight = h / 6f
                    for (i in 1..5) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(colWidth * i, 0f),
                            end = Offset(colWidth * i, h),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, rowHeight * i),
                            end = Offset(w, rowHeight * i),
                            strokeWidth = 1f
                        )
                    }
                }

                // 2. Draw Simulated Coastal outlines
                val coastPath = Path().apply {
                    moveTo(0f, h * 0.4f)
                    quadraticTo(w * 0.3f, h * 0.2f, w * 0.5f, h * 0.5f)
                    quadraticTo(w * 0.7f, h * 0.8f, w, h * 0.6f)
                }
                drawPath(
                    path = coastPath,
                    color = if (isSatelliteView) Color.White.copy(alpha = 0.1f) else NeonCyan.copy(alpha = 0.25f),
                    style = Stroke(width = 2f)
                )

                // 3. Draw Weather Layers
                if (isPlaying) {
                    val angleRad = Math.toRadians(driftProgress.toDouble())
                    val cellOffset1 = Offset((50f * cos(angleRad)).toFloat(), (30f * sin(angleRad)).toFloat())
                    val cellOffset2 = Offset((80f * sin(angleRad)).toFloat(), (40f * cos(angleRad)).toFloat())

                    when (mapLayer) {
                        MapLayer.RAIN_RADAR -> {
                            // Pulsing storm cells (heavy rain yellow/red center, green fringe)
                            drawCircle(
                                color = Color(0xFF2ECC71).copy(alpha = 0.45f), // Light rain
                                radius = 90f,
                                center = center + cellOffset1
                            )
                            drawCircle(
                                color = Color(0xFFF1C40F).copy(alpha = 0.55f), // Moderate
                                radius = 50f,
                                center = center + cellOffset1
                            )
                            drawCircle(
                                color = Color(0xFFE74C3C).copy(alpha = 0.65f), // Extreme heavy storm
                                radius = 25f,
                                center = center + cellOffset1
                            )

                            // Second storm cluster
                            drawCircle(
                                color = Color(0xFF2ECC71).copy(alpha = 0.35f),
                                radius = 120f,
                                center = center + Offset(150f, -100f) + cellOffset2
                            )
                        }
                        MapLayer.CLOUD_RADAR -> {
                            // Fluffy, soft, drifting clouds
                            drawCircle(
                                color = Color.White.copy(alpha = 0.25f),
                                radius = 140f,
                                center = center + cellOffset1
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.2f),
                                radius = 180f,
                                center = center + Offset(-120f, 80f) + cellOffset2
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = 0.15f),
                                radius = 100f,
                                center = center + Offset(180f, 120f) - cellOffset1
                            )
                        }
                        MapLayer.TEMPERATURE -> {
                            // Heat/Cold contour maps (Red-Orange vs Blue)
                            drawCircle(
                                color = SunsetOrange.copy(alpha = 0.3f), // Heat zone
                                radius = 160f,
                                center = center + cellOffset1
                            )
                            drawCircle(
                                color = Color(0xFFFFA726).copy(alpha = 0.2f),
                                radius = 240f,
                                center = center + cellOffset1
                            )
                            drawCircle(
                                color = Color(0xFF29B6F6).copy(alpha = 0.25f), // Cold zone
                                radius = 150f,
                                center = center - cellOffset2
                            )
                        }
                        MapLayer.WIND -> {
                            // Animated floating vectors
                            val windCount = 8
                            for (i in 0 until windCount) {
                                val segmentSeed = i * 45
                                val xBase = (segmentSeed % w.toInt()).toFloat()
                                val yBase = ((segmentSeed * 3) % h.toInt()).toFloat()
                                val segmentDrift = (driftProgress * 1.5f) % 80f

                                drawLine(
                                    color = Color.White.copy(alpha = 0.4f),
                                    start = Offset(xBase + segmentDrift, yBase),
                                    end = Offset(xBase + segmentDrift + 30f, yBase),
                                    strokeWidth = 1.5.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.2f),
                                    start = Offset(xBase + segmentDrift + 30f, yBase),
                                    end = Offset(xBase + segmentDrift + 45f, yBase),
                                    strokeWidth = 1f
                                )
                            }
                        }
                        MapLayer.LIGHTNING -> {
                            // Blinking high voltage markers
                            if ((driftProgress.toInt() / 20) % 3 == 0) {
                                val strikePoint1 = center + cellOffset1 + Offset(10f, -15f)
                                drawLine(Color(0xFFF1C40F), strikePoint1 - Offset(15f, 0f), strikePoint1 + Offset(15f, 0f), strokeWidth = 2f)
                                drawLine(Color(0xFFF1C40F), strikePoint1 - Offset(0f, 15f), strikePoint1 + Offset(0f, 15f), strokeWidth = 2f)
                                drawCircle(Color.White.copy(alpha = 0.4f), radius = 15f, center = strikePoint1)
                            }
                            if ((driftProgress.toInt() / 15) % 4 == 0) {
                                val strikePoint2 = center - cellOffset2 + Offset(120f, 40f)
                                drawLine(Color(0xFFF1C40F), strikePoint2 - Offset(12f, 0f), strikePoint2 + Offset(12f, 0f), strokeWidth = 2f)
                                drawLine(Color(0xFFF1C40F), strikePoint2 - Offset(0f, 12f), strikePoint2 + Offset(0f, 12f), strokeWidth = 2f)
                            }
                        }
                    }
                }

                // 4. Draw Sweeping radar beam indicator
                drawCircle(
                    color = NeonCyan.copy(alpha = (1f - pulseProgress) * 0.35f),
                    radius = pulseProgress * scanRadius * 1.5f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )

                // 5. Draw City Anchor Pin
                drawCircle(
                    color = Color.Red,
                    radius = 5.dp.toPx(),
                    center = center
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = center
                )
                // Ping outline
                drawCircle(
                    color = Color.Red.copy(alpha = 0.4f),
                    radius = (5f + 15f * pulseProgress).dp.toPx(),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Draw Touch click point response
                clickPoint?.let { pt ->
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = 8.dp.toPx(),
                        center = pt
                    )
                    // Display details (coordinate calculation)
                    val touchLat = lat + (pt.y - center.y) / 500f
                    val touchLon = lon + (pt.x - center.x) / 500f
                    // Drawing detail overlays
                }
            }

            // Radar Status Badge
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color.Green else Color.Red)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE FEED • ${cityName}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Latitude/Longitude HUD on bottom right
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Text(
                    text = String.format("LAT: %.4f° | LON: %.4f°", lat, lon),
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Layer Toggles Toolbar
        Surface(
            color = Color.Black.copy(alpha = 0.3f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Layer Buttons
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MapLayer.values().forEach { layer ->
                        val isSelected = mapLayer == layer
                        IconButton(
                            onClick = { mapLayer = layer },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NeonCyan.copy(alpha = 0.25f) else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = when (layer) {
                                    MapLayer.RAIN_RADAR -> Icons.Default.WaterDrop
                                    MapLayer.CLOUD_RADAR -> Icons.Default.Cloud
                                    MapLayer.TEMPERATURE -> Icons.Default.Thermostat
                                    MapLayer.WIND -> Icons.Default.Air
                                    MapLayer.LIGHTNING -> Icons.Default.FlashOn
                                },
                                contentDescription = layer.label,
                                tint = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Satellite Switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isSatelliteView = !isSatelliteView }
                ) {
                    Text(
                        text = "SAT",
                        color = if (isSatelliteView) NeonCyan else Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = isSatelliteView,
                        onCheckedChange = { isSatelliteView = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.scale(0.7f)
                    )
                }

                // Play / Pause Feed
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Control Feed",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Utility extension to scale Compose components easily
private fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
)

enum class MapLayer(val label: String) {
    RAIN_RADAR("Rain Radar"),
    CLOUD_RADAR("Cloud Radar"),
    TEMPERATURE("Temperature Map"),
    WIND("Wind Map"),
    LIGHTNING("Lightning Map")
}
