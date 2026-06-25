package com.example.aiweathermonitor.ui.main

import com.example.aiweathermonitor.GeocodingResult
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun testInitialState() = runTest {
    val viewModel = MainScreenViewModel()
    val state = viewModel.weatherState.first()
    assertEquals("New York", state.selectedCity)
    assertEquals("", state.searchQuery)
    assertTrue(state.isCelsius)
  }

  @Test
  fun testToggleCelsius() = runTest {
    val viewModel = MainScreenViewModel()
    assertTrue(viewModel.weatherState.value.isCelsius)
    viewModel.toggleCelsius()
    assertFalse(viewModel.weatherState.value.isCelsius)
    viewModel.toggleCelsius()
    assertTrue(viewModel.weatherState.value.isCelsius)
  }

  @Test
  fun testUpdateSearchQuery() = runTest {
    val viewModel = MainScreenViewModel()
    assertEquals("", viewModel.weatherState.value.searchQuery)
    viewModel.updateSearchQuery("London")
    assertEquals("London", viewModel.weatherState.value.searchQuery)
  }

  @Test
  fun testSaveAndRemoveFavoriteCity() = runTest {
    val viewModel = MainScreenViewModel()
    assertTrue(viewModel.weatherState.value.savedCities.isEmpty())
    viewModel.saveCurrentCity()
    assertEquals(1, viewModel.weatherState.value.savedCities.size)
    assertEquals("New York", viewModel.weatherState.value.savedCities[0].name)

    val cityToRemove = viewModel.weatherState.value.savedCities[0]
    viewModel.removeCityFromFavorites(cityToRemove)
    assertTrue(viewModel.weatherState.value.savedCities.isEmpty())
  }

  @Test
  fun testWeatherApiInitialState() = runTest {
    val viewModel = MainScreenViewModel()
    val state = viewModel.weatherState.value
    assertEquals("", state.weatherApiKey)
    assertEquals(false, state.showKeyDialog)
  }
}
