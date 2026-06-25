package com.example.aiweathermonitor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aiweathermonitor.*
import com.example.aiweathermonitor.data.models.PollenData
import com.example.aiweathermonitor.data.models.PollenResponse
import com.example.aiweathermonitor.BuildConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.content.edit
import com.example.aiweathermonitor.config.WeatherApiConfig
import com.example.aiweathermonitor.data.WeatherRepository
import com.example.aiweathermonitor.data.DefaultWeatherRepository
import com.example.aiweathermonitor.util.HttpClientFactory
import com.example.aiweathermonitor.util.AppLogger
import com.example.aiweathermonitor.util.ErrorHandler
import com.example.aiweathermonitor.util.UrlBuilder
import com.example.aiweathermonitor.data.models.*
import com.example.aiweathermonitor.ui.AppSettings
import com.example.aiweathermonitor.ui.WindUnit
import com.example.aiweathermonitor.ui.PressureUnit
import com.example.aiweathermonitor.ui.VisibilityUnit
import com.example.aiweathermonitor.ui.AppTheme
import com.example.aiweathermonitor.data.NewsRepository
import com.example.aiweathermonitor.data.DefaultNewsRepository

private const val TAG = "MainScreenViewModel"
private const val TIME_DEFAULT_SUNRISE = "6:00 AM"
private const val TIME_DEFAULT_SUNSET = "6:00 PM"
private const val WEATHER_ALERT_NOTIFICATION_ID = 1001

