package com.example.aiweathermonitor.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * WeatherAPI.com response models
 * Endpoint: https://api.weatherapi.com/v1/forecast.json
 * Docs: https://www.weatherapi.com/docs/
 */

@Serializable
data class WeatherApiCondition(
    val text: String? = null,
    val icon: String? = null,
    val code: Int? = null
)

@Serializable
data class WeatherApiCurrent(
    @SerialName("temp_c") val tempC: Float? = null,
    @SerialName("temp_f") val tempF: Float? = null,
    @SerialName("feelslike_c") val feelsLikeC: Float? = null,
    @SerialName("is_day") val isDay: Int? = null,
    val humidity: Int? = null,
    @SerialName("wind_kph") val windKph: Float? = null,
    @SerialName("wind_mph") val windMph: Float? = null,
    @SerialName("wind_dir") val windDir: String? = null,
    @SerialName("pressure_mb") val pressureMb: Float? = null,
    @SerialName("vis_km") val visKm: Float? = null,
    val uv: Float? = null,
    @SerialName("air_quality") val airQuality: WeatherApiAirQuality? = null,
    val condition: WeatherApiCondition? = null
)

@Serializable
data class WeatherApiAirQuality(
    @SerialName("pm2_5") val pm25: Float? = null,
    @SerialName("pm10") val pm10: Float? = null,
    @SerialName("us-epa-index") val usEpaIndex: Int? = null,
    @SerialName("gb-defra-index") val gbDefraIndex: Int? = null
)

@Serializable
data class WeatherApiAstro(
    val sunrise: String? = null,
    val sunset: String? = null,
    @SerialName("moon_phase") val moonPhase: String? = null
)

@Serializable
data class WeatherApiDayDetail(
    @SerialName("maxtemp_c") val maxTempC: Float? = null,
    @SerialName("mintemp_c") val minTempC: Float? = null,
    @SerialName("maxtemp_f") val maxTempF: Float? = null,
    @SerialName("mintemp_f") val minTempF: Float? = null,
    @SerialName("avghumidity") val avgHumidity: Float? = null,
    @SerialName("daily_chance_of_rain") val chanceOfRain: Int? = null,
    val uv: Float? = null,
    val condition: WeatherApiCondition? = null
)

@Serializable
data class WeatherApiHour(
    val time: String? = null,
    @SerialName("temp_c") val tempC: Float? = null,
    @SerialName("temp_f") val tempF: Float? = null,
    val humidity: Int? = null,
    @SerialName("chance_of_rain") val chanceOfRain: Int? = null,
    val condition: WeatherApiCondition? = null,
    @SerialName("is_day") val isDay: Int? = null
)

@Serializable
data class WeatherApiForecastDay(
    val date: String? = null,
    val day: WeatherApiDayDetail? = null,
    val astro: WeatherApiAstro? = null,
    val hour: List<WeatherApiHour>? = null
)

@Serializable
data class WeatherApiForecast(
    val forecastday: List<WeatherApiForecastDay>? = null
)

@Serializable
data class WeatherApiLocation(
    val name: String? = null,
    val region: String? = null,
    val country: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val localtime: String? = null,
    @SerialName("tz_id") val tzId: String? = null
)

@Serializable
data class WeatherApiAlert(
    val headline: String? = null,
    val msgtype: String? = null,
    val severity: String? = null,
    val urgency: String? = null,
    val event: String? = null,
    val desc: String? = null,
    val instruction: String? = null
)

@Serializable
data class WeatherApiAlerts(
    val alert: List<WeatherApiAlert>? = null
)

@Serializable
data class WeatherApiForecastResponse(
    val location: WeatherApiLocation? = null,
    val current: WeatherApiCurrent? = null,
    val forecast: WeatherApiForecast? = null,
    val alerts: WeatherApiAlerts? = null
)

/**
 * Maps a WeatherAPI.com condition code to a WMO weather code
 * used throughout the app for icon/description display.
 * Reference: https://www.weatherapi.com/docs/weather_conditions.json
 */
fun mapWeatherApiCodeToWmo(code: Int?): Int = when (code) {
    1000 -> 0   // Sunny / Clear
    1003 -> 2   // Partly cloudy
    1006 -> 3   // Cloudy
    1009 -> 3   // Overcast
    1030 -> 45  // Mist
    1063, 1180, 1183 -> 61   // Patchy / light rain
    1066, 1210, 1213 -> 71   // Patchy / light snow
    1069, 1204, 1207 -> 73   // Sleet
    1072, 1168, 1171 -> 51   // Drizzle / freezing drizzle
    1087 -> 95  // Thundery outbreaks
    1114, 1117 -> 75  // Blowing / blizzard
    1135, 1147 -> 45  // Fog
    1150, 1153 -> 51  // Light drizzle
    1189, 1192 -> 63  // Moderate / heavy rain
    1195 -> 65  // Heavy rain
    1198, 1201 -> 61  // Light / heavy freezing rain
    1216, 1219 -> 73  // Moderate / heavy snow
    1222, 1225 -> 75  // Heavy snow
    1237 -> 75  // Ice pellets
    1240 -> 80  // Light rain shower
    1243, 1246 -> 82  // Moderate / heavy rain shower
    1249, 1252 -> 81  // Sleet showers
    1255, 1258 -> 81  // Snow showers
    1261, 1264 -> 82  // Ice pellet showers
    1273, 1276 -> 95  // Thunderstorm with rain
    1279, 1282 -> 95  // Thunderstorm with snow
    else -> 0
}

// ── Search endpoint models ───────────────────────────────────────────────────

@Serializable
data class WeatherApiSearchResult(
    val id: Int? = null,
    val name: String? = null,
    val region: String? = null,
    val country: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val url: String? = null
)
