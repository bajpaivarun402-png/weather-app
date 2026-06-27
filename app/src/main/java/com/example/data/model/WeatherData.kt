package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WeatherData(
    val cityName: String,
    val stateName: String,
    val countryName: String,
    val latitude: Double,
    val longitude: Double,
    val temperature: Float,
    val feelsLike: Float,
    val condition: String,
    val conditionDescription: String,
    val minTemperature: Float,
    val maxTemperature: Float,
    val humidity: Int,
    val pressure: Int,
    val visibility: Float,
    val uvIndex: Int,
    val aqi: Int,
    val aqiLabel: String,
    val dewPoint: Float,
    val cloudCoverage: Int,
    val sunrise: String,
    val sunset: String,
    val moonrise: String,
    val moonset: String,
    val windSpeed: Float,
    val windDirection: String,
    val windGust: Float,
    val chanceOfRain: Int,
    val pollenIndex: Int,
    val pollenLabel: String,
    val hourlyForecast: List<HourlyForecastItem>,
    val dailyForecast: List<DailyForecastItem>,
    val alerts: List<WeatherAlert>
)

@JsonClass(generateAdapter = true)
data class HourlyForecastItem(
    val time: String,
    val temperature: Float,
    val condition: String,
    val rainProbability: Int,
    val windSpeed: Float,
    val humidity: Int,
    val uvIndex: Int
)

@JsonClass(generateAdapter = true)
data class DailyForecastItem(
    val date: String,
    val dayOfWeek: String,
    val maxTemp: Float,
    val minTemp: Float,
    val condition: String,
    val rainProbability: Int,
    val windSpeed: Float,
    val sunrise: String,
    val sunset: String
)

@JsonClass(generateAdapter = true)
data class WeatherAlert(
    val id: String,
    val title: String,
    val sender: String,
    val severity: String, // Info, Warning, Emergency
    val area: String,
    val description: String,
    val recommendation: String,
    val startEpoch: Long,
    val endEpoch: Long
)

data class SavedLocation(
    val id: Int = 0,
    val city: String,
    val state: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isFavorite: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
