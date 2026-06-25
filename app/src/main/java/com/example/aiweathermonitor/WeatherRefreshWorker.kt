package com.example.aiweathermonitor

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.aiweathermonitor.config.WeatherApiConfig
import com.example.aiweathermonitor.data.models.OpenMeteoResponse
import com.example.aiweathermonitor.data.models.WeatherApiForecastResponse
import com.example.aiweathermonitor.util.AppLogger
import com.example.aiweathermonitor.util.UrlBuilder
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "WeatherRefreshWorker"
private const val WORK_NAME = "weather_periodic_refresh"

class WeatherRefreshWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences(
                WeatherApiConfig.SharedPrefsKeys.PREFS_NAME, Context.MODE_PRIVATE
            )

            val cachedJson = prefs.getString(WeatherApiConfig.SharedPrefsKeys.WEATHER_STATE_KEY, null)
                ?: return Result.success()

            val state = try {
                json.decodeFromString(WeatherState.serializer(), cachedJson)
            } catch (e: Exception) {
                AppLogger.warning("WeatherRefreshWorker: corrupt cache, skipping", TAG, e)
                return Result.success()
            }

            val lat = state.latitude.toFloat()
            val lon = state.longitude.toFloat()
            val apiKey = state.weatherApiKey.ifBlank {
                try { BuildConfig.WEATHER_API_KEY } catch (e: Exception) { "" }
            }

            // ── Fetch ─────────────────────────────────────────────────────────
            val waData: WeatherApiForecastResponse? = if (apiKey.isNotBlank()) {
                runCatching {
                    val url = UrlBuilder.weatherApiForecast(lat, lon, apiKey)
                    client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                        if (!r.isSuccessful) null
                        else r.body?.string()?.let { json.decodeFromString<WeatherApiForecastResponse>(it) }
                    }
                }.getOrNull()
            } else null

            val omData: OpenMeteoResponse? = runCatching {
                val url = UrlBuilder.openMeteoForecast(lat, lon, hourly = true, daily = true)
                client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    if (!r.isSuccessful) null
                    else r.body?.string()?.let { json.decodeFromString<OpenMeteoResponse>(it) }
                }
            }.getOrNull()

            if (waData == null && omData == null) return Result.retry()

            // ── Persist minimal update ────────────────────────────────────────
            val newTemp = waData?.current?.tempC ?: omData?.current?.temperature2m ?: state.temperature
            val newCode = omData?.current?.weatherCode ?: state.weatherCode
            val newUv = waData?.current?.uv ?: omData?.current?.uv_index ?: state.uvIndex
            val newWind = waData?.current?.windKph ?: state.windSpeed

            val updatedState = state.copy(
                temperature = newTemp,
                weatherCode = newCode,
                uvIndex = newUv,
                windSpeed = newWind,
                lastRefreshedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                smartSummary = generateSmartSummary(state)
            )

            prefs.edit().putString(
                WeatherApiConfig.SharedPrefsKeys.WEATHER_STATE_KEY,
                json.encodeToString(WeatherState.serializer(), updatedState)
            ).apply()

            // ── Widget update ─────────────────────────────────────────────────
            val widgetIntent = android.content.Intent(applicationContext, WeatherWidgetProvider::class.java).apply {
                action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
                val ids = android.appwidget.AppWidgetManager.getInstance(applicationContext)
                    .getAppWidgetIds(android.content.ComponentName(applicationContext, WeatherWidgetProvider::class.java))
                putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                setPackage(applicationContext.packageName)
            }
            applicationContext.sendBroadcast(widgetIntent) // NOSONAR

            // ── Notifications ─────────────────────────────────────────────────
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val cityName = state.selectedCity.ifBlank { "your location" }

            // 1. Severe weather alert (from WeatherAPI alerts)
            val notifySevere = prefs.getBoolean("notif_severe", true)
            if (notifySevere) {
                val alertEvent = waData?.alerts?.alert?.firstOrNull()
                if (alertEvent != null && !alertEvent.event.isNullOrBlank()) {
                    val lastAlert = prefs.getString("last_alert_event", null)
                    if (lastAlert != alertEvent.event) {
                        NotificationHelper.notifySevere(
                            applicationContext,
                            alertEvent.event,
                            alertEvent.headline ?: alertEvent.desc ?: "Severe weather in your area"
                        )
                        prefs.edit().putString("last_alert_event", alertEvent.event).apply()
                    }
                }
            }

            // 2. Rain in next ~30 min (first hourly slot ≥40% precip chance)
            val notifyRain = prefs.getBoolean("notif_rain", true)
            if (notifyRain) {
                val rainSlot = updatedState.hourlyForecast.take(3).firstOrNull { it.precipChance >= 40 }
                if (rainSlot != null) {
                    val lastRainNotifDate = prefs.getString("last_rain_notif_date", null)
                    if (lastRainNotifDate != todayStr) {
                        NotificationHelper.notifyRainSoon(
                            applicationContext, cityName, rainSlot.precipChance, rainSlot.hour
                        )
                        prefs.edit().putString("last_rain_notif_date", todayStr).apply()
                    }
                }
            }

            // 3. Daily briefing at 7 AM (once per day)
            val notifyBriefing = prefs.getBoolean("notif_briefing", false)
            if (notifyBriefing && hour == 7) {
                val lastBriefingDate = prefs.getString("last_briefing_date", null)
                if (lastBriefingDate != todayStr) {
                    val summary = updatedState.smartSummary.ifBlank {
                        "Today: ${newTemp.toInt()}° · ${newCode.getWeatherCodeDescription()}"
                    }
                    NotificationHelper.notifyDailyBriefing(applicationContext, cityName, summary)
                    prefs.edit().putString("last_briefing_date", todayStr).apply()
                }
            }

            // 4. High UV alert (once per day when UV ≥ 8)
            val notifyUv = prefs.getBoolean("notif_uv", false)
            if (notifyUv && newUv >= 8f) {
                val lastUvDate = prefs.getString("last_uv_notif_date", null)
                if (lastUvDate != todayStr && hour in 9..11) {
                    NotificationHelper.notifyHighUv(applicationContext, newUv)
                    prefs.edit().putString("last_uv_notif_date", todayStr).apply()
                }
            }

            Result.success()
        } catch (e: Exception) {
            AppLogger.error("WeatherRefreshWorker failed", TAG, e)
            Result.retry()
        }
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
