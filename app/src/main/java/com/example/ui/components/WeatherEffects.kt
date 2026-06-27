package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.sin

@Composable
fun WeatherBackground(
    condition: String,
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Determine background gradient brush based on weather condition
    val gradientColors = when (condition.lowercase()) {
        "clear" -> if (isDark) listOf(SunnyNightStart, SunnyNightEnd) else listOf(SunnyDayStart, SunnyDayEnd)
        "rain", "drizzle" -> if (isDark) listOf(RainyNightStart, RainyNightEnd) else listOf(RainyDayStart, RainyDayEnd)
        "snow" -> if (isDark) listOf(SnowyNightStart, SnowyNightEnd) else listOf(SnowyDayStart, SnowyDayEnd)
        "thunderstorm" -> if (isDark) listOf(StormyNightStart, StormyNightEnd) else listOf(StormyDayStart, StormyDayEnd)
        else -> if (isDark) listOf(SunnyNightStart, SunnyNightEnd) else listOf(SunnyDayStart, SunnyDayEnd) // Cloud/Default
    }

    val brush = Brush.verticalGradient(gradientColors)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    ) {
        // Overlay Animations (Rain, Snow, Clouds, Lightning) based on condition
        when (condition.lowercase()) {
            "rain", "drizzle" -> FallingRainAnimation()
            "snow" -> DriftingSnowAnimation()
            "thunderstorm" -> StormLightningAnimation()
            "cloudy", "partly cloudy", "overcast", "haze", "fog" -> FloatingCloudsAnimation()
        }

        // Screen content
        content()
    }
}

@Composable
fun FallingRainAnimation() {
    val transition = rememberInfiniteTransition(label = "rain")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Draw 45 rain streaks
        for (i in 0 until 45) {
            val seed = i * 157
            val xStart = (seed % width.toInt()).toFloat()
            val yOffset = progress * (height + 200f)
            val yStart = ((seed % height.toInt()) + yOffset) % (height + 200f) - 100f

            // Rain lines tilted slightly
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(xStart, yStart),
                end = Offset(xStart - 10f, yStart + 35f),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
fun DriftingSnowAnimation() {
    val transition = rememberInfiniteTransition(label = "snow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        for (i in 0 until 35) {
            val seed = i * 233
            val xStart = (seed % width.toInt()).toFloat()
            val yOffset = progress * (height + 100f)
            val yStart = ((seed % height.toInt()) + yOffset) % (height + 100f) - 50f

            // Drifting sway motion using sine wave
            val sway = sin((yStart / 50f) + seed) * 15f

            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = (3f + (seed % 4)).dp.toPx(),
                center = Offset(xStart + sway, yStart)
            )
        }
    }
}

@Composable
fun FloatingCloudsAnimation() {
    val transition = rememberInfiniteTransition(label = "clouds")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cloudProgress"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        for (i in 0 until 4) {
            val seed = i * 987
            val yPos = 120f + (seed % 250f)
            val xOffset = progress * (width + 400f)
            val xPos = ((seed % width.toInt()) + xOffset) % (width + 400f) - 200f
            val radius = 60f + (seed % 40)

            // Overlapping soft circles to represent atmospheric mist / clouds
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius * 1.5f,
                center = Offset(xPos, yPos)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = radius,
                center = Offset(xPos - 80f, yPos + 20f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = radius * 1.2f,
                center = Offset(xPos + 80f, yPos + 10f)
            )
        }
    }
}

