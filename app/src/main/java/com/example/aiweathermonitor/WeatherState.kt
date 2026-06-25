package com.example.aiweathermonitor

import com.example.aiweathermonitor.data.models.PollenData
import com.example.aiweathermonitor.data.models.NewsArticle
import kotlinx.serialization.Serializable

// ── Weather code → description ───────────────────────────────────────────────

fun Int.getWeatherCodeDescription(): String = when (this) {
    0 -> "Clear Sky"
    1, 2, 3 -> "Partly Cloudy"
    45, 48 -> "Fog / Rime Fog"
    51, 53, 55 -> "Drizzle"
    61, 63, 65 -> "Rainy"
    71, 73, 75 -> "Snowy"
    80, 81, 82 -> "Rain Showers"
    95, 96, 99 -> "Thunderstorm"
    else -> "Unstable / Varied"
}

// ── UV index → description ───────────────────────────────────────────────────

fun Float.getUvDescription(): String = when {
    this < 3f  -> "Low"
    this < 6f  -> "Moderate"
    this < 8f  -> "High"
    this < 11f -> "Very High"
    else       -> "Extreme"
}

// ── AQI (US EPA 1-6) → description ──────────────────────────────────────────

fun Int.getAqiDescription(): String = when (this) {
    1 -> "Good"
    2 -> "Moderate"
    3 -> "Unhealthy for Sensitive Groups"
    4 -> "Unhealthy"
    5 -> "Very Unhealthy"
    6 -> "Hazardous"
    else -> "Unknown"
}

// ── Data models ──────────────────────────────────────────────────────────────

@Serializable
data class GeocodingResult(
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val country: String? = null,
    val admin1: String? = null
)

fun createGeocodingResult(
    name: String?,
    latitude: Double?,
    longitude: Double?,
    country: String? = null,
    admin1: String? = null
): GeocodingResult = GeocodingResult(
    name = name ?: "",
    latitude = latitude ?: 0.0,
    longitude = longitude ?: 0.0,
    country = country,
    admin1 = admin1
)

@Serializable
data class HourForecast(
    val hour: String,
    val temperature: Float,
    val weatherCode: Int,
    val precipChance: Int = 0        // % chance of rain (0-100)
)

@Serializable
data class DayForecast(
    val day: String,
    val tempMin: Float,
    val tempMax: Float,
    val weatherCode: Int,
    val uvIndex: Float = 0f,         // peak UV for the day
    val precipChance: Int = 0        // % chance of rain (0-100)
)

@Serializable
data class WeatherState(
    // Geocoding & City
    val searchQuery: String = "",
    val selectedCity: String = "New York",
    val timezone: String = "",
    val latitude: Double = 40.7128,
    val longitude: Double = -74.0060,
    val searchResults: List<GeocodingResult> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val errorMessage: String? = null,

    // WeatherAPI.com key — runtime override; build-time default from BuildConfig
    val weatherApiKey: String = "",
    val showKeyDialog: Boolean = false,

    // Saved Cities & Units
    val savedCities: List<GeocodingResult> = emptyList(),
    val isCelsius: Boolean = true,
    val showSavedCitiesScreen: Boolean = false,

    // Current conditions
    val temperature: Float = 22.0f,
    val feelsLike: Float = 22.0f,
    val humidity: Float = 50.0f,
    val pressure: Float = 1013.0f,
    val windSpeed: Float = 12.0f,
    val windDir: String = "",        // e.g. "NNE"
    val visibility: Float = 10.0f,   // km
    val aqi: Int = 0,                // US EPA index 1-6 (0 = unknown)
    val uvIndex: Float = 3f,
    val weatherCode: Int = 0,
    val sunrise: String = "6:00 AM",
    val sunset: String = "6:00 PM",
    val sunriseMinutes: Int = 360,
    val sunsetMinutes: Int = 1080,

    // Forecasts
    val hourlyForecast: List<HourForecast> = emptyList(),
    val dailyForecast: List<DayForecast> = emptyList(),

    // Connectivity & cache
    val isOnline: Boolean = true,
    val lastRefreshedTime: String = "",
    val lastRefreshedEpoch: Long = 0L,   // System.currentTimeMillis() at last successful fetch
    val smartSummary: String = "",      // Generated one-line briefing
    @kotlinx.serialization.Transient
    val pollenData: PollenData? = null,   // Pollen concentrations (not persisted)

    // Alerts
    val alertEvent: String? = null,
    val alertHeadline: String? = null,
    val alertDescription: String? = null,

    // News (RSS) — not persisted; refreshed from the feed on each launch
    @kotlinx.serialization.Transient
    val newsArticles: List<NewsArticle> = emptyList(),        // weather news for the city
    @kotlinx.serialization.Transient
    val localNewsArticles: List<NewsArticle> = emptyList(),   // general/local news for the city
    @kotlinx.serialization.Transient
    val isNewsLoading: Boolean = false,
    @kotlinx.serialization.Transient
    val newsError: String? = null
)

// ── Smart summary generator ──────────────────────────────────────────────────

fun generateSmartSummary(state: WeatherState): String {
    val parts = mutableListOf<String>()

    // Rain alert: find first hourly slot with precip >= 40%
    val rainHour = state.hourlyForecast.firstOrNull { it.precipChance >= 40 }
    if (rainHour != null) {
        parts.add("Carry an umbrella — rain likely around ${rainHour.hour}")
    }

    // UV peak warning
    if (state.uvIndex >= 8f) {
        parts.add("UV peaks at ${state.uvIndex.toInt()} — apply sunscreen")
    } else if (state.uvIndex >= 6f) {
        parts.add("Moderate UV today — sunscreen recommended")
    }

    // Temperature extremes
    when {
        state.temperature >= 38f -> parts.add("Extreme heat — stay hydrated and limit outdoor activity")
        state.temperature >= 35f -> parts.add("Very hot today — stay cool and drink water")
        state.temperature <= 0f  -> parts.add("Below zero — ice risk, drive carefully")
        state.temperature <= 5f  -> parts.add("Near-freezing temps — dress in layers")
    }

    // Wind advisory
    if (state.windSpeed >= 50f) {
        parts.add("Strong winds (${state.windSpeed.toInt()} km/h) — secure loose objects outdoors")
    }

    // Air quality
    when (state.aqi) {
        4 -> parts.add("Air quality is unhealthy — limit prolonged outdoor exertion")
        5, 6 -> parts.add("Very poor air quality — avoid outdoor activity if possible")
    }

    // Visibility
    if (state.visibility < 2f) {
        parts.add("Dense fog — drive with caution")
    }

    // Good day
    if (parts.isEmpty()) {
        val code = state.weatherCode
        parts.add(when {
            code == 0 -> "Clear skies all day — great conditions"
            code in 1..2 -> "Mostly clear with light clouds — pleasant day ahead"
            code in 3..45 -> "Overcast and calm — comfortable for outdoor plans"
            else -> "Conditions look manageable today"
        })
    }
    return parts.take(3).joinToString("  •  ")
}
