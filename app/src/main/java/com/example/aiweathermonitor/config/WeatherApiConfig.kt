package com.example.aiweathermonitor.config

/**
 * Centralized configuration for Weather API endpoints, timing, and defaults.
 * Prevents magic numbers and strings scattered throughout the codebase.
 */
object WeatherApiConfig {
    // Timing Configuration
    const val SEARCH_DEBOUNCE_MS = 400
    const val SEARCH_MIN_CHARS = 2
    const val SEARCH_RESULT_LIMIT = 10
    const val API_TIMEOUT_SECONDS = 30L
    const val FORECAST_HOURS_LIMIT = 24
    const val FORECAST_DAYS_LIMIT = 14
    const val REFRESH_INTERVAL_MINUTES = 30
    const val CACHE_EXPIRATION_MS = 3600000L  // 1 hour in milliseconds

    // WeatherAPI.com free tier supports up to 3-day forecast
    const val WEATHER_API_FORECAST_DAYS = 3

    // News (RSS) configuration — how many headlines to keep from the feed
    const val NEWS_ITEM_LIMIT = 15

    // API Endpoints
    object Endpoints {
        const val OPEN_METEO_GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"
        const val OPEN_METEO_FORECAST     = "https://api.open-meteo.com/v1/forecast"
        const val OPEN_METEO_AIR_QUALITY  = "https://air-quality-api.open-meteo.com/v1/air-quality"
        const val WEATHER_API_FORECAST = "https://api.weatherapi.com/v1/forecast.json"
        const val WEATHER_API_SEARCH   = "https://api.weatherapi.com/v1/search.json"

        // Google News RSS — weather-related top stories. HTTPS so it satisfies
        // the app's network-security config (no cleartext allowed).
        const val NEWS_RSS_FEED =
            "https://news.google.com/rss/search?q=weather&hl=en-US&gl=US&ceid=US:en"
    }

    // Default Values
    object Defaults {
        const val DEFAULT_CITY = "New York"
        const val DEFAULT_LATITUDE = 40.7128
        const val DEFAULT_LONGITUDE = -74.0060
        const val DEFAULT_SUNRISE = "6:00 AM"
        const val DEFAULT_SUNSET = "6:00 PM"
        const val DEFAULT_SUNRISE_MINUTES = 360
        const val DEFAULT_SUNSET_MINUTES = 1080
        const val DEFAULT_TEMPERATURE = 22f
        const val DEFAULT_HUMIDITY = 50f
        const val DEFAULT_PRESSURE = 1013f
        const val DEFAULT_WIND_SPEED = 12f
        const val DEFAULT_AQI = 45
        const val DEFAULT_UV_INDEX = 3f
    }

    // Error Messages
    object ErrorMessages {
        const val ERROR_NO_INTERNET = "No internet connection - using cached data"
        const val ERROR_INVALID_API_KEY = "Invalid API key configuration"
        const val ERROR_LOCATION_PERMISSION = "Location permission required"
        const val ERROR_TIMEOUT = "Request timeout - please try again"
        const val ERROR_SERVER = "Server error - please try again later"
        const val ERROR_INVALID_DATA = "Invalid data received from server"
        const val ERROR_SEARCH_FAILED = "Search failed - please try another query"
    }

    // Shared Preferences Keys
    object SharedPrefsKeys {
        const val PREFS_NAME = "weather_cache_prefs"
        const val WEATHER_STATE_KEY = "cached_weather_state"
        const val LAST_REFRESH_KEY = "last_refresh_time"
        const val API_KEY_KEY = "weatherapi_key"
    }

    // Logging Tags
    object LogTags {
        const val VIEW_MODEL = "MainScreenViewModel"
        const val NETWORK = "NetworkConnectivity"
        const val REPOSITORY = "WeatherRepository"
        const val WIDGET = "WeatherWidget"
    }
}
