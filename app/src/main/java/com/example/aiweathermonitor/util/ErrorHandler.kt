package com.example.aiweathermonitor.util

import com.example.aiweathermonitor.config.WeatherApiConfig
import com.example.aiweathermonitor.exception.WeatherException

/**
 * Utility for handling and translating exceptions into user-friendly messages.
 * Consolidates error detection logic to avoid duplication.
 */
object ErrorHandler {

    /**
     * Determines if an exception is network-related.
     */
    fun isNetworkError(exception: Throwable): Boolean {
        return exception is java.net.UnknownHostException ||
                exception.cause is java.net.UnknownHostException ||
                exception is java.net.SocketTimeoutException ||
                exception is java.io.IOException ||
                exception.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                exception.message?.contains("Connection refused", ignoreCase = true) == true ||
                exception.message?.contains("Network unreachable", ignoreCase = true) == true
    }

    /**
     * Determines if an exception is a data parsing/format error.
     */
    fun isDataError(exception: Throwable): Boolean {
        return exception.javaClass.simpleName.contains("JsonSyntax", ignoreCase = true) ||
               exception.javaClass.simpleName.contains("JsonEOF", ignoreCase = true) ||
               (exception is java.io.IOException && 
                exception.message?.contains("JSON", ignoreCase = true) == true)
    }

    /**
     * Determines if an exception is an API configuration error.
     */
    fun isConfigurationError(exception: Throwable): Boolean {
        return exception.message?.contains("Invalid API", ignoreCase = true) == true ||
               exception.message?.contains("Unauthorized", ignoreCase = true) == true ||
               exception.message?.contains("API key", ignoreCase = true) == true
    }

    /**
     * Converts an exception to a user-friendly error message.
     * Returns a message suitable for display in the UI.
     */
    fun getFriendlyErrorMessage(exception: Throwable): String {
        return when {
            isNetworkError(exception) -> WeatherApiConfig.ErrorMessages.ERROR_NO_INTERNET
            isConfigurationError(exception) -> WeatherApiConfig.ErrorMessages.ERROR_INVALID_API_KEY
            exception.message?.contains("timeout", ignoreCase = true) ?: false -> 
                WeatherApiConfig.ErrorMessages.ERROR_TIMEOUT
            (exception.message?.contains("500") ?: false) ||
            (exception.message?.contains("503") ?: false) ->
                WeatherApiConfig.ErrorMessages.ERROR_SERVER
            isDataError(exception) -> WeatherApiConfig.ErrorMessages.ERROR_INVALID_DATA
            else -> exception.message ?: "An unexpected error occurred"
        }
    }

    /**
     * Converts an exception to a weather exception for consistent handling.
     */
    fun toWeatherException(exception: Throwable): WeatherException {
        return when {
            exception is WeatherException -> exception
            isNetworkError(exception) -> com.example.aiweathermonitor.exception.NetworkException(
                WeatherApiConfig.ErrorMessages.ERROR_NO_INTERNET,
                exception
            )
            isConfigurationError(exception) -> com.example.aiweathermonitor.exception.ConfigurationException(
                WeatherApiConfig.ErrorMessages.ERROR_INVALID_API_KEY,
                exception
            )
            isDataError(exception) -> com.example.aiweathermonitor.exception.DataParsingException(
                WeatherApiConfig.ErrorMessages.ERROR_INVALID_DATA,
                exception
            )
            exception is java.net.SocketTimeoutException -> 
                com.example.aiweathermonitor.exception.NetworkException(
                    WeatherApiConfig.ErrorMessages.ERROR_TIMEOUT,
                    exception
                )
            else -> com.example.aiweathermonitor.exception.NetworkException(
                getFriendlyErrorMessage(exception),
                exception
            )
        }
    }

    /**
     * Safely validates coordinate values.
     * Returns true if coordinates are valid, false otherwise.
     */
    fun isValidCoordinate(latitude: Float, longitude: Float): Boolean {
        return latitude in -90f..90f && longitude in -180f..180f
    }

    /**
     * Safely validates temperature value.
     * Valid range: -50°C to 70°C (covers extreme weather conditions)
     */
    fun isValidTemperature(temp: Float): Boolean {
        return temp in -50f..70f
    }

    /**
     * Safely validates percentage-based metrics (humidity, UV index normalized).
     * Valid range: 0-100
     */
    fun isValidPercentage(value: Float): Boolean {
        return value in 0f..100f
    }

    /**
     * Safely validates pressure value.
     * Valid range: 870-1050 hPa (normal atmospheric variations)
     */
    fun isValidPressure(pressure: Float): Boolean {
        return pressure in 870f..1050f
    }

    /**
     * Safely validates wind speed.
     * Valid range: 0-150 km/h (covers even extreme hurricane conditions)
     */
    fun isValidWindSpeed(windSpeed: Float): Boolean {
        return windSpeed >= 0f && windSpeed <= 150f
    }
}
