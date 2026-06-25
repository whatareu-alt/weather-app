package com.example.aiweathermonitor.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── SOFT DAYLIGHT ─────────────────────────────────────────────────────────────
// A calm, soft, minimal personal-weather palette.
//   · warm off-white canvas, pure-white cards
//   · one soft cornflower-blue accent
//   · muted slate-gray for secondary text
//   · surfaceTint matched to surface so cards stay clean white (shadow gives depth)

private val LightScheme = lightColorScheme(
    primary              = Color(0xFF5B7FE0),   // soft cornflower blue — the one accent
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFDEE7FF),   // pale blue tint
    onPrimaryContainer   = Color(0xFF0E2A66),
    secondary            = Color(0xFF6E7390),   // muted slate
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFE6E8F2),
    onSecondaryContainer = Color(0xFF2A2E45),
    tertiary             = Color(0xFF4FA899),   // soft teal — calm/clear accent
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFD3F0E9),
    onTertiaryContainer  = Color(0xFF0A2F29),
    background           = Color(0xFFF5F6FA),   // soft off-white canvas
    onBackground         = Color(0xFF1A1B22),   // soft near-black ink
    surface              = Color(0xFFFFFFFF),   // pure white — cards
    onSurface            = Color(0xFF1A1B22),
    surfaceVariant       = Color(0xFFECEEF5),   // soft gray — chips / tracks
    onSurfaceVariant     = Color(0xFF6E7390),   // muted secondary text
    surfaceTint          = Color(0xFFFFFFFF),   // keep elevated cards pure white
    outline              = Color(0xFFD7DAE5),   // whisper-soft separator
    outlineVariant       = Color(0xFFE8EAF1),
    error                = Color(0xFFDE5A57),   // soft red
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFE5E3),
    onErrorContainer     = Color(0xFF5A1512),
    inverseSurface       = Color(0xFF2E2F38),
    inverseOnSurface     = Color(0xFFF2F3F8),
    inversePrimary       = Color(0xFFAFC3FF)
)

private val DarkScheme = darkColorScheme(
    primary              = Color(0xFFD97757),   // Claude coral / clay — the one accent
    onPrimary            = Color(0xFF3A1505),
    primaryContainer     = Color(0xFF5C2E1A),
    onPrimaryContainer   = Color(0xFFFFDBCB),
    secondary            = Color(0xFFB7B2A7),   // warm taupe-gray
    onSecondary          = Color(0xFF2C2823),
    secondaryContainer   = Color(0xFF3B3731),
    onSecondaryContainer = Color(0xFFEBE7DD),
    tertiary             = Color(0xFFD9A77C),   // warm sand
    onTertiary           = Color(0xFF3C2410),
    tertiaryContainer    = Color(0xFF553820),
    onTertiaryContainer  = Color(0xFFFBE0C6),
    background           = Color(0xFF1A1916),   // warm near-black canvas
    onBackground         = Color(0xFFF0EEE6),   // cream ink
    surface              = Color(0xFF242320),   // lifted warm card
    onSurface            = Color(0xFFF0EEE6),
    surfaceVariant       = Color(0xFF302E2A),   // chips / tracks
    onSurfaceVariant     = Color(0xFFB7B2A7),   // muted warm gray
    surfaceTint          = Color(0xFF242320),   // keep cards warm, not blue-tinted
    outline              = Color(0xFF4A4740),   // soft separator
    outlineVariant       = Color(0xFF35332E),
    error                = Color(0xFFFFB4AB),
    onError              = Color(0xFF690005),
    errorContainer       = Color(0xFF93000A),
    onErrorContainer     = Color(0xFFFFDAD6),
    inverseSurface       = Color(0xFFF0EEE6),
    inverseOnSurface     = Color(0xFF2E2D2A),
    inversePrimary       = Color(0xFFC15F3C)
)

@Composable
fun AIWeatherMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
