package com.example.aiweathermonitor.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.FileProvider
import com.example.aiweathermonitor.WeatherState
import com.example.aiweathermonitor.getAqiDescription
import com.example.aiweathermonitor.getUvDescription
import com.example.aiweathermonitor.getWeatherCodeDescription
import java.io.File

object ShareWeatherCard {

    /**
     * Draws a 1080×600 weather card bitmap and shares it via the system share sheet.
     */
    fun share(context: Context, state: WeatherState) {
        val bmp = drawCard(state)
        val file = saveBitmap(context, bmp)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "Weather in ${state.selectedCity}: ${state.temperature.toInt()}° · ${state.weatherCode.getWeatherCodeDescription()}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share weather"))
    }

    private fun drawCard(state: WeatherState): Bitmap {
        val w = 1080; val h = 600
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF1A2744.toInt() }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), 48f, 48f, bgPaint)

        // Accent circle (decorative)
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x220EA5E9.toInt() }
        canvas.drawCircle(w * 0.78f, h * 0.25f, 260f, accentPaint)

        val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
        val muted = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF90B0C8.toInt() }
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF60A5FA.toInt() }
        val uvColor = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFCD34D.toInt() }

        // City
        white.textSize = 52f; white.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(state.selectedCity, 60f, 100f, white)

        // Condition
        muted.textSize = 34f; muted.typeface = Typeface.DEFAULT
        canvas.drawText(state.weatherCode.getWeatherCodeDescription(), 60f, 150f, muted)

        // Big temperature
        white.textSize = 200f; white.typeface = Typeface.DEFAULT_BOLD
        val tempStr = if (state.isCelsius) "${state.temperature.toInt()}°C" else "${((state.temperature * 9 / 5) + 32).toInt()}°F"
        canvas.drawText(tempStr, 60f, 380f, white)

        // H/L line
        val today = state.dailyForecast.firstOrNull()
        if (today != null) {
            val hi = if (state.isCelsius) "${today.tempMax.toInt()}°" else "${((today.tempMax * 9 / 5) + 32).toInt()}°"
            val lo = if (state.isCelsius) "${today.tempMin.toInt()}°" else "${((today.tempMin * 9 / 5) + 32).toInt()}°"
            muted.textSize = 36f
            canvas.drawText("H: $hi  ·  L: $lo", 60f, 440f, muted)
        }

        // Divider
        val divPaint = Paint().apply { color = 0x40FFFFFF.toInt(); strokeWidth = 1.5f }
        canvas.drawLine(60f, 480f, w - 60f, 480f, divPaint)

        // Bottom row: feels like, UV, AQI, Wind
        val metrics = buildList {
            add("Feels like" to "${state.feelsLike.toInt()}°")
            if (state.uvIndex > 0f) add("UV" to "${state.uvIndex.toInt()} · ${state.uvIndex.getUvDescription()}")
            if (state.aqi > 0) add("AQI" to "${state.aqi} · ${state.aqi.getAqiDescription()}")
            add("Wind" to "${state.windSpeed.toInt()} km/h ${state.windDir}".trim())
        }.take(4)

        val colW = (w - 120f) / metrics.size
        metrics.forEachIndexed { i, (label, value) ->
            val x = 60f + i * colW
            muted.textSize = 24f
            canvas.drawText(label, x, 530f, muted)
            white.textSize = 30f; white.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText(value, x, 568f, white)
        }

        // Source badge
        accent.textSize = 22f
        canvas.drawText("Open-Meteo · WeatherAPI.com", 60f, h - 20f, accent)

        return bmp
    }

    private fun saveBitmap(context: Context, bmp: Bitmap): File {
        val dir = File(context.cacheDir, "weather_shares").apply { mkdirs() }
        val file = File(dir, "weather_card.png")
        file.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        return file
    }
}
