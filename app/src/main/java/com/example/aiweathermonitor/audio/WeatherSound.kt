package com.example.aiweathermonitor.audio

import androidx.annotation.RawRes
import com.example.aiweathermonitor.R

/**
 * A single ambient audio layer: a looping raw resource played at [volume] (0f..1f).
 * Multiple layers are mixed together at runtime to build a soundscape.
 */
data class SoundLayer(@RawRes val res: Int, val volume: Float)

/**
 * Maps a WMO weather code to a set of layered ambient sounds.
 *
 * This mirrors [com.example.aiweathermonitor.ui.getWeatherLottieRes] so the audio
 * always matches the animated scene on screen. All clips are procedurally
 * generated, royalty-free loops bundled in res/raw.
 */
fun getWeatherSoundscape(code: Int): List<SoundLayer> = when (code) {
    // 0 — Clear sky: birdsong over a faint breeze (the "sunny" ambience)
    0 -> listOf(
        SoundLayer(R.raw.amb_birds, 0.70f),
        SoundLayer(R.raw.amb_wind, 0.18f)
    )

    // 1,2,3 — Partly cloudy / cloudy: gentle wind layered with birds
    1, 2, 3 -> listOf(
        SoundLayer(R.raw.amb_wind, 0.55f),
        SoundLayer(R.raw.amb_birds, 0.45f)
    )

    // 45,48 — Fog / rime fog: soft, low wind only
    45, 48 -> listOf(
        SoundLayer(R.raw.amb_wind, 0.40f)
    )

    // 51..82 — Drizzle / rain / rain showers: rain bed with a faint breeze
    51, 53, 55, 61, 63, 65, 80, 81, 82 -> listOf(
        SoundLayer(R.raw.amb_rain, 0.95f),
        SoundLayer(R.raw.amb_wind, 0.25f)
    )

    // 71,73,75 — Snow: hushed, gentle wind
    71, 73, 75 -> listOf(
        SoundLayer(R.raw.amb_wind, 0.45f)
    )

    // 95,96,99 — Thunderstorm: rain + rolling thunder + wind
    95, 96, 99 -> listOf(
        SoundLayer(R.raw.amb_rain, 0.80f),
        SoundLayer(R.raw.amb_thunder, 1.0f),
        SoundLayer(R.raw.amb_wind, 0.40f)
    )

    // Fallback (varied / unknown): wind + birds
    else -> listOf(
        SoundLayer(R.raw.amb_wind, 0.50f),
        SoundLayer(R.raw.amb_birds, 0.40f)
    )
}
