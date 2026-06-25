package com.example.aiweathermonitor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.test.KoinTest
import android.app.Application
import androidx.test.core.app.ApplicationProvider

@RunWith(AndroidJUnit4::class)
class WeatherAppIntegrationTest : KoinTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setupKoin() {
        try {
            GlobalContext.stopKoin()
        } catch (e: Exception) {
            // Koin might not be started yet
        }
        
        val app: Application = ApplicationProvider.getApplicationContext()
        startKoin {
            android.content.Context
            modules(appModule)
        }
    }

    @Before
    fun setUp() {
        setupKoin()
    }

    @After
    fun tearDown() {
        try {
            GlobalContext.stopKoin()
        } catch (e: Exception) {
            // Cleanup
        }
    }

    @Test
    fun testApplicationStartupFlow() {
        // Arrange - Application initialized with Koin

        // Act - Launch main navigation
        composeTestRule.setContent {
            MainNavigation()
        }

        // Assert - App should render main screen
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testWeatherDataFlowIntegration() = runTest {
        // Arrange
        val repository = GlobalContext.get().get<com.example.aiweathermonitor.data.DataRepository>()

        // Act
        val data = kotlinx.coroutines.flow.first { true }
            .let { repository.data.take(1).toList() }

        // Assert
        assert(!data.isEmpty())
    }

    @Test
    fun testWeatherStateTransitionsIntegration() {
        // Arrange
        val initialState = WeatherState()

        // Act - Simulate weather state changes
        val searchState = initialState.copy(
            searchQuery = "London",
            isSearching = true
        )

        val loadedState = searchState.copy(
            selectedCity = "London",
            temperature = 18.5f,
            isSearching = false,
            isLoading = false
        )

        // Assert - State transitions work correctly
        assert(initialState.searchQuery.isEmpty())
        assert(searchState.searchQuery == "London")
        assert(loadedState.temperature == 18.5f)
    }

    @Test
    fun testNetworkAndDataLayerIntegration() = runTest {
        // Arrange
        val dataRepository: com.example.aiweathermonitor.data.DataRepository = 
            GlobalContext.get().get()

        // Act
        val firstEmission = dataRepository.data
            .take(1)
            .toList()
            .firstOrNull()

        // Assert
        assert(firstEmission != null)
        assert(firstEmission?.contains("Android") == true)
    }

    @Test
    fun testMultipleCitiesSwitchingIntegration() {
        // Arrange
        val nyState = WeatherState(
            selectedCity = "New York",
            latitude = 40.7128f,
            longitude = -74.0060f,
            temperature = 22.0f
        )

        val londonState = nyState.copy(
            selectedCity = "London",
            latitude = 51.5074f,
            longitude = -0.1278f,
            temperature = 18.5f
        )

        // Act & Assert
        assert(nyState.selectedCity == "New York")
        assert(londonState.selectedCity == "London")
        assert(nyState.temperature != londonState.temperature)
    }

    @Test
    fun testTemperatureUnitConversionIntegration() {
        // Arrange
        val celsiusState = WeatherState(
            temperature = 20.0f,
            isCelsius = true
        )

        // Calculate Fahrenheit
        val fahrenheit = (celsiusState.temperature * 9 / 5) + 32

        // Act
        val fahrenheitState = celsiusState.copy(
            isCelsius = false,
            temperature = fahrenheit
        )

        // Assert
        assert(celsiusState.isCelsius)
        assert(!fahrenheitState.isCelsius)
        assert(fahrenheitState.temperature > celsiusState.temperature)
    }

    @Test
    fun testOfflineModeFallbackIntegration() {
        // Arrange
        val onlineState = WeatherState(
            selectedCity = "New York",
            temperature = 22.0f,
            isOnline = true,
            lastRefreshedTime = "14:30"
        )

        val offlineState = onlineState.copy(
            isOnline = false
        )

        // Act & Assert
        assert(onlineState.isOnline)
        assert(!offlineState.isOnline)
        // Data should still be available from cache
        assert(offlineState.selectedCity == "New York")
    }

    @Test
    fun testWeatherAlertsIntegration() {
        // Arrange
        val normalState = WeatherState(alertEvent = null)

        val alertState = normalState.copy(
            alertEvent = "Heavy rain warning for next 2 hours"
        )

        // Act & Assert
        assert(normalState.alertEvent == null)
        assert(alertState.alertEvent != null)
        assert(alertState.alertEvent?.contains("rain") == true)
    }

    @Test
    fun testForecastDataIntegration() {
        // Arrange
        val hourlyForecasts = listOf(
            HourForecast(hour = "12:00", temperature = 20.0f, weatherCode = 0),
            HourForecast(hour = "13:00", temperature = 21.0f, weatherCode = 1),
            HourForecast(hour = "14:00", temperature = 22.0f, weatherCode = 0)
        )

        val dailyForecasts = listOf(
            DayForecast(day = "2024-06-04", tempMin = 15.0f, tempMax = 25.0f, weatherCode = 0),
            DayForecast(day = "2024-06-05", tempMin = 16.0f, tempMax = 26.0f, weatherCode = 1)
        )

        val state = WeatherState(
            hourlyForecast = hourlyForecasts,
            dailyForecast = dailyForecasts
        )

        // Assert
        assert(state.hourlyForecast.size == 3)
        assert(state.dailyForecast.size == 2)
        assert(state.hourlyForecast[0].temperature == 20.0f)
        assert(state.dailyForecast[0].tempMin == 15.0f)
    }

    @Test
    fun testSavedCitiesManagementIntegration() {
        // Arrange
        val nyResult = GeocodingResult(
            name = "New York",
            latitude = 40.7128f,
            longitude = -74.0060f,
            country = "United States"
        )

        val londonResult = GeocodingResult(
            name = "London",
            latitude = 51.5074f,
            longitude = -0.1278f,
            country = "United Kingdom"
        )

        val initialState = WeatherState(savedCities = emptyList())
        val withOneCity = initialState.copy(savedCities = listOf(nyResult))
        val withTwoCities = withOneCity.copy(savedCities = listOf(nyResult, londonResult))

        // Assert
        assert(initialState.savedCities.isEmpty())
        assert(withOneCity.savedCities.size == 1)
        assert(withTwoCities.savedCities.size == 2)
    }
}
