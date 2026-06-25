package com.example.aiweathermonitor.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import kotlin.math.cos
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiweathermonitor.getWeatherCodeDescription
import com.example.aiweathermonitor.DayForecast
import com.example.aiweathermonitor.GeocodingResult
import com.example.aiweathermonitor.HourForecast
import com.example.aiweathermonitor.WeatherState
import com.example.aiweathermonitor.data.models.NewsArticle
import com.example.aiweathermonitor.getAqiDescription
import com.example.aiweathermonitor.getUvDescription
import com.example.aiweathermonitor.ui.AppSettings
import com.example.aiweathermonitor.ui.WindUnit
import com.example.aiweathermonitor.ui.PressureUnit
import com.example.aiweathermonitor.ui.VisibilityUnit
import com.example.aiweathermonitor.ui.LiquidGlassTextField
import com.example.aiweathermonitor.util.formatTemp
import com.example.aiweathermonitor.util.formatWind
import com.example.aiweathermonitor.util.formatPressure
import com.example.aiweathermonitor.util.formatVisibility
import com.example.aiweathermonitor.theme.Spacing
import kotlin.math.roundToInt

import androidx.compose.ui.graphics.graphicsLayer

data class DashboardActions(
    val onQueryChange: (String) -> Unit,
    val onSelectCity: (GeocodingResult) -> Unit,
    val onRefresh: () -> Unit,
    val onLocationClick: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onNavigateToSettings: () -> Unit = {},
    val onSaveWeatherApiKey: (String) -> Unit,
    val onDismissSettings: () -> Unit,
    val onSaveCity: () -> Unit,
    val onRemoveCity: (GeocodingResult) -> Unit,
    val onToggleCelsius: () -> Unit,
    val onToggleSavedCitiesScreen: (Boolean) -> Unit,
    val onShareWeather: () -> Unit = {},
    val onOpenArticle: (String) -> Unit = {},
    val onRefreshNews: () -> Unit = {}
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: WeatherState,
    settings: AppSettings,
    actions: DashboardActions,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val isDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
    val mainTextColor = MaterialTheme.colorScheme.onBackground
    val subTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Day vs night for icon selection — current time within sunrise..sunset.
    val nowMins = run {
        val cal = java.util.Calendar.getInstance()
        cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
    }
    val isDay = nowMins in state.sunriseMinutes..state.sunsetMinutes

    CompositionLocalProvider(LocalWeatherCode provides state.weatherCode) {
        Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = Spacing.screenHorizontal)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(Spacing.sectionGap)
        ) {
            Spacer(modifier = Modifier.height(Spacing.small))

            // 1. Offline Banner — liquid glass
            LiquidGlassOfflineBanner(isOnline = state.isOnline, lastRefreshedTime = state.lastRefreshedTime, lastRefreshedEpoch = state.lastRefreshedEpoch)

            // 2. Search Bar — liquid glass
            LiquidGlassSearchBar(
                searchQuery = state.searchQuery,
                onQueryChange = actions.onQueryChange,
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            // 3. Action Toolbar — liquid glass buttons
            GlassActionToolbar(
                onLocationClick = actions.onLocationClick,
                onToggleSavedCitiesScreen = actions.onToggleSavedCitiesScreen,
                onSettingsClick = actions.onSettingsClick,
                onNavigateToSettings = actions.onNavigateToSettings,
                onRefresh = actions.onRefresh,
                onShareWeather = actions.onShareWeather
            )

            // 4. Search Results — glass dropdown
            LiquidGlassSearchResults(
                searchResults = state.searchResults,
                onSelectCity = actions.onSelectCity
            )

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                )
            }

            state.errorMessage?.let { error ->
                LiquidGlassAlertCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W600,
                        modifier = Modifier.padding(4.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 5. Weather Header — ultra-thin glass typography
            WeatherHeader(
                selectedCity = state.selectedCity,
                timezone = state.timezone,
                savedCities = state.savedCities,
                weatherCode = state.weatherCode,
                temperature = state.temperature,
                isCelsius = state.isCelsius,
                dailyForecast = state.dailyForecast,
                onRemoveCity = actions.onRemoveCity,
                onSaveCity = actions.onSaveCity,
                onToggleCelsius = actions.onToggleCelsius,
                mainTextColor = mainTextColor,
                subTextColor = subTextColor,
                isDay = isDay
            )

            // 5b. Smart Summary Card
            if (state.smartSummary.isNotBlank()) {
                SmartSummaryCard(summary = state.smartSummary, mainTextColor = mainTextColor)
            }

            // 6. Severe Weather Alert — glass alert card
            SevereWeatherAlertCard(
                alertEvent = state.alertEvent,
                alertHeadline = state.alertHeadline,
                alertDesc = state.alertDescription
            )

            // ── Forecast & all weather details — one continuous, calm scroll ──
            run {
                        if (state.hourlyForecast.isNotEmpty()) {
                            LiquidGlassCard(
                                title = "HOURLY FORECAST",
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                ) {
                                    itemsIndexed(state.hourlyForecast) { index, hour ->
                                        // Hourly slots are consecutive from "Now"; derive day/night per slot.
                                        val slotMins = (nowMins + index * 60) % 1440
                                        HourlyItem(
                                            hour = hour.hour,
                                            temp = formatTemp(hour.temperature, state.isCelsius),
                                            weatherCode = hour.weatherCode,
                                            precipChance = hour.precipChance,
                                            isDay = slotMins in state.sunriseMinutes..state.sunsetMinutes
                                        )
                                    }
                                }
                            }
                            
                            HourlyTemperatureChartCard(state.hourlyForecast, state.isCelsius)
                        }

                        if (state.dailyForecast.isNotEmpty()) {
                            LiquidGlassCard(
                                title = "7-DAY FORECAST",
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    val weekMinC = state.dailyForecast.minOf { it.tempMin }
                                    val weekMaxC = state.dailyForecast.maxOf { it.tempMax }
                                    state.dailyForecast.forEach { day ->
                                        DailyItem(
                                            day = day.day,
                                            weatherCode = day.weatherCode,
                                            tempMin = formatTemp(day.tempMin, state.isCelsius),
                                            tempMax = formatTemp(day.tempMax, state.isCelsius),
                                            tempMinC = day.tempMin,
                                            tempMaxC = day.tempMax,
                                            weekMinC = weekMinC,
                                            weekMaxC = weekMaxC,
                                            uvIndex = day.uvIndex,
                                            precipChance = day.precipChance
                                        )
                                    }
                                }
                            }
                        }
            }

            run {
                        Text(
                            text = "Weather Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.W300,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val dpC = state.temperature - (100f - state.humidity) / 5f
                            val dpDesc = "The dew point is approx ${formatTemp(dpC, state.isCelsius)} right now."

                            LiquidGlassMetricCard(
                                title = "Humidity",
                                value = "${state.humidity.roundToInt()}%",
                                desc = dpDesc,
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(state.humidity / 100f)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }

                            LiquidGlassMetricCard(
                                title = "Wind Speed",
                                value = formatWind(state.windSpeed, settings.windUnit) + if (state.windDir.isNotBlank()) "  ${state.windDir}" else "",
                                desc = if (state.windDir.isNotBlank()) {
                                    val windDesc = when {
                                        state.windSpeed < 20f -> "Light breeze."
                                        state.windSpeed < 40f -> "Moderate winds."
                                        else -> "Strong winds — take care outdoors."
                                    }
                                    "Direction: ${state.windDir}. $windDesc"
                                } else "Wind speed from current source.",
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Static speed bar (0–120 km/h scale)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((state.windSpeed / 120f).coerceIn(0f, 1f))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LiquidGlassMetricCard(
                                title = "Pressure",
                                value = formatPressure(state.pressure, settings.pressureUnit),
                                desc = if (state.pressure < 1009) "Low pressure center. Rain showers possible." else "High pressure system. Clear and dry.",
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                val pTrack = MaterialTheme.colorScheme.surfaceVariant
                                val pTick = MaterialTheme.colorScheme.outline
                                val pAccent = MaterialTheme.colorScheme.primary
                                Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val barHeight = 4.dp.toPx()
                                    val y = (h - barHeight) / 2f
                                    drawRoundRect(
                                        color = pTrack,
                                        topLeft = Offset(0f, y),
                                        size = Size(w, barHeight),
                                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                    )
                                    val normX = w / 2f
                                    drawLine(
                                        color = pTick,
                                        start = Offset(normX, y - 2.dp.toPx()),
                                        end = Offset(normX, y + barHeight + 2.dp.toPx()),
                                        strokeWidth = 1.dp.toPx()
                                    )
                                    val fraction = ((state.pressure - 980f) / 60f).coerceIn(0f, 1f)
                                    val cx = fraction * w
                                    drawCircle(
                                        color = pAccent,
                                        radius = 4.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                }
                            }

                            LiquidGlassMetricCard(
                                title = "Air Quality (US-AQI)",
                                value = if (state.aqi > 0) "${state.aqi} · ${state.aqi.getAqiDescription()}" else "N/A",
                                desc = when (state.aqi) {
                                    1 -> "Good air quality. Perfect for outdoor activities!"
                                    2 -> "Moderate. Acceptable for most people."
                                    3 -> "Unhealthy for sensitive groups. Limit strenuous outdoor activity."
                                    4 -> "Unhealthy. Avoid prolonged outdoor exertion."
                                    5 -> "Very Unhealthy. Avoid outdoor activity."
                                    6 -> "Hazardous. Stay indoors."
                                    else -> "AQI unavailable. Enable WeatherAPI key for air quality data."
                                },
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val barHeight = 6.dp.toPx()
                                    val y = (h - barHeight) / 2f
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF10B981),
                                                Color(0xFFFBBF24),
                                                Color(0xFFF97316),
                                                Color(0xFFEF4444),
                                                Color(0xFF8B5CF6)
                                            )
                                        ),
                                        topLeft = Offset(0f, y),
                                        size = Size(w, barHeight),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                    val fraction = (state.aqi / 200f).coerceIn(0f, 1f)
                                    val cx = fraction * w
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                    drawCircle(
                                        color = Color(0xFF0F172A),
                                        radius = 2.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LiquidGlassMetricCard(
                                title = "UV Index",
                                value = "${if (state.uvIndex % 1 == 0f) state.uvIndex.toInt().toString() else state.uvIndex.toString()} · ${state.uvIndex.getUvDescription()}",
                                desc = when {
                                    state.uvIndex >= 11f -> "Extreme — stay indoors between 10 AM–4 PM."
                                    state.uvIndex >= 8f  -> "Very High — SPF 30+ & protective clothing required."
                                    state.uvIndex >= 6f  -> "High — seek shade, wear a hat."
                                    state.uvIndex >= 3f  -> "Moderate — sunscreen recommended."
                                    else                 -> "Low UV radiation. Safe for outdoor activity."
                                },
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
                                    val w = size.width
                                    val h = size.height
                                    val barHeight = 6.dp.toPx()
                                    val y = (h - barHeight) / 2f
                                    drawRoundRect(
                                        brush = Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF10B981),
                                                Color(0xFFFBBF24),
                                                Color(0xFFF97316),
                                                Color(0xFFEF4444),
                                                Color(0xFF8B5CF6)
                                            )
                                        ),
                                        topLeft = Offset(0f, y),
                                        size = Size(w, barHeight),
                                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                                    )
                                    val fraction = (state.uvIndex / 11f).coerceIn(0f, 1f)
                                    val cx = fraction * w
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                    drawCircle(
                                        color = Color(0xFF0F172A),
                                        radius = 2.dp.toPx(),
                                        center = Offset(cx, h / 2f)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                        }
            }

            run {
                        SunriseSunsetArcCard(state)

                        val pulseAlpha = 0.85f  // static — removed infinite transition

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LiquidGlassMetricCard(
                                title = "Feels Like",
                                value = formatTemp(state.feelsLike, state.isCelsius),
                                desc = when {
                                    state.feelsLike < state.temperature - 3 -> "Feels colder due to wind chill."
                                    state.feelsLike > state.temperature + 3 -> "Feels hotter due to humidity."
                                    else -> "Feels close to the actual temperature."
                                },
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(((state.feelsLike + 20f) / 60f).coerceIn(0f, 1f))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }

                            LiquidGlassMetricCard(
                                title = "Visibility",
                                value = formatVisibility(state.visibility, settings.visibilityUnit),
                                desc = when {
                                    state.visibility >= 10f -> "Clear visibility. Great conditions."
                                    state.visibility >= 5f  -> "Good visibility with minor haze."
                                    state.visibility >= 2f  -> "Moderate fog or haze present."
                                    else                    -> "Poor visibility. Drive with caution."
                                },
                                isDark = isDarkTheme,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((state.visibility / 20f).coerceIn(0f, 1f))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                                                // Pollen card — shown when data is available
                        val pollen = state.pollenData
                        if (pollen != null && pollen.peak > 0f) {
                            PollenCard(
                                pollen = pollen,
                                isDark = isDarkTheme,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                                                LiquidGlassMetricCard(
                            title = "Climatic Anomalies",
                            value = "Normal",
                            desc = "Current telemetry values align with 30-year seasonal models.",
                            isDark = isDarkTheme,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val statusDot = MaterialTheme.colorScheme.primary
                                Canvas(modifier = Modifier.size(8.dp)) {
                                    drawCircle(
                                        color = statusDot.copy(alpha = pulseAlpha),
                                        radius = 4.dp.toPx()
                                    )
                                }
                                Text(
                                    text = "Telemetry Aligned",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.W700,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }


            }

            // ── Location-aware news (RSS): weather + local ─────────────────
            NewsCard(
                weatherArticles = state.newsArticles,
                localArticles = state.localNewsArticles,
                isLoading = state.isNewsLoading,
                errorMessage = state.newsError,
                onOpenArticle = actions.onOpenArticle,
                onRefreshNews = actions.onRefreshNews,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Pinned compact header — appears once the hero scrolls out of view.
        val showCompactHeader = scrollState.value > with(LocalDensity.current) { 330.dp.toPx() }
        AnimatedVisibility(
            visible = showCompactHeader,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            CompactWeatherHeader(
                city = state.selectedCity,
                temp = formatTemp(state.temperature, state.isCelsius),
                condition = state.weatherCode.getWeatherCodeDescription(),
                weatherCode = state.weatherCode,
                isDay = isDay
            )
        }

        // Settings Dialog — liquid glass
        if (state.showKeyDialog) {
            SettingsDialog(
                state = state,
                onDismissSettings = actions.onDismissSettings,
                onSaveWeatherApiKey = actions.onSaveWeatherApiKey,
                onToggleCelsius = actions.onToggleCelsius
            )
        }

        // Saved Cities Dialog — liquid glass
        if (state.showSavedCitiesScreen) {
            SavedCitiesDialog(
                state = state,
                onToggleSavedCitiesScreen = actions.onToggleSavedCitiesScreen,
                onSelectCity = actions.onSelectCity,
                onRemoveCity = actions.onRemoveCity
            )
        }
    }
    }
}

// ─── COMPACT (PINNED) HEADER ─────────────────────────────────────

@Composable
private fun CompactWeatherHeader(
    city: String,
    temp: String,
    condition: String,
    weatherCode: Int,
    isDay: Boolean
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = city,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = condition,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = temp,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            WeatherIcon(weatherCode, modifier = Modifier.size(30.dp), isDay = isDay)
        }
    }
}

