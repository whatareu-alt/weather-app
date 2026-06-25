package com.example.aiweathermonitor.util

import com.example.aiweathermonitor.ui.PressureUnit
import com.example.aiweathermonitor.ui.VisibilityUnit
import com.example.aiweathermonitor.ui.WindUnit
import kotlin.math.roundToInt

/**
 * Shared unit-formatting helpers used by both the main dashboard UI and the
 * home-screen widget. Centralising these keeps conversion/rounding rules in one
 * place so the app and the widget can never drift apart.
 */

/** Full temperature with unit suffix, e.g. "21°C" / "70°F". */
fun formatTemp(tempC: Float, isCelsius: Boolean): String =
    if (isCelsius) "${tempC.roundToInt()}°C"
    else "${(tempC * 9f / 5f + 32f).roundToInt()}°F"

/** Compact temperature without unit suffix, e.g. "21°" — for space-constrained surfaces (widget). */
fun formatTempShort(tempC: Float, isCelsius: Boolean): String =
    if (isCelsius) "${tempC.roundToInt()}°"
    else "${(tempC * 9f / 5f + 32f).roundToInt()}°"

fun formatWind(speedKmh: Float, unit: WindUnit): String = when (unit) {
    WindUnit.KMH -> "${speedKmh.roundToInt()} km/h"
    WindUnit.MPH -> "${(speedKmh / 1.60934f).roundToInt()} mph"
    WindUnit.MS  -> "${(speedKmh / 3.6f).roundToInt()} m/s"
}

fun formatPressure(hpa: Float, unit: PressureUnit): String = when (unit) {
    PressureUnit.HPA  -> "${hpa.roundToInt()} hPa"
    PressureUnit.INHG -> "${"%.2f".format(hpa * 0.02953f)} inHg"
}

fun formatVisibility(km: Float, unit: VisibilityUnit): String = when (unit) {
    VisibilityUnit.KM -> "${"%.1f".format(km)} km"
    VisibilityUnit.MI -> "${"%.1f".format(km * 0.621371f)} mi"
}
