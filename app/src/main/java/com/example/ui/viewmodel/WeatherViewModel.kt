package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.FavoriteLocation
import com.example.data.local.RecentSearch
import com.example.data.model.WeatherData
import com.example.data.remote.AiInsights
import com.example.data.remote.GeminiService
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WeatherRepository(application)
    private val prefs = application.getSharedPreferences("weather_app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "WeatherViewModel"
    }

    // --- Navigation UI Page State ---
    private val _currentPage = MutableStateFlow(
        if (prefs.getBoolean("onboarding_completed", false)) WeatherPage.HOME else WeatherPage.ONBOARDING
    )
    val currentPage: StateFlow<WeatherPage> = _currentPage.asStateFlow()

    fun navigateTo(page: WeatherPage) {
        _currentPage.value = page
        if (page == WeatherPage.ONBOARDING) {
            prefs.edit().putBoolean("onboarding_completed", false).apply()
        }
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean("onboarding_completed", true).apply()
        _currentPage.value = WeatherPage.HOME
    }

    // --- Weather State ---
    private val _weatherState = MutableStateFlow<WeatherState>(WeatherState.Loading)
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _currentQuery = MutableStateFlow("New York")
    val currentQuery: StateFlow<String> = _currentQuery.asStateFlow()

    // --- Favorites ---
    val favoriteLocations: StateFlow<List<FavoriteLocation>> = repository.allFavorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Recent Searches ---
    val recentSearches: StateFlow<List<RecentSearch>> = repository.recentSearches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Settings Preferences ---
    private val _isFahrenheit = MutableStateFlow(prefs.getBoolean("pref_is_fahrenheit", false))
    val isFahrenheit: StateFlow<Boolean> = _isFahrenheit.asStateFlow()

    fun toggleTemperatureUnit() {
        val newValue = !_isFahrenheit.value
        _isFahrenheit.value = newValue
        prefs.edit().putBoolean("pref_is_fahrenheit", newValue).apply()
    }

    private val _selectedLanguage = MutableStateFlow(prefs.getString("pref_language", "English") ?: "English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun setLanguage(language: String) {
        _selectedLanguage.value = language
        prefs.edit().putString("pref_language", language).apply()
    }

    private val _refreshInterval = MutableStateFlow(prefs.getInt("pref_refresh_interval", 30)) // minutes
    val refreshInterval: StateFlow<Int> = _refreshInterval.asStateFlow()

    fun setRefreshInterval(minutes: Int) {
        _refreshInterval.value = minutes
        prefs.edit().putInt("pref_refresh_interval", minutes).apply()
    }

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("pref_notifications", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("pref_notifications", enabled).apply()
    }

    private val _privacyEnabled = MutableStateFlow(prefs.getBoolean("pref_privacy", true))
    val privacyEnabled: StateFlow<Boolean> = _privacyEnabled.asStateFlow()

    fun setPrivacyEnabled(enabled: Boolean) {
        _privacyEnabled.value = enabled
        prefs.edit().putBoolean("pref_privacy", enabled).apply()
    }

    private val _gpsPermissionGranted = MutableStateFlow(prefs.getBoolean("pref_gps_allowed", false))
    val gpsPermissionGranted: StateFlow<Boolean> = _gpsPermissionGranted.asStateFlow()

    fun setGpsPermissionGranted(granted: Boolean) {
        _gpsPermissionGranted.value = granted
        prefs.edit().putBoolean("pref_gps_allowed", granted).apply()
    }

    // Dynamic checks
    val isCurrentCityFavorite = MutableStateFlow(false)

    init {
        // Load default weather
        val lastCity = prefs.getString("last_searched_city", "New York") ?: "New York"
        _currentQuery.value = lastCity
        loadWeather(lastCity)

        // Observe favorite status for the currently loaded city
        viewModelScope.launch {
            _currentQuery.collectLatest { query ->
                repository.isFavorite(query, "").collect { isFav ->
                    isCurrentCityFavorite.value = isFav
                }
            }
        }
    }

    fun loadWeather(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _weatherState.value = WeatherState.Loading
            _currentQuery.value = query
            prefs.edit().putString("last_searched_city", query).apply()

            try {
                val weather = repository.getWeatherData(query)
                // Async load Gemini insights
                _weatherState.value = WeatherState.Success(weather, null, true)
                loadGeminiInsights(weather)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading weather: ${e.message}", e)
                _weatherState.value = WeatherState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    private fun loadGeminiInsights(weather: WeatherData) {
        viewModelScope.launch {
            try {
                val insights = GeminiService.getAiInsights(weather)
                val currentState = _weatherState.value
                if (currentState is WeatherState.Success && currentState.weatherData.cityName == weather.cityName) {
                    _weatherState.value = WeatherState.Success(weather, insights, false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading Gemini insights: ${e.message}", e)
            }
        }
    }

    fun toggleFavoriteCurrent() {
        val currentState = _weatherState.value
        if (currentState is WeatherState.Success) {
            val weather = currentState.weatherData
            viewModelScope.launch {
                if (isCurrentCityFavorite.value) {
                    repository.removeFavorite(weather.cityName, weather.countryName)
                } else {
                    repository.addFavorite(
                        city = weather.cityName,
                        state = weather.stateName,
                        country = weather.countryName,
                        lat = weather.latitude,
                        lon = weather.longitude
                    )
                }
            }
        }
    }

    fun addFavoriteLocation(city: String, state: String, country: String, lat: Double, lon: Double) {
        viewModelScope.launch {
            repository.addFavorite(city, state, country, lat, lon)
        }
    }

    fun removeFavoriteLocation(city: String, country: String) {
        viewModelScope.launch {
            repository.removeFavorite(city, country)
        }
    }

    fun deleteRecentSearch(query: String) {
        viewModelScope.launch {
            repository.deleteRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            repository.clearRecentSearches()
        }
    }
}

sealed interface WeatherState {
    object Loading : WeatherState
    data class Success(
        val weatherData: WeatherData,
        val aiInsights: AiInsights?,
        val loadingInsights: Boolean
    ) : WeatherState
    data class Error(val message: String) : WeatherState
}

enum class WeatherPage {
    SPLASH, ONBOARDING, HOME, SEARCH, CITY_DETAILS, HOURLY_FORECAST, WEEKLY_FORECAST, WEATHER_MAPS, ALERTS, SETTINGS, FAVORITES
}
