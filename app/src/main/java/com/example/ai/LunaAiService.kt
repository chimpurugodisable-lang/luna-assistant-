package com.example.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI Service for LUNA voice assistant.
 * Powered by Gemini 3.5 Flash for high-speed, voice-optimized conversational answers.
 */
class LunaAiService {

    companion object {
        private const val TAG = "LunaAiService"
        private const val MODEL = "gemini-3.5-flash"
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun askAi(prompt: String): String = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        // Offline / Fallback knowledge if API key is not yet set or unavailable
        if (apiKey.isNullOrEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext provideOfflineAnswer(prompt)
        }

        try {
            val url = "$BASE_URL/$MODEL:generateContent?key=$apiKey"

            // Construct payload with voice-oriented system instruction
            val jsonPayload = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                }
                put("contents", contents)

                // Voice system prompt for concise spoken responses
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "You are LUNA, a fast, intelligent voice assistant for Android. Answer naturally and concisely in 1 to 2 spoken sentences suitable for text-to-speech."))
                    })
                })

                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 150)
                })
            }

            val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                Log.w(TAG, "Gemini API error code: ${response.code}, body: $responseString")
                return@withContext provideOfflineAnswer(prompt)
            }

            val jsonResponse = JSONObject(responseString)
            val candidates = jsonResponse.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val content = firstCandidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text")?.trim().orEmpty()

            if (text.isNotEmpty()) {
                cleanVoiceResponse(text)
            } else {
                provideOfflineAnswer(prompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Gemini API", e)
            provideOfflineAnswer(prompt)
        }
    }

    private fun cleanVoiceResponse(raw: String): String {
        return raw.replace(Regex("[*#`_~]"), "").replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Offline instant answers for common queries when offline or without API key.
     */
    private fun provideOfflineAnswer(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("who are you") || lower.contains("your name") ->
                "I am LUNA, your personal AI voice assistant on Android."
            lower.contains("how are you") ->
                "I'm operating at peak performance and ready to help you."
            lower.contains("quantum computing") ->
                "Quantum computing uses quantum mechanics principles like superposition and entanglement to solve calculations far faster than classic computers."
            lower.contains("mathematics") || lower.contains("math") ->
                "Mathematics is the foundation of science, patterns, and logic. You can ask me specific calculations or concepts anytime."
            lower.contains("photosynthesis") ->
                "Photosynthesis is the biological process where green plants transform sunlight, water, and carbon dioxide into oxygen and glucose."
            lower.contains("time") ->
                "The current time is ${java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date())}."
            lower.contains("date") ->
                "Today is ${java.text.SimpleDateFormat("EEEE, MMMM d", java.util.Locale.getDefault()).format(java.util.Date())}."
            else ->
                "I understand your request about \"$prompt\". Configure your Gemini API key in settings for in-depth AI answers, or ask for any phone command."
        }
    }
}
