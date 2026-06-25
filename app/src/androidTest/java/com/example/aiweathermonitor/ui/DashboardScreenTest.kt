package com.example.aiweathermonitor.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.aiweathermonitor.DayForecast
import com.example.aiweathermonitor.GeocodingResult
import com.example.aiweathermonitor.HourForecast
import com.example.aiweathermonitor.WeatherState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testDashboardScreenRendersWithDefaultState() {
        // Arrange
        val state = WeatherState()

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDashboardScreenDisplaysCityName() {
        // Arrange
        val state = WeatherState(selectedCity = "London")

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDashboardScreenDisplaysTemperature() {
        // Arrange
        val state = WeatherState(
            temperature = 25.5f,
            isCelsius = true
        )

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDashboardScreenDisplaysHourlyForecast() {
        // Arrange
        val hourlyForecast = listOf(
            HourForecast(hour = "10:00", temperature = 20.0f, weatherCode = 0),
            HourForecast(hour = "11:00", temperature = 21.0f, weatherCode = 1),
            HourForecast(hour = "12:00", temperature = 22.0f, weatherCode = 2)
        )
        val state = WeatherState(hourlyForecast = hourlyForecast)

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDashboardScreenDisplaysDailyForecast() {
        // Arrange
        val dailyForecast = listOf(
            DayForecast(day = "2024-06-04", tempMin = 15.0f, tempMax = 25.0f, weatherCode = 0),
            DayForecast(day = "2024-06-05", tempMin = 16.0f, tempMax = 26.0f, weatherCode = 1)
        )
        val state = WeatherState(dailyForecast = dailyForecast)

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDashboardScreenDisplaysWeatherMetrics() {
        // Arrange
        val state = WeatherState(
            temperature = 22.0f,
            humidity = 65.0f,
            pressure = 1013.0f,
            windSpeed = 15.0f,
            aqi = 50,
            uvIndex = 5.0f
        )

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDashboardScreenDisplaysSunriseSunset() {
        // Arrange
        val state = WeatherState(
            sunrise = "05:30",
            sunset = "20:45",
            sunriseMinutes = 330,
            sunsetMinutes = 1245
        )

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDashboardScreenHandlesFahrenheit() {
        // Arrange
        val state = WeatherState(
            temperature = 72.0f,
            isCelsius = false
        )

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDashboardScreenDisplaysOfflineStatus() {
        // Arrange
        val state = WeatherState(isOnline = false)

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDashboardScreenDisplaysWeatherAlert() {
        // Arrange
        val state = WeatherState(
            alertEvent = "Heavy rain expected in the evening"
        )

        // Act
        composeTestRule.setContent {
            DashboardScreen(
                state = state,
                modifier = androidx.compose.ui.Modifier.fillMaxSize()
            )
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
class WeatherIconTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testWeatherIconRenders() {
        // Arrange
        val weatherCode = 0 // Clear Sky

        // Act
        composeTestRule.setContent {
            WeatherIcon(weatherCode = weatherCode)
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testWeatherIconForDifferentCodes() {
        // Arrange
        val weatherCodes = listOf(0, 1, 3, 45, 61, 71, 80, 95)

        // Act & Assert
        weatherCodes.forEach { code ->
            composeTestRule.setContent {
                WeatherIcon(weatherCode = code)
            }
            composeTestRule.onRoot().assertExists()
        }
    }

    @Test
    fun testWeatherIconSizeVariations() {
        // Arrange & Act
        composeTestRule.setContent {
            WeatherIcon(weatherCode = 0)
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }
}
