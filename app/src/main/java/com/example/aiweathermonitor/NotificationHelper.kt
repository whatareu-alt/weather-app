package com.example.aiweathermonitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    // ── Channel IDs ───────────────────────────────────────────────────────────
    const val CHANNEL_SEVERE    = "severe_weather"
    const val CHANNEL_RAIN      = "rain_alert"
    const val CHANNEL_DAILY     = "daily_briefing"
    const val CHANNEL_UV        = "uv_alert"

    // ── Notification IDs ──────────────────────────────────────────────────────
    private const val NOTIF_SEVERE  = 1001
    private const val NOTIF_RAIN    = 1002
    private const val NOTIF_DAILY   = 1003
    private const val NOTIF_UV      = 1004

    // ── Channel setup (call once from Application) ────────────────────────────
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_SEVERE, "Severe weather alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Storm warnings, heat advisories, and flood alerts"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_RAIN, "Rain alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifications when rain is expected soon"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_DAILY, "Daily briefing", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Morning weather summary"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_UV, "UV alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Alerts when UV index is very high or extreme"
            }
        )
    }

    // ── Builder helper ────────────────────────────────────────────────────────
    private fun baseBuilder(context: Context, channelId: String): NotificationCompat.Builder {
        val tapIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val tapPi = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setContentIntent(tapPi)
    }

    // ── 1. Severe weather ─────────────────────────────────────────────────────
    fun notifySevere(context: Context, event: String, headline: String) {
        if (!hasPermission(context)) return
        val n = baseBuilder(context, CHANNEL_SEVERE)
            .setContentTitle("⚠ $event")
            .setContentText(headline)
            .setStyle(NotificationCompat.BigTextStyle().bigText(headline))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_SEVERE, n)
    }

    // ── 2. Rain in ~30 min ───────────────────────────────────────────────────
    fun notifyRainSoon(context: Context, cityName: String, precipChance: Int, hour: String) {
        if (!hasPermission(context)) return
        val n = baseBuilder(context, CHANNEL_RAIN)
            .setContentTitle("Rain expected in $cityName")
            .setContentText("$precipChance% chance of rain around $hour — carry an umbrella")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_RAIN, n)
    }

    // ── 3. Daily briefing ────────────────────────────────────────────────────
    fun notifyDailyBriefing(context: Context, cityName: String, summary: String) {
        if (!hasPermission(context)) return
        val n = baseBuilder(context, CHANNEL_DAILY)
            .setContentTitle("Good morning · $cityName")
            .setContentText(summary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(summary))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_DAILY, n)
    }

    // ── 4. High UV ───────────────────────────────────────────────────────────
    fun notifyHighUv(context: Context, uvIndex: Float) {
        if (!hasPermission(context)) return
        val label = uvIndex.getUvDescription()
        val n = baseBuilder(context, CHANNEL_UV)
            .setContentTitle("High UV index: ${uvIndex.toInt()} ($label)")
            .setContentText("Apply SPF 30+ sunscreen and seek shade between 10 AM – 4 PM")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIF_UV, n)
    }

    // ── Permission guard (Android 13+) ────────────────────────────────────────
    private fun hasPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
