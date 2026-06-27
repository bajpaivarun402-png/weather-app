package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastItem
import com.example.data.model.WeatherAlert
import com.example.data.model.WeatherData
import com.example.ui.components.*
import com.example.ui.viewmodel.WeatherPage
import com.example.ui.viewmodel.WeatherState
import com.example.ui.viewmodel.WeatherViewModel
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.GlassBg
import com.example.ui.theme.GlassBorder
import com.example.data.local.RecentSearch
import com.example.data.local.FavoriteLocation
import com.example.data.remote.AiInsights
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha

// Extension function to format temp based on Celsius/Fahrenheit unit
fun Float.formatTemp(isFahrenheit: Boolean): String {
    return if (isFahrenheit) {
        "${(this * 1.8f + 32f).toInt()}°"
    } else {
        "${this.toInt()}°"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val currentPage by viewModel.currentPage.collectAsState()
    val weatherState by viewModel.weatherState.collectAsState()
    val isFahrenheit by viewModel.isFahrenheit.collectAsState()
    val favoriteLocations by viewModel.favoriteLocations.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val isCurrentCityFavorite by viewModel.isCurrentCityFavorite.collectAsState()

    // Sub-tab selection inside Main pages
    var analyticsType by remember { mutableStateOf(ChartType.TEMPERATURE) }

    // Search query local state
    var searchQuery by remember { mutableStateOf("") }
    var activeSearchScreen by remember { mutableStateOf(false) }

    // Selected weather data for detailed analysis sheet
    var activeAiInsightsSheet by remember { mutableStateOf(false) }

    // Helper: current condition
    val currentCondition = when (val ws = weatherState) {
        is WeatherState.Success -> ws.weatherData.condition
        else -> "clear"
    }

    WeatherBackground(
        condition = currentCondition,
        isDark = true,
        modifier = modifier
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                // Customized Floating Glassmorphic Bottom Navigation Bar
                if (currentPage != WeatherPage.ONBOARDING && currentPage != WeatherPage.SPLASH) {
                    GlassNavigationBar(
                        currentPage = currentPage,
                        onTabSelected = { viewModel.navigateTo(it) }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (weatherState) {
                    is WeatherState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = NeonCyan)
                        }
                    }
                    is WeatherState.Error -> {
                        val errorMsg = (weatherState as WeatherState.Error).message
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = Color.Red,
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "Unable to Load Weather",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        errorMsg,
                                        color = Color.White.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.loadWeather(viewModel.currentQuery.value) },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                    ) {
                                        Text("RETRY", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    is WeatherState.Success -> {
                        val successState = weatherState as WeatherState.Success
                        val weather = successState.weatherData
                        val insights = successState.aiInsights
                        val loadingInsights = successState.loadingInsights

                        // Content Router based on current Navigation Page
                        AnimatedContent(
                            targetState = currentPage,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "screenRouter"
                        ) { targetPage ->
                            when (targetPage) {
                                WeatherPage.HOME -> {
                                    HomeScreenContent(
                                        weather = weather,
                                        insights = insights,
                                        loadingInsights = loadingInsights,
                                        isFahrenheit = isFahrenheit,
                                        isFavorite = isCurrentCityFavorite,
                                        onFavoriteToggle = { viewModel.toggleFavoriteCurrent() },
                                        onSearchClick = { activeSearchScreen = true },
                                        onAiInsightsClick = { activeAiInsightsSheet = true }
                                    )
                                }
                                WeatherPage.SEARCH -> {
                                    SearchScreenContent(
                                        recentSearches = recentSearches,
                                        favoriteLocations = favoriteLocations,
                                        isFahrenheit = isFahrenheit,
                                        onSearch = { query ->
                                            viewModel.loadWeather(query)
                                            viewModel.navigateTo(WeatherPage.HOME)
                                        },
                                        onDeleteRecent = { query -> viewModel.deleteRecentSearch(query) },
                                        onClearRecent = { viewModel.clearRecentSearches() },
                                        onRemoveFavorite = { city, country -> viewModel.removeFavoriteLocation(city, country) }
                                    )
                                }
                                WeatherPage.WEATHER_MAPS -> {
                                    MapScreenContent(weather = weather)
                                }
                                WeatherPage.HOURLY_FORECAST -> {
                                    HourlyScreenContent(weather = weather, isFahrenheit = isFahrenheit)
                                }
                                WeatherPage.WEEKLY_FORECAST -> {
                                    WeeklyScreenContent(weather = weather, isFahrenheit = isFahrenheit)
                                }
                                WeatherPage.ALERTS -> {
                                    AlertsScreenContent(alerts = weather.alerts)
                                }
                                WeatherPage.SETTINGS -> {
                                    SettingsScreenContent(viewModel = viewModel)
                                }
                                else -> {
                                    // Fallback
                                    HomeScreenContent(
                                        weather = weather,
                                        insights = insights,
                                        loadingInsights = loadingInsights,
                                        isFahrenheit = isFahrenheit,
                                        isFavorite = isCurrentCityFavorite,
                                        onFavoriteToggle = { viewModel.toggleFavoriteCurrent() },
                                        onSearchClick = { activeSearchScreen = true },
                                        onAiInsightsClick = { activeAiInsightsSheet = true }
                                    )
                                }
                            }
                        }

                        // AI Insights Sheet Dialog Overlay
                        if (activeAiInsightsSheet) {
                            AiInsightsDialog(
                                insights = insights,
                                loading = loadingInsights,
                                cityName = weather.cityName,
                                onDismiss = { activeAiInsightsSheet = false }
                            )
                        }
                    }
                }

                // Global Floating Quick Search Overlay Screen
                if (activeSearchScreen) {
                    SearchOverlay(
                        onDismiss = { activeSearchScreen = false },
                        onQuerySubmitted = { query ->
                            viewModel.loadWeather(query)
                            activeSearchScreen = false
                        },
                        recentSearches = recentSearches
                    )
                }
            }
        }
    }
}

