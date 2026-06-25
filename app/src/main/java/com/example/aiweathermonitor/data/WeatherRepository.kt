package com.example.aiweathermonitor.data

import com.example.aiweathermonitor.DayForecast
import com.example.aiweathermonitor.GeocodingResult
import com.example.aiweathermonitor.HourForecast
import com.example.aiweathermonitor.WeatherState
import com.example.aiweathermonitor.createGeocodingResult
import com.example.aiweathermonitor.generateSmartSummary
import com.example.aiweathermonitor.config.WeatherApiConfig
import com.example.aiweathermonitor.data.models.OpenMeteoGeocodingResponse
import com.example.aiweathermonitor.data.models.OpenMeteoResponse
import com.example.aiweathermonitor.data.models.PollenData
import com.example.aiweathermonitor.data.models.PollenResponse
import com.example.aiweathermonitor.data.models.WeatherApiForecastResponse
import com.example.aiweathermonitor.data.models.WeatherApiSearchResult
import com.example.aiweathermonitor.data.models.mapWeatherApiCodeToWmo
import com.example.aiweathermonitor.util.AppLogger
import com.example.aiweathermonitor.util.UrlBuilder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "WeatherRepository"
private const val TIME_DEFAULT_SUNRISE = "6:00 AM"
private const val TIME_DEFAULT_SUNSET = "6:00 PM"

/**
 * Data-access layer for weather. Owns all networking, JSON parsing, and the
 * WeatherAPI.com <-> Open-Meteo merge so the ViewModel can stay focused on UI
 * state. Pure and side-effect free (no notifications, no Android Context) which
 * makes every method unit-testable with a mock [OkHttpClient].
 */
interface WeatherRepository {

    /**
     * Searches for cities. Tries Open-Meteo geocoding first, then falls back to
     * WeatherAPI.com search when [apiKey] is present and Open-Meteo is empty.
     */
    suspend fun searchCities(query: String, apiKey: String): Result<List<GeocodingResult>>

    /**
     * Fetches current conditions + forecast for the given coordinates and merges
     * the result into [base], returning a fully populated [WeatherState]
     * (isLoading = false, smartSummary regenerated, pollen attached).
     */
    suspend fun getWeather(
        base: WeatherState,
        cityName: String,
        latitude: Float,
        longitude: Float,
        apiKey: String
    ): Result<WeatherState>
}

