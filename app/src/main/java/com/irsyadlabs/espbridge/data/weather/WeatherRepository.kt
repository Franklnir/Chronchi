package com.irsyadlabs.espbridge.data.weather

import com.irsyadlabs.espbridge.core.model.WeatherState
import com.irsyadlabs.espbridge.data.local.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WeatherRepository(private val settingsRepository: SettingsRepository) {
    suspend fun fetch(latitude: Double, longitude: Double, label: String = "Current location"): WeatherState =
        withContext(Dispatchers.IO) {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude&longitude=$longitude" +
                    "&current=temperature_2m,relative_humidity_2m,weather_code" +
                    "&timezone=auto"
            )
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            try {
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).getJSONObject("current")
                val temp = current.getDouble("temperature_2m")
                val humidity = current.optInt("relative_humidity_2m")
                val condition = weatherCodeToCondition(current.optInt("weather_code"))
                val now = System.currentTimeMillis()
                settingsRepository.cacheWeather(temp, condition, label, now)
                WeatherState(temp, condition, humidity, label, now, false)
            } finally {
                connection.disconnect()
            }
        }

    private fun weatherCodeToCondition(code: Int): String = when (code) {
        0 -> "sunny"
        1, 2 -> "partly_cloudy"
        3 -> "cloudy"
        45, 48 -> "fog"
        in 51..67, in 80..82 -> "rain"
        in 71..77, 85, 86 -> "snow"
        in 95..99 -> "storm"
        else -> "unknown"
    }
}