// ─── NEWS CARD (RSS headlines) ───────────────────────────────────

@Composable
fun NewsCard(
    weatherArticles: List<NewsArticle>,
    localArticles: List<NewsArticle>,
    isLoading: Boolean,
    errorMessage: String?,
    onOpenArticle: (String) -> Unit,
    onRefreshNews: () -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by rememberSaveable { mutableStateOf(0) }   // 0 = Weather, 1 = Local
    val articles = if (tab == 0) weatherArticles else localArticles

    LiquidGlassCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NewsTabLabel("Weather", selected = tab == 0) { tab = 0 }
                NewsTabLabel("Local", selected = tab == 1) { tab = 1 }
            }
            IconButton(onClick = onRefreshNews, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh news",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.small))

        when {
            isLoading && articles.isEmpty() -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Loading headlines…",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            articles.isEmpty() && errorMessage != null -> {
                Text(
                    text = errorMessage,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            articles.isEmpty() -> {
                Text(
                    text = if (tab == 0) "No weather news right now." else "No local news right now.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    articles.forEachIndexed { index, article ->
                        NewsItem(article = article, onClick = { onOpenArticle(article.link) })
                        if (index != articles.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsTabLabel(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.titleSmall,
        color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun NewsItem(
    article: NewsArticle,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = article.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W600,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        val meta = listOf(article.source, article.pubDate)
            .filter { it.isNotBlank() }
            .joinToString("  •  ")
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── BACKGROUND MESH (static — no animation overhead) ─────────────

@Composable
fun LiquidMeshOverlay(modifier: Modifier = Modifier) {
    // Intentionally empty: removed heavy infinite-transition Canvas blobs.
    // Background gradient is handled by the screen's Box background.
}

// ─── GLASS ACTION TOOLBAR ───────────────────────────────────────

@Composable
fun GlassActionToolbar(
    onLocationClick: () -> Unit,
    onToggleSavedCitiesScreen: (Boolean) -> Unit,
    onSettingsClick: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onRefresh: () -> Unit,
    onShareWeather: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LiquidGlassToolbarButton(onClick = onLocationClick) {
                Icon(Icons.Default.Place, contentDescription = "Use Current Location", modifier = Modifier.size(20.dp))
            }

            LiquidGlassToolbarButton(onClick = { onToggleSavedCitiesScreen(true) }) {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Favorites", modifier = Modifier.size(20.dp))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LiquidGlassToolbarButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Weather", modifier = Modifier.size(20.dp))
            }
            LiquidGlassToolbarButton(onClick = onShareWeather) {
                Icon(Icons.Default.Share, contentDescription = "Share weather", modifier = Modifier.size(20.dp))
            }
            LiquidGlassToolbarButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── WEATHER HEADER ─────────────────────────────────────────────

@Composable
fun WeatherHeader(
    selectedCity: String,
    timezone: String,
    savedCities: List<GeocodingResult>,
    weatherCode: Int,
    temperature: Float,
    isCelsius: Boolean,
    dailyForecast: List<DayForecast>,
    onRemoveCity: (GeocodingResult) -> Unit,
    onSaveCity: () -> Unit,
    onToggleCelsius: () -> Unit,
    mainTextColor: Color,
    subTextColor: Color,
    isDay: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left spacer balances the favourite button so the title stays centered.
            Spacer(modifier = Modifier.size(36.dp))
            Text(
                text = selectedCity,
                style = MaterialTheme.typography.displaySmall,
                color = mainTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )

            val isFavorite = savedCities.any { it.name.equals(selectedCity, ignoreCase = true) }
            IconButton(
                onClick = {
                    if (isFavorite) {
                        val matched = savedCities.firstOrNull { it.name.equals(selectedCity, ignoreCase = true) }
                        if (matched != null) onRemoveCity(matched)
                    } else {
                        onSaveCity()
                    }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Toggle Favorite",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        DigitalClock(timezoneId = timezone, textColor = subTextColor)
        Spacer(modifier = Modifier.height(4.dp))

        // Temperature gently counts up to its value on load / change.
        val animatedTemp by animateFloatAsState(
            targetValue = temperature,
            animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            label = "tempCountUp"
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = formatTemp(animatedTemp, isCelsius),
                style = MaterialTheme.typography.displayLarge,
                color = mainTextColor,
                modifier = Modifier.clickable { onToggleCelsius() }
            )
            Spacer(modifier = Modifier.width(12.dp))
            WeatherIcon(
                code = weatherCode,
                modifier = Modifier.size(76.dp),
                isDay = isDay
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = weatherCode.getWeatherCodeDescription(),
            style = MaterialTheme.typography.titleMedium,
            color = subTextColor
        )

        val firstDay = dailyForecast.firstOrNull()
        if (firstDay != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "H:${formatTemp(firstDay.tempMax, isCelsius)}   L:${formatTemp(firstDay.tempMin, isCelsius)}",
                style = MaterialTheme.typography.bodyMedium,
                color = subTextColor
            )
        }
    }
}

@Composable
fun DigitalClock(
    timezoneId: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    var timeText by remember(timezoneId) { mutableStateOf("") }
    LaunchedEffect(timezoneId) {
        val formatter = java.text.SimpleDateFormat("EEEE, MMMM d • h:mm:ss a", java.util.Locale.getDefault()).apply {
            timeZone = if (timezoneId.isNotBlank()) {
                java.util.TimeZone.getTimeZone(timezoneId)
            } else {
                java.util.TimeZone.getDefault()
            }
        }
        while (true) {
            timeText = formatter.format(java.util.Date())
            kotlinx.coroutines.delay(1000L)
        }
    }
    if (timeText.isNotEmpty()) {
        Text(
            text = timeText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.W500,
            color = textColor,
            modifier = modifier
        )
    }
}

// ─── POLLEN CARD ─────────────────────────────────────────────────────────────

@Composable
fun PollenCard(
    pollen: com.example.aiweathermonitor.data.models.PollenData,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val riskColor = when (pollen.riskLevel) {
        "Low"       -> Color(0xFF22C55E)
        "Moderate"  -> Color(0xFFEAB308)
        "High"      -> Color(0xFFF97316)
        else         -> Color(0xFFEF4444)  // Very High
    }

    LiquidGlassMetricCard(
        title = "Pollen",
        value = pollen.riskLevel,
        desc = if (pollen.activeTypes.isNotEmpty())
            "Active: ${pollen.activeTypes.joinToString(", ")}"
        else
            "No significant pollen detected today",
        isDark = isDark,
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Risk bar
            val fraction = (pollen.peak / 100f).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp))
                        .background(riskColor)
                )
            }
            // Per-type breakdown (non-zero only)
            val types = listOf(
                "Grass" to pollen.grass,
                "Birch" to pollen.birch,
                "Ragweed" to pollen.ragweed,
                "Alder" to pollen.alder,
                "Mugwort" to pollen.mugwort,
                "Olive" to pollen.olive
            ).filter { it.second > 1f }
            if (types.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    types.forEach { (name, count) ->
                        Surface(
                            color = riskColor.copy(alpha = 0.15f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = " ${count.toInt()}",
                                fontSize = 11.sp,
                                color = riskColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── SMART SUMMARY CARD ─────────────────────────────────────────────────────

@Composable
fun SmartSummaryCard(
    summary: String,
    mainTextColor: Color,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "✦",
                fontSize = 14.sp,
                color = mainTextColor.copy(alpha = 0.70f)
            )
            Text(
                text = summary,
                fontSize = 14.sp,
                color = mainTextColor.copy(alpha = 0.90f),
                fontWeight = FontWeight.W400,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── SEVERE WEATHER ALERT ───────────────────────────────────────

@Composable
fun SevereWeatherAlertCard(
    alertEvent: String?,
    alertHeadline: String?,
    alertDesc: String?,
    modifier: Modifier = Modifier
) {
    if (!alertEvent.isNullOrBlank()) {
        LiquidGlassAlertCard(
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = "⚠️ ${alertEvent.uppercase()}",
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 13.sp,
                fontWeight = FontWeight.W700
            )
            if (!alertHeadline.isNullOrBlank()) {
                Text(
                    text = alertHeadline,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.W600
                )
            }
            if (!alertDesc.isNullOrBlank()) {
                Text(
                    text = alertDesc,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.80f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

// ─── SETTINGS DIALOG ────────────────────────────────────────────

@Composable
fun SettingsDialog(
    state: WeatherState,
    onDismissSettings: () -> Unit,
    onSaveWeatherApiKey: (String) -> Unit,
    onToggleCelsius: () -> Unit
) {
    var tempWeatherApiKeyText by remember { mutableStateOf(state.weatherApiKey) }
    
    LiquidGlassDialog(
        onDismissRequest = onDismissSettings,
        title = {
            Text(
                "Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Temperature Units",
                    fontWeight = FontWeight.W700,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Use Fahrenheit (°F)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = !state.isCelsius,
                        onCheckedChange = { onToggleCelsius() }
                    )
                }

                HorizontalDivider()

                Text(
                    text = "WeatherAPI.com Key (Optional)",
                    fontWeight = FontWeight.W700,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                LiquidGlassTextField(
                    value = tempWeatherApiKeyText,
                    onValueChange = { tempWeatherApiKeyText = it },
                    placeholder = { Text("Enter WeatherAPI.com Key", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Optional: unlocks UV index, AQI, weather alerts & 3-day forecast from weatherapi.com. Free tier available.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                Text(
                    text = "Open-Meteo (No Key Required)",
                    fontWeight = FontWeight.W700,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Always active as primary or fallback source. Provides 14-day forecast with zero configuration.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveWeatherApiKey(tempWeatherApiKeyText.trim())
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissSettings) {
                Text("Cancel")
            }
        }
    )
}

// ─── SAVED CITIES DIALOG ────────────────────────────────────────

@Composable
fun SavedCitiesDialog(
    state: WeatherState,
    onToggleSavedCitiesScreen: (Boolean) -> Unit,
    onSelectCity: (GeocodingResult) -> Unit,
    onRemoveCity: (GeocodingResult) -> Unit
) {
    LiquidGlassDialog(
        onDismissRequest = { onToggleSavedCitiesScreen(false) },
        title = { 
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Saved Favorites",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W600,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = { onToggleSavedCitiesScreen(false) }) {
                    Text("Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.savedCities.isEmpty()) {
                    Text(
                        text = "No favorite cities saved yet. Tap the star button next to any city name to bookmark it!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                    ) {
                        items(state.savedCities) { city ->
                            val cardShape = RoundedCornerShape(16.dp)
                            Card(
                                shape = cardShape,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            onSelectCity(city)
                                            onToggleSavedCitiesScreen(false)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = city.name, fontWeight = FontWeight.W600, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                        if (city.country != null) {
                                            Text(text = city.country, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = { onRemoveCity(city) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Remove Favorite",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

// ─── SUNRISE/SUNSET ARC CARD ────────────────────────────────────

private fun calculateSunPosition(
    timeInMins: Int,
    sunriseMins: Int,
    sunsetMins: Int,
    width: Float,
    height: Float
): Offset? {
    if (timeInMins in sunriseMins..sunsetMins) {
        val progress = if (sunsetMins > sunriseMins) {
            (timeInMins - sunriseMins).toFloat() / (sunsetMins - sunriseMins).toFloat()
        } else 0.5f
        val t = progress
        val u = 1f - t
        val x = u * u * 0f + 2f * u * t * (width / 2f) + t * t * width
        val y = u * u * height + 2f * u * t * (-height * 0.4f) + t * t * height
        return Offset(x, y)
    }
    return null
}

private fun calculateChartPoints(
    temps: List<Float>,
    minTemp: Float,
    tempRange: Float,
    width: Float,
    topY: Float,        // top of the plotting band (leaves room for temp labels)
    bottomY: Float,     // bottom of the plotting band (leaves room for hour labels)
    xInset: Float       // horizontal inset so edge dots/labels don't clip
): List<Offset> {
    val n = temps.size
    val usableWidth = (width - 2f * xInset).coerceAtLeast(1f)
    val stepX = if (n > 1) usableWidth / (n - 1) else 0f
    val bandHeight = (bottomY - topY).coerceAtLeast(1f)
    val points = ArrayList<Offset>(n)
    for (i in temps.indices) {
        val x = xInset + i * stepX
        val normalized = (temps[i] - minTemp) / tempRange   // 0 = coldest, 1 = hottest
        val y = bottomY - normalized * bandHeight            // hottest sits at topY
        points.add(Offset(x, y))
    }
    return points
}

@Composable
fun SunriseSunsetArcCard(
    state: WeatherState,
    modifier: Modifier = Modifier
) {
    val sunriseMins = state.sunriseMinutes
    val sunsetMins = state.sunsetMinutes
    val diffMins = (sunsetMins - sunriseMins).coerceAtLeast(0)
    val diffHours = diffMins / 60
    val diffMinsRemain = diffMins % 60
    val daylightStr = if (diffMinsRemain > 0) {
        "$diffHours h $diffMinsRemain m of daylight"
    } else {
        "$diffHours hours of daylight"
    }

    LiquidGlassCard(
        title = "SUNRISE & SUNSET",
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val arcColor = MaterialTheme.colorScheme.outline
            val baseLineColor = MaterialTheme.colorScheme.outlineVariant
            val sunColor = MaterialTheme.colorScheme.primary
            val offColor = MaterialTheme.colorScheme.onSurfaceVariant

            val nowMins = run {
                val cal = java.util.Calendar.getInstance()
                cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
            }
            val total = (sunsetMins - sunriseMins).coerceAtLeast(1)
            val progress = ((nowMins - sunriseMins).toFloat() / total).coerceIn(0f, 1f)
            val isUp = nowMins in sunriseMins..sunsetMins

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(horizontal = 16.dp)
            ) {
                val wDp = maxWidth
                val hDp = maxHeight
                Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val arcPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height)
                    quadraticTo(width / 2f, -height * 0.4f, width, height)
                }
                
                drawPath(
                    path = arcPath,
                    color = arcColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                )
                
                drawLine(
                    color = baseLineColor,
                    start = androidx.compose.ui.geometry.Offset(0f, height),
                    end = androidx.compose.ui.geometry.Offset(width, height),
                    strokeWidth = 1.dp.toPx()
                )

            }

                // Sun icon riding the arc at the current sun position.
                val iconSize = 26.dp
                if (isUp) {
                    val t = progress
                    val u = 1f - t
                    val yFrac = u * u + t * t - 0.8f * u * t
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = "Sun position",
                        tint = sunColor,
                        modifier = Modifier
                            .size(iconSize)
                            .offset(x = wDp * progress - iconSize / 2, y = hDp * yFrac - iconSize / 2)
                    )
                } else {
                    // Below the horizon — dimmed at the appropriate edge.
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = "Sun below horizon",
                        tint = offColor.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(20.dp)
                            .offset(
                                x = if (nowMins < sunriseMins) (-4).dp else wDp - 16.dp,
                                y = hDp - 14.dp
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WbSunny,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Sunrise", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.W700)
                    }
                    Text(state.sunrise, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W600)
                }
                Text(
                    text = daylightStr,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.W500
                )
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WbTwilight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("Sunset", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.W700)
                    }
                    Text(state.sunset, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W600)
                }
            }
        }
    }
}

// ─── HOURLY TEMPERATURE CHART ───────────────────────────────────

@Composable
fun HourlyTemperatureChartCard(
    hourlyForecast: List<HourForecast>,
    isCelsius: Boolean,
    modifier: Modifier = Modifier
) {
    LiquidGlassCard(
        title = "TEMPERATURE TREND",
        modifier = modifier.fillMaxWidth()
    ) {
        if (hourlyForecast.isEmpty()) {
            Text("No forecast data available", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                val chartLine = MaterialTheme.colorScheme.primary
                val surfaceColor = MaterialTheme.colorScheme.surface
                val tempLabelArgb = MaterialTheme.colorScheme.onSurface.toArgb()
                val hourLabelArgb = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                val readoutArgb = MaterialTheme.colorScheme.onPrimary.toArgb()
                val n = hourlyForecast.size
                var selectedIndex by remember(hourlyForecast) { mutableStateOf(-1) }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .pointerInput(hourlyForecast) {
                            val xi = 18.dp.toPx()
                            fun idxFor(x: Float): Int {
                                val usable = (size.width - 2f * xi).coerceAtLeast(1f)
                                val step = if (n > 1) usable / (n - 1) else 0f
                                return if (step <= 0f) 0 else ((x - xi) / step).roundToInt().coerceIn(0, n - 1)
                            }
                            detectTapGestures { o -> selectedIndex = idxFor(o.x) }
                        }
                        .pointerInput(hourlyForecast) {
                            val xi = 18.dp.toPx()
                            fun idxFor(x: Float): Int {
                                val usable = (size.width - 2f * xi).coerceAtLeast(1f)
                                val step = if (n > 1) usable / (n - 1) else 0f
                                return if (step <= 0f) 0 else ((x - xi) / step).roundToInt().coerceIn(0, n - 1)
                            }
                            detectHorizontalDragGestures(
                                onDragStart = { o -> selectedIndex = idxFor(o.x) },
                                onHorizontalDrag = { change, _ -> selectedIndex = idxFor(change.position.x) }
                            )
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val temps = hourlyForecast.map {
                        if (isCelsius) it.temperature else (it.temperature * 9f / 5f + 32f)
                    }
                    val minTemp = temps.minOrNull() ?: 0f
                    val maxTemp = temps.maxOrNull() ?: 100f
                    val tempRange = (maxTemp - minTemp).coerceAtLeast(1f)

                    // Insets: room above for temp labels, below for hour labels, sides for edge dots.
                    val xInset = 18.dp.toPx()
                    val topY = 30.dp.toPx()
                    val bottomY = height - 22.dp.toPx()
                    val points = calculateChartPoints(temps, minTemp, tempRange, width, topY, bottomY, xInset)

                    // Gradient fill under the line (down to the baseline)
                    val fillPath = androidx.compose.ui.graphics.Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points.first().x, bottomY)
                            for (p in points) lineTo(p.x, p.y)
                            lineTo(points.last().x, bottomY)
                            close()
                        }
                    }
                    drawPath(
                        path = fillPath,
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                chartLine.copy(alpha = 0.18f),
                                chartLine.copy(alpha = 0.04f),
                                Color.Transparent
                            ),
                            startY = topY,
                            endY = bottomY
                        )
                    )

                    val linePath = androidx.compose.ui.graphics.Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = chartLine,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 2.5.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )

                    val tempPaint = android.graphics.Paint().apply {
                        color = tempLabelArgb
                        textSize = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }
                    val hourPaint = android.graphics.Paint().apply {
                        color = hourLabelArgb
                        textSize = 9.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        isAntiAlias = true
                    }

                    // Thin out labels so they never crowd, regardless of point count.
                    val n = points.size
                    val tempStep = when { n > 16 -> 3; n > 10 -> 2; else -> 1 }
                    val hourStep = ((n + 5) / 6).coerceAtLeast(1)
                    for (i in points.indices) {
                        drawCircle(color = chartLine.copy(alpha = 0.15f), radius = 5.dp.toPx(), center = points[i])
                        drawCircle(color = chartLine, radius = 2.5.dp.toPx(), center = points[i])

                        if (i % tempStep == 0 || i == n - 1) {
                            drawContext.canvas.nativeCanvas.drawText(
                                temps[i].roundToInt().toString() + "°",
                                points[i].x,
                                points[i].y - 9.dp.toPx(),
                                tempPaint
                            )
                        }
                        if (i % hourStep == 0 || i == n - 1) {
                            drawContext.canvas.nativeCanvas.drawText(
                                hourlyForecast[i].hour,
                                points[i].x,
                                height - 5.dp.toPx(),
                                hourPaint
                            )
                        }
                    }

                    // ── Scrubber: guide line, highlight + readout for the touched point ──
                    if (selectedIndex in points.indices) {
                        val sp = points[selectedIndex]
                        drawLine(
                            color = chartLine.copy(alpha = 0.45f),
                            start = androidx.compose.ui.geometry.Offset(sp.x, topY),
                            end = androidx.compose.ui.geometry.Offset(sp.x, bottomY),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                        drawCircle(color = chartLine.copy(alpha = 0.20f), radius = 9.dp.toPx(), center = sp)
                        drawCircle(color = surfaceColor, radius = 5.dp.toPx(), center = sp)
                        drawCircle(color = chartLine, radius = 4.dp.toPx(), center = sp)

                        val readoutPaint = android.graphics.Paint().apply {
                            color = readoutArgb
                            textSize = 10.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            isAntiAlias = true
                        }
                        val label = hourlyForecast[selectedIndex].hour + "   " + temps[selectedIndex].roundToInt() + "°"
                        val padH = 10.dp.toPx()
                        val pillW = readoutPaint.measureText(label) + 2f * padH
                        val pillH = 20.dp.toPx()
                        val cx = sp.x.coerceIn(pillW / 2f, width - pillW / 2f)
                        val pillTop = 1.dp.toPx()
                        drawRoundRect(
                            color = chartLine,
                            topLeft = androidx.compose.ui.geometry.Offset(cx - pillW / 2f, pillTop),
                            size = androidx.compose.ui.geometry.Size(pillW, pillH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pillH / 2f, pillH / 2f)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            label, cx, pillTop + pillH / 2f + readoutPaint.textSize / 3f, readoutPaint
                        )
                    }
                }
            }
        }
    }
}

// ─── HOURLY & DAILY ITEMS ───────────────────────────────────────

@Composable
fun HourlyItem(
    hour: String,
    temp: String,
    weatherCode: Int,
    precipChance: Int = 0,
    isDay: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Text(hour, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.W600)
        WeatherIcon(weatherCode, modifier = Modifier.size(24.dp), isDay = isDay)
        Text(temp, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.W600)
        if (precipChance > 0) {
            Text(
                text = "$precipChance%",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.W700
            )
        }
    }
}

/** Maps a Celsius temperature to a cool→warm colour (icy blue → hot red). */
private fun tempColor(c: Float): Color = when {
    c <= 0f  -> Color(0xFF93C5FD)   // icy light blue
    c <= 8f  -> Color(0xFF60A5FA)   // cool blue
    c <= 16f -> Color(0xFF38BDF8)   // sky
    c <= 22f -> Color(0xFF34D399)   // mild green
    c <= 28f -> Color(0xFFFBBF24)   // warm amber
    c <= 34f -> Color(0xFFFB923C)   // hot orange
    else     -> Color(0xFFEF4444)   // very hot red
}

@Composable
fun DailyItem(
    day: String,
    weatherCode: Int,
    tempMin: String,
    tempMax: String,
    tempMinC: Float,
    tempMaxC: Float,
    weekMinC: Float,
    weekMaxC: Float,
    uvIndex: Float = 0f,
    precipChance: Int = 0
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = day,
                fontSize = 14.sp,
                fontWeight = FontWeight.W600,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.width(60.dp)
            )

            Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                WeatherIcon(weatherCode, modifier = Modifier.size(22.dp))
            }

            // Precip chance
            if (precipChance > 0) {
                Text(
                    text = "$precipChance%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.W700,
                    modifier = Modifier.width(34.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.width(34.dp))
            }

            Text(
                text = tempMin,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.W600,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )

            // Realistic range bar: positioned within the week's min..max,
            // filled with a cool→warm gradient (light/cool on the low side,
            // bright/warm on the high side).
            val weekRange = (weekMaxC - weekMinC).coerceAtLeast(1f)
            val startFrac = ((tempMinC - weekMinC) / weekRange).coerceIn(0f, 1f)
            val endFrac = ((tempMaxC - weekMinC) / weekRange).coerceIn(0f, 1f)
            BoxWithConstraints(
                modifier = Modifier
                    .width(80.dp)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val trackW = maxWidth
                Box(
                    modifier = Modifier
                        .offset(x = trackW * startFrac)
                        .width((trackW * (endFrac - startFrac)).coerceAtLeast(8.dp))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(tempColor(tempMinC), tempColor(tempMaxC))
                            )
                        )
                )
            }

            Text(
                text = tempMax,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.W600,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )
        }

        // UV label below if noteworthy
        if (uvIndex >= 3f) {
            Text(
                text = "UV ${if (uvIndex % 1 == 0f) uvIndex.toInt().toString() else "%.1f".format(uvIndex)} · ${uvIndex.getUvDescription()}",
                fontSize = 10.sp,
                color = when {
                    uvIndex >= 8f -> Color(0xFFF97316).copy(alpha = 0.90f)
                    uvIndex >= 6f -> Color(0xFFFBBF24).copy(alpha = 0.80f)
                    else          -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontWeight = FontWeight.W600,
                modifier = Modifier.padding(start = 68.dp, top = 2.dp)
            )
        }
    }
}

// ─── WEATHER BACKGROUND EFFECTS ─────────────────────────────────

// ─── WEATHER BACKGROUND (static tint — no animation overhead) ────────
@Composable
fun WeatherBackgroundEffect(weatherCode: Int, modifier: Modifier = Modifier) {
    // Static per-condition tint instead of animated particles.
    // This eliminates the always-running infinite transition that was
    // re-drawing every frame even when the screen was idle.
    val tint = when (weatherCode) {
        0, 1            -> Color(0xFFFFD700).copy(alpha = 0.04f)   // clear / sunny
        2, 3            -> Color(0xFFB0C4DE).copy(alpha = 0.04f)   // cloudy
        45, 48          -> Color(0xFF9E9E9E).copy(alpha = 0.04f)   // fog
        51, 53, 55,
        61, 63, 65,
        80, 81, 82      -> Color(0xFF4FC3F7).copy(alpha = 0.05f)   // rain
        71, 73, 75,
        77, 85, 86      -> Color(0xFFE1F5FE).copy(alpha = 0.06f)   // snow
        95, 96, 99      -> Color(0xFFF57F17).copy(alpha = 0.05f)   // storm
        else            -> Color.Transparent
    }
    androidx.compose.foundation.layout.Box(
        modifier = modifier.background(tint)
    )
}
