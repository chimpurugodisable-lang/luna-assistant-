package com.example.intent

sealed interface LunaIntent {
    data class OpenApp(
        val appName: String,
        val suggestedPackage: String? = null
    ) : LunaIntent

    data class CallContact(
        val query: String
    ) : LunaIntent

    data class SendMessage(
        val recipient: String,
        val messageText: String,
        val isWhatsApp: Boolean = false,
        val isWhatsAppBusiness: Boolean = false
    ) : LunaIntent

    enum class MediaAction {
        PLAY, PAUSE, RESUME, NEXT, PREVIOUS, STOP
    }

    data class MediaControl(
        val action: MediaAction,
        val specificApp: String? = null // e.g. "spotify", "youtube music", "mi music"
    ) : LunaIntent

    data class WebSearch(
        val query: String
    ) : LunaIntent

    enum class SettingsCategory {
        MAIN, WIFI, BLUETOOTH, DISPLAY, NOTIFICATIONS, SOUND, APPS, BATTERY
    }

    data class OpenSettings(
        val category: SettingsCategory
    ) : LunaIntent

    data class SetAlarm(
        val hour: Int,
        val minute: Int,
        val message: String
    ) : LunaIntent

    data class SetReminder(
        val taskText: String,
        val triggerTimeMillis: Long,
        val repeatRule: String? = null
    ) : LunaIntent

    data class CheckWeather(
        val location: String? = null
    ) : LunaIntent

    data class AiQuery(
        val question: String
    ) : LunaIntent

    object Help : LunaIntent

    data class Unknown(
        val rawText: String
    ) : LunaIntent
}
