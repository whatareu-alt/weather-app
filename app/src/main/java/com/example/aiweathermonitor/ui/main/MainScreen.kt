package com.example.aiweathermonitor.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation3.runtime.NavKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.aiweathermonitor.audio.WeatherSoundPlayer
import com.example.aiweathermonitor.ui.DashboardScreen
import com.example.aiweathermonitor.ui.DashboardActions
import com.example.aiweathermonitor.ui.AppSettings
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    settings: AppSettings = AppSettings(),
    onNavigateToSettings: () -> Unit = {},
    viewModel: MainScreenViewModel = koinViewModel()
) {
    val state by viewModel.weatherState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    // ── Weather ambience: layered background sound matching the weather code ──
    val soundPlayer = remember { WeatherSoundPlayer(context.applicationContext) }
    DisposableEffect(Unit) {
        onDispose { soundPlayer.release() }
    }
    // Start/stop & switch soundscape when the toggle or weather condition changes
    LaunchedEffect(settings.soundEnabled, state.weatherCode, state.isLoading) {
        if (settings.soundEnabled && !state.isLoading) {
            soundPlayer.play(state.weatherCode)
        } else if (!settings.soundEnabled) {
            soundPlayer.stop()
        }
    }
    // Pause when the app is backgrounded, resume when it returns
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, settings.soundEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> soundPlayer.pause()
                Lifecycle.Event.ON_START -> if (settings.soundEnabled) soundPlayer.resume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Launcher for POST_NOTIFICATIONS permission (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Handled silently
    }

    LaunchedEffect(Unit) {
        viewModel.initializeCache(context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            viewModel.fetchWeatherForCurrentLocation(context)
        } else {
            viewModel.updateWeatherState(
                state.copy(errorMessage = "Location permission denied. Cannot fetch current location weather.")
            )
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        val actions = remember(viewModel, context, state) {
            DashboardActions(
                onQueryChange = { viewModel.updateSearchQuery(it) },
                onSelectCity = { viewModel.selectCity(it, context) },
                onRefresh = { viewModel.refreshWeather(context) },
                onLocationClick = {
                    val hasFine = ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val hasCoarse = ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasFine || hasCoarse) {
                        viewModel.fetchWeatherForCurrentLocation(context)
                    } else {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                onSettingsClick = { viewModel.updateWeatherState(state.copy(showKeyDialog = true)) },
                onNavigateToSettings = onNavigateToSettings,
                onSaveWeatherApiKey = { key -> viewModel.updateWeatherState(state.copy(weatherApiKey = key, showKeyDialog = false)) },
                onDismissSettings = { viewModel.updateWeatherState(state.copy(showKeyDialog = false)) },
                onSaveCity = { viewModel.saveCurrentCity() },
                onRemoveCity = { viewModel.removeCityFromFavorites(it) },
                onToggleCelsius = { viewModel.toggleCelsius() },
                onToggleSavedCitiesScreen = { viewModel.updateWeatherState(state.copy(showSavedCitiesScreen = it)) },
                onShareWeather = { com.example.aiweathermonitor.util.ShareWeatherCard.share(context, state) },
                onOpenArticle = { url ->
                    if (url.isNotBlank()) {
                        runCatching {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(url)
                            ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        }
                    }
                },
                onRefreshNews = { viewModel.fetchNews() }
            )
        }

        DashboardScreen(
            state = state,
            settings = settings,
            actions = actions,
            modifier = Modifier.fillMaxSize()
        )
    }
}
