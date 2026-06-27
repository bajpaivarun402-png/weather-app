package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimatedWeatherIcon
import com.example.ui.components.GlassCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SunnyDayEnd
import com.example.ui.theme.SunnyDayStart
import com.example.ui.theme.SunsetOrange

@Composable
fun OnboardingScreen(
    onComplete: (isFahrenheit: Boolean, allowGps: Boolean) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var selectedFahrenheit by remember { mutableStateOf(false) }
    var allowGps by remember { mutableStateOf(true) }
    var allowNotifications by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SunnyDayStart, SunnyDayEnd)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .navigationBarsPadding()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AURA",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "STEP $step OF 3",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Middle Interactive Panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    },
                    label = "stepAnimation"
                ) { currentStep ->
                    when (currentStep) {
                        1 -> OnboardingStep1()
                        2 -> OnboardingStep2(
                            isFahrenheit = selectedFahrenheit,
                            onUnitChange = { selectedFahrenheit = it }
                        )
                        3 -> OnboardingStep3(
                            gpsEnabled = allowGps,
                            onGpsChange = { allowGps = it },
                            notifyEnabled = allowNotifications,
                            onNotifyChange = { allowNotifications = it }
                        )
                    }
                }
            }

            // Bottom Navigation Controller
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in 1..3) {
                        Box(
                            modifier = Modifier
                                .size(width = if (i == step) 20.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(if (i == step) Color.White else Color.White.copy(alpha = 0.4f))
                        )
                    }
                }

                // Next Button
                Button(
                    onClick = {
                        if (step < 3) {
                            step++
                        } else {
                            onComplete(selectedFahrenheit, allowGps)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Text(
                        text = if (step == 3) "GET STARTED" else "CONTINUE",
                        color = SunnyDayStart,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Next",
                        tint = SunnyDayStart,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingStep1() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        AnimatedWeatherIcon(
            condition = "clear",
            modifier = Modifier.size(150.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Extreme High Fidelity Forecasts",
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 36.sp
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Experience weather in beautiful Glassmorphic card design with custom radar maps, dynamic animations, and micro-analytics.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun OnboardingStep2(
    isFahrenheit: Boolean,
    onUnitChange: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Thermostat,
            contentDescription = "Units",
            tint = Color.White,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Preferred Temperature Unit",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Select your global format standard. You can toggle this any time in Settings.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Custom Glass Choice Blocks
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            // Celsius Card
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = if (!isFahrenheit) 2.dp else 0.dp,
                        color = if (!isFahrenheit) NeonCyan else Color.Transparent,
                        shape = RoundedCornerShape(24.dp)
                    ),
                onClick = { onUnitChange(false) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "°C",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = if (!isFahrenheit) NeonCyan else Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Celsius",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Fahrenheit Card
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .border(
                        width = if (isFahrenheit) 2.dp else 0.dp,
                        color = if (isFahrenheit) SunsetOrange else Color.Transparent,
                        shape = RoundedCornerShape(24.dp)
                    ),
                onClick = { onUnitChange(true) }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text(
                        text = "°F",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = if (isFahrenheit) SunsetOrange else Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Fahrenheit",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingStep3(
    gpsEnabled: Boolean,
    onGpsChange: (Boolean) -> Unit,
    notifyEnabled: Boolean,
    onNotifyChange: (Boolean) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Permissions",
            tint = Color.White,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Personalize Permissions",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Grant these preferences to fully unleash advanced location detection and live alerts.",
            style = TextStyle(
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))

        // Permissions sliders/switches in Glass card
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // GPS Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonCyan)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Auto Detect GPS", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Enable hyper-local forecasting", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = gpsEnabled,
                        onCheckedChange = onGpsChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.1f))

                // Notification Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = SunsetOrange)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Heavy Rain Alerts", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Receive storm warning push notifications", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                    Switch(
                        checked = notifyEnabled,
                        onCheckedChange = onNotifyChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = SunsetOrange)
                    )
                }
            }
        }
    }
}
