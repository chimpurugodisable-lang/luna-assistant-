package com.example.voice

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * Text-to-speech engine for LUNA.
 * Handles speech synthesis with utterance tracking, state synchronization,
 * and audio-feedback prevention.
 */
class LunaTtsEngine(
    private val context: Context,
    private val onInitComplete: (success: Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "LunaTtsEngine"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val pendingCompletions = mutableMapOf<String, () -> Unit>()

    var isSpeaking: Boolean = false
        private set

    var onSpeakingStateChanged: ((isSpeaking: Boolean) -> Unit)? = null

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to instantiate TextToSpeech", e)
            onInitComplete(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language US not supported, falling back to default locale")
                tts?.language = Locale.getDefault()
            }
            tts?.setPitch(1.05f)
            tts?.setSpeechRate(1.0f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    isSpeaking = true
                    onSpeakingStateChanged?.invoke(true)
                }

                override fun onDone(utteranceId: String?) {
                    isSpeaking = false
                    onSpeakingStateChanged?.invoke(false)
                    utteranceId?.let { id ->
                        val action = synchronized(pendingCompletions) {
                            pendingCompletions.remove(id)
                        }
                        action?.invoke()
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    onSpeakingStateChanged?.invoke(false)
                    utteranceId?.let { id ->
                        val action = synchronized(pendingCompletions) {
                            pendingCompletions.remove(id)
                        }
                        action?.invoke()
                    }
                }
            })

            isInitialized = true
            Log.i(TAG, "TTS initialized successfully")
            onInitComplete(true)
        } else {
            Log.e(TAG, "TTS initialization failed with code $status")
            isInitialized = false
            onInitComplete(false)
        }
    }

    /**
     * Speaks the given text and optionally triggers a callback when completed.
     */
    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not ready yet. Skipping voice output: $text")
            onDone?.invoke()
            return
        }

        val utteranceId = UUID.randomUUID().toString()
        if (onDone != null) {
            synchronized(pendingCompletions) {
                pendingCompletions[utteranceId] = onDone
            }
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    /**
     * Stops current speech immediately.
     */
    fun stop() {
        try {
            tts?.stop()
            isSpeaking = false
            onSpeakingStateChanged?.invoke(false)
            synchronized(pendingCompletions) {
                pendingCompletions.clear()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    /**
     * Releases TTS resources.
     */
    fun shutdown() {
        stop()
        try {
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
