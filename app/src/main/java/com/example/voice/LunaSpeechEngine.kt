package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * Speech Recognition Engine for LUNA.
 * Converts user command speech into text once activated.
 */
class LunaSpeechEngine(
    private val context: Context,
    private val listener: SpeechEngineListener
) {

    interface SpeechEngineListener {
        fun onReadyForCommand()
        fun onRmsChanged(rmsdB: Float)
        fun onPartialResult(partialText: String)
        fun onFinalResult(spokenText: String)
        fun onError(errorCode: Int, message: String)
    }

    companion object {
        private const val TAG = "LunaSpeechEngine"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    var isListening: Boolean = false
        private set

    init {
        mainHandler.post {
            initRecognizer()
        }
    }

    private fun initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e(TAG, "Speech recognition is not available on this device")
            listener.onError(-1, "Speech recognition unavailable on device")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "SpeechRecognizer: Ready for speech")
                        isListening = true
                        listener.onReadyForCommand()
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: Beginning of speech")
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        listener.onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: End of speech")
                        isListening = false
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out"
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client recognition error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission error"
                            SpeechRecognizer.ERROR_NETWORK -> "Network required for online recognition"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                            SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                            else -> "Recognition error code: $error"
                        }
                        Log.w(TAG, "SpeechRecognizer onError: $error ($errorMsg)")
                        listener.onError(error, errorMsg)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim().orEmpty()
                        Log.i(TAG, "SpeechRecognizer onResults: $text")
                        if (text.isNotEmpty()) {
                            listener.onFinalResult(text)
                        } else {
                            listener.onError(SpeechRecognizer.ERROR_NO_MATCH, "No words heard")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            listener.onPartialResult(it)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create SpeechRecognizer", e)
        }
    }

    /**
     * Starts listening for user command.
     */
    fun startListening() {
        mainHandler.post {
            if (speechRecognizer == null) {
                initRecognizer()
            }

            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition", e)
                listener.onError(-1, "Unable to start speech recognizer: ${e.message}")
            }
        }
    }

    /**
     * Stops listening for speech.
     */
    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping SpeechRecognizer", e)
            } finally {
                isListening = false
            }
        }
    }

    /**
     * Cancels active recognition.
     */
    fun cancel() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling SpeechRecognizer", e)
            } finally {
                isListening = false
            }
        }
    }

    /**
     * Releases recognizer.
     */
    fun destroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying SpeechRecognizer", e)
            } finally {
                isListening = false
            }
        }
    }
}
