package com.example.aiweathermonitor.util

import java.net.URLEncoder
import com.example.aiweathermonitor.config.WeatherApiConfig

/**
 * Safe URL builder to prevent injection vulnerabilities and encoding issues.
 * All parameters are properly URL-encoded.
 */
class SafeUrlBuilder(private val baseUrl: String) {
    private val queryParams = mutableMapOf<String, String>()

    /** Adds a query parameter with safe encoding. Skips blank values. */
    fun addParam(key: String, value: String): SafeUrlBuilder {
        if (value.isNotBlank()) {
            queryParams[key] = URLEncoder.encode(value, "UTF-8")
        }
        return this
    }

    fun addParam(key: String, value: Number): SafeUrlBuilder {
        queryParams[key] = value.toString()
        return this
    }

    fun addParam(key: String, value: Boolean): SafeUrlBuilder {
        queryParams[key] = value.toString()
        return this
    }

    fun build(): String {
        if (queryParams.isEmpty()) return baseUrl
        return baseUrl + "?" + queryParams.entries.joinToString("&") { (k, v) -> "$k=$v" }
    }
}

// ── Type conversion helpers ──────────────────────────────────────────────────

fun Double?.toFloatSafe(): Float = this?.toFloat() ?: 0f
fun Float?.toDoubleSafe(): Double = this?.toDouble() ?: 0.0
fun String?.orEmpty(): String = this ?: ""
fun <T> List<T>?.getOrNull(index: Int): T? = this?.getOrNull(index)
fun <T> List<T>?.getOrDefault(index: Int, default: T): T = this?.getOrNull(index) ?: default

// ── URL factory ──────────────────────────────────────────────────────────────

object UrlBuilder {

    /** Google News RSS search feed for an arbitrary query (HTTPS, US English). */
    fun googleNewsSearch(query: String): String =
        "https://news.google.com/rss/search?q=" +
            URLEncoder.encode(query, "UTF-8") +
            "&hl=en-US&gl=US&ceid=US:en"

    /** Open-Meteo geocoding search. */
    fun openMeteoSearch(query: String, limit: Int = 10): String =
        SafeUrlBuilder(WeatherApiConfig.Endpoints.OPEN_METEO_GEOCODING)
            .addParam("name", query)
            .addParam("count", limit)
            .addParam("language", "en")
            .build()

    /**
     * Open-Meteo weather forecast.
     * Includes current, hourly, and daily blocks with UV index and humidity.
     */
    fun openMeteoForecast(
        latitude: Float,
        longitude: Float,
        hourly: Boolean = true,
        daily: Boolean = true
    ): String = SafeUrlBuilder(WeatherApiConfig.Endpoints.OPEN_METEO_FORECAST)
        .addParam("latitude", latitude)
        .addParam("longitude", longitude)
        .addParam("current", "temperature_2m,relative_humidity_2m,apparent_temperature,surface_pressure,wind_speed_10m,wind_direction_10m,weather_code,uv_index")
        .apply {
            if (hourly) addParam("hourly", "temperature_2m,weather_code,precipitation_probability,visibility")
            if (daily) addParam(
                "daily",
                "weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max"
            )
        }
        .addParam("forecast_days", WeatherApiConfig.FORECAST_DAYS_LIMIT)
        .addParam("timezone", "auto")
        .build()

    /**
     * WeatherAPI.com forecast endpoint.
     * Returns current conditions + hourly/daily forecast + alerts in one call.
     * Requires a free API key from https://www.weatherapi.com/
     */
    /** WeatherAPI.com city search — returns up to [limit] matches. */
    fun weatherApiSearch(query: String, apiKey: String, limit: Int = 10): String =
        SafeUrlBuilder(WeatherApiConfig.Endpoints.WEATHER_API_SEARCH)
            .addParam("key", apiKey)
            .addParam("q", query)
            .build()

    fun weatherApiForecast(
        latitude: Float,
        longitude: Float,
        apiKey: String,
        days: Int = WeatherApiConfig.WEATHER_API_FORECAST_DAYS,
        aqi: Boolean = true,
        alerts: Boolean = true
    ): String = SafeUrlBuilder(WeatherApiConfig.Endpoints.WEATHER_API_FORECAST)
        .addParam("key", apiKey)
        .addParam("q", "$latitude,$longitude")
        .addParam("days", days)
        .addParam("aqi", if (aqi) "yes" else "no")
        .addParam("alerts", if (alerts) "yes" else "no")
        .build()

    /** Open-Meteo air quality — hourly pollen concentrations (grains/m³). */
    fun openMeteoPollen(latitude: Float, longitude: Float): String =
        SafeUrlBuilder(WeatherApiConfig.Endpoints.OPEN_METEO_AIR_QUALITY)
            .addParam("latitude", latitude)
            .addParam("longitude", longitude)
            .addParam("hourly", "alder_pollen,birch_pollen,grass_pollen,mugwort_pollen,olive_pollen,ragweed_pollen")
            .addParam("forecast_days", 1)
            .addParam("timezone", "auto")
            .build()
}