// --- Glassmorphic Bottom Navigation ---
@Composable
fun GlassNavigationBar(
    currentPage: WeatherPage,
    onTabSelected: (WeatherPage) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        GlassCard(
            shape = RoundedCornerShape(32.dp),
            bgColor = Color(0x1F000000), // Darker translucent backing for legibility
            borderColor = Color(0x33FFFFFF),
            borderWidth = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavigationItem(
                    icon = Icons.Default.Home,
                    label = "Home",
                    isSelected = currentPage == WeatherPage.HOME,
                    onClick = { onTabSelected(WeatherPage.HOME) }
                )
                NavigationItem(
                    icon = Icons.Default.Search,
                    label = "Search",
                    isSelected = currentPage == WeatherPage.SEARCH,
                    onClick = { onTabSelected(WeatherPage.SEARCH) }
                )
                NavigationItem(
                    icon = Icons.Default.Map,
                    label = "Radar",
                    isSelected = currentPage == WeatherPage.WEATHER_MAPS,
                    onClick = { onTabSelected(WeatherPage.WEATHER_MAPS) }
                )
                NavigationItem(
                    icon = Icons.Default.Timeline,
                    label = "Trends",
                    isSelected = currentPage == WeatherPage.HOURLY_FORECAST, // Group trends inside Hourly screen
                    onClick = { onTabSelected(WeatherPage.HOURLY_FORECAST) }
                )
                NavigationItem(
                    icon = Icons.Default.Warning,
                    label = "Alerts",
                    isSelected = currentPage == WeatherPage.ALERTS,
                    onClick = { onTabSelected(WeatherPage.ALERTS) }
                )
                NavigationItem(
                    icon = Icons.Default.Settings,
                    label = "Settings",
                    isSelected = currentPage == WeatherPage.SETTINGS,
                    onClick = { onTabSelected(WeatherPage.SETTINGS) }
                )
            }
        }
    }
}

@Composable
fun RowScope.NavigationItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isSelected) 1.15f else 1.0f, label = "navScale")
    val alpha by animateFloatAsState(if (isSelected) 1f else 0.55f, label = "navAlpha")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .weight(1f)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) NeonCyan else Color.White,
            modifier = Modifier
                .size(22.dp)
                .rotate(if (isSelected && label == "Settings") 45f else 0f)
                .scale(scale)
                .alpha(alpha)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.6f)
        )
    }
}

