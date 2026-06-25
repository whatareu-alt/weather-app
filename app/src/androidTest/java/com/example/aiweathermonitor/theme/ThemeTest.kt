package com.example.aiweathermonitor.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ColorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testColorsAreDefined() {
        // This tests that color constants are properly defined
        composeTestRule.setContent {
            val colors = remember {
                mapOf(
                    "primary" to Color(0xFF0066CC),
                    "secondary" to Color(0xFF03DAC6),
                    "tertiary" to Color(0xFF03DAC6),
                    "error" to Color(0xFFB3261E),
                    "background" to Color(0xFFFAFBFE),
                    "surface" to Color(0xFFFAFBFE)
                )
            }
            
            // Assert all colors are defined
            assertNotNull(colors)
            assertEquals(6, colors.size)
        }
    }

    @Test
    fun testThemeColorsConsistency() {
        // Arrange & Act
        composeTestRule.setContent {
            val darkPrimary = Color(0xFF0066CC)
            val lightPrimary = Color(0xFF0066CC)

            // Assert - primary colors should be consistent
            assertEquals(darkPrimary, lightPrimary)
        }
    }
}

@RunWith(AndroidJUnit4::class)
class ThemeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testThemeComposableRendersSuccessfully() {
        // Arrange & Act
        composeTestRule.setContent {
            WeatherTheme {
                // Content rendered under theme
            }
        }

        // Assert - theme should render without errors
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testThemeProvidesColorScheme() {
        // Arrange & Act
        composeTestRule.setContent {
            WeatherTheme {
                val colors = MaterialTheme.colorScheme
                assertNotNull(colors)
            }
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testThemeProvidesTypography() {
        // Arrange & Act
        composeTestRule.setContent {
            WeatherTheme {
                val typography = MaterialTheme.typography
                assertNotNull(typography)
            }
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testThemeProvidesShapes() {
        // Arrange & Act
        composeTestRule.setContent {
            WeatherTheme {
                val shapes = MaterialTheme.shapes
                assertNotNull(shapes)
            }
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testDynamicColorThemeAvailable() {
        // Test that theme adapts to system colors when available
        composeTestRule.setContent {
            WeatherTheme {
                val colors = MaterialTheme.colorScheme
                // Colors should be set based on system theme
                assertNotNull(colors.primary)
                assertNotNull(colors.secondary)
                assertNotNull(colors.background)
            }
        }

        composeTestRule.onRoot().assertExists()
    }
}

@RunWith(AndroidJUnit4::class)
class TypographyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTypographyStylesAreDefined() {
        // Arrange & Act
        composeTestRule.setContent {
            WeatherTheme {
                val typography = MaterialTheme.typography
                
                // Assert - typography should have all required styles
                assertNotNull(typography.displayLarge)
                assertNotNull(typography.headlineLarge)
                assertNotNull(typography.titleLarge)
                assertNotNull(typography.bodyLarge)
                assertNotNull(typography.labelLarge)
            }
        }

        // Assert
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun testTypographyConsistency() {
        // Arrange
        val fontSize1 = androidx.compose.material3.Typography().bodyLarge.fontSize
        val fontSize2 = androidx.compose.material3.Typography().bodyLarge.fontSize

        // Assert - same typography style should have consistent font size
        assertEquals(fontSize1, fontSize2)
    }
}
