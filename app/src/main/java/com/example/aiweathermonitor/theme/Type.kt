package com.example.aiweathermonitor.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── SOFT DAYLIGHT TYPE SCALE ──────────────────────────────────────────────────
// Light, airy display weights for the hero; calm, legible body. System sans so it
// feels native and soft. Roles not listed here fall back to the M3 defaults.

private val Sans = FontFamily.SansSerif

val Typography = Typography(
    // Hero temperature — large but soft (W200, not fragile ultra-thin)
    displayLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.W200,
        fontSize = 82.sp, lineHeight = 86.sp, letterSpacing = (-2).sp
    ),
    // City name
    displaySmall = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.W400,
        fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp
    ),
    // Condition line / prominent secondary text
    titleMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.W400,
        fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = 0.sp
    ),
    // Metric values
    titleLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.W500,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = 0.sp
    ),
    // Card section eyebrow (UPPERCASE)
    titleSmall = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.W700,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.sp
    ),
    // Body
    bodyLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.W400,
        fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.2.sp
    ),
    // Body (secondary)
    bodyMedium = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.W400,
        fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp
    ),
    // Buttons / chips
    labelLarge = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.W500,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.4.sp
    ),
    // Captions / fine print
    labelSmall = TextStyle(
        fontFamily = Sans, fontWeight = FontWeight.W500,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.4.sp
    )
)
