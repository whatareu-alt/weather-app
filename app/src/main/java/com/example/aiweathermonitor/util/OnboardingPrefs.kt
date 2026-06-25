package com.example.aiweathermonitor.util

import android.content.Context
import com.example.aiweathermonitor.config.WeatherApiConfig

/**
 * Small wrapper around the onboarding-completed flag in SharedPreferences so the
 * key and prefs name live in exactly one place instead of being re-typed at every
 * navigation call site.
 */
object OnboardingPrefs {
    private const val KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding"

    private fun prefs(context: Context) = context.getSharedPreferences(
        WeatherApiConfig.SharedPrefsKeys.PREFS_NAME, Context.MODE_PRIVATE
    )

    fun hasSeenOnboarding(context: Context): Boolean =
        prefs(context).getBoolean(KEY_HAS_SEEN_ONBOARDING, false)

    fun markOnboardingSeen(context: Context) {
        prefs(context).edit().putBoolean(KEY_HAS_SEEN_ONBOARDING, true).apply()
    }
}
