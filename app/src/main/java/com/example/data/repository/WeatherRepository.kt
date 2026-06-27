package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteLocation
import com.example.data.local.RecentSearch
import com.example.data.local.WeatherOfflineCache
import com.example.data.model.DailyForecastItem
import com.example.data.model.HourlyForecastItem
import com.example.data.model.WeatherAlert
import com.example.data.model.WeatherData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sin

class WeatherRepository(private val context: Context) {
    private val db = AppDatabase.getDatabase(context)
    private val dao = db.weatherDao()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val weatherAdapter = moshi.adapter(WeatherData::class.java)

    companion object {
        private const val TAG = "WeatherRepository"
    }

    // --- Favorites Flow ---
    val allFavorites: Flow<List<FavoriteLocation>> = dao.getAllFavorites()

    suspend fun addFavorite(city: String, state: String, country: String, lat: Double, lon: Double) = withContext(Dispatchers.IO) {
        dao.insertFavorite(
            FavoriteLocation(
                city = city,
                state = state,
                country = country,
                latitude = lat,
                longitude = lon
            )
        )
    }

    suspend fun removeFavorite(city: String, country: String) = withContext(Dispatchers.IO) {
        dao.deleteFavoriteByCity(city, country)
    }

    fun isFavorite(city: String, country: String): Flow<Boolean> {
        return dao.isFavorite(city, country)
    }

    // --- Recent Searches Flow ---
    val recentSearches: Flow<List<RecentSearch>> = dao.getRecentSearches()

