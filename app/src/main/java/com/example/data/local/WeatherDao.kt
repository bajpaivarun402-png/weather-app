package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    // Favorites
    @Query("SELECT * FROM favorite_locations ORDER BY timestamp DESC")
    fun getAllFavorites(): Flow<List<FavoriteLocation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(location: FavoriteLocation)

    @Query("DELETE FROM favorite_locations WHERE LOWER(city) = LOWER(:city) AND LOWER(country) = LOWER(:country)")
    suspend fun deleteFavoriteByCity(city: String, country: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_locations WHERE LOWER(city) = LOWER(:city) AND LOWER(country) = LOWER(:country))")
    fun isFavorite(city: String, country: String): Flow<Boolean>


    // Recent Searches
    @Query("SELECT * FROM recent_searches ORDER BY timestamp DESC LIMIT 10")
    fun getRecentSearches(): Flow<List<RecentSearch>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecentSearch(search: RecentSearch)

    @Query("DELETE FROM recent_searches WHERE LOWER(`query`) = LOWER(:query)")
    suspend fun deleteRecentSearch(query: String)

    @Query("DELETE FROM recent_searches")
    suspend fun clearRecentSearches()


    // Weather Offline Cache
    @Query("SELECT * FROM weather_cache WHERE LOWER(cityKey) = LOWER(:cityKey)")
    suspend fun getCachedWeather(cityKey: String): WeatherOfflineCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherCache(cache: WeatherOfflineCache)
}
