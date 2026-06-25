package com.example.aiweathermonitor

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.widget.RemoteViews
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class WeatherWidgetProviderTest {

    private lateinit var context: Context
    private lateinit var appWidgetManager: AppWidgetManager
    private lateinit var widget: WeatherWidgetProvider
    private lateinit var sharedPreferences: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appWidgetManager = mockk(relaxed = true)
        widget = WeatherWidgetProvider()
        
        // Setup shared preferences mock
        sharedPreferences = mockk(relaxed = true)
        every { context.getSharedPreferences("weather_cache_prefs", Context.MODE_PRIVATE) } returns sharedPreferences
    }

    @Test
    fun testOnUpdateWithoutCachedWeather() {
        // Arrange
        every { sharedPreferences.getString("cached_weather_state", null) } returns null
        val appWidgetIds = intArrayOf(1, 2)

        // Act
        widget.onUpdate(context, appWidgetManager, appWidgetIds)

        // Assert - should call updateAppWidget for each widget
        verify(atLeast = 1) { appWidgetManager.updateAppWidget(any<Int>(), any<RemoteViews>()) }
    }

    @Test
    fun testOnUpdateWithCachedWeather() {
        // Arrange
        val weatherState = WeatherState(
            selectedCity = "London",
            temperature = 18.5f,
            isCelsius = true,
            isOnline = true,
            weatherCode = 3
        )
        
        val json = Json { ignoreUnknownKeys = true }
        val jsonString = json.encodeToString(WeatherState.serializer(), weatherState)
        
        every { sharedPreferences.getString("cached_weather_state", null) } returns jsonString
        val appWidgetIds = intArrayOf(1)

        // Act
        widget.onUpdate(context, appWidgetManager, appWidgetIds)

        // Assert
        verify { appWidgetManager.updateAppWidget(any<Int>(), any<RemoteViews>()) }
    }

    @Test
    fun testOnUpdateWithMultipleWidgets() {
        // Arrange
        every { sharedPreferences.getString("cached_weather_state", null) } returns null
        val appWidgetIds = intArrayOf(1, 2, 3, 4, 5)

        // Act
        widget.onUpdate(context, appWidgetManager, appWidgetIds)

        // Assert - should update all widgets
        appWidgetIds.forEach { widgetId ->
            verify { appWidgetManager.updateAppWidget(widgetId, any<RemoteViews>()) }
        }
    }

    @Test
    fun testOnUpdateWithFahrenheitTemperature() {
        // Arrange
        val weatherState = WeatherState(
            selectedCity = "New York",
            temperature = 20.0f,
            isCelsius = false,
            isOnline = true,
            weatherCode = 0
        )
        
        val json = Json { ignoreUnknownKeys = true }
        val jsonString = json.encodeToString(WeatherState.serializer(), weatherState)
        
        every { sharedPreferences.getString("cached_weather_state", null) } returns jsonString
        val appWidgetIds = intArrayOf(1)

        // Act
        widget.onUpdate(context, appWidgetManager, appWidgetIds)

        // Assert
        verify { appWidgetManager.updateAppWidget(any<Int>(), any<RemoteViews>()) }
    }

    @Test
    fun testOnUpdateWithOfflineStatus() {
        // Arrange
        val weatherState = WeatherState(
            selectedCity = "Paris",
            temperature = 16.0f,
            isCelsius = true,
            isOnline = false,
            weatherCode = 45
        )
        
        val json = Json { ignoreUnknownKeys = true }
        val jsonString = json.encodeToString(WeatherState.serializer(), weatherState)
        
        every { sharedPreferences.getString("cached_weather_state", null) } returns jsonString
        val appWidgetIds = intArrayOf(1)

        // Act
        widget.onUpdate(context, appWidgetManager, appWidgetIds)

        // Assert
        verify { appWidgetManager.updateAppWidget(any<Int>(), any<RemoteViews>()) }
    }

    @Test
    fun testOnUpdateWithInvalidJson() {
        // Arrange
        every { sharedPreferences.getString("cached_weather_state", null) } returns "{invalid json}"
        val appWidgetIds = intArrayOf(1)

        // Act & Assert - should not crash
        widget.onUpdate(context, appWidgetManager, appWidgetIds)
        
        verify { appWidgetManager.updateAppWidget(any<Int>(), any<RemoteViews>()) }
    }

    @Test
    fun testWeatherCodeDisplayOnWidget() {
        // Arrange & Act & Assert
        assertEquals("Clear Sky", 0.getWeatherCodeDescription())
        assertEquals("Partly Cloudy", 2.getWeatherCodeDescription())
        assertEquals("Fog / Rime Fog", 45.getWeatherCodeDescription())
        assertEquals("Rainy", 63.getWeatherCodeDescription())
    }
}
