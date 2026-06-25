package com.example.aiweathermonitor.exception

/**
 * Base exception class for all weather-related errors.
 * Enables specific exception handling and logging.
 */
sealed class WeatherException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Exception thrown when network operations fail.
 * Examples: No internet, DNS resolution failure, timeout
 */
class NetworkException(
    message: String = "Network operation failed",
    cause: Throwable? = null
) : WeatherException(message, cause)

/**
 * Exception thrown for HTTP-level errors (4xx, 5xx).
 */
class HttpException(
    val code: Int,
    val statusMessage: String,
    cause: Throwable? = null
) : WeatherException("HTTP $code: $statusMessage", cause)

/**
 * Exception thrown when API response data cannot be parsed.
 * Examples: Invalid JSON, missing required fields
 */
class DataParsingException(
    message: String = "Failed to parse response data",
    cause: Throwable? = null
) : WeatherException(message, cause)

/**
 * Exception thrown when search operations fail.
 */
class SearchException(
    message: String = "Search operation failed",
    cause: Throwable? = null
) : WeatherException(message, cause)

/**
 * Exception thrown when location operations fail.
 * Examples: Permission denied, location provider unavailable
 */
class LocationException(
    message: String = "Location operation failed",
    cause: Throwable? = null
) : WeatherException(message, cause)

/**
 * Exception thrown when API configuration is invalid.
 * Examples: Missing API key, invalid configuration
 */
class ConfigurationException(
    message: String = "Invalid configuration",
    cause: Throwable? = null
) : WeatherException(message, cause)

/**
 * Extension function to convert generic exceptions to weather exceptions.
 */
fun Throwable.toWeatherException(): WeatherException = when (this) {
    is WeatherException -> this
    is java.net.UnknownHostException -> NetworkException("No internet connection", this)
    is java.net.SocketTimeoutException -> NetworkException("Request timeout", this)
    is java.io.IOException -> NetworkException("Network error", this)
    else -> NetworkException(this.message ?: "Unknown error", this)
}