    suspend fun addRecentSearch(query: String) = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            dao.insertRecentSearch(RecentSearch(query = query.trim(), timestamp = System.currentTimeMillis()))
        }
    }

    suspend fun deleteRecentSearch(query: String) = withContext(Dispatchers.IO) {
        dao.deleteRecentSearch(query)
    }

    suspend fun clearRecentSearches() = withContext(Dispatchers.IO) {
        dao.clearRecentSearches()
    }

    // --- Main Weather Retriever with Cache Fallback ---
    suspend fun getWeatherData(query: String): WeatherData = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        val cacheKey = trimmedQuery.lowercase()

        // Log recent search
        addRecentSearch(trimmedQuery)

        try {
            // In a real app we would call a remote weather API here.
            // We use our Meteorologically Consistent Generator to create beautifully structured and realistic data.
            val freshData = generateWeatherForQuery(trimmedQuery)

            // Cache it for offline access
            val json = weatherAdapter.toJson(freshData)
            dao.insertWeatherCache(
                WeatherOfflineCache(
                    cityKey = cacheKey,
                    weatherDataJson = json,
                    timestamp = System.currentTimeMillis()
                )
            )
            return@withContext freshData
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate fresh weather, trying offline cache for: $trimmedQuery", e)
            val cached = dao.getCachedWeather(cacheKey)
            if (cached != null) {
                try {
                    val cachedData = weatherAdapter.fromJson(cached.weatherDataJson)
                    if (cachedData != null) {
                        return@withContext cachedData
                    }
                } catch (pe: Exception) {
                    Log.e(TAG, "Error parsing offline weather cache", pe)
                }
            }
            // Absolute fallback if cache is empty too
            return@withContext generateWeatherForQuery("San Francisco")
        }
    }

    // --- Meteorologically Consistent Generator ---
    private fun generateWeatherForQuery(query: String): WeatherData {
        // Parse "City, State, Country"
        val parts = query.split(",").map { it.trim() }
        val city = parts.getOrNull(0) ?: "San Francisco"
        val state = parts.getOrNull(1) ?: when (city.lowercase()) {
            "san francisco", "los angeles", "san diego", "sacramento" -> "CA"
            "new york", "brooklyn" -> "NY"
            "miami", "orlando", "tampa" -> "FL"
            "london" -> "England"
            "tokyo" -> "Kanto"
            "paris" -> "Île-de-France"
            "cairo" -> "Greater Cairo"
            "sydney" -> "NSW"
            else -> "Region"
        }
        val country = parts.getOrNull(2) ?: when (city.lowercase()) {
            "san francisco", "los angeles", "san diego", "sacramento", "new york", "brooklyn", "miami", "orlando", "tampa", "phoenix", "denver" -> "United States"
            "london", "manchester", "birmingham" -> "United Kingdom"
            "tokyo", "kyoto", "osaka" -> "Japan"
            "paris", "nice", "lyon" -> "France"
            "cairo", "giza", "alexandria" -> "Egypt"
            "sydney", "melbourne", "brisbane" -> "Australia"
            "mumbai", "delhi", "bangalore" -> "India"
            "toronto", "vancouver", "montreal" -> "Canada"
            "singapore" -> "Singapore"
            "jakarta" -> "Indonesia"
            else -> "Global"
        }

        // Generate stable coordinates from city name hash
        val cityHash = abs(city.hashCode())
        val lat = -90.0 + (cityHash % 180000) / 1000.0
        val lon = -180.0 + ((cityHash / 180) % 360000) / 1000.0

        // Classify Climate Base
        val climate = getClimateType(city, country)
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)

        // Seasonal variation using sine wave
        val seasonOffset = 12 * sin(2 * Math.PI * (dayOfYear - 80) / 365.0).toFloat()

        val baseTemp = when (climate) {
            ClimateType.HOT -> 32f + (cityHash % 8) - 4f
            ClimateType.COLD -> -5f + (cityHash % 10) - 5f
            ClimateType.RAINY -> 22f + (cityHash % 6) - 3f
            ClimateType.TROPICAL -> 28f + (cityHash % 4) - 2f
            ClimateType.NORMAL -> 17f + (cityHash % 10) - 5f
        }
        val currentTemp = baseTemp + seasonOffset

        // Weather Condition based on climate and seed
        val condSeed = (cityHash + dayOfYear) % 100
        val conditionAndDesc = determineWeatherCondition(climate, condSeed)
        val condition = conditionAndDesc.first
        val conditionDesc = conditionAndDesc.second

        // Min / Max
        val minTemp = currentTemp - 4f - (cityHash % 4)
        val maxTemp = currentTemp + 5f + (cityHash % 4)

        // Metrics matching condition
        val humidity = when (condition) {
            "Rain", "Thunderstorm" -> 85 + (cityHash % 15)
            "Snow" -> 75 + (cityHash % 15)
            "Cloudy", "Overcast" -> 70 + (cityHash % 15)
            "Clear" -> 35 + (cityHash % 20)
            else -> 50 + (cityHash % 20)
        }

        val pressure = 1013 + when (condition) {
            "Rain", "Thunderstorm" -> -15 + (cityHash % 8)
            "Clear" -> 10 + (cityHash % 5)
            else -> (cityHash % 10) - 5
        }

        val visibility = when (condition) {
            "Fog", "Haze" -> 1.5f + (cityHash % 20) / 10f
            "Rain" -> 5f + (cityHash % 4)
            "Thunderstorm" -> 3f + (cityHash % 3)
            "Snow" -> 4f + (cityHash % 4)
            else -> 9.5f + (cityHash % 10) / 10f
        }

        val uvIndex = when (condition) {
            "Clear" -> if (climate == ClimateType.HOT || climate == ClimateType.TROPICAL) 10 else 7
            "Cloudy", "Overcast" -> 2 + (cityHash % 2)
            "Rain", "Thunderstorm" -> 1
            else -> 4 + (cityHash % 3)
        }

        // AQI (Air Quality Index) - Higher in hot/dense cities
        val baseAqi = when (city.lowercase()) {
            "delhi", "cairo", "mumbai", "jakarta" -> 140
            "tokyo", "paris", "new york" -> 55
            "sydney", "vancouver", "reykjavik" -> 15
            else -> 30
        }
        val aqiOffset = (cityHash % 45)
        val aqi = baseAqi + aqiOffset
        val aqiLabel = getAqiLabel(aqi)

        val dewPoint = currentTemp - ((100 - humidity) / 5)
        val cloudCoverage = when (condition) {
            "Clear" -> 5 + (cityHash % 10)
            "Partly Cloudy" -> 35 + (cityHash % 25)
            "Cloudy", "Haze" -> 70 + (cityHash % 20)
            "Rain", "Thunderstorm", "Snow", "Overcast" -> 90 + (cityHash % 10)
            else -> 50
        }

        // Sunrise/Sunset dynamic times based on season
        val sunriseHour = 5 + (seasonOffset / 10).toInt()
        val sunriseMin = abs((cityHash % 60))
        val sunsetHour = 18 - (seasonOffset / 10).toInt()
        val sunsetMin = abs((cityHash % 60))

        val sunrise = String.format(Locale.getDefault(), "%02d:%02d AM", sunriseHour, sunriseMin)
        val sunset = String.format(Locale.getDefault(), "%02d:%02d PM", sunsetHour, sunsetMin)

        // Moonrise/Moonset
        val moonriseHour = (sunriseHour + 12) % 24
        val moonrise = String.format(Locale.getDefault(), "%02d:%02d PM", moonriseHour, sunriseMin)
        val moonsetHour = (sunsetHour + 12) % 24
        val moonset = String.format(Locale.getDefault(), "%02d:%02d AM", moonsetHour, sunsetMin)

        // Wind Speed & Gusts
        val windSpeed = (8f + (cityHash % 15) + (if (condition == "Thunderstorm") 20f else 0f))
        val windGust = windSpeed * 1.35f
        val windDirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
        val windDirection = windDirs[cityHash % windDirs.size]

        val chanceOfRain = when (condition) {
            "Rain" -> 85 + (cityHash % 15)
            "Thunderstorm" -> 90 + (cityHash % 10)
            "Snow" -> 10 // snow is rain chance 10, snow is high
            "Cloudy" -> 20 + (cityHash % 20)
            "Partly Cloudy" -> 10 + (cityHash % 10)
            else -> 0
        }

        // Pollen Index
        val pollenIndex = when (condition) {
            "Rain", "Thunderstorm", "Snow" -> 1 + (cityHash % 2)
            "Clear" -> 7 + (cityHash % 4)
            else -> 4 + (cityHash % 3)
        }
        val pollenLabel = when (pollenIndex) {
            in 0..2 -> "Low"
            in 3..5 -> "Moderate"
            in 6..8 -> "High"
            else -> "Very High"
        }

        // Generate Hourly 24 hours
        val hourlyList = ArrayList<HourlyForecastItem>()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        for (i in 0 until 24) {
            val forecastHour = (currentHour + i) % 24
            val ampm = if (forecastHour >= 12) "PM" else "AM"
            val displayHour = when {
                forecastHour == 0 -> 12
                forecastHour > 12 -> forecastHour - 12
                else -> forecastHour
            }
            val timeString = "$displayHour $ampm"

            // Temp progression: warm in afternoon, cool in morning
            val hourAngle = 2 * Math.PI * (forecastHour - 15) / 24.0
            val diurnalOffset = 3f * sin(hourAngle).toFloat()
            val hourlyTemp = currentTemp + diurnalOffset + ((cityHash + i) % 3 - 1)

            // Rain probability varies hourly
            val hourlyRainProb = when (condition) {
                "Rain", "Thunderstorm" -> (70 + ((cityHash + i) % 31))
                "Partly Cloudy", "Cloudy" -> (15 + ((cityHash + i) % 20))
                else -> (cityHash + i) % 10
            }

            // Hour condition
            val hourlyCond = when {
                hourlyRainProb > 70 -> if (condition == "Thunderstorm") "Thunderstorm" else "Rain"
                hourlyRainProb > 30 -> "Partly Cloudy"
                else -> "Clear"
            }

            // Wind speed fluctuations
            val hourlyWind = windSpeed + ((cityHash + i) % 5 - 2f)

            // Humidity hourly
            val hourlyHumidity = (humidity - (diurnalOffset * 4).toInt()).coerceIn(15, 100)

            // UV hourly
            val hourlyUv = if (forecastHour in 8..17) {
                val dist = abs(forecastHour - 12)
                (uvIndex - dist).coerceAtLeast(0)
            } else {
                0
            }

            hourlyList.add(
                HourlyForecastItem(
                    time = if (i == 0) "Now" else timeString,
                    temperature = hourlyTemp,
                    condition = hourlyCond,
                    rainProbability = hourlyRainProb,
                    windSpeed = hourlyWind,
                    humidity = hourlyHumidity,
                    uvIndex = hourlyUv
                )
            )
        }

        // Generate 15 Days
        val dailyList = ArrayList<DailyForecastItem>()
        val sfd = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        for (i in 0 until 15) {
            val fCal = Calendar.getInstance()
            fCal.add(Calendar.DAY_OF_YEAR, i)
            val dateStr = sfd.format(fCal.time)
            val dayName = fCal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.getDefault()) ?: "Day"

            // Daily variability
            val daySeed = (cityHash + dayOfYear + i) % 100
            val dClimate = getClimateType(city, country)
            val dayCondAndDesc = determineWeatherCondition(dClimate, daySeed)
            val dayCond = dayCondAndDesc.first

            val dMax = maxTemp + ((cityHash + i) % 6 - 3f)
            val dMin = minTemp + ((cityHash + i) % 4 - 2f)

            val dRainProb = when (dayCond) {
                "Rain" -> 75 + (daySeed % 25)
                "Thunderstorm" -> 80 + (daySeed % 20)
                "Snow" -> 5
                "Cloudy" -> 30 + (daySeed % 20)
                else -> daySeed % 15
            }

            dailyList.add(
                DailyForecastItem(
                    date = dateStr,
                    dayOfWeek = dayName,
                    maxTemp = dMax,
                    minTemp = dMin,
                    condition = dayCond,
                    rainProbability = dRainProb,
                    windSpeed = windSpeed + ((cityHash + i) % 6 - 3f),
                    sunrise = sunrise,
                    sunset = sunset
                )
            )
        }

        // Severe Alerts
        val alerts = ArrayList<WeatherAlert>()
        if (condition == "Thunderstorm") {
            alerts.add(
                WeatherAlert(
                    id = "alert_ts_${cityHash}",
                    title = "Severe Thunderstorm Warning",
                    sender = "National Meteorological Service",
                    severity = "Emergency",
                    area = "$city and surrounding suburbs",
                    description = "A strong thunderstorm cluster is moving through the area, capable of producing destructive winds, frequent cloud-to-ground lightning, and localized torrential downpours. Street-level flash flooding is highly possible.",
                    recommendation = "Seek interior shelter immediately inside a sturdy building. Disconnect sensitive electronic devices. Do not attempt to drive through waterlogged roads.",
                    startEpoch = System.currentTimeMillis() - 1200000,
                    endEpoch = System.currentTimeMillis() + 10800000
                )
            )
        } else if (condition == "Rain" && chanceOfRain > 90) {
            alerts.add(
                WeatherAlert(
                    id = "alert_flood_${cityHash}",
                    title = "Heavy Rainfall Advisory",
                    sender = "Regional Hydrology Agency",
                    severity = "Warning",
                    area = "$city metropolitan area",
                    description = "Continuous intense rain showers have led to water saturation of local drainage systems. River banks are reaching alert capacity, and heavy rainfall is expected to persist for the next 4 hours.",
                    recommendation = "Avoid non-essential outdoor travel. Keep storm drains clear and monitor local weather channels for evacuation routes if living in low-lying zones.",
                    startEpoch = System.currentTimeMillis(),
                    endEpoch = System.currentTimeMillis() + 14400000
                )
            )
        }

        if (currentTemp > 35f) {
            alerts.add(
                WeatherAlert(
                    id = "alert_heat_${cityHash}",
                    title = "Extreme Heat Index Advisory",
                    sender = "Public Health Authority",
                    severity = "Warning",
                    area = "Entire $city District",
                    description = "An extreme heatwave is triggering high heat index indicators exceeding 40°C. Prolonged solar exposure can cause rapid heat exhaustion or heatstroke in infants and high-risk populations.",
                    recommendation = "Stay inside air-conditioned rooms. Wear lightweight clothing. Keep pets indoors with fresh water and never leave children inside closed vehicles.",
                    startEpoch = System.currentTimeMillis() - 3600000,
                    endEpoch = System.currentTimeMillis() + 86400000
                )
            )
        }

        if (aqi > 150) {
            alerts.add(
                WeatherAlert(
                    id = "alert_aqi_${cityHash}",
                    title = "Air Pollution Alert",
                    sender = "Environmental Quality Agency",
                    severity = "Warning",
                    area = "$city Air Basin",
                    description = "Fine particulate matter (PM2.5) concentrations have peaked. Air quality indicators have reached the 'Unhealthy' tier, showing visible haze and triggering respiratory irritation.",
                    recommendation = "Sensitive individuals should wear N95 respirators. Limit heavy outdoor activity, keep indoor windows closed, and run HEPA air purifiers.",
                    startEpoch = System.currentTimeMillis(),
                    endEpoch = System.currentTimeMillis() + 43200000
                )
            )
        }

        return WeatherData(
            cityName = city,
            stateName = state,
            countryName = country,
            latitude = lat,
            longitude = lon,
            temperature = currentTemp,
            feelsLike = currentTemp + (if (humidity > 70) 2f else -1f) + (if (windSpeed > 15) -1.5f else 0f),
            condition = condition,
            conditionDescription = conditionDesc,
            minTemperature = minTemp,
            maxTemperature = maxTemp,
            humidity = humidity,
            pressure = pressure,
            visibility = visibility,
            uvIndex = uvIndex,
            aqi = aqi,
            aqiLabel = aqiLabel,
            dewPoint = dewPoint,
            cloudCoverage = cloudCoverage,
            sunrise = sunrise,
            sunset = sunset,
            moonrise = moonrise,
            moonset = moonset,
            windSpeed = windSpeed,
            windDirection = windDirection,
            windGust = windGust,
            chanceOfRain = chanceOfRain,
            pollenIndex = pollenIndex,
            pollenLabel = pollenLabel,
            hourlyForecast = hourlyList,
            dailyForecast = dailyList,
            alerts = alerts
        )
    }

    private fun getClimateType(city: String, country: String): ClimateType {
        val q = "${city.lowercase()}, ${country.lowercase()}"
        return when {
            q.contains("oslo") || q.contains("toronto") || q.contains("reykjavik") || q.contains("moscow") || q.contains("anchorage") || q.contains("canada") || q.contains("alaska") || q.contains("sweden") || q.contains("norway") || q.contains("finland") || q.contains("ice") -> ClimateType.COLD
            q.contains("cairo") || q.contains("dubai") || q.contains("phoenix") || q.contains("riyadh") || q.contains("egypt") || q.contains("saudi") || q.contains("dehli") || q.contains("desert") || q.contains("arizona") -> ClimateType.HOT
            q.contains("london") || q.contains("seattle") || q.contains("vancouver") || q.contains("uk") || q.contains("singapore") || q.contains("jakarta") || q.contains("ireland") -> ClimateType.RAINY
            q.contains("miami") || q.contains("honolulu") || q.contains("bali") || q.contains("mumbai") || q.contains("bangkok") || q.contains("caribbean") || q.contains("hawaii") || q.contains("tropical") -> ClimateType.TROPICAL
            else -> ClimateType.NORMAL
        }
    }

    private fun determineWeatherCondition(climate: ClimateType, seed: Int): Pair<String, String> {
        return when (climate) {
            ClimateType.COLD -> {
                when {
                    seed < 30 -> Pair("Snow", "Light Snow Flurries")
                    seed < 55 -> Pair("Snow", "Heavy Snowfall")
                    seed < 75 -> Pair("Cloudy", "Overcast Skies")
                    seed < 90 -> Pair("Fog", "Freezing Fog")
                    else -> Pair("Clear", "Sunny & Frozen")
                }
            }
            ClimateType.HOT -> {
                when {
                    seed < 65 -> Pair("Clear", "Sunny and Hot")
                    seed < 85 -> Pair("Clear", "Mostly Sunny")
                    seed < 95 -> Pair("Haze", "Dusty Haze")
                    else -> Pair("Rain", "Sudden Desert Shower")
                }
            }
            ClimateType.RAINY -> {
                when {
                    seed < 40 -> Pair("Rain", "Light Steady Drizzle")
                    seed < 65 -> Pair("Rain", "Moderate Showers")
                    seed < 80 -> Pair("Cloudy", "Damp & Overcast")
                    seed < 92 -> Pair("Partly Cloudy", "Passing Sun Breaks")
                    else -> Pair("Thunderstorm", "Scattered Storm Cells")
                }
            }
            ClimateType.TROPICAL -> {
                when {
                    seed < 35 -> Pair("Clear", "Bright & Sunny")
                    seed < 60 -> Pair("Partly Cloudy", "Humid and Sun")
                    seed < 85 -> Pair("Rain", "Tropical Monsoon Showers")
                    else -> Pair("Thunderstorm", "Tropical Thunderstorm")
                }
            }
            ClimateType.NORMAL -> {
                when {
                    seed < 40 -> Pair("Clear", "Sunny and Clear")
                    seed < 65 -> Pair("Partly Cloudy", "Passing Clouds")
                    seed < 80 -> Pair("Cloudy", "Mostly Cloudy")
                    seed < 92 -> Pair("Rain", "Scattered Rain Showers")
                    else -> Pair("Thunderstorm", "Local Thunderstorm")
                }
            }
        }
    }

    private fun getAqiLabel(aqi: Int): String {
        return when {
            aqi <= 50 -> "Good"
            aqi <= 100 -> "Moderate"
            aqi <= 150 -> "Unhealthy for Sensitive Groups"
            aqi <= 200 -> "Unhealthy"
            aqi <= 300 -> "Very Unhealthy"
            else -> "Hazardous"
        }
    }

    private enum class ClimateType {
        HOT, COLD, RAINY, TROPICAL, NORMAL
    }
}
