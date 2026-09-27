package com.example.executor

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import com.example.intent.LunaIntent

class LunaMediaController(private val context: Context) {

    companion object {
        private const val TAG = "LunaMediaController"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    data class MediaResult(
        val success: Boolean,
        val message: String
    )

    /**
     * Executes media session control action.
     */
    fun execute(action: LunaIntent.MediaAction, specificAppPkg: String? = null): MediaResult {
        // If user specifically requested e.g. Spotify / YouTube Music / Mi Music, open the player
        if (!specificAppPkg.isNullOrEmpty()) {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage(specificAppPkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY)
                return MediaResult(true, "Playing music on ${pm.getApplicationLabel(pm.getApplicationInfo(specificAppPkg, 0))}.")
            }
        }

        val keycode = when (action) {
            LunaIntent.MediaAction.PLAY -> KeyEvent.KEYCODE_MEDIA_PLAY
            LunaIntent.MediaAction.PAUSE -> KeyEvent.KEYCODE_MEDIA_PAUSE
            LunaIntent.MediaAction.RESUME -> KeyEvent.KEYCODE_MEDIA_PLAY
            LunaIntent.MediaAction.NEXT -> KeyEvent.KEYCODE_MEDIA_NEXT
            LunaIntent.MediaAction.PREVIOUS -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            LunaIntent.MediaAction.STOP -> KeyEvent.KEYCODE_MEDIA_STOP
        }

        val actionName = when (action) {
            LunaIntent.MediaAction.PLAY -> "Playing music."
            LunaIntent.MediaAction.PAUSE -> "Paused music."
            LunaIntent.MediaAction.RESUME -> "Resuming music."
            LunaIntent.MediaAction.NEXT -> "Skipping to next song."
            LunaIntent.MediaAction.PREVIOUS -> "Playing previous song."
            LunaIntent.MediaAction.STOP -> "Stopping playback."
        }

        val sent = sendMediaKeyEvent(keycode)

        // If Play failed because no active media player was running, try launching default music player
        if (action == LunaIntent.MediaAction.PLAY && audioManager?.isMusicActive == false) {
            val musicIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MUSIC)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(musicIntent)
            } catch (e: Exception) {
                Log.w(TAG, "No default music app found for CATEGORY_APP_MUSIC", e)
            }
        }

        return MediaResult(sent, actionName)
    }

    private fun sendMediaKeyEvent(keyCode: Int): Boolean {
        return try {
            val eventDown = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0)
            val eventUp = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)

            audioManager?.dispatchMediaKeyEvent(eventDown)
            audioManager?.dispatchMediaKeyEvent(eventUp)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch media key event $keyCode", e)
            false
        }
    }
}
