package com.example.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.example.intent.LunaIntent

class LunaAppLauncher(private val context: Context) {

    companion object {
        private const val TAG = "LunaAppLauncher"
    }

    data class LaunchResult(
        val success: Boolean,
        val message: String
    )

    /**
     * Attempts to launch an installed application by query name or package.
     */
    fun launchApp(appName: String, suggestedPackage: String? = null): LaunchResult {
        val pm = context.packageManager

        // 1. Try suggested package first
        if (!suggestedPackage.isNullOrEmpty()) {
            val intent = pm.getLaunchIntentForPackage(suggestedPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                val friendlyName = when (suggestedPackage) {
                    "com.whatsapp.w4b" -> "WhatsApp Business"
                    "com.whatsapp" -> "WhatsApp"
                    "com.android.chrome" -> "Chrome"
                    "com.facebook.katana" -> "Facebook"
                    "com.google.android.youtube" -> "YouTube"
                    "com.spotify.music" -> "Spotify"
                    else -> appName
                }
                return LaunchResult(true, "Opening $friendlyName.")
            }
        }

        // 2. Search installed applications by label
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            val queryClean = appName.trim().lowercase()

            for (appInfo in installedApps) {
                val label = pm.getApplicationLabel(appInfo).toString().lowercase()
                if (label == queryClean || label.contains(queryClean) || queryClean.contains(label)) {
                    val intent = pm.getLaunchIntentForPackage(appInfo.packageName)
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        val appLabel = pm.getApplicationLabel(appInfo).toString()
                        return LaunchResult(true, "Opening $appLabel.")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying applications", e)
        }

        return LaunchResult(false, "I couldn't find $appName installed on your phone.")
    }

    /**
     * Opens real Android Settings categories.
     */
    fun openSettings(category: LunaIntent.SettingsCategory): LaunchResult {
        val action = when (category) {
            LunaIntent.SettingsCategory.MAIN -> Settings.ACTION_SETTINGS
            LunaIntent.SettingsCategory.WIFI -> Settings.ACTION_WIFI_SETTINGS
            LunaIntent.SettingsCategory.BLUETOOTH -> Settings.ACTION_BLUETOOTH_SETTINGS
            LunaIntent.SettingsCategory.DISPLAY -> Settings.ACTION_DISPLAY_SETTINGS
            LunaIntent.SettingsCategory.NOTIFICATIONS -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
            LunaIntent.SettingsCategory.SOUND -> Settings.ACTION_SOUND_SETTINGS
            LunaIntent.SettingsCategory.APPS -> Settings.ACTION_APPLICATION_SETTINGS
            LunaIntent.SettingsCategory.BATTERY -> Settings.ACTION_BATTERY_SAVER_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (category == LunaIntent.SettingsCategory.NOTIFICATIONS) {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            }
            context.startActivity(intent)
            val catName = when (category) {
                LunaIntent.SettingsCategory.WIFI -> "Wi-Fi settings"
                LunaIntent.SettingsCategory.BLUETOOTH -> "Bluetooth settings"
                LunaIntent.SettingsCategory.DISPLAY -> "display settings"
                LunaIntent.SettingsCategory.NOTIFICATIONS -> "notification settings"
                LunaIntent.SettingsCategory.SOUND -> "sound settings"
                LunaIntent.SettingsCategory.BATTERY -> "battery settings"
                LunaIntent.SettingsCategory.APPS -> "app settings"
                LunaIntent.SettingsCategory.MAIN -> "Settings"
            }
            LaunchResult(true, "Opening $catName.")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening settings $category", e)
            LaunchResult(false, "Could not open settings.")
        }
    }
}