class DefaultWeatherRepository(
    private val client: OkHttpClient,
    private val jsonParser: Json = Json { ignoreUnknownKeys = true },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : WeatherRepository {

    // ── City search ──────────────────────────────────────────────────────────

    override suspend fun searchCities(
        query: String,
        apiKey: String
    ): Result<List<GeocodingResult>> = withContext(ioDispatcher) {
        try {
            // Primary: Open-Meteo geocoding
            val omResults = runCatching {
                val url = UrlBuilder.openMeteoSearch(query, WeatherApiConfig.SEARCH_RESULT_LIMIT)
                val body = client.newCall(Request.Builder().url(url).build()).execute()
                    .use { r -> if (!r.isSuccessful) null else r.body?.string() }
                if (!body.isNullOrBlank()) {
                    jsonParser.decodeFromString<OpenMeteoGeocodingResponse>(body)
                        .results
                        ?.map {
                            createGeocodingResult(
                                name = it.name,
                                latitude = it.latitude,
                                longitude = it.longitude,
                                country = it.country,
                                admin1 = it.admin1
                            )
                        } ?: emptyList()
                } else emptyList()
            }.getOrElse { emptyList() }

            if (omResults.isNotEmpty()) {
                return@withContext Result.success(omResults)
            }

            // Fallback: WeatherAPI search (only if a key is configured)
            if (apiKey.isBlank()) return@withContext Result.success(emptyList())
            val waResults = runCatching {
                val url = UrlBuilder.weatherApiSearch(query, apiKey)
                val body = client.newCall(Request.Builder().url(url).build()).execute()
                    .use { r -> if (!r.isSuccessful) null else r.body?.string() }
                if (!body.isNullOrBlank()) {
                    jsonParser.decodeFromString<List<WeatherApiSearchResult>>(body)
                        .map {
                            createGeocodingResult(
                                name = it.name,
                                latitude = it.lat,
                                longitude = it.lon,
                                country = it.country,
                                admin1 = it.region
                            )
                        }
                } else emptyList()
            }.getOrElse { emptyList() }
            Result.success(waResults)
        } catch (e: Exception) {
            AppLogger.error("City search failed", TAG, e)
            Result.failure(e)
        }
    }

    // ── Weather fetch orchestration ───────────────────────────────────────────

    override suspend fun getWeather(
        base: WeatherState,
        cityName: String,
        latitude: Float,
        longitude: Float,
        apiKey: String
    ): Result<WeatherState> = withContext(ioDispatcher) {
        try {
            var state: WeatherState? = null

            // 1. WeatherAPI.com (richer current data) + Open-Meteo extended forecast
            if (apiKey.isNotBlank()) {
                try {
                    val waData = fetchWeatherApiData(apiKey, latitude, longitude)
                    val omData = runCatching { fetchOpenMeteoWeather(latitude, longitude) }
                        .onFailure { AppLogger.warning("Open-Meteo extended unavailable: ${it.message}", TAG) }
                        .getOrNull()
                    state = mapWeatherApiToState(base, waData, omData, cityName)
                } catch (e: Exception) {
                    AppLogger.warning("WeatherAPI.com failed, falling back to Open-Meteo: ${e.message}", TAG)
                }
            }

            // 2. Open-Meteo only fallback
            if (state == null) {
                val data = fetchOpenMeteoWeather(latitude, longitude)
                state = mapOpenMeteoToState(base, data, cityName)
            }

            // Regenerate smart summary + attach pollen after a successful fetch
            val pollen = runCatching { fetchPollenData(latitude, longitude) }.getOrNull()
            val finalState = state.copy(
                smartSummary = generateSmartSummary(state),
                lastRefreshedEpoch = System.currentTimeMillis(),
                pollenData = pollen
            )
            Result.success(finalState)
        } catch (e: Exception) {
            AppLogger.error("Failed to fetch weather", TAG, e)
            Result.failure(e)
        }
    }

    // ── Network fetchers ──────────────────────────────────────────────────────

    private suspend fun fetchWeatherApiData(
        apiKey: String,
        latitude: Float,
        longitude: Float
    ): WeatherApiForecastResponse = withContext(ioDispatcher) {
        val url = UrlBuilder.weatherApiForecast(latitude, longitude, apiKey)
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            when (response.code) {
                401, 403 -> throw Exception("Invalid WeatherAPI key. Check Settings.")
                else -> if (!response.isSuccessful) throw Exception("WeatherAPI.com error: ${response.code}")
            }
            val body = response.body?.string() ?: throw Exception("Empty WeatherAPI.com response")
            jsonParser.decodeFromString<WeatherApiForecastResponse>(body)
        }
    }

    private suspend fun fetchOpenMeteoWeather(
        latitude: Float,
        longitude: Float
    ): OpenMeteoResponse = withContext(ioDispatcher) {
        val url = UrlBuilder.openMeteoForecast(latitude, longitude, hourly = true, daily = true)
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Open-Meteo error: ${response.code}")
            val body = response.body?.string() ?: throw Exception("Empty Open-Meteo response")
            jsonParser.decodeFromString<OpenMeteoResponse>(body)
        }
    }

    private suspend fun fetchPollenData(latitude: Float, longitude: Float): PollenData? =
        withContext(ioDispatcher) {
            runCatching {
                val url = UrlBuilder.openMeteoPollen(latitude, longitude)
                client.newCall(Request.Builder().url(url).build()).execute().use { r ->
                    if (!r.isSuccessful) return@withContext null
                    val body = r.body?.string() ?: return@withContext null
                    val resp = jsonParser.decodeFromString<PollenResponse>(body)
                    val h = resp.hourly ?: return@withContext null
                    // Average the first 8 hours (daytime today)
                    fun avg(list: List<Float?>?): Float =
                        list?.take(8)?.filterNotNull()?.average()?.toFloat() ?: 0f
                    PollenData(
                        alder   = avg(h.alderPollen),
                        birch   = avg(h.birchPollen),
                        grass   = avg(h.grassPollen),
                        mugwort = avg(h.mugwortPollen),
                        olive   = avg(h.olivePollen),
                        ragweed = avg(h.ragweedPollen)
                    )
                }
            }.getOrNull()
        }

    // ── State mappers (side-effect free) ───────────────────────────────────────

    private fun mapWeatherApiToState(
        currentState: WeatherState,
        waData: WeatherApiForecastResponse,
        omData: OpenMeteoResponse?,
        cityName: String
    ): WeatherState {
        val current = waData.current
        val forecastDays = waData.forecast?.forecastday ?: emptyList()
        val todayDay = forecastDays.firstOrNull()

        val tempVal      = current?.tempC ?: 0f
        val feelsLikeVal = current?.feelsLikeC ?: tempVal
        val humidityVal  = current?.humidity?.toFloat() ?: 0f
        val pressureVal  = current?.pressureMb ?: 1013f
        val windSpeedVal = current?.windKph ?: 0f
        val windDirVal   = current?.windDir ?: ""
        val visibilityVal = current?.visKm ?: 10f
        val uvVal        = current?.uv ?: todayDay?.day?.uv ?: 0f
        val aqiVal       = current?.airQuality?.usEpaIndex ?: 0
        val wmoCode      = mapWeatherApiCodeToWmo(current?.condition?.code)

        val astro = todayDay?.astro
        val parsedSunrise = astro?.sunrise?.trim() ?: TIME_DEFAULT_SUNRISE
        val parsedSunset  = astro?.sunset?.trim()  ?: TIME_DEFAULT_SUNSET
        val sunriseMinutes = parseFormattedTimeToMins(parsedSunrise)
        val sunsetMinutes  = parseFormattedTimeToMins(parsedSunset)

        val allHours = forecastDays.flatMap { it.hour ?: emptyList() }
        val nowHourStr = SimpleDateFormat("yyyy-MM-dd HH", Locale.getDefault()).format(Date())
        var startIdx = allHours.indexOfFirst { h ->
            h.time?.substring(0, minOf(13, h.time.length)) == nowHourStr
        }
        if (startIdx == -1) startIdx = 0

        val hourlyList = mutableListOf<HourForecast>()
        for (i in startIdx until minOf(startIdx + WeatherApiConfig.FORECAST_HOURS_LIMIT, allHours.size)) {
            val h = allHours[i]
            hourlyList.add(
                HourForecast(
                    hour = if (i == startIdx) "Now" else parseHour(h.time ?: ""),
                    temperature = h.tempC ?: 0f,
                    weatherCode = mapWeatherApiCodeToWmo(h.condition?.code),
                    precipChance = h.chanceOfRain ?: 0
                )
            )
        }

        val dailyList = mutableListOf<DayForecast>()
        forecastDays.forEachIndexed { i, fd ->
            dailyList.add(
                DayForecast(
                    day = parseDay(fd.date ?: "", i),
                    tempMin = fd.day?.minTempC ?: 20f,
                    tempMax = fd.day?.maxTempC ?: 30f,
                    weatherCode = mapWeatherApiCodeToWmo(fd.day?.condition?.code),
                    uvIndex = fd.day?.uv ?: 0f,
                    precipChance = fd.day?.chanceOfRain ?: 0
                )
            )
        }

        if (omData != null) {
            val omDaily = omData.daily
            val omTimes = omDaily?.time ?: emptyList()
            val lastWaDate = forecastDays.lastOrNull()?.date ?: ""
            val startOmIdx = omTimes.indexOfFirst { it > lastWaDate }.takeIf { it >= 0 } ?: omTimes.size
            for (i in startOmIdx until omTimes.size) {
                val idx = dailyList.size
                dailyList.add(
                    DayForecast(
                        day = parseDay(omTimes[i], idx),
                        tempMin = omDaily?.temperature2mMin?.getOrElse(i) { 20f } ?: 20f,
                        tempMax = omDaily?.temperature2mMax?.getOrElse(i) { 30f } ?: 30f,
                        weatherCode = omDaily?.weatherCode?.getOrElse(i) { 0 } ?: 0,
                        uvIndex = omDaily?.uvIndexMax?.getOrElse(i) { 0f } ?: 0f,
                        precipChance = omDaily?.precipProbabilityMax?.getOrElse(i) { 0 } ?: 0
                    )
                )
            }
        }

        val firstAlert = waData.alerts?.alert?.firstOrNull()

        return currentState.copy(
            // Honour the city the user actually chose; WeatherAPI's location.name
            // can resolve to a nearby district (e.g. London → "Strand").
            selectedCity = cityName.ifBlank { waData.location?.name ?: "" },
            timezone = waData.location?.tzId ?: "",
            temperature = tempVal,
            feelsLike = feelsLikeVal,
            humidity = humidityVal,
            pressure = pressureVal,
            windSpeed = windSpeedVal,
            windDir = windDirVal,
            visibility = visibilityVal,
            weatherCode = wmoCode,
            aqi = aqiVal,
            uvIndex = uvVal,
            sunrise = parsedSunrise,
            sunset = parsedSunset,
            sunriseMinutes = sunriseMinutes,
            sunsetMinutes = sunsetMinutes,
            hourlyForecast = hourlyList,
            dailyForecast = dailyList,
            isLoading = false,
            errorMessage = null,
            alertEvent = firstAlert?.event,
            alertHeadline = firstAlert?.headline,
            alertDescription = firstAlert?.desc,
            lastRefreshedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        )
    }

    private fun mapOpenMeteoToState(
        currentState: WeatherState,
        data: OpenMeteoResponse,
        cityName: String
    ): WeatherState {
        val current = data.current ?: throw Exception("Current block missing in Open-Meteo response")

        val hourlyList = mutableListOf<HourForecast>()
        val hourlyData = data.hourly
        var currentVisibilityKm = 10f
        if (hourlyData != null) {
            val timeList = hourlyData.time ?: emptyList()
            if (timeList.isNotEmpty()) {
                val nowHourStr = SimpleDateFormat("yyyy-MM-dd'T'HH", Locale.getDefault()).format(Date())
                var startIdx = timeList.indexOfFirst { it.startsWith(nowHourStr) }
                if (startIdx == -1) startIdx = 0
                hourlyData.visibility?.getOrNull(startIdx)?.let { currentVisibilityKm = it / 1000f }
                for (i in startIdx until minOf(startIdx + WeatherApiConfig.FORECAST_HOURS_LIMIT, timeList.size)) {
                    hourlyList.add(
                        HourForecast(
                            hour = if (i == startIdx) "Now" else parseHour(timeList[i].replace('T', ' ')),
                            temperature = hourlyData.temperature2m?.getOrElse(i) { 20f } ?: 20f,
                            weatherCode = hourlyData.weatherCode?.getOrElse(i) { 0 } ?: 0,
                            precipChance = hourlyData.precipitationProbability?.getOrElse(i) { 0 } ?: 0
                        )
                    )
                }
            }
        }

        val dailyList = mutableListOf<DayForecast>()
        var parsedSunrise = TIME_DEFAULT_SUNRISE
        var parsedSunset = TIME_DEFAULT_SUNSET
        var parsedSunriseMinutes = 360
        var parsedSunsetMinutes = 1080
        val dailyData = data.daily
        if (dailyData != null) {
            val dayTimes = dailyData.time ?: emptyList()
            if (dayTimes.isNotEmpty()) {
                parsedSunrise = parseIsoTime(dailyData.sunrise?.firstOrNull() ?: "")
                parsedSunset = parseIsoTime(dailyData.sunset?.firstOrNull() ?: "")
                parsedSunriseMinutes = parseFormattedTimeToMins(parsedSunrise)
                parsedSunsetMinutes = parseFormattedTimeToMins(parsedSunset)
                for (i in dayTimes.indices) {
                    dailyList.add(
                        DayForecast(
                            day = parseDay(dayTimes[i], i),
                            tempMin = dailyData.temperature2mMin?.getOrElse(i) { 20f } ?: 20f,
                            tempMax = dailyData.temperature2mMax?.getOrElse(i) { 30f } ?: 30f,
                            weatherCode = dailyData.weatherCode?.getOrElse(i) { 0 } ?: 0,
                            uvIndex = dailyData.uvIndexMax?.getOrElse(i) { 0f } ?: 0f,
                            precipChance = dailyData.precipProbabilityMax?.getOrElse(i) { 0 } ?: 0
                        )
                    )
                }
            }
        }

        return currentState.copy(
            selectedCity = cityName,
            timezone = data.timezone ?: "",
            temperature = current.temperature2m ?: 0f,
            feelsLike = current.apparentTemperature ?: current.temperature2m ?: 0f,
            humidity = current.relativeHumidity2m ?: 0f,
            pressure = current.surfacePressure ?: 1013f,
            windSpeed = current.windSpeed10m ?: 0f,
            windDir = current.windDirection10m?.let { degreesToCompass(it) } ?: "",
            visibility = currentVisibilityKm,
            uvIndex = current.uv_index ?: 0f,
            weatherCode = current.weatherCode ?: 0,
            sunrise = parsedSunrise,
            sunset = parsedSunset,
            sunriseMinutes = parsedSunriseMinutes,
            sunsetMinutes = parsedSunsetMinutes,
            hourlyForecast = hourlyList,
            dailyForecast = dailyList,
            isLoading = false,
            errorMessage = null,
            lastRefreshedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        )
    }

    // ── Formatting / parsing helpers ────────────────────────────────────────────

    private fun parseHour(raw: String): String {
        if (raw.isBlank()) return ""
        return runCatching {
            val hour = raw.replace('T', ' ').substringAfter(' ').trim()
                .substringBefore(':').toInt()
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, 0)
            }
            SimpleDateFormat("h a", Locale.getDefault()).format(cal.time)
        }.getOrDefault(raw)
    }

    private fun parseDay(dateStr: String, index: Int): String {
        if (index == 0) return "Today"
        if (index == 1) return "Tomorrow"
        return runCatching {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
            SimpleDateFormat("EEE", Locale.getDefault()).format(d!!)
        }.getOrDefault(dateStr)
    }

    private fun parseIsoTime(iso: String): String {
        if (iso.isBlank()) return TIME_DEFAULT_SUNRISE
        return runCatching {
            val d = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault()).parse(iso)
            SimpleDateFormat("h:mm a", Locale.getDefault()).format(d!!)
        }.getOrDefault(iso)
    }

    private fun parseFormattedTimeToMins(formatted: String): Int = runCatching {
        val d = SimpleDateFormat("h:mm a", Locale.getDefault()).parse(formatted.trim())
        val cal = java.util.Calendar.getInstance().apply { time = d!! }
        cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
    }.getOrDefault(WeatherApiConfig.Defaults.DEFAULT_SUNRISE_MINUTES)

    private fun degreesToCompass(deg: Float): String {
        val dirs = arrayOf(
            "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
            "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
        )
        var idx = (((deg % 360f) / 22.5f) + 0.5f).toInt() % 16
        if (idx < 0) idx += 16
        return dirs[idx]
    }
}
