package com.kruju.habits.data

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kruju.habits.MainActivity
import com.kruju.habits.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object Reminders {
    private const val CHANNEL_ID = "daily_reminder"
    private const val NOTIFICATION_ID = 1

    /** Schedules the next reminder, or cancels it when disabled. Uses an inexact alarm so no special permission is needed. */
    fun schedule(context: Context, settings: ReminderSettings) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        alarmManager.cancel(pending)
        if (!settings.enabled) return

        val now = LocalDateTime.now()
        var next = LocalDateTime.of(LocalDate.now(), LocalTime.of(settings.hour, settings.minute))
        if (!next.isAfter(now)) next = next.plusDays(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 5 * 60 * 1000L, pending)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showIfHabitsLeft(context: Context, data: AppData) {
        val today = todayEpochDay()
        val left = data.habits.filterNot { it.isDone(today) }
        if (left.isEmpty() || !canNotify(context)) return

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Daily reminder", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (left.size == 1) "1 habit left today" else "${left.size} habits left today"
        val text = left.joinToString("  ") { "${it.emoji} ${it.name}" }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call; nothing to do.
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val data = HabitStore(context).load()
        Reminders.showIfHabitsLeft(context, data)
        Reminders.schedule(context, data.reminder)
    }
}

/** Alarms are wiped on reboot and go stale on clock changes, so re-arm them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.schedule(context, HabitStore(context).load().reminder)
    }
}
