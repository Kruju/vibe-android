package com.kruju.habits.data

import java.time.LocalDate

/** Days are stored as epoch days (LocalDate.toEpochDay), which keeps the data tiny and timezone-free. */
data class Habit(
    val id: Long,
    val name: String,
    val emoji: String,
    val color: Int,
    val createdDay: Long,
    val doneDays: Set<Long> = emptySet(),
) {
    fun isDone(day: Long) = day in doneDays

    /** Consecutive days up to today. An unticked today doesn't break the streak until the day is over. */
    fun currentStreak(today: Long): Int {
        var day = if (isDone(today)) today else today - 1
        var streak = 0
        while (day in doneDays) {
            streak++
            day--
        }
        return streak
    }

    fun bestStreak(): Int {
        var best = 0
        var run = 0
        var previous = Long.MIN_VALUE
        for (day in doneDays.sorted()) {
            run = if (day == previous + 1) run + 1 else 1
            if (run > best) best = run
            previous = day
        }
        return best
    }

    /** Share of days completed over the last [days] days, not counting days before the habit started. */
    fun completionRate(today: Long, days: Int): Float {
        val start = maxOf(today - days + 1, firstDay())
        if (start > today) return 0f
        val done = doneDays.count { it in start..today }
        return done.toFloat() / (today - start + 1)
    }

    private fun firstDay(): Long = minOf(createdDay, doneDays.minOrNull() ?: createdDay)
}

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 20,
    val minute: Int = 0,
)

data class AppData(
    val habits: List<Habit> = emptyList(),
    val reminder: ReminderSettings = ReminderSettings(),
)

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()