// --- HOME PAGE CONTENT ---
@Composable
fun HomeScreenContent(
    weather: WeatherData,
    insights: AiInsights?,
    loadingInsights: Boolean,
    isFahrenheit: Boolean,
    isFavorite: Boolean,
    onFavoriteToggle: () -> Unit,
    onSearchClick: () -> Unit,
    onAiInsightsClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App bar & Search trigger
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = weather.cityName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.size(24.dp).testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorite location",
                                tint = if (isFavorite) Color(0xFFFFD700) else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = "${weather.stateName}, ${weather.countryName}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }

                // Interactive quick search trigger card
                GlassCard(
                    shape = CircleShape,
                    bgColor = Color.White.copy(alpha = 0.15f),
                    borderWidth = 1.dp,
                    modifier = Modifier.size(44.dp).clickable(onClick = onSearchClick),
                    content = {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.Search, contentDescription = "Search city", tint = Color.White)
                        }
                    }
                )
            }
        }

        // Current weather conditions card (Main Card)
        item {
            GlassCard(modifier = Modifier.fillMaxWidth().testTag("current_weather_card")) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Weather",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedWeatherIcon(
                            condition = weather.condition,
                            modifier = Modifier.size(110.dp)
                        )
                        Column {
                            Text(
                                text = weather.temperature.formatTemp(isFahrenheit),
                                fontSize = 64.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = weather.condition,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Feels like ${weather.feelsLike.formatTemp(isFahrenheit)}",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Weather mini indicators Grid row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        WeatherMetricItem(icon = Icons.Default.Air, value = "${weather.windSpeed.toInt()} km/h", label = "Wind")
                        WeatherMetricItem(icon = Icons.Default.WaterDrop, value = "${weather.humidity}%", label = "Humidity")
                        WeatherMetricItem(icon = Icons.Default.WbSunny, value = "UV ${weather.uvIndex}", label = "UV Index")
                        WeatherMetricItem(icon = Icons.Default.Cloud, value = "${weather.chanceOfRain}%", label = "Rain Prob")
                    }
                }
            }
        }

        // Smart Gemini AI Recommendations Box
        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .clickable(onClick = onAiInsightsClick)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = NeonCyan.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = NeonCyan)
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AURA AI INSIGHTS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NeonCyan)
                            Spacer(modifier = Modifier.width(6.dp))
                            if (loadingInsights) {
                                CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.5.dp, color = NeonCyan)
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = insights?.aiSummary ?: "Generating personalized health and travel advice...",
                            color = Color.White,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Active Weather Alerts Badge if alerts exist
        if (weather.alerts.isNotEmpty()) {
            item {
                val activeAlert = weather.alerts.first()
                GlassCard(
                    bgColor = Color(0x33E74C3C), // Alert translucent red
                    borderColor = Color(0x88E74C3C),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAiInsightsClick) // Clicking moves to alerts or dialog
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Alert", tint = Color(0xFFE74C3C), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = activeAlert.title,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Affected Area: ${activeAlert.area}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        // Quick Metrics Scroll (AQI Details, Pollen index, Sunset/sunrise progress)
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // AQI Card
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Eco, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AQI Air Quality", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(weather.aqi.toString(), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(weather.aqiLabel, color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Pollen Card
                GlassCard(modifier = Modifier.weight(1f)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Yard, contentDescription = null, tint = Color(0xFFF1C40F), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pollen Level", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Index: ${weather.pollenIndex}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text(weather.pollenLabel, color = Color(0xFFF1C40F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Hourly Forecast Horizontal Strip
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Hourly Forecast (24h)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(weather.hourlyForecast.take(12)) { hourItem ->
                        GlassCard {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(hourItem.time, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                Spacer(modifier = Modifier.height(6.dp))
                                AnimatedWeatherIcon(condition = hourItem.condition, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(hourItem.temperature.formatTemp(isFahrenheit), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(8.dp))
                                    Text("${hourItem.rainProbability}%", fontSize = 9.sp, color = NeonCyan)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 15-Day Forecast Preview
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "15-Day Forecast",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        weather.dailyForecast.take(5).forEach { dayItem ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dayItem.dayOfWeek,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White,
                                    modifier = Modifier.width(60.dp)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedWeatherIcon(condition = dayItem.condition, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = dayItem.condition,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.width(80.dp)) {
                                    Text(
                                        text = dayItem.maxTemp.formatTemp(isFahrenheit),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = dayItem.minTemp.formatTemp(isFahrenheit),
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Divider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }
        }

        // Expanded metrics section
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Atmospheric Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailIndicatorItem(label = "Visibility", value = "${weather.visibility} km")
                        DetailIndicatorItem(label = "Pressure", value = "${weather.pressure} hPa")
                    }
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailIndicatorItem(label = "Dew Point", value = weather.dewPoint.formatTemp(isFahrenheit))
                        DetailIndicatorItem(label = "Cloud Coverage", value = "${weather.cloudCoverage}%")
                    }
                    Divider(color = Color.White.copy(alpha = 0.05f))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailIndicatorItem(label = "Sunrise", value = weather.sunrise)
                        DetailIndicatorItem(label = "Sunset", value = weather.sunset)
                    }
                }
            }
        }

        // Spacing bottom
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun WeatherMetricItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = label, tint = NeonCyan, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
        Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
fun DetailIndicatorItem(label: String, value: String) {
    Column(modifier = Modifier.width(130.dp)) {
        Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

// --- SEARCH / FAVORITES CONTENT ---
@Composable
fun SearchScreenContent(
    recentSearches: List<RecentSearch>,
    favoriteLocations: List<FavoriteLocation>,
    isFahrenheit: Boolean,
    onSearch: (String) -> Unit,
    onDeleteRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
    onRemoveFavorite: (String, String) -> Unit
) {
    var queryText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Text(
            "Locations Manager",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Custom Search Input Bar
        GlassCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("search_field_input"),
                    decorationBox = { innerTextField ->
                        if (queryText.isEmpty()) {
                            Text("Search city, state, country...", color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp)
                        }
                        innerTextField()
                    }
                )
                if (queryText.isNotEmpty()) {
                    IconButton(
                        onClick = { onSearch(queryText) },
                        modifier = Modifier.size(36.dp).testTag("search_submit_button")
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Submit search", tint = NeonCyan)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Searches Block
        if (recentSearches.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Searches", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                Text(
                    "Clear All",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onClearRecent)
                )
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
            ) {
                items(recentSearches) { recent ->
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        onClick = { onSearch(recent.query) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(recent.query, fontSize = 12.sp, color = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { onDeleteRecent(recent.query) }
                            )
                        }
                    }
                }
            }
        }

        // Favorites Block
        Text("Favorite Locations ⭐", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(bottom = 8.dp))
        
        if (favoriteLocations.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.StarBorder, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Starred Cities", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                    Text("Search for a city and tap the star to save here.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favoriteLocations) { fav ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSearch("${fav.city}, ${fav.state}, ${fav.country}") }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = NeonCyan)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(fav.city, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("${fav.state}, ${fav.country}", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                            IconButton(onClick = { onRemoveFavorite(fav.city, fav.country) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.Red.copy(alpha = 0.7f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- RADAR MAP SCREEN CONTENT ---
@Composable
fun MapScreenContent(weather: WeatherData) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding()
    ) {
        Text(
            "Meteorological Radar Feed",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            "Live simulated satellite analysis for ${weather.cityName}",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        InteractiveWeatherMap(
            cityName = weather.cityName,
            lat = weather.latitude,
            lon = weather.longitude,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "You can interact with the map feed! Toggle layers below the feed, switch on Satellite topography, or tap the map to measure coordinates.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// --- HOURLY FORECAST & TRENDS ANALYTICS CONTENT ---
@Composable
fun HourlyScreenContent(
    weather: WeatherData,
    isFahrenheit: Boolean
) {
    var selectedChart by remember { mutableStateOf(ChartType.TEMPERATURE) }

    val dataPoints = when (selectedChart) {
        ChartType.TEMPERATURE -> weather.hourlyForecast.take(12).map { it.temperature }
        ChartType.HUMIDITY -> weather.hourlyForecast.take(12).map { it.humidity.toFloat() }
        ChartType.WIND -> weather.hourlyForecast.take(12).map { it.windSpeed }
        ChartType.AQI -> weather.hourlyForecast.take(12).map { (weather.aqi + it.uvIndex * 3).toFloat() } // generated trend
        ChartType.PRESSURE -> weather.hourlyForecast.take(12).map { (weather.pressure + (it.temperature / 10)).toFloat() }
    }

    val labels = weather.hourlyForecast.take(12).map { it.time }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Trends & Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Dynamic Bezier progression analysis for ${weather.cityName}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Horizontal metric tabs selectors
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChartType.values().forEach { type ->
                    val isSelected = selectedChart == type
                    GlassCard(
                        shape = CircleShape,
                        bgColor = if (isSelected) NeonCyan.copy(alpha = 0.25f) else GlassBg,
                        borderColor = if (isSelected) NeonCyan else GlassBorder,
                        modifier = Modifier.clickable { selectedChart = type }
                    ) {
                        Text(
                            text = type.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isSelected) NeonCyan else Color.White
                        )
                    }
                }
            }
        }

        // Main Bezier Chart block
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        "${selectedChart.name} Curve Analysis",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeatherAnalyticsChart(
                        dataPoints = dataPoints,
                        labels = labels,
                        chartType = selectedChart,
                        modifier = Modifier.fillMaxWidth().height(180.dp)
                    )
                }
            }
        }

        // Hourly scrolling card lists
        item {
            Text("Detailed Chronological Feed", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }

        items(weather.hourlyForecast.take(12)) { hr ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(hr.time, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedWeatherIcon(condition = hr.condition, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(hr.condition, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(hr.temperature.formatTemp(isFahrenheit), fontWeight = FontWeight.Black, color = Color.White)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(10.dp))
                            Text("${hr.rainProbability}%", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                }
            }
        }
    }
}

// --- WEEKLY FORECAST CONTENT ---
@Composable
fun WeeklyScreenContent(
    weather: WeatherData,
    isFahrenheit: Boolean
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "15-Day Extended Forecast",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Daily meteorological progression model",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(weather.dailyForecast) { day ->
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.width(90.dp)) {
                        Text(day.dayOfWeek, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                        Text(day.date, fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedWeatherIcon(condition = day.condition, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(day.condition, fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row {
                            Text(day.maxTemp.formatTemp(isFahrenheit), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(day.minTemp.formatTemp(isFahrenheit), color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Air, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(10.dp))
                            Text("${day.windSpeed.toInt()} km/h", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
    }
}

// --- SEVERE WEATHER ALERTS CONTENT ---
@Composable
fun AlertsScreenContent(alerts: List<WeatherAlert>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Severe Weather Warnings",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Authorized emergency weather alert tracking",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (alerts.isEmpty()) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2ECC71), modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Active Warnings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 18.sp)
                        Text("Atmospheric conditions are stable with zero emergency risk warnings logged.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            }
        } else {
            items(alerts) { alert ->
                GlassCard(
                    bgColor = if (alert.severity == "Emergency") Color(0x33E74C3C) else Color(0x33F39C12),
                    borderColor = if (alert.severity == "Emergency") Color(0x88E74C3C) else Color(0x88F39C12),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (alert.severity == "Emergency") Color(0xFFE74C3C) else Color(0xFFF39C12)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(alert.title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                            }
                            Surface(
                                color = if (alert.severity == "Emergency") Color(0xFFE74C3C) else Color(0xFFF39C12),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    alert.severity.uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text("Sender: ${alert.sender}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f))
                        Text("Area: ${alert.area}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        
                        Text(alert.description, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 20.sp)
                        
                        Divider(color = Color.White.copy(alpha = 0.1f))
                        
                        Text("Safety Recommendations:", fontWeight = FontWeight.Bold, color = NeonCyan, fontSize = 12.sp)
                        Text(alert.recommendation, color = Color.White, fontSize = 13.sp, lineHeight = 20.sp)
                    }
                }
            }
        }
    }
}

// --- SETTINGS CONTENT ---
@Composable
fun SettingsScreenContent(viewModel: WeatherViewModel) {
    val isFahrenheit by viewModel.isFahrenheit.collectAsState()
    val language by viewModel.selectedLanguage.collectAsState()
    val refreshInterval by viewModel.refreshInterval.collectAsState()
    val notifyEnabled by viewModel.notificationsEnabled.collectAsState()
    val privacyEnabled by viewModel.privacyEnabled.collectAsState()
    val gpsEnabled by viewModel.gpsPermissionGranted.collectAsState()

    var showLanguageDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Preferences & Configs",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Standard units card
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Measurement Standard", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Temperature Scale", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(if (isFahrenheit) "Currently Fahrenheit (°F)" else "Currently Celsius (°C)", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = isFahrenheit,
                            onCheckedChange = { viewModel.toggleTemperatureUnit() },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }
                }
            }
        }

        // Notification Preferences
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Safety Permissions", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Severe Rain Warnings", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Toggle automatic notification pings", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = notifyEnabled,
                            onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("GPS Location Services", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Permit background location queries", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = gpsEnabled,
                            onCheckedChange = { viewModel.setGpsPermissionGranted(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Search Privacy Mode", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Mask queries from database indexing", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        Switch(
                            checked = privacyEnabled,
                            onCheckedChange = { viewModel.setPrivacyEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan)
                        )
                    }
                }
            }
        }

        // Language & Refreshes Selector
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("System Customization", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)

                    // Language Selector Dropdown Trigger
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showLanguageDropdown = !showLanguageDropdown },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("App Language", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text("Current: $language", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                    }

                    if (showLanguageDropdown) {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("English", "Español", "Français", "Deutsch", "日本語").forEach { lang ->
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (language == lang) NeonCyan.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (language == lang) NeonCyan else Color.Transparent, CircleShape)
                                        .clickable {
                                            viewModel.setLanguage(lang)
                                            showLanguageDropdown = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(lang, color = if (language == lang) NeonCyan else Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Refresh interval Selector
                    Column {
                        Text("Weather Refresh Cycle", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            listOf(15, 30, 60, 120).forEach { mins ->
                                val selected = refreshInterval == mins
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selected) NeonCyan.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
                                        .border(1.dp, if (selected) NeonCyan else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { viewModel.setRefreshInterval(mins) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("${mins}m", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) NeonCyan else Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Onboarding Reset Trigger & Credits
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Aura Weather v1.0.0",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        "Intelligence in every breeze",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.navigateTo(WeatherPage.ONBOARDING) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("RESET ONBOARDING SETUP", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- FLOATING AI DIALOG SHEET OVERLAY ---
@Composable
fun AiInsightsDialog(
    insights: AiInsights?,
    loading: Boolean,
    cityName: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("DISMISS", color = NeonCyan, fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Aura AI Insights • $cityName", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = NeonCyan)
                    }
                } else {
                    // Summary Section
                    Text(
                        text = insights?.aiSummary ?: "AI recommendation summary is loading...",
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Health Advice Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFE74C3C), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Health & Exercise Suggestions", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }

                        insights?.healthSuggestions?.forEach { suggestion ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("•", color = Color(0xFFE74C3C), modifier = Modifier.width(16.dp))
                                Text(suggestion, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 20.sp)
                            }
                        }
                    }

                    Divider(color = Color.White.copy(alpha = 0.1f))

                    // Travel Section
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Travel & Transit Guidelines", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        }

                        insights?.travelTips?.forEach { tip ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("•", color = NeonCyan, modifier = Modifier.width(16.dp))
                                Text(tip, color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFF1C2833),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.border(1.dp, GlassBorder.copy(alpha = 0.4f), RoundedCornerShape(28.dp))
    )
}

// --- QUICK FLOATING SEARCH OVERLAY ---
@Composable
fun SearchOverlay(
    onDismiss: () -> Unit,
    onQuerySubmitted: (String) -> Unit,
    recentSearches: List<RecentSearch>
) {
    var textState by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(onClick = onDismiss) // Click outside to dismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(24.dp)
                .clickable(enabled = false) {} // Disable clicking content box to dismiss
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Where to?", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Text search input
            GlassCard(
                bgColor = Color.White.copy(alpha = 0.15f),
                borderColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicTextField(
                        value = textState,
                        onValueChange = { textState = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("quick_search_input"),
                        decorationBox = { innerTextField ->
                            if (textState.isEmpty()) {
                                Text("Search New York, London, Tokyo...", color = Color.White.copy(alpha = 0.5f))
                            }
                            innerTextField()
                        }
                    )
                    if (textState.isNotEmpty()) {
                        IconButton(onClick = { onQuerySubmitted(textState) }, modifier = Modifier.size(36.dp).testTag("quick_search_submit")) {
                            Icon(Icons.Default.ArrowForward, contentDescription = "Submit", tint = NeonCyan)
                        }
                    }
                }
            }

            // Quick Recommendations list
            Spacer(modifier = Modifier.height(24.dp))
            Text("Popular Destinations", fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("London", "Tokyo", "Cairo", "Paris", "New York", "Sydney").forEach { pl ->
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                            .clickable { onQuerySubmitted(pl) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(pl, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
