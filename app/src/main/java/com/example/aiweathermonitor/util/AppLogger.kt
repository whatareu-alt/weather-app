package com.example.aiweathermonitor.util

import android.util.Log
import com.example.aiweathermonitor.exception.WeatherException

/**
 * Centralized logging utility for the application.
 * Provides structured logging with proper tags and error tracking.
 */
object AppLogger {
    private const val TAG = "WeatherApp"
    
    /**
     * Log debug messages (only in debug builds)
     */
    fun debug(message: String, tag: String = TAG) {
        if (Log.isLoggable(tag, Log.DEBUG)) {
            Log.d(tag, message)
        }
    }
    
    /**
     * Log info messages
     */
    fun info(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }
    
    /**
     * Log warning messages
     */
    fun warning(message: String, tag: String = TAG, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * Log error messages with exception
     */
    fun error(message: String, tag: String = TAG, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Log exception with stack trace
     */
    fun exception(tag: String = TAG, throwable: Throwable) {
        Log.e(tag, "Exception occurred: ${throwable.message}", throwable)
    }

    /**
     * Logs a weather-related exception with structured information.
     */
    fun logWeatherException(tag: String, message: String, exception: WeatherException) {
        val errorDetails = """
            |Error: $message
            |Type: ${exception.javaClass.simpleName}
            |Cause: ${exception.cause?.javaClass?.simpleName ?: "None"}
            |Message: ${exception.message}
        """.trimMargin()
        Log.e(tag, errorDetails, exception)
    }

    /**
     * Logs network request details for debugging.
     */
    fun logNetworkRequest(tag: String, url: String, method: String = "GET") {
        Log.d(tag, "→ $method $url")
    }

    /**
     * Logs network response details for debugging.
     */
    fun logNetworkResponse(tag: String, statusCode: Int, responseTime: Long) {
        Log.d(tag, "← HTTP $statusCode (${responseTime}ms)")
    }

    /**
     * Logs data parsing operations.
     */
    fun logDataParsing(tag: String, dataType: String, successful: Boolean) {
        val status = if (successful) "✓" else "✗"
        Log.d(tag, "$status Parsing $dataType")
    }
}