@Composable
fun StormLightningAnimation() {
    val transition = rememberInfiniteTransition(label = "lightning")
    val flashState by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5000
                0f at 0
                0f at 3800
                0.8f at 3900 // Sudden spike
                0.2f at 4000
                1.0f at 4100 // Double flash
                0f at 4300
                0f at 5000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "lightningFlash"
    )

    // Also draw falling rain as storm background
    FallingRainAnimation()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width

        // Background flash overlay
        if (flashState > 0.1f) {
            drawRect(
                color = Color.White.copy(alpha = flashState * 0.25f),
                size = size
            )

            // Draw lightning bolts
            if (flashState > 0.5f) {
                val path = Path().apply {
                    val startX = width * 0.4f + (width * 0.2f * sin(flashState * 10f))
                    moveTo(startX, 0f)
                    lineTo(startX - 50f, 250f)
                    lineTo(startX + 20f, 230f)
                    lineTo(startX - 80f, 480f)
                    lineTo(startX + 40f, 440f)
                    lineTo(startX - 20f, 700f)
                }

                drawPath(
                    path = path,
                    color = Color(0xFFE3F2FD),
                    style = Stroke(width = 3.dp.toPx())
                )
                // Outer neon glow
                drawPath(
                    path = path,
                    color = NeonCyan.copy(alpha = flashState * 0.5f),
                    style = Stroke(width = 7.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun AnimatedWeatherIcon(
    condition: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "weatherIcon")
    
    // Rotating element
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sunRotate"
    )

    // Bouncing/scaling element
    val bounce by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = SineBlock),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloudBounce"
    )

    Canvas(modifier = modifier.size(64.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2, h / 2)

        when (condition.lowercase()) {
            "clear" -> {
                // Drawing Spinning Sun
                drawCircle(
                    color = GoldYellow,
                    radius = w * 0.28f,
                    center = center
                )
                
                // Draw Sun Rays
                for (i in 0 until 8) {
                    val angle = (i * 45) + rotation
                    val angleRad = Math.toRadians(angle.toDouble())
                    val innerDist = w * 0.35f
                    val outerDist = w * 0.46f

                    val rayStart = Offset(
                        (center.x + innerDist * Math.cos(angleRad)).toFloat(),
                        (center.y + innerDist * Math.sin(angleRad)).toFloat()
                    )
                    val rayEnd = Offset(
                        (center.x + outerDist * Math.cos(angleRad)).toFloat(),
                        (center.y + outerDist * Math.sin(angleRad)).toFloat()
                    )

                    drawLine(
                        color = GoldYellow,
                        start = rayStart,
                        end = rayEnd,
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            "rain", "drizzle" -> {
                // Drawing Cloud with Raindrops
                val cloudPath = createCloudPath(w, h, bounce)
                drawPath(
                    path = cloudPath,
                    color = Color.White.copy(alpha = 0.9f)
                )

                // Bouncing raindrops below cloud
                val dropOffset = (bounce + 5f) / 10f // 0 to 1
                for (i in 0 until 3) {
                    val dropX = w * 0.3f + i * (w * 0.2f)
                    val dropY = h * 0.72f + (dropOffset * 15f)
                    
                    drawLine(
                        color = NeonCyan,
                        start = Offset(dropX, dropY),
                        end = Offset(dropX - 3f, dropY + 8f),
                        strokeWidth = 2.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            "snow" -> {
                // Cloud with Bouncing Snowflakes
                val cloudPath = createCloudPath(w, h, bounce)
                drawPath(
                    path = cloudPath,
                    color = Color.White.copy(alpha = 0.9f)
                )

                // Snowflake indicators
                val flakeOffset = (bounce + 5f) / 10f
                for (i in 0 until 3) {
                    val fX = w * 0.3f + i * (w * 0.2f)
                    val fY = h * 0.72f + (flakeOffset * 12f)
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = Offset(fX, fY)
                    )
                }
            }
            "thunderstorm" -> {
                // Dark Cloud with Yellow Lightning Bolt
                val cloudPath = createCloudPath(w, h, bounce)
                drawPath(
                    path = cloudPath,
                    color = Color(0xFF455A64) // Darker stormy cloud
                )

                // Lightning bolt
                if (bounce > -1f) {
                    val boltPath = Path().apply {
                        moveTo(w * 0.5f, h * 0.6f)
                        lineTo(w * 0.42f, h * 0.8f)
                        lineTo(w * 0.52f, h * 0.78f)
                        lineTo(w * 0.45f, h * 0.95f)
                    }
                    drawPath(
                        path = boltPath,
                        color = GoldYellow
                    )
                }
            }
            else -> { // "cloudy", "partly cloudy", "overcast", "haze", "fog"
                if (condition.lowercase() == "partly cloudy") {
                    // Peek-a-boo Sun behind cloud
                    drawCircle(
                        color = GoldYellow,
                        radius = w * 0.22f,
                        center = Offset(w * 0.65f, h * 0.35f + bounce)
                    )
                }
                
                // Static cloud shape
                val cloudPath = createCloudPath(w, h, bounce)
                drawPath(
                    path = cloudPath,
                    color = Color.White.copy(alpha = 0.95f)
                )
            }
        }
    }
}

private fun createCloudPath(w: Float, h: Float, bounce: Float): Path {
    return Path().apply {
        val yBase = h * 0.62f + bounce
        moveTo(w * 0.2f, yBase)
        // Draw standard bubble-style cloud vectors
        cubicTo(w * 0.1f, yBase, w * 0.1f, yBase - 40f, w * 0.25f, yBase - 40f)
        cubicTo(w * 0.25f, yBase - 70f, w * 0.5f, yBase - 80f, w * 0.6f, yBase - 45f)
        cubicTo(w * 0.75f, yBase - 50f, w * 0.88f, yBase - 30f, w * 0.8f, yBase)
        lineTo(w * 0.2f, yBase)
        close()
    }
}

private val SineBlock = Easing { x ->
    sin(x * Math.PI).toFloat()
}
