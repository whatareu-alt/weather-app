package com.example.aiweathermonitor

import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherStateTest {

    @Test
    fun testWeatherCodeDescriptionClearSky() {
        val description = 0.getWeatherCodeDescription()
        assertEquals("Clear Sky", description)
    }

    @Test
    fun testWeatherCodeDescriptionPartlyCloudy() {
        assertEquals("Partly Cloudy", 1.getWeatherCodeDescription())
        assertEquals("Partly Cloudy", 2.getWeatherCodeDescription())
        assertEquals("Partly Cloudy", 3.getWeatherCodeDescription())
    }

    @Test
    fun testWeatherCodeDescriptionFog() {
        assertEquals("Fog / Rime Fog", 45.getWeatherCodeDescription())
        assertEquals("Fog / Rime Fog", 48.getWeatherCodeDescription())
    }

    @Test
    fun testWeatherCodeDescriptionDrizzle() {
        assertEquals("Drizzle", 51.getWeatherCodeDescription())
        assertEquals("Drizzle", 53.getWeatherCodeDescription())
        assertEquals("Drizzle", 55.getWeatherCodeDescription())
    }

    @Test
    fun testWeatherCodeDescriptionRainy() {
        assertEquals("Rainy", 61.getWeatherCodeDescription())
        assertEquals("Rainy", 63.getWeatherCodeDescription())
        assertEquals("Rainy", 65.getWeatherCodeDescription())
    }

    @Test
    fun testWeatherCodeDescriptionSnowy() {
        assertEquals("Snowy", 71.getWeatherCodeDescription())
        assertEquals("Snowy", 73.getWeatherCodeDescription())
        assertEquals("Snowy", 75.getWeatherCodeDescription())
    }

    @Test
    fun testWeatherCodeDescriptionRainShowers() {
        assertEquals("Rain Showers", 80.getWeatherCodeDescription())
        assertEquals("Rain Showers", 81.getWeatherCodeDescription())
        assertEquals("Rain Showers", 82.getWeatherCodeDescription())
    }

    @Test
    fun testWeatherCodeDescriptionThunderstorm() {
        assertEquals("Thunderstorm", 95.getWeatherCodeDescription())
        assertEquals("Thunderstorm", 96.getWeatherCodeDescription())
        assertEquals("Thunderstorm", 99.getWeatherCodeDescription())
    }

    @Test
    fun testWeatherCodeDescriptionUnknown() {
        val description = 999.getWeatherCodeDescription()
        assertEquals("Unstable / Varied", description)
    }

    @Test
    fun testWeatherStateDefaultValues() {
        val state = WeatherState()

        assertEquals("", state.searchQuery)
        assertEquals("New York", state.selectedCity)
        assertEquals(40.7128, state.latitude, 0.0001)
        assertEquals(-74.0060, state.longitude, 0.0001)
        assertEquals(emptyList<GeocodingResult>(), state.searchResults)
        assertEquals(false, state.isLoading)
        assertEquals(false, state.isSearching)
        assertEquals(null, state.errorMessage)
        assertEquals("", state.weatherApiKey)
        assertEquals(false, state.showKeyDialog)
        assertEquals(emptyList<GeocodingResult>(), state.savedCities)
        assertEquals(true, state.isCelsius)
        assertEquals(false, state.showSavedCitiesScreen)
        assertEquals(22.0f, state.temperature)
        assertEquals(50.0f, state.humidity)
        assertEquals(1013.0f, state.pressure)
        assertEquals(12.0f, state.windSpeed)
        assertEquals(0, state.aqi)
        assertEquals(3f, state.uvIndex)
        assertEquals(0, state.weatherCode)
        assertEquals(true, state.isOnline)
        assertEquals("", state.timezone)
    }

    @Test
    fun testWeatherStateWithCustomValues() {
        val customState = WeatherState(
            searchQuery = "London",
            selectedCity = "London",
            latitude = 51.5074,
            longitude = -0.1278,
            temperature = 15.0f,
            isCelsius = true,
            isOnline = false,
            weatherCode = 61,
            timezone = "Europe/London"
        )

        assertEquals("London", customState.searchQuery)
        assertEquals("London", customState.selectedCity)
        assertEquals(51.5074, customState.latitude, 0.0001)
        assertEquals(-0.1278, customState.longitude, 0.0001)
        assertEquals(15.0f, customState.temperature)
        assertEquals(true, customState.isCelsius)
        assertEquals(false, customState.isOnline)
        assertEquals(61, customState.weatherCode)
        assertEquals("Europe/London", customState.timezone)
        assertEquals("Rainy", customState.weatherCode.getWeatherCodeDescription())
    }

    @Test
    fun testHourForecastCreation() {
        val forecast = HourForecast(
            hour = "14:00",
            temperature = 18.5f,
            weatherCode = 3
        )

        assertEquals("14:00", forecast.hour)
        assertEquals(18.5f, forecast.temperature)
        assertEquals(3, forecast.weatherCode)
        assertEquals("Partly Cloudy", forecast.weatherCode.getWeatherCodeDescription())
    }

    @Test
    fun testDayForecastCreation() {
        val forecast = DayForecast(
            day = "2024-06-04",
            tempMin = 15.0f,
            tempMax = 25.0f,
            weatherCode = 1
        )

        assertEquals("2024-06-04", forecast.day)
        assertEquals(15.0f, forecast.tempMin)
        assertEquals(25.0f, forecast.tempMax)
        assertEquals(1, forecast.weatherCode)
    }

    @Test
    fun testGeocodingResultCreation() {
        val result = GeocodingResult(
            name = "Paris",
            latitude = 48.8566,
            longitude = 2.3522,
            country = "France",
            admin1 = "Île-de-France"
        )

        assertEquals("Paris", result.name)
        assertEquals(48.8566, result.latitude, 0.0001)
        assertEquals(2.3522, result.longitude, 0.0001)
        assertEquals("France", result.country)
        assertEquals("Île-de-France", result.admin1)
    }
}
