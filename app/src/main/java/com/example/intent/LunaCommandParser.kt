package com.example.intent

import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

/**
 * Natural language intent parser for LUNA.
 * Separates offline local device commands from general AI reasoning tasks.
 */
class LunaCommandParser {

    fun parse(rawSpokenText: String): LunaIntent {
        var cleanText = rawSpokenText.trim()

        // Strip wake phrase prefixes like "Luna", "Hey Luna", "Ok Luna", "Please"
        cleanText = cleanText.replace(Regex("^(hey\\s+luna|hi\\s+luna|ok\\s+luna|luna|please)[,\\s]*", RegexOption.IGNORE_CASE), "").trim()

        val lower = cleanText.lowercase(Locale.ROOT)
        if (lower.isEmpty()) {
            return LunaIntent.Help
        }

        // 1. HELP / STATUS
        if (lower == "help" || lower == "what can you do" || lower.contains("how do you work") || lower == "commands") {
            return LunaIntent.Help
        }

        // 2. SETTINGS
        if (lower.contains("settings") || lower.contains("setting")) {
            return when {
                lower.contains("wi-fi") || lower.contains("wifi") || lower.contains("internet") ->
                    LunaIntent.OpenSettings(LunaIntent.SettingsCategory.WIFI)
                lower.contains("bluetooth") ->
                    LunaIntent.OpenSettings(LunaIntent.SettingsCategory.BLUETOOTH)
                lower.contains("display") || lower.contains("screen") || lower.contains("brightness") ->
                    LunaIntent.OpenSettings(LunaIntent.SettingsCategory.DISPLAY)
                lower.contains("notification") ->
                    LunaIntent.OpenSettings(LunaIntent.SettingsCategory.NOTIFICATIONS)
                lower.contains("sound") || lower.contains("volume") || lower.contains("ring") ->
                    LunaIntent.OpenSettings(LunaIntent.SettingsCategory.SOUND)
                lower.contains("app") ->
                    LunaIntent.OpenSettings(LunaIntent.SettingsCategory.APPS)
                lower.contains("battery") ->
                    LunaIntent.OpenSettings(LunaIntent.SettingsCategory.BATTERY)
                else ->
                    LunaIntent.OpenSettings(LunaIntent.SettingsCategory.MAIN)
            }
        }

        // 3. MEDIA CONTROL (Must check before generic app launch or general queries)
        if (lower.startsWith("play music") || lower == "play" || lower.startsWith("play song") || lower == "start music" || lower.contains("play some music")) {
            return LunaIntent.MediaControl(LunaIntent.MediaAction.PLAY)
        }
        if (lower == "pause" || lower == "pause music" || lower == "pause song") {
            return LunaIntent.MediaControl(LunaIntent.MediaAction.PAUSE)
        }
        if (lower == "resume" || lower == "resume music" || lower == "continue music" || lower == "unpause") {
            return LunaIntent.MediaControl(LunaIntent.MediaAction.RESUME)
        }
        if (lower == "next song" || lower == "next track" || lower == "skip" || lower == "skip song" || lower == "next") {
            return LunaIntent.MediaControl(LunaIntent.MediaAction.NEXT)
        }
        if (lower == "previous song" || lower == "previous track" || lower == "go back" || lower == "prev song" || lower == "previous") {
            return LunaIntent.MediaControl(LunaIntent.MediaAction.PREVIOUS)
        }
        if (lower == "stop music" || lower == "stop playing" || lower == "stop the song") {
            return LunaIntent.MediaControl(LunaIntent.MediaAction.STOP)
        }

        // Specific music apps
        if (lower.contains("on spotify")) {
            return LunaIntent.MediaControl(LunaIntent.MediaAction.PLAY, "com.spotify.music")
        }
        if (lower.contains("on youtube music")) {
            return LunaIntent.MediaControl(LunaIntent.MediaAction.PLAY, "com.google.android.apps.youtube.music")
        }
        if (lower.contains("on mi music")) {
            return LunaIntent.MediaControl(LunaIntent.MediaAction.PLAY, "com.miui.player")
        }

        // 4. PHONE CALLS
        val callPattern = Pattern.compile("^(?:call|phone|ring|dial)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val callMatcher = callPattern.matcher(cleanText)
        if (callMatcher.matches()) {
            val target = callMatcher.group(1)?.trim().orEmpty()
            if (target.isNotEmpty()) {
                return LunaIntent.CallContact(target)
            }
        }

        // 5. MESSAGING / WHATSAPP
        // Examples:
        // "send John a message saying I'll call later"
        // "send John a WhatsApp message saying I'll call later"
        // "message John on WhatsApp saying hello"
        // "message Mum"
        val isWhatsAppMsg = lower.contains("whatsapp")
        val isBusiness = lower.contains("whatsapp business") || lower.contains("business")

        val complexMsgPattern = Pattern.compile(
            "^(?:send\\s+)?([a-zA-Z0-9\\s]+?)(?:\\s+(?:a\\s+)?(?:whatsapp\\s+)?message|\\s+on\\s+whatsapp)?\\s+(?:saying|that)\\s+(.+)$",
            Pattern.CASE_INSENSITIVE
        )
        val complexMatcher = complexMsgPattern.matcher(cleanText)
        if (complexMatcher.matches()) {
            val recipient = complexMatcher.group(1)?.replace(Regex("(message|to|send)", RegexOption.IGNORE_CASE), "")?.trim().orEmpty()
            val text = complexMatcher.group(2)?.trim().orEmpty()
            if (recipient.isNotEmpty()) {
                return LunaIntent.SendMessage(
                    recipient = recipient,
                    messageText = text,
                    isWhatsApp = isWhatsAppMsg,
                    isWhatsAppBusiness = isBusiness
                )
            }
        }

        val simpleMsgPattern = Pattern.compile(
            "^(?:message|text|send\\s+a\\s+message\\s+to|send\\s+message\\s+to|send\\s+a\\s+whatsapp\\s+to)\\s+([a-zA-Z0-9\\s]+?)(?:\\s+on\\s+whatsapp)?$",
            Pattern.CASE_INSENSITIVE
        )
        val simpleMatcher = simpleMsgPattern.matcher(cleanText)
        if (simpleMatcher.matches()) {
            val recipient = simpleMatcher.group(1)?.trim().orEmpty()
            if (recipient.isNotEmpty()) {
                return LunaIntent.SendMessage(
                    recipient = recipient,
                    messageText = "",
                    isWhatsApp = isWhatsAppMsg,
                    isWhatsAppBusiness = isBusiness
                )
            }
        }

        // 6. ALARMS
        // "set an alarm for 6 AM", "wake me up at 5:30 tomorrow", "alarm at 7"
        if (lower.contains("alarm") || lower.startsWith("wake me up")) {
            val (hour, minute) = parseTime(cleanText)
            return LunaIntent.SetAlarm(hour = hour, minute = minute, message = "LUNA Alarm")
        }

        // 7. REMINDERS
        // "remind me to study Economics at 7 PM", "remind me tomorrow to call John", "remind me every Monday to study"
        if (lower.startsWith("remind me") || lower.startsWith("reminder")) {
            val (task, timeMillis, repeat) = parseReminderDetails(cleanText)
            return LunaIntent.SetReminder(taskText = task, triggerTimeMillis = timeMillis, repeatRule = repeat)
        }

        // 8. APP LAUNCHING
        val appLaunchPattern = Pattern.compile("^(?:open|launch|take\\s+me\\s+to|start|go\\s+to)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val appMatcher = appLaunchPattern.matcher(cleanText)
        if (appMatcher.matches()) {
            val appQuery = appMatcher.group(1)?.trim().orEmpty()
            val appLower = appQuery.lowercase(Locale.ROOT)

            val suggestedPkg = when {
                appLower == "whatsapp business" -> "com.whatsapp.w4b"
                appLower == "whatsapp" -> "com.whatsapp"
                appLower == "chrome" || appLower == "google chrome" || appLower == "browser" -> "com.android.chrome"
                appLower == "facebook" -> "com.facebook.katana"
                appLower == "youtube" -> "com.google.android.youtube"
                appLower == "youtube music" -> "com.google.android.apps.youtube.music"
                appLower == "spotify" -> "com.spotify.music"
                appLower == "mi music" -> "com.miui.player"
                else -> null
            }
            return LunaIntent.OpenApp(appName = appQuery, suggestedPackage = suggestedPkg)
        }

        // 9. WEB SEARCH
        val searchPattern = Pattern.compile("^(?:search\\s+for|search|google|look\\s+up|find\\s+online)\\s+(.+)$", Pattern.CASE_INSENSITIVE)
        val searchMatcher = searchPattern.matcher(cleanText)
        if (searchMatcher.matches()) {
            val query = searchMatcher.group(1)?.trim().orEmpty()
            if (query.isNotEmpty()) {
                return LunaIntent.WebSearch(query)
            }
        }

        // 10. WEATHER
        if (lower.contains("weather")) {
            val locationPattern = Pattern.compile("(?:in|for|at)\\s+([a-zA-Z\\s]+)$", Pattern.CASE_INSENSITIVE)
            val locMatcher = locationPattern.matcher(cleanText)
            val location = if (locMatcher.find()) locMatcher.group(1)?.trim() else null
            return LunaIntent.CheckWeather(location)
        }

        // 11. GENERAL AI QUERIES
        // "explain ...", "help me ...", "what is ...", "why ...", "summarize ...", "write ...", "who is ..."
        val isAiStyleQuestion = lower.startsWith("explain") ||
                lower.startsWith("help me") ||
                lower.startsWith("what is") ||
                lower.startsWith("what are") ||
                lower.startsWith("what does") ||
                lower.startsWith("why") ||
                lower.startsWith("how to") ||
                lower.startsWith("summarize") ||
                lower.startsWith("write") ||
                lower.startsWith("tell me about") ||
                lower.contains("meaning of")

        if (isAiStyleQuestion || cleanText.endsWith("?")) {
            return LunaIntent.AiQuery(cleanText)
        }

        // Default: If unrecognized local command, treat as AI prompt to provide conversational intelligence
        return if (cleanText.length > 3) {
            LunaIntent.AiQuery(cleanText)
        } else {
            LunaIntent.Unknown(cleanText)
        }
    }

    /**
     * Extracts hour (0..23) and minute (0..59) from time string.
     */
    private fun parseTime(text: String): Pair<Int, Int> {
        val pattern = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        if (matcher.find()) {
            var hour = matcher.group(1)?.toIntOrNull() ?: 7
            val minute = matcher.group(2)?.toIntOrNull() ?: 0
            val amPm = matcher.group(3)?.lowercase(Locale.ROOT)

            if (amPm == "pm" && hour < 12) {
                hour += 12
            } else if (amPm == "am" && hour == 12) {
                hour = 0
            }
            return Pair(hour, minute)
        }
        return Pair(8, 0) // default 8:00 AM
    }

    /**
     * Parses reminder text, timestamp, and optional repeat rule.
     */
    private fun parseReminderDetails(rawText: String): Triple<String, Long, String?> {
        val lower = rawText.lowercase(Locale.ROOT)
        var repeatRule: String? = null
        if (lower.contains("every monday")) repeatRule = "MONDAY"
        else if (lower.contains("every day") || lower.contains("daily")) repeatRule = "DAILY"

        val (hour, minute) = parseTime(rawText)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (lower.contains("tomorrow") || timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        var task = rawText.replace(Regex("^(remind\\s+me\\s+(to)?|reminder\\s+(to)?)[,\\s]*", RegexOption.IGNORE_CASE), "")
        task = task.replace(Regex("\\s+(at|on|tomorrow|every\\s+\\w+)\\s+\\d.*$", RegexOption.IGNORE_CASE), "").trim()
        if (task.isEmpty()) {
            task = "Reminder"
        }

        return Triple(task, cal.timeInMillis, repeatRule)
    }
}
