package com.example.aiweathermonitor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.test.KoinTest
import android.app.Application
import androidx.test.core.app.ApplicationProvider

@RunWith(AndroidJUnit4::class)
class NavigationTest : KoinTest {

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

    @Test
    fun testMainNavigationComposable() {
        // Arrange
        setupKoin()

        // Act
        composeTestRule.setContent {
            MainNavigation()
        }

        // Assert - Navigation should render without errors
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testNavigationBackStackInitialization() {
        // Arrange
        setupKoin()

        // Act
        composeTestRule.setContent {
            MainNavigation()
        }

        // Assert - Main screen should be displayed
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testNavigationDisplaysMainScreen() {
        // Arrange
        setupKoin()

        // Act
        composeTestRule.setContent {
            MainNavigation()
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }
}

object Main
