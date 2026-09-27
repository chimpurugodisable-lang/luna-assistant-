package com.example.intent

import android.content.Context
import android.util.Log
import com.example.ai.LunaAiService
import com.example.data.LunaDatabase
import com.example.data.model.CommandHistoryEntity
import com.example.executor.LunaAlarmManager
import com.example.executor.LunaAppLauncher
import com.example.executor.LunaContactsManager
import com.example.executor.LunaMediaController
import com.example.executor.LunaMessagingManager
import com.example.executor.LunaWeatherExecutor
import com.example.executor.LunaWebSearchExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ExecutionResult(
    val intentName: String,
    val spokenResponse: String,
    val isSuccess: Boolean,
    val intent: LunaIntent
)

class LunaCommandProcessor(
    private val context: Context,
    private val parser: LunaCommandParser = LunaCommandParser(),
    private val appLauncher: LunaAppLauncher = LunaAppLauncher(context),
    private val contactsManager: LunaContactsManager = LunaContactsManager(context),
    private val messagingManager: LunaMessagingManager = LunaMessagingManager(context, contactsManager),
    private val mediaController: LunaMediaController = LunaMediaController(context),
    private val alarmManager: LunaAlarmManager = LunaAlarmManager(context),
    private val webSearchExecutor: LunaWebSearchExecutor = LunaWebSearchExecutor(context),
    private val weatherExecutor: LunaWeatherExecutor = LunaWeatherExecutor(),
    private val aiService: LunaAiService = LunaAiService()
) {

    companion object {
        private const val TAG = "LunaCommandProcessor"
    }

    private val database = LunaDatabase.getInstance(context)

    suspend fun processAndExecute(spokenText: String): ExecutionResult = withContext(Dispatchers.IO) {
        val intent = parser.parse(spokenText)
        Log.i(TAG, "Parsed intent: $intent for input: \"$spokenText\"")

        val result = when (intent) {
            is LunaIntent.OpenApp -> {
                val res = appLauncher.launchApp(intent.appName, intent.suggestedPackage)
                ExecutionResult("OPEN_APP", res.message, res.success, intent)
            }
            is LunaIntent.CallContact -> {
                val res = contactsManager.callContactOrNumber(intent.query)
                ExecutionResult("CALL_CONTACT", res.message, res.success, intent)
            }
            is LunaIntent.SendMessage -> {
                val res = messagingManager.sendMessage(
                    recipient = intent.recipient,
                    messageText = intent.messageText,
                    isWhatsApp = intent.isWhatsApp,
                    isWhatsAppBusiness = intent.isWhatsAppBusiness
                )
                ExecutionResult("SEND_MESSAGE", res.message, res.success, intent)
            }
            is LunaIntent.MediaControl -> {
                val res = mediaController.execute(intent.action, intent.specificApp)
                ExecutionResult("MEDIA_CONTROL", res.message, res.success, intent)
            }
            is LunaIntent.WebSearch -> {
                val res = webSearchExecutor.performSearch(intent.query)
                ExecutionResult("WEB_SEARCH", res.message, res.success, intent)
            }
            is LunaIntent.OpenSettings -> {
                val res = appLauncher.openSettings(intent.category)
                ExecutionResult("OPEN_SETTINGS", res.message, res.success, intent)
            }
            is LunaIntent.SetAlarm -> {
                val res = alarmManager.setSystemAlarm(intent.hour, intent.minute, intent.message)
                ExecutionResult("SET_ALARM", res.message, res.success, intent)
            }
            is LunaIntent.SetReminder -> {
                val res = alarmManager.scheduleReminder(intent.taskText, intent.triggerTimeMillis, intent.repeatRule)
                ExecutionResult("SET_REMINDER", res.message, res.success, intent)
            }
            is LunaIntent.CheckWeather -> {
                val res = weatherExecutor.getWeather(intent.location)
                ExecutionResult("CHECK_WEATHER", res.message, res.success, intent)
            }
            is LunaIntent.AiQuery -> {
                val answer = aiService.askAi(intent.question)
                ExecutionResult("AI_QUERY", answer, true, intent)
            }
            is LunaIntent.Help -> {
                val text = "I am LUNA. You can ask me to open apps, call contacts, message on WhatsApp, play music, search the web, set alarms, or ask general questions."
                ExecutionResult("HELP", text, true, intent)
            }
            is LunaIntent.Unknown -> {
                val text = "I heard you say: ${intent.rawText}. How can I assist you with that?"
                ExecutionResult("UNKNOWN", text, false, intent)
            }
        }

        // Save into command history database
        try {
            database.commandHistoryDao().insert(
                CommandHistoryEntity(
                    spokenQuery = spokenText,
                    identifiedIntent = result.intentName,
                    responseText = result.spokenResponse,
                    executionSuccess = result.isSuccess
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save command history", e)
        }

        result
    }
}
