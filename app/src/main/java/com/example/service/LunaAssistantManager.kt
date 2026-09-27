package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.core.AssistantStatus
import com.example.core.LunaVoiceState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Singleton state coordinator between the UI and LunaVoiceService.
 */
object LunaAssistantManager {

    private const val TAG = "LunaAssistantManager"

    private val _status = MutableStateFlow(AssistantStatus())
    val status: StateFlow<AssistantStatus> = _status.asStateFlow()

    private val _rmsAmplitude = MutableStateFlow(0f)
    val rmsAmplitude: StateFlow<Float> = _rmsAmplitude.asStateFlow()

    var activeService: LunaVoiceService? = null

    fun updateState(newState: LunaVoiceState) {
        _status.update { it.copy(state = newState) }
    }

    fun updateRms(rms: Float) {
        _rmsAmplitude.value = rms
    }

    fun setMicPermissionGranted(granted: Boolean) {
        _status.update { it.copy(isMicPermissionGranted = granted) }
    }

    fun setServiceRunning(running: Boolean) {
        _status.update { it.copy(isServiceRunning = running) }
    }

    fun setLastSpokenUserText(text: String) {
        _status.update { it.copy(lastSpokenUserText = text) }
    }

    fun setLastAssistantResponse(response: String) {
        _status.update { it.copy(lastAssistantResponse = response) }
    }

    fun setWakeWordEngineStatus(statusText: String) {
        _status.update { it.copy(wakeWordEngineStatus = statusText) }
    }

    fun setErrorMessage(msg: String?) {
        _status.update { it.copy(errorMessage = msg) }
    }

    fun startService(context: Context) {
        try {
            val intent = Intent(context, LunaVoiceService::class.java).apply {
                action = LunaVoiceService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start LunaVoiceService", e)
            setErrorMessage("Could not start background voice service: ${e.message}")
        }
    }

    fun stopService(context: Context) {
        try {
            val intent = Intent(context, LunaVoiceService::class.java).apply {
                action = LunaVoiceService.ACTION_STOP
            }
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop LunaVoiceService", e)
        }
    }

    fun triggerVoiceCommand() {
        activeService?.startCommandListeningFlow()
    }

    fun submitTextCommand(text: String) {
        activeService?.processUserText(text)
    }
}
