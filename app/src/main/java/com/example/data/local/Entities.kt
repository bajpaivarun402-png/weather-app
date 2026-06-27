package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_locations")
data class FavoriteLocation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val city: String,
    val state: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "recent_searches")
data class RecentSearch(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weather_cache")
data class WeatherOfflineCache(
    @PrimaryKey val cityKey: String, // e.g., "London, United Kingdom"
    val weatherDataJson: String,
    val timestamp: Long = System.currentTimeMillis()
)
