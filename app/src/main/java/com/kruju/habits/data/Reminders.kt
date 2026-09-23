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

/** One inexact alarm per habit with a reminder. Inexact alarms need no special permission. */
object Reminders {
    private const val CHANNEL_ID = "habit_reminders"
    private const val EXTRA_HABIT_ID = "habit_id"

    private fun requestCode(habitId: Long): Int = (habitId xor (habitId ushr 32)).toInt()

    private fun pendingIntent(context: Context, habitId: Long): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode(habitId),
        Intent(context, ReminderReceiver::class.java).putExtra(EXTRA_HABIT_ID, habitId),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun cancel(context: Context, habitId: Long) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pendingIntent(context, habitId))
    }

    fun schedule(context: Context, habit: Habit) {
        val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
        val pending = pendingIntent(context, habit.id)
        alarmManager.cancel(pending)
        val minutes = habit.reminder ?: return

        val now = LocalDateTime.now()
        val time = LocalTime.of(minutes / 60, minutes % 60)
        val today = LocalDate.now().toEpochDay()
        val nextDay = (today..today + 400).firstOrNull { day ->
            habit.isScheduled(day) && LocalDateTime.of(LocalDate.ofEpochDay(day), time).isAfter(now)
        } ?: return
        val triggerAt = LocalDateTime.of(LocalDate.ofEpochDay(nextDay), time)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarmManager.setWindow(AlarmManager.RTC_WAKEUP, triggerAt, 5 * 60 * 1000L, pending)
    }

    fun scheduleAll(context: Context, data: AppData) {
        data.habits.forEach { schedule(context, it) }
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun show(context: Context, habit: Habit) {
        if (!canNotify(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val text = when (habit.type) {
            EvalType.NUMERIC -> "Goal: ${formatNumber(habit.goal)} ${habit.unit}".trim()
            EvalType.TIMER -> "Goal: ${formatNumber(habit.goal)} min"
            else -> habit.description.ifBlank { "Time to keep your streak going" }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(habit.name)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(requestCode(habit.id), notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the call; nothing to do.
        }
    }

    internal fun habitIdFrom(intent: Intent): Long = intent.getLongExtra(EXTRA_HABIT_ID, -1)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habit = HabitStore(context).load().habits.find { it.id == Reminders.habitIdFrom(intent) } ?: return
        val today = todayEpochDay()
        if (habit.isScheduled(today) && !habit.isComplete(today)) Reminders.show(context, habit)
        Reminders.schedule(context, habit)
    }
}

/** Alarms are wiped on reboot and go stale on clock changes, so re-arm them. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.scheduleAll(context, HabitStore(context).load())
    }
}
