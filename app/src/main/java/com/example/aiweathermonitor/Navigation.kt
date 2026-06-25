package com.example.aiweathermonitor

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.aiweathermonitor.ui.LocalWeatherCode
import com.example.aiweathermonitor.util.OnboardingPrefs
import com.example.aiweathermonitor.ui.OnboardingScreen
import com.example.aiweathermonitor.ui.SettingsScreen
import com.example.aiweathermonitor.ui.main.MainScreen
import com.example.aiweathermonitor.ui.main.MainScreenViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainNavigation() {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel = koinViewModel()
    val state by viewModel.weatherState.collectAsState()
    val settings by viewModel.appSettings.collectAsState()

    // Determine start destination: show onboarding only on first launch
    val startDestination: NavKey = remember {
        if (OnboardingPrefs.hasSeenOnboarding(context)) Main else Onboarding
    }

    val backStack = rememberNavBackStack(startDestination)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            // ── Onboarding ────────────────────────────────────────────────────
            entry<Onboarding> {
                val locationLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { perms ->
                    val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
                    // Mark onboarding done regardless of permission outcome
                    OnboardingPrefs.markOnboardingSeen(context)
                    if (granted) viewModel.fetchWeatherForCurrentLocation(context)
                    // Navigate to Main, removing Onboarding from back stack
                    backStack.removeLastOrNull()
                    backStack.add(Main)
                }

                OnboardingScreen(
                    onRequestLocation = {
                        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasFine || hasCoarse) {
                            // Already granted — mark done and go to Main
                            OnboardingPrefs.markOnboardingSeen(context)
                            viewModel.fetchWeatherForCurrentLocation(context)
                            backStack.removeLastOrNull()
                            backStack.add(Main)
                        } else {
                            locationLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    onSkipToSearch = {
                        OnboardingPrefs.markOnboardingSeen(context)
                        backStack.removeLastOrNull()
                        backStack.add(Main)
                    }
                )
            }

            // ── Main ──────────────────────────────────────────────────────────
            entry<Main> {
                MainScreen(
                    modifier = Modifier.fillMaxSize(),
                    settings = settings,
                    onNavigateToSettings = { backStack.add(Settings) }
                )
            }

            // ── Settings ──────────────────────────────────────────────────────
            entry<Settings> {
                CompositionLocalProvider(LocalWeatherCode provides state.weatherCode) {
                    SettingsScreen(
                        state = state,
                        settings = settings,
                        onSettingsChange = { viewModel.updateSettings(it) },
                        onSaveWeatherApiKey = { key ->
                            viewModel.updateWeatherState(
                                state.copy(weatherApiKey = key, showKeyDialog = false)
                            )
                        },
                        onNavigateBack = { backStack.removeLastOrNull() }
                    )
                }
            }
        }
    )
}
