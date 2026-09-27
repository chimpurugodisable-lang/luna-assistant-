package com.example.executor

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.util.Log
import com.example.data.LunaDatabase
import com.example.data.model.ReminderEntity
import com.example.receiver.ReminderBroadcastReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LunaAlarmManager(private val context: Context) {

    companion object {
        private const val TAG = "LunaAlarmManager"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    private val database = LunaDatabase.getInstance(context)

    data class AlarmResult(
        val success: Boolean,
        val message: String
    )

    /**
     * Sets a standard system alarm using AlarmClock intent.
     */
    fun setSystemAlarm(hour: Int, minute: Int, message: String): AlarmResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, message)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val displayTime = formatTime(hour, minute)
        return try {
            context.startActivity(intent)
            AlarmResult(true, "Alarm set for $displayTime.")
        } catch (e: Exception) {
            Log.e(TAG, "Error setting system alarm via AlarmClock intent", e)
            // Fallback: Schedule using AlarmManager directly
            scheduleDirectAlarm(hour, minute, message)
        }
    }

    private fun scheduleDirectAlarm(hour: Int, minute: Int, message: String): AlarmResult {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_REMINDER_ALERT
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, "Alarm: $message")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (hour * 100 + minute),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager?.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
            val displayTime = formatTime(hour, minute)
            AlarmResult(true, "Alarm set for $displayTime.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm", e)
            AlarmResult(false, "Could not set alarm.")
        }
    }

    /**
     * Saves a reminder in Room Database and schedules the notification alert via AlarmManager.
     */
    suspend fun scheduleReminder(
        taskText: String,
        triggerTimeMillis: Long,
        repeatRule: String? = null
    ): AlarmResult = withContext(Dispatchers.IO) {
        val entity = ReminderEntity(
            title = taskText,
            triggerTimeMillis = triggerTimeMillis,
            repeatRule = repeatRule
        )

        val id = database.reminderDao().insertReminder(entity)

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_REMINDER_ALERT
            putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, id)
            putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, taskText)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager?.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            } else {
                alarmManager?.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent)
            }

            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val timeStr = sdf.format(Date(triggerTimeMillis))
            AlarmResult(true, "Okay, I'll remind you to $taskText at $timeStr.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set reminder alarm", e)
            AlarmResult(false, "Reminder saved, but could not set exact alarm alert.")
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val h = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val m = if (minute < 10) "0$minute" else "$minute"
        return "$h:$m $amPm"
    }
}
