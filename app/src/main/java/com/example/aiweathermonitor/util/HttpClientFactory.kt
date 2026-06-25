package com.example.aiweathermonitor.util

import com.example.aiweathermonitor.config.WeatherApiConfig
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Single source of truth for the app's [OkHttpClient] configuration. Both the
 * Koin graph and the [com.example.aiweathermonitor.ui.main.MainScreenViewModel]
 * default construct their client here so timeout/retry settings can never drift.
 */
object HttpClientFactory {
    fun create(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(WeatherApiConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(WeatherApiConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WeatherApiConfig.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
}
