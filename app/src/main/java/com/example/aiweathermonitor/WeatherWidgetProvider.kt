package com.example.aiweathermonitor

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import com.example.aiweathermonitor.config.WeatherApiConfig
import com.example.aiweathermonitor.util.formatTempShort
import kotlinx.serialization.json.Json

class WeatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val state = loadState(context)
        for (id in appWidgetIds) {
            updateWidget(context, appWidgetManager, id, state)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidget(context, appWidgetManager, appWidgetId, loadState(context))
    }

    private fun loadState(context: Context): WeatherState? {
        val prefs = context.getSharedPreferences(WeatherApiConfig.SharedPrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(WeatherApiConfig.SharedPrefsKeys.WEATHER_STATE_KEY, null) ?: return null
        return try {
            Json { ignoreUnknownKeys = true }.decodeFromString(WeatherState.serializer(), json)
        } catch (e: Exception) {
            null
        }
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        state: WeatherState?
    ) {
        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
        val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        val use4x2 = minWidth >= 250  // ~4 cells wide

        val views = if (use4x2) build4x2Views(context, state) else build2x2Views(context, state)

        // Tap → open app
        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val containerId = if (use4x2) R.id.widget_container_4x2 else R.id.widget_container
        views.setOnClickPendingIntent(containerId, tapIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    // ── 2×2 compact layout ────────────────────────────────────────────────────
    private fun build2x2Views(context: Context, state: WeatherState?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.weather_widget_layout)
        if (state == null) {
            views.setTextViewText(R.id.widget_temp, "--°")
            views.setTextViewText(R.id.widget_city, "Friday")
            views.setTextViewText(R.id.widget_description, "Open app to load weather")
            views.setTextViewText(R.id.widget_online_status, "")
            return views
        }
        val tempStr = formatTempShort(state.temperature, state.isCelsius)
        views.setTextViewText(R.id.widget_temp, tempStr)
        views.setTextViewText(R.id.widget_city, state.selectedCity)
        views.setTextViewText(R.id.widget_description, state.weatherCode.getWeatherCodeDescription())
        views.setTextViewText(R.id.widget_online_status, if (state.isOnline) "" else "Offline")
        return views
    }

    // ── 4×2 wide layout ───────────────────────────────────────────────────────
    private fun build4x2Views(context: Context, state: WeatherState?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.weather_widget_4x2_layout)
        if (state == null) {
            views.setTextViewText(R.id.widget_temp_4x2, "--°")
            views.setTextViewText(R.id.widget_city_4x2, "Friday")
            views.setTextViewText(R.id.widget_desc_4x2, "Open app to load weather")
            views.setTextViewText(R.id.widget_hl_4x2, "")
            views.setTextViewText(R.id.widget_uv_4x2, "")
            return views
        }

        // Header
        views.setTextViewText(R.id.widget_temp_4x2, formatTempShort(state.temperature, state.isCelsius))
        views.setTextViewText(R.id.widget_city_4x2, state.selectedCity)
        views.setTextViewText(R.id.widget_desc_4x2, state.weatherCode.getWeatherCodeDescription())

        val today = state.dailyForecast.firstOrNull()
        if (today != null) {
            val hiStr = formatTempShort(today.tempMax, state.isCelsius)
            val loStr = formatTempShort(today.tempMin, state.isCelsius)
            views.setTextViewText(R.id.widget_hl_4x2, "H: $hiStr · L: $loStr")
        } else {
            views.setTextViewText(R.id.widget_hl_4x2, "")
        }

        val uvLabel = if (state.uvIndex > 0f) "UV ${state.uvIndex.toInt()} · ${state.uvIndex.getUvDescription()}" else ""
        views.setTextViewText(R.id.widget_uv_4x2, uvLabel)

        // Hourly strip — up to 5 slots
        val hourlySlots = listOf(
            Triple(R.id.widget_hour_0_time, R.id.widget_hour_0_temp, R.id.widget_hour_0_precip) to R.id.widget_hour_0_icon,
            Triple(R.id.widget_hour_1_time, R.id.widget_hour_1_temp, R.id.widget_hour_1_precip) to R.id.widget_hour_1_icon,
            Triple(R.id.widget_hour_2_time, R.id.widget_hour_2_temp, R.id.widget_hour_2_precip) to R.id.widget_hour_2_icon,
            Triple(R.id.widget_hour_3_time, R.id.widget_hour_3_temp, R.id.widget_hour_3_precip) to R.id.widget_hour_3_icon,
            Triple(R.id.widget_hour_4_time, R.id.widget_hour_4_temp, R.id.widget_hour_4_precip) to R.id.widget_hour_4_icon
        )

        hourlySlots.forEachIndexed { i, (ids, iconId) ->
            val hour = state.hourlyForecast.getOrNull(i)
            if (hour != null) {
                views.setTextViewText(ids.first, if (i == 0) "Now" else hour.hour)
                views.setTextViewText(ids.second, formatTempShort(hour.temperature, state.isCelsius))
                views.setTextViewText(ids.third, if (hour.precipChance > 0) "${hour.precipChance}%" else "")
                views.setTextViewText(iconId, weatherCodeToEmoji(hour.weatherCode))
            } else {
                views.setTextViewText(ids.first, "")
                views.setTextViewText(ids.second, "")
                views.setTextViewText(ids.third, "")
                views.setTextViewText(iconId, "")
            }
        }

        return views
    }

    private fun weatherCodeToEmoji(code: Int): String = when (code) {
        0 -> "☀"
        1, 2 -> "⛅"
        3 -> "☁"
        45, 48 -> "🌫"
        51, 53, 55, 56, 57 -> "🌦"
        61, 63, 65, 66, 67 -> "🌧"
        71, 73, 75, 77 -> "❄"
        80, 81, 82 -> "🌦"
        85, 86 -> "🌨"
        95 -> "⛈"
        96, 99 -> "⛈"
        else -> "🌡"
    }
}
