package com.example.aiweathermonitor.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PollenResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    val hourly: PollenHourly? = null
)

@Serializable
data class PollenHourly(
    val time: List<String>? = null,
    @SerialName("alder_pollen")    val alderPollen:    List<Float?>? = null,
    @SerialName("birch_pollen")    val birchPollen:    List<Float?>? = null,
    @SerialName("grass_pollen")    val grassPollen:    List<Float?>? = null,
    @SerialName("mugwort_pollen")  val mugwortPollen:  List<Float?>? = null,
    @SerialName("olive_pollen")    val olivePollen:    List<Float?>? = null,
    @SerialName("ragweed_pollen")  val ragweedPollen:  List<Float?>? = null
)

// ── Domain model ──────────────────────────────────────────────────────────────

data class PollenData(
    val alder: Float = 0f,
    val birch: Float = 0f,
    val grass: Float = 0f,
    val mugwort: Float = 0f,
    val olive: Float = 0f,
    val ragweed: Float = 0f
) {
    /** Highest single pollen count across all types */
    val peak: Float get() = maxOf(alder, birch, grass, mugwort, olive, ragweed)

    /** Human-readable risk level based on peak count (grains/m³) */
    val riskLevel: String get() = when {
        peak < 10f   -> "Low"
        peak < 30f   -> "Moderate"
        peak < 80f   -> "High"
        else         -> "Very High"
    }

    /** List of types with meaningful concentration (>5 grains/m³) */
    val activeTypes: List<String> get() = buildList {
        if (grass   > 5f) add("Grass")
        if (birch   > 5f) add("Birch")
        if (ragweed > 5f) add("Ragweed")
        if (alder   > 5f) add("Alder")
        if (mugwort > 5f) add("Mugwort")
        if (olive   > 5f) add("Olive")
    }
}