class MainScreenViewModel(
    private val repository: WeatherRepository = DefaultWeatherRepository(
        HttpClientFactory.create(),
        Json { ignoreUnknownKeys = true }
    ),
    private val newsRepository: NewsRepository = DefaultNewsRepository(
        HttpClientFactory.create()
    ),
    private val jsonParser: Json = Json { ignoreUnknownKeys = true },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val _weatherState = MutableStateFlow(WeatherState())
    val weatherState: StateFlow<WeatherState> = _weatherState.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private var searchJob: Job? = null
    private val isCacheInitialized = AtomicBoolean(false)

    private val effectiveWeatherApiKey: String
        get() = _weatherState.value.weatherApiKey.ifBlank { BuildConfig.WEATHER_API_KEY }

    // ── Initialization ───────────────────────────────────────────────────────

    fun initializeCache(context: android.content.Context) {
        if (!isCacheInitialized.compareAndSet(false, true)) return

        val connectivityObserver = com.example.aiweathermonitor.NetworkConnectivityObserver(context)
        viewModelScope.launch {
            connectivityObserver.isConnected.collect { online ->
                _weatherState.value = _weatherState.value.copy(isOnline = online)
            }
        }

        _appSettings.value = loadSettingsFromPrefs(context)

        // Kick off a news fetch alongside weather on launch.
        fetchNews()

        val cached = loadStateFromPrefs(context)
        if (cached != null) {
            _weatherState.value = cached
            refreshWeather(context)
        } else {
            fetchWeatherForCoordinates(
                cityName = WeatherApiConfig.Defaults.DEFAULT_CITY,
                latitude = WeatherApiConfig.Defaults.DEFAULT_LATITUDE.toFloat(),
                longitude = WeatherApiConfig.Defaults.DEFAULT_LONGITUDE.toFloat(),
                context = context
            )
        }

        viewModelScope.launch {
            // Debounce + filter: only persist when real weather data changes.
            // Skips transient UI state (search query, loading flag, etc.)
            // so we don't hit SharedPreferences on every keystroke.
            _weatherState
                .debounce(500L)
                .distinctUntilChanged { a, b ->
                    a.temperature    == b.temperature    &&
                    a.weatherCode    == b.weatherCode    &&
                    a.selectedCity   == b.selectedCity   &&
                    a.hourlyForecast == b.hourlyForecast &&
                    a.dailyForecast  == b.dailyForecast  &&
                    a.uvIndex        == b.uvIndex        &&
                    a.aqi            == b.aqi            &&
                    a.weatherApiKey  == b.weatherApiKey  &&
                    a.isCelsius      == b.isCelsius      &&
                    a.savedCities    == b.savedCities
                }
                .collect { state ->
                    withContext(ioDispatcher) { saveStateToPrefs(context, state) }
                }
        }
        viewModelScope.launch {
            _appSettings.collect { settings ->
                withContext(ioDispatcher) { saveSettingsToPrefs(context, settings) }
                if (settings.backgroundRefresh) {
                    WeatherRefreshWorker.schedule(context)
                } else {
                    WeatherRefreshWorker.cancel(context)
                }
            }
        }
    }

    private fun saveSettingsToPrefs(context: android.content.Context, settings: AppSettings) {
        try {
            val prefs = context.getSharedPreferences(
                WeatherApiConfig.SharedPrefsKeys.PREFS_NAME,
                android.content.Context.MODE_PRIVATE
            )
            prefs.edit(commit = false) {
                putString("wind_unit", settings.windUnit.name)
                putString("pressure_unit", settings.pressureUnit.name)
                putString("visibility_unit", settings.visibilityUnit.name)
                putString("app_theme", settings.theme.name)
                putBoolean("notif_severe", settings.notifySevere)
                putBoolean("notif_rain", settings.notifyRain)
                putBoolean("notif_briefing", settings.notifyDailyBriefing)
                putBoolean("notif_uv", settings.notifyHighUv)
                putBoolean("bg_refresh", settings.backgroundRefresh)
                putBoolean("sound_enabled", settings.soundEnabled)
            }
        } catch (e: Exception) {
            AppLogger.error("Failed to save settings: ${e.message}", TAG, e)
        }
    }

    private fun loadSettingsFromPrefs(context: android.content.Context): AppSettings {
        return try {
            val prefs = context.getSharedPreferences(
                WeatherApiConfig.SharedPrefsKeys.PREFS_NAME,
                android.content.Context.MODE_PRIVATE
            )
            AppSettings(
                windUnit = WindUnit.valueOf(prefs.getString("wind_unit", WindUnit.KMH.name) ?: WindUnit.KMH.name),
                pressureUnit = PressureUnit.valueOf(prefs.getString("pressure_unit", PressureUnit.HPA.name) ?: PressureUnit.HPA.name),
                visibilityUnit = VisibilityUnit.valueOf(prefs.getString("visibility_unit", VisibilityUnit.KM.name) ?: VisibilityUnit.KM.name),
                theme = AppTheme.valueOf(prefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name),
                notifySevere = prefs.getBoolean("notif_severe", true),
                notifyRain = prefs.getBoolean("notif_rain", true),
                notifyDailyBriefing = prefs.getBoolean("notif_briefing", false),
                notifyHighUv = prefs.getBoolean("notif_uv", false),
                backgroundRefresh = prefs.getBoolean("bg_refresh", true),
                soundEnabled = prefs.getBoolean("sound_enabled", false)
            )
        } catch (e: Exception) {
            AppSettings()
        }
    }

    private fun saveStateToPrefs(context: android.content.Context, state: WeatherState) {
        try {
            val prefs = context.getSharedPreferences(
                WeatherApiConfig.SharedPrefsKeys.PREFS_NAME,
                android.content.Context.MODE_PRIVATE
            )
            val json = jsonParser.encodeToString(WeatherState.serializer(), state)
            prefs.edit(commit = false) {
                putString(WeatherApiConfig.SharedPrefsKeys.WEATHER_STATE_KEY, json)
            }
            // Only update widgets when they are actually present — skips IPC overhead otherwise
            val widgetIds = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetIds(
                android.content.ComponentName(context, WeatherWidgetProvider::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                val intent = android.content.Intent(context, WeatherWidgetProvider::class.java).apply {
                    action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
                    setPackage(context.packageName)
                }
                context.sendBroadcast(intent) // NOSONAR
            }
        } catch (e: java.io.IOException) {
            AppLogger.error("IO error saving state: ${e.message}", TAG, e)
        } catch (e: kotlinx.serialization.SerializationException) {
            AppLogger.error("Serialization error saving state: ${e.message}", TAG, e)
        } catch (e: Exception) {
            AppLogger.error("Failed to save state: ${e.message}", TAG, e)
        }
    }

    private fun loadStateFromPrefs(context: android.content.Context): WeatherState? {
        return try {
            val prefs = context.getSharedPreferences(
                WeatherApiConfig.SharedPrefsKeys.PREFS_NAME,
                android.content.Context.MODE_PRIVATE
            )
            val jsonStr = prefs.getString(WeatherApiConfig.SharedPrefsKeys.WEATHER_STATE_KEY, null)
            if (!jsonStr.isNullOrBlank()) {
                jsonParser.decodeFromString(WeatherState.serializer(), jsonStr).copy(
                    isLoading = false,
                    isSearching = false,
                    searchQuery = "",
                    searchResults = emptyList(),
                    showKeyDialog = false,
                    showSavedCitiesScreen = false,
                    errorMessage = null
                )
            } else null
        } catch (e: kotlinx.serialization.SerializationException) {
            AppLogger.debug("Cached state corrupted, clearing", TAG)
            null
        } catch (e: Exception) {
            AppLogger.warning("Failed to load cached state", TAG, e)
            null
        }
    }

    // ── Public state updaters ────────────────────────────────────────────────

    fun updateWeatherState(newState: WeatherState) { _weatherState.value = newState }

    fun updateSettings(settings: AppSettings) { _appSettings.value = settings }

    fun updateSearchQuery(query: String) {
        _weatherState.value = _weatherState.value.copy(searchQuery = query)
        searchJob?.cancel()
        if (query.trim().length >= WeatherApiConfig.SEARCH_MIN_CHARS) {
            searchJob = viewModelScope.launch {
                delay(WeatherApiConfig.SEARCH_DEBOUNCE_MS.toLong())
                searchCities(query.trim())
            }
        } else {
            _weatherState.value = _weatherState.value.copy(searchResults = emptyList())
        }
    }

    fun selectCity(result: GeocodingResult, context: android.content.Context? = null) {
        _weatherState.value = _weatherState.value.copy(
            searchQuery = "",
            searchResults = emptyList(),
            selectedCity = result.name,
            latitude = result.latitude,
            longitude = result.longitude
        )
        fetchWeatherForCoordinates(result.name, result.latitude.toFloat(), result.longitude.toFloat(), context)
    }

    fun refreshWeather(context: android.content.Context? = null) {
        val s = _weatherState.value
        fetchWeatherForCoordinates(s.selectedCity, s.latitude.toFloat(), s.longitude.toFloat(), context)
    }

    // ── News (delegated to NewsRepository) ────────────────────────────────────

    /**
     * Fetches two location-aware feeds for the current city — weather news and
     * general/local news — in parallel, into [WeatherState.newsArticles] and
     * [WeatherState.localNewsArticles].
     */
    fun fetchNews() {
        val city = _weatherState.value.selectedCity.trim()
        val weatherQuery = if (city.isNotBlank()) "$city weather" else "weather"
        val localQuery = if (city.isNotBlank()) city else "top stories"

        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isNewsLoading = true, newsError = null)

            val weatherDeferred = async { newsRepository.fetchNews(UrlBuilder.googleNewsSearch(weatherQuery)) }
            val localDeferred = async { newsRepository.fetchNews(UrlBuilder.googleNewsSearch(localQuery)) }
            val weatherRes = weatherDeferred.await()
            val localRes = localDeferred.await()

            val bothFailed = weatherRes.isFailure && localRes.isFailure
            if (bothFailed) {
                val e = weatherRes.exceptionOrNull()
                AppLogger.error("News fetch failed", TAG, e)
            }
            _weatherState.value = _weatherState.value.copy(
                newsArticles = weatherRes.getOrNull() ?: emptyList(),
                localNewsArticles = localRes.getOrNull() ?: emptyList(),
                isNewsLoading = false,
                newsError = if (bothFailed) {
                    val e = weatherRes.exceptionOrNull()
                    if (e != null && ErrorHandler.isNetworkError(e))
                        WeatherApiConfig.ErrorMessages.ERROR_NO_INTERNET
                    else "Couldn't load news right now."
                } else null
            )
        }
    }

    fun saveCurrentCity() {
        val s = _weatherState.value
        if (s.savedCities.none { it.name.equals(s.selectedCity, ignoreCase = true) }) {
            _weatherState.value = s.copy(
                savedCities = s.savedCities + GeocodingResult(
                    name = s.selectedCity,
                    latitude = s.latitude,
                    longitude = s.longitude
                )
            )
        }
    }

    fun removeCityFromFavorites(city: GeocodingResult) {
        val s = _weatherState.value
        _weatherState.value = s.copy(
            savedCities = s.savedCities.filterNot { it.name.equals(city.name, ignoreCase = true) }
        )
    }

    fun toggleCelsius() {
        _weatherState.value = _weatherState.value.copy(isCelsius = !_weatherState.value.isCelsius)
    }

    // ── City search (delegated to WeatherRepository) ─────────────────────────

    private suspend fun searchCities(query: String) {
        _weatherState.value = _weatherState.value.copy(isSearching = true)
        repository.searchCities(query, effectiveWeatherApiKey)
            .onSuccess { results ->
                _weatherState.value = _weatherState.value.copy(
                    searchResults = results,
                    isSearching = false,
                    errorMessage = null
                )
            }
            .onFailure { e ->
                val msg = if (ErrorHandler.isNetworkError(e))
                    WeatherApiConfig.ErrorMessages.ERROR_NO_INTERNET
                else
                    ErrorHandler.getFriendlyErrorMessage(e)
                AppLogger.error("City search failed", TAG, e)
                _weatherState.value = _weatherState.value.copy(isSearching = false, errorMessage = msg)
            }
    }

    // ── Weather fetch (delegated to WeatherRepository) ────────────────────────

    fun fetchWeatherForCoordinates(
        cityName: String,
        latitude: Float,
        longitude: Float,
        context: android.content.Context? = null
    ) {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isLoading = true)
            repository.getWeather(
                base = _weatherState.value,
                cityName = cityName,
                latitude = latitude,
                longitude = longitude,
                apiKey = effectiveWeatherApiKey
            ).onSuccess { newState ->
                val cityChanged = newState.selectedCity != _weatherState.value.selectedCity
                _weatherState.value = newState
                if (context != null) maybeNotify(context, newState)
                // Refresh location-aware news whenever the city changes.
                if (cityChanged) fetchNews()
            }.onFailure { e ->
                AppLogger.error("Failed to fetch weather", TAG, e)
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false,
                    errorMessage = ErrorHandler.getFriendlyErrorMessage(e)
                )
            }
        }
    }

    /** Fires a local alert notification for extreme UV or thunderstorm conditions. */
    private fun maybeNotify(context: android.content.Context, s: WeatherState) {
        val alertMsg = when {
            s.uvIndex >= 8 -> "🧴 Extreme UV (${s.uvIndex}) in ${s.selectedCity} — protect yourself!"
            s.weatherCode in listOf(95, 96, 99) -> "⛈️ Thunderstorm warning active in ${s.selectedCity}!"
            else -> null
        }
        if (alertMsg != null) triggerLocalNotification(context, "Friday Alert: ${s.selectedCity}", alertMsg)
    }

    // ── Location ─────────────────────────────────────────────────────────────

    fun fetchWeatherForCurrentLocation(context: android.content.Context) {
        viewModelScope.launch {
            _weatherState.value = _weatherState.value.copy(isLoading = true, errorMessage = null)
            try {
                // Permission check
                val hasFine   = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.ACCESS_COARSE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                if (!hasFine && !hasCoarse) {
                    _weatherState.value = _weatherState.value.copy(
                        isLoading = false, errorMessage = "Location permission not granted."
                    )
                    return@launch
                }

                // Provider check
                val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE)
                        as android.location.LocationManager
                if (!lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) &&
                    !lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                    _weatherState.value = _weatherState.value.copy(
                        isLoading = false,
                        errorMessage = "Location services disabled. Enable GPS and try again."
                    )
                    return@launch
                }

                // Suspend coroutine wrapper — avoids nested launch-in-callback anti-pattern.
                // Strategy: try lastLocation (instant) → balanced accuracy (fast, cell/WiFi) →
                //           high accuracy (GPS, slower) — each with an 8-second timeout.
                val loc: android.location.Location? = getLocationSuspending(context)

                if (loc == null) {
                    _weatherState.value = _weatherState.value.copy(
                        isLoading = false,
                        errorMessage = "Could not get location. Open GPS, wait a moment outdoors, then tap the location button again."
                    )
                    return@launch
                }

                val city = geocodeCoordinates(loc.latitude.toFloat(), loc.longitude.toFloat(), context)
                    ?: "Current Location"
                _weatherState.value = _weatherState.value.copy(
                    selectedCity = city, latitude = loc.latitude, longitude = loc.longitude
                )
                fetchWeatherForCoordinates(city, loc.latitude.toFloat(), loc.longitude.toFloat(), context)

            } catch (e: SecurityException) {
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false, errorMessage = "Permission error: ${e.message}"
                )
            } catch (e: Exception) {
                _weatherState.value = _weatherState.value.copy(
                    isLoading = false, errorMessage = "Location failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Suspending location fetch — 4-tier strategy.
     * 1. FusedLocationProvider lastLocation   — cached, instant
     * 2. FusedLocationProvider getCurrentLocation(HIGH_ACCURACY) — 10s
     * 3. FusedLocationProvider requestLocationUpdates — 12s (powers GPS radio)
     * 4. Raw LocationManager fallback — works even with limited Play Services
     */
    @Suppress("MissingPermission")
    private suspend fun getLocationSuspending(
        context: android.content.Context
    ): android.location.Location? = withContext(ioDispatcher) {

        // ── Tier 1-3: FusedLocationProvider ──────────────────────────────────
        val fusedResult = runCatching {
            val fusedClient = com.google.android.gms.location.LocationServices
                .getFusedLocationProviderClient(context)

            // Tier 1: cached last location (< 2 min old)
            val lastKnown = kotlinx.coroutines.suspendCancellableCoroutine<android.location.Location?> { cont ->
                fusedClient.lastLocation
                    .addOnSuccessListener { loc -> cont.resume(loc, null) }
                    .addOnFailureListener { cont.resume(null, null) }
            }
            if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < 2 * 60 * 1000L) {
                return@runCatching lastKnown
            }

            // Tier 2: getCurrentLocation HIGH_ACCURACY — 10s
            val freshLoc = kotlinx.coroutines.withTimeoutOrNull(10_000L) {
                kotlinx.coroutines.suspendCancellableCoroutine<android.location.Location?> { cont ->
                    val cts = com.google.android.gms.tasks.CancellationTokenSource()
                    fusedClient.getCurrentLocation(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        cts.token
                    )
                        .addOnSuccessListener { loc -> cont.resume(loc, null) }
                        .addOnFailureListener { cont.resume(null, null) }
                    cont.invokeOnCancellation { cts.cancel() }
                }
            }
            if (freshLoc != null) return@runCatching freshLoc

            // Tier 3: requestLocationUpdates — 12s (actually powers GPS radio)
            kotlinx.coroutines.withTimeoutOrNull(12_000L) {
                kotlinx.coroutines.suspendCancellableCoroutine<android.location.Location?> { cont ->
                    val req = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000L
                    ).setMaxUpdates(1).setMinUpdateIntervalMillis(500L).build()

                    val cb = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(r: com.google.android.gms.location.LocationResult) {
                            fusedClient.removeLocationUpdates(this)
                            cont.resume(r.lastLocation, null)
                        }
                    }
                    fusedClient.requestLocationUpdates(req, cb, android.os.Looper.getMainLooper())
                    cont.invokeOnCancellation { fusedClient.removeLocationUpdates(cb) }
                }
            }
        }.getOrNull()

        if (fusedResult != null) return@withContext fusedResult

        // ── Tier 4: Raw LocationManager (works without full Play Services) ───
        AppLogger.warning("FusedLocationProvider failed, falling back to LocationManager", TAG)
        rawLocationManagerFix(context)
    }

    @Suppress("MissingPermission")
    private suspend fun rawLocationManagerFix(
        context: android.content.Context
    ): android.location.Location? = withContext(ioDispatcher) {
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE)
                as android.location.LocationManager

        // Quick: try last known from both providers
        val lastGps = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
        val lastNet = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
        val freshThreshold = System.currentTimeMillis() - 5 * 60 * 1000L
        val lastKnown = listOfNotNull(lastGps, lastNet)
            .filter { it.time > freshThreshold }
            .maxByOrNull { it.time }
        if (lastKnown != null) return@withContext lastKnown

        // Active fix: try GPS first (15s), then network (8s)
        val gpsFix = if (lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
            kotlinx.coroutines.withTimeoutOrNull(15_000L) {
                kotlinx.coroutines.suspendCancellableCoroutine<android.location.Location?> { cont ->
                    val listener = object : android.location.LocationListener {
                        override fun onLocationChanged(loc: android.location.Location) {
                            lm.removeUpdates(this)
                            cont.resume(loc, null)
                        }
                        @Deprecated("") override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                        override fun onProviderEnabled(p: String) {}
                        override fun onProviderDisabled(p: String) { cont.resume(null, null) }
                    }
                    lm.requestLocationUpdates(
                        android.location.LocationManager.GPS_PROVIDER, 0L, 0f, listener,
                        android.os.Looper.getMainLooper()
                    )
                    cont.invokeOnCancellation { lm.removeUpdates(listener) }
                }
            }
        } else null
        if (gpsFix != null) return@withContext gpsFix

        // Network fix (cell/WiFi triangulation) — faster but less accurate
        return@withContext if (lm.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
            kotlinx.coroutines.withTimeoutOrNull(8_000L) {
                kotlinx.coroutines.suspendCancellableCoroutine<android.location.Location?> { cont ->
                    val listener = object : android.location.LocationListener {
                        override fun onLocationChanged(loc: android.location.Location) {
                            lm.removeUpdates(this)
                            cont.resume(loc, null)
                        }
                        @Deprecated("") override fun onStatusChanged(p: String?, s: Int, e: android.os.Bundle?) {}
                        override fun onProviderEnabled(p: String) {}
                        override fun onProviderDisabled(p: String) { cont.resume(null, null) }
                    }
                    lm.requestLocationUpdates(
                        android.location.LocationManager.NETWORK_PROVIDER, 0L, 0f, listener,
                        android.os.Looper.getMainLooper()
                    )
                    cont.invokeOnCancellation { lm.removeUpdates(listener) }
                }
            }
        } else null
    }

    /** Reverse-geocode coordinates to a human-readable city/area name. */
    private suspend fun geocodeCoordinates(
        latitude: Float,
        longitude: Float,
        context: android.content.Context
    ): String? = withContext(ioDispatcher) {
        runCatching {
            val geocoder = android.location.Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude.toDouble(), longitude.toDouble(), 1)
            addresses?.firstOrNull()?.let { addr ->
                addr.locality ?: addr.subAdminArea ?: addr.adminArea ?: addr.countryName
            }
        }.getOrNull()
    }

    /** Posts a high-priority local notification on the severe-weather channel. */
    private fun triggerLocalNotification(
        context: android.content.Context,
        title: String,
        message: String
    ) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) return
        val notification = androidx.core.app.NotificationCompat.Builder(
            context, NotificationHelper.CHANNEL_SEVERE
        )
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        androidx.core.app.NotificationManagerCompat.from(context)
            .notify(WEATHER_ALERT_NOTIFICATION_ID, notification)
    }
}
