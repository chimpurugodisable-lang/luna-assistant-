package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.LunaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER_ALERT = "com.example.luna.ACTION_REMINDER_ALERT"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_TITLE = "extra_title"
        private const val CHANNEL_ID = "luna_reminders_channel"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Reminder"
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LUNA Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for scheduled LUNA reminders and alarms"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("LUNA Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationId = if (reminderId > 0) reminderId.toInt() else System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)

        // Mark as completed in Room database
        if (reminderId > 0) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = LunaDatabase.getInstance(context)
                    db.reminderDao().markCompleted(reminderId)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
