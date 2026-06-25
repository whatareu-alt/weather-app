package com.example.aiweathermonitor.data.persistence

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.aiweathermonitor.WeatherState
import com.example.aiweathermonitor.config.WeatherApiConfig
import com.example.aiweathermonitor.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * DataStore instance for weather app preferences.
 * Replaces deprecated SharedPreferences with a more modern approach.
 */
private val Context.weatherDataStore by preferencesDataStore(
    name = "weather_prefs"
)

/**
 * Utility for managing weather data persistence using DataStore.
 * Thread-safe and supports Flow-based reactive updates.
 * 
 * DataStore provides:
 * - Type-safe key-value storage
 * - ACID transactions
 * - Coroutine-based API
 * - Migration from SharedPreferences
 * 
 * @param context Application context
 * @param json JSON serializer for complex objects
 */
class WeatherDataStore(
    private val context: Context,
    private val json: Json
) {
    private val TAG = "WeatherDataStore"
    
    private val WEATHER_STATE_KEY = stringPreferencesKey(
        WeatherApiConfig.SharedPrefsKeys.WEATHER_STATE_KEY
    )
    private val LAST_UPDATED_KEY = stringPreferencesKey(
        WeatherApiConfig.SharedPrefsKeys.LAST_REFRESH_KEY
    )
    private val API_KEY_KEY = stringPreferencesKey(
        WeatherApiConfig.SharedPrefsKeys.API_KEY_KEY
    )
    
    /**
     * Observes cached weather state as a Flow.
     * Emits current value immediately, then any updates.
     */
    val cachedWeatherState: Flow<WeatherState?> = context.weatherDataStore.data
        .map { preferences ->
            try {
                val jsonStr = preferences[WEATHER_STATE_KEY]
                if (jsonStr != null) {
                    json.decodeFromString(WeatherState.serializer(), jsonStr)
                } else {
                    null
                }
            } catch (e: Exception) {
                AppLogger.error("Failed to decode cached weather state", TAG, e)
                null
            }
        }
    
    /**
     * Saves weather state to cache.
     * 
     * @param state Weather state to cache
     * @return Result indicating success or failure
     */
    suspend fun cacheWeatherState(state: WeatherState): Result<Unit> = try {
        val jsonStr = json.encodeToString(WeatherState.serializer(), state)
        context.weatherDataStore.edit { preferences ->
            preferences[WEATHER_STATE_KEY] = jsonStr
            preferences[LAST_UPDATED_KEY] = System.currentTimeMillis().toString()
        }
        AppLogger.info("Weather state cached successfully", TAG)
        Result.success(Unit)
    } catch (e: Exception) {
        AppLogger.error("Failed to cache weather state", TAG, e)
        Result.failure(e)
    }
    
    /**
     * Retrieves cached weather state.
     * 
     * @return Result containing cached state or null if not cached
     */
    suspend fun getCachedWeatherState(): Result<WeatherState?> = try {
        val state = context.weatherDataStore.data
            .map { prefs ->
                val jsonStr = prefs[WEATHER_STATE_KEY] ?: return@map null
                json.decodeFromString(WeatherState.serializer(), jsonStr)
            }
            .first()
        Result.success(state)
    } catch (e: Exception) {
        AppLogger.error("Failed to retrieve cached weather state", TAG, e)
        Result.failure(e)
    }
    
    /**
     * Clears all cached weather data.
     * 
     * @return Result indicating success or failure
     */
    suspend fun clearCache(): Result<Unit> = try {
        context.weatherDataStore.edit { preferences ->
            preferences.clear()
        }
        AppLogger.info("Weather cache cleared", TAG)
        Result.success(Unit)
    } catch (e: Exception) {
        AppLogger.error("Failed to clear cache", TAG, e)
        Result.failure(e)
    }
    
    /**
     * Checks if cached data is still valid.
     * Cache is valid if it exists and is less than CACHE_EXPIRATION_MS old.
     * 
     * @return true if cache exists and is fresh, false otherwise
     */
    suspend fun isCacheValid(): Boolean = try {
        val prefs = context.weatherDataStore.data.first()
        val lastUpdated = prefs[LAST_UPDATED_KEY]?.toLongOrNull() ?: return false
        val ageMs = System.currentTimeMillis() - lastUpdated
        ageMs < WeatherApiConfig.CACHE_EXPIRATION_MS && prefs[WEATHER_STATE_KEY] != null
    } catch (e: Exception) {
        AppLogger.error("Failed to check cache validity", TAG, e)
        false
    }
}
