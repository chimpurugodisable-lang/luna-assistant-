package com.example.executor

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Fetches real weather data via Open-Meteo (free, no API key required).
 * Replaces the previous approach of asking the Gemini LLM to guess a forecast,
 * which produced fluent but fabricated numbers.
 */
class LunaWeatherExecutor {

    companion object {
        private const val TAG = "LunaWeatherExecutor"
        private const val GEOCODE_URL = "https://geocoding-api.open-meteo.com/v1/search"
        private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class WeatherResult(val success: Boolean, val message: String)

    suspend fun getWeather(location: String?): WeatherResult = withContext(Dispatchers.IO) {
        if (location.isNullOrBlank()) {
            return@withContext WeatherResult(
                false,
                "Which city's weather do you want? Luna doesn't have access to your location yet."
            )
        }

        try {
            val geocodeRequest = Request.Builder()
                .url("$GEOCODE_URL?name=${java.net.URLEncoder.encode(location, "UTF-8")}&count=1")
                .build()
            val geocodeResponse = client.newCall(geocodeRequest).execute()
            val geocodeBody = geocodeResponse.body?.string().orEmpty()
            if (!geocodeResponse.isSuccessful) {
                return@withContext WeatherResult(false, "Couldn't look up $location right now.")
            }

            val results = JSONObject(geocodeBody).optJSONArray("results")
            val place = results?.optJSONObject(0)
                ?: return@withContext WeatherResult(false, "I couldn't find a place called $location.")

            val lat = place.getDouble("latitude")
            val lon = place.getDouble("longitude")
            val resolvedName = place.optString("name", location)
            val country = place.optString("country", "")

            val forecastRequest = Request.Builder()
                .url(
                    "$FORECAST_URL?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                        "&temperature_unit=celsius&wind_speed_unit=kmh"
                )
                .build()
            val forecastResponse = client.newCall(forecastRequest).execute()
            val forecastBody = forecastResponse.body?.string().orEmpty()
            if (!forecastResponse.isSuccessful) {
                return@withContext WeatherResult(false, "Couldn't fetch the forecast for $resolvedName.")
            }

            val current = JSONObject(forecastBody).getJSONObject("current")
            val tempC = current.getDouble("temperature_2m")
            val humidity = current.getInt("relative_humidity_2m")
            val windKmh = current.getDouble("wind_speed_10m")
            val code = current.getInt("weather_code")

            val condition = describeWeatherCode(code)
            val place2 = if (country.isNotBlank()) "$resolvedName, $country" else resolvedName

            WeatherResult(
                true,
                "It's currently ${tempC.toInt()} degrees and $condition in $place2, " +
                    "with ${humidity}% humidity and wind at ${windKmh.toInt()} kilometers per hour."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Weather lookup failed", e)
            WeatherResult(false, "I couldn't reach the weather service right now.")
        }
    }

    // WMO weather interpretation codes: https://open-meteo.com/en/docs
    private fun describeWeatherCode(code: Int): String = when (code) {
        0 -> "clear sky"
        1, 2 -> "partly cloudy"
        3 -> "overcast"
        45, 48 -> "foggy"
        51, 53, 55 -> "drizzling"
        56, 57 -> "freezing drizzle"
        61, 63, 65 -> "rainy"
        66, 67 -> "freezing rain"
        71, 73, 75 -> "snowy"
        77 -> "snow grains"
        80, 81, 82 -> "rain showers"
        85, 86 -> "snow showers"
        95 -> "thunderstorms"
        96, 99 -> "thunderstorms with hail"
        else -> "unpredictable"
    }
}
