package com.example.aiweathermonitor.data.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Open-Meteo Weather API response models
 * Separated for better organization and maintainability
 */

@Serializable
data class OpenMeteoCurrentWeather(
    val temperature: Float? = null,
    @SerialName("windspeed")
    val windSpeed: Float? = null,
    @SerialName("weathercode")
    val weatherCode: Int? = null,
    @SerialName("is_day")
    val isDay: Int? = null,
    val time: String? = null
)

@Serializable
data class OpenMeteoHourly(
    val time: List<String>? = null,
    @SerialName("temperature_2m")
    val temperature2m: List<Float>? = null,
    @SerialName("weather_code")
    val weatherCode: List<Int>? = null,
    @SerialName("relative_humidity_2m")
    val relativeHumidity2m: List<Int>? = null,
    @SerialName("apparent_temperature")
    val apparentTemperature: List<Float>? = null,
    @SerialName("weather_description")
    val weatherDescription: List<String>? = null,
    @SerialName("precipitation_probability")
    val precipitationProbability: List<Int>? = null,
    val visibility: List<Float>? = null
)

@Serializable
data class OpenMeteoDaily(
    val time: List<String>? = null,
    @SerialName("weather_code")
    val weatherCode: List<Int>? = null,
    @SerialName("temperature_2m_max")
    val temperature2mMax: List<Float>? = null,
    @SerialName("temperature_2m_min")
    val temperature2mMin: List<Float>? = null,
    val sunrise: List<String>? = null,
    val sunset: List<String>? = null,
    @SerialName("weather_description")
    val weatherDescription: List<String>? = null,
    @SerialName("uv_index_max")
    val uvIndexMax: List<Float>? = null,
    @SerialName("precipitation_probability_max")
    val precipProbabilityMax: List<Int>? = null
)

@Serializable
data class OpenMeteoCurrent(
    val time: String? = null,
    val interval: Int? = null,
    @SerialName("temperature_2m")
    val temperature2m: Float? = null,
    @SerialName("relative_humidity_2m")
    val relativeHumidity2m: Float? = null,
    @SerialName("apparent_temperature")
    val apparentTemperature: Float? = null,
    @SerialName("surface_pressure")
    val surfacePressure: Float? = null,
    @SerialName("wind_speed_10m")
    val windSpeed10m: Float? = null,
    @SerialName("wind_direction_10m")
    val windDirection10m: Float? = null,
    @SerialName("weather_code")
    val weatherCode: Int? = null,
    @SerialName("uv_index")
    val uv_index: Float? = null
)

@Serializable
data class OpenMeteoForecastResponse(
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("generationtime_ms")
    val generationTimeMs: Double? = null,
    @SerialName("utc_offset_seconds")
    val utcOffsetSeconds: Int? = null,
    val timezone: String? = null,
    @SerialName("timezone_abbreviation")
    val timezoneAbbreviation: String? = null,
    val elevation: Float? = null,
    @SerialName("current_weather")
    val currentWeather: OpenMeteoCurrentWeather? = null,
    val current: OpenMeteoCurrent? = null,
    val hourly: OpenMeteoHourly? = null,
    val daily: OpenMeteoDaily? = null
)

@Serializable
data class OpenMeteoGeocodingResult(
    val id: Int? = null,
    val name: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val elevation: Float? = null,
    @SerialName("feature_code")
    val featureCode: String? = null,
    @SerialName("country_code")
    val countryCode: String? = null,
    val country: String? = null,
    val admin1: String? = null,
    val admin2: String? = null,
    val timezone: String? = null,
    val population: Int? = null,
    val postcodes: List<String>? = null
)

@Serializable
data class OpenMeteoGeocodingResponse(
    val results: List<OpenMeteoGeocodingResult>? = null,
    @SerialName("generationtime_ms")
    val generationTimeMs: Double? = null
)

/**
 * The rest of the codebase refers to the forecast payload simply as
 * [OpenMeteoResponse]; keep that name as an alias of the concrete model.
 */
typealias OpenMeteoResponse = OpenMeteoForecastResponse
