package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.LunaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Device rebooted. Rescheduling active reminders...")
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = LunaDatabase.getInstance(context)
                    val activeReminders = db.reminderDao().getActiveReminders().firstOrNull() ?: emptyList()
                    val now = System.currentTimeMillis()

                    for (reminder in activeReminders) {
                        if (reminder.triggerTimeMillis > now) {
                            val alertIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                                this.action = ReminderBroadcastReceiver.ACTION_REMINDER_ALERT
                                putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminder.id)
                                putExtra(ReminderBroadcastReceiver.EXTRA_TITLE, reminder.title)
                            }

                            val pendingIntent = PendingIntent.getBroadcast(
                                context,
                                reminder.id.toInt(),
                                alertIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                alarmManager.setExactAndAllowWhileIdle(
                                    AlarmManager.RTC_WAKEUP,
                                    reminder.triggerTimeMillis,
                                    pendingIntent
                                )
                            } else {
                                alarmManager.setExact(
                                    AlarmManager.RTC_WAKEUP,
                                    reminder.triggerTimeMillis,
                                    pendingIntent
                                )
                            }
                        }
                    }
                    Log.i(TAG, "Successfully restored ${activeReminders.size} reminders.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring reminders on boot", e)
                }
            }
        }
    }
}
