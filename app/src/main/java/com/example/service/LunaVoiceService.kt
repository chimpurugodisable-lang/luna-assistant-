package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.core.LunaVoiceState
import com.example.intent.LunaCommandProcessor
import com.example.voice.LunaSpeechEngine
import com.example.voice.LunaTtsEngine
import com.example.voice.LunaWakeWordEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * LUNA Background Foreground Service for hands-free voice interaction.
 * Coordinates wake-word detection, speech-to-text, intent execution, and TTS.
 */
class LunaVoiceService : Service() {

    companion object {
        private const val TAG = "LunaVoiceService"
        const val ACTION_START = "com.example.luna.ACTION_START_VOICE_SERVICE"
        const val ACTION_STOP = "com.example.luna.ACTION_STOP_VOICE_SERVICE"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "luna_voice_service_channel"
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var wakeLock: PowerManager.WakeLock? = null
    private var wakeWordEngine: LunaWakeWordEngine? = null
    private var speechEngine: LunaSpeechEngine? = null
    private var ttsEngine: LunaTtsEngine? = null
    private var commandProcessor: LunaCommandProcessor? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Creating LunaVoiceService")
        LunaAssistantManager.activeService = this
        LunaAssistantManager.setServiceRunning(true)

        acquireWakeLock()
        initComponents()
    }

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "LUNA:WakeWordLock")?.apply {
                setReferenceCounted(false)
                acquire(10 * 60 * 1000L) // Safe 10 minute timeout, refreshed while active
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire partial wakelock", e)
        }
    }

    private fun initComponents() {
        commandProcessor = LunaCommandProcessor(this)

        ttsEngine = LunaTtsEngine(this) { success ->
            Log.d(TAG, "TTS init complete: $success")
        }.apply {
            onSpeakingStateChanged = { speaking ->
                wakeWordEngine?.setTtsSpeaking(speaking)
                if (speaking) {
                    LunaAssistantManager.updateState(LunaVoiceState.SPEAKING)
                }
            }
        }

        speechEngine = LunaSpeechEngine(this, object : LunaSpeechEngine.SpeechEngineListener {
            override fun onReadyForCommand() {
                LunaAssistantManager.updateState(LunaVoiceState.LISTENING_FOR_COMMAND)
            }

            override fun onRmsChanged(rmsdB: Float) {
                LunaAssistantManager.updateRms(rmsdB)
            }

            override fun onPartialResult(partialText: String) {
                LunaAssistantManager.setLastSpokenUserText(partialText)
            }

            override fun onFinalResult(spokenText: String) {
                LunaAssistantManager.setLastSpokenUserText(spokenText)
                handleCommandExecution(spokenText)
            }

            override fun onError(errorCode: Int, message: String) {
                Log.w(TAG, "Speech engine error: $message ($errorCode)")
                // If listening timed out or didn't hear speech, recover safely to wake word
                returnToWakeWordListening()
            }
        })

        initWakeWordEngine()
    }

    private fun initWakeWordEngine() {
        try {
            wakeWordEngine = LunaWakeWordEngine(
                context = this,
                onWakeWordDetected = { wakePhrase ->
                    onWakeWordSpotted(wakePhrase)
                },
                onError = { error ->
                    Log.e(TAG, "WakeWordEngine error: $error")
                    LunaAssistantManager.setWakeWordEngineStatus("Wake word: $error")
                }
            )

            if (wakeWordEngine?.isConfigValid == true) {
                LunaAssistantManager.setWakeWordEngineStatus("Offline wake-word active (\"Luna\", \"Hey Luna\")")
            } else {
                LunaAssistantManager.setWakeWordEngineStatus("Acoustic wake-word engine active")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LunaWakeWordEngine safely", e)
            LunaAssistantManager.setWakeWordEngineStatus("Wake-word initialization skipped: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        Log.i(TAG, "onStartCommand action: $action")

        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundWithNotification()
        startWakeWordListening()

        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        createNotificationChannel()

        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, LunaVoiceService::class.java).apply {
            this.action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.voice_service_notification_title))
            .setContentText(getString(R.string.voice_service_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(0, "Stop", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to startForeground", e)
            // Even if foreground notification fails, continue running service gracefully
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.voice_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.voice_service_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Resumes wake-word listening loop.
     */
    private fun startWakeWordListening() {
        try {
            LunaAssistantManager.updateState(LunaVoiceState.LISTENING_FOR_WAKE_WORD)
            wakeWordEngine?.setTtsSpeaking(false)
            wakeWordEngine?.startListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting wake word listening", e)
        }
    }

    /**
     * Called when "Luna" or "Hey Luna" is detected.
     */
    private fun onWakeWordSpotted(wakePhrase: String) {
        Log.i(TAG, "Wake phrase detected: $wakePhrase")
        wakeWordEngine?.stopListening()
        LunaAssistantManager.updateState(LunaVoiceState.WAKE_WORD_DETECTED)

        // LUNA responds "Yes?" then begins listening for command
        ttsEngine?.speak("Yes?") {
            serviceScope.launch(Dispatchers.Main) {
                startCommandListeningFlow()
            }
        }
    }

    /**
     * Begins listening for the user's speech command.
     */
    fun startCommandListeningFlow() {
        wakeWordEngine?.stopListening()
        LunaAssistantManager.updateState(LunaVoiceState.LISTENING_FOR_COMMAND)
        speechEngine?.startListening()
    }

    /**
     * Executes the parsed command and speaks the result.
     */
    private fun handleCommandExecution(spokenText: String) {
        LunaAssistantManager.updateState(LunaVoiceState.THINKING)

        serviceScope.launch {
            LunaAssistantManager.updateState(LunaVoiceState.EXECUTING)
            val result = commandProcessor?.processAndExecute(spokenText)

            val responseText = result?.spokenResponse ?: "Done."
            LunaAssistantManager.setLastAssistantResponse(responseText)

            LunaAssistantManager.updateState(LunaVoiceState.SPEAKING)
            ttsEngine?.speak(responseText) {
                serviceScope.launch(Dispatchers.Main) {
                    returnToWakeWordListening()
                }
            }
        }
    }

    /**
     * Executes a manual text command entered by the user.
     */
    fun processUserText(text: String) {
        LunaAssistantManager.setLastSpokenUserText(text)
        handleCommandExecution(text)
    }

    /**
     * Transitions state cleanly back to wake-word detection.
     */
    private fun returnToWakeWordListening() {
        speechEngine?.stopListening()
        startWakeWordListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Destroying LunaVoiceService")
        LunaAssistantManager.activeService = null
        LunaAssistantManager.setServiceRunning(false)
        LunaAssistantManager.updateState(LunaVoiceState.IDLE)

        serviceJob.cancel()
        try {
            wakeWordEngine?.stopListening()
            speechEngine?.destroy()
            ttsEngine?.shutdown()
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up LunaVoiceService", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
