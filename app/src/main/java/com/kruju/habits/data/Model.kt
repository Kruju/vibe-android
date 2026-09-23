package com.kruju.habits.data

import java.time.LocalDate

/** Days are stored as epoch days (LocalDate.toEpochDay), which keeps the data tiny and timezone-free. */

enum class EvalType { YES_NO, NUMERIC, TIMER, CHECKLIST }

enum class FreqType { DAILY, WEEK_DAYS, TIMES_PER_WEEK, MONTH_DAYS, EVERY_N_DAYS }

/**
 * WEEK_DAYS: [days] holds ISO day-of-week numbers (Mon = 1 … Sun = 7).
 * MONTH_DAYS: [days] holds days of the month (1..31).
 * TIMES_PER_WEEK / EVERY_N_DAYS: [count] holds the number.
 */
data class Frequency(
    val type: FreqType = FreqType.DAILY,
    val days: Set<Int> = emptySet(),
    val count: Int = 1,
)

data class Category(
    val id: String,
    val name: String,
    val icon: String,
    val color: Long,
    val custom: Boolean = false,
)

val DefaultCategories = listOf(
    Category("quit", "Quit a bad habit", "block", 0xFFF4511E),
    Category("art", "Art", "brush", 0xFFE91E63),
    Category("meditation", "Meditation", "meditation", 0xFFAB47BC),
    Category("study", "Study", "school", 0xFF5C6BC0),
    Category("sports", "Sports", "bike", 0xFF1E88E5),
    Category("entertainment", "Entertainment", "movie", 0xFF00ACC1),
    Category("social", "Social", "forum", 0xFF26A69A),
    Category("finance", "Finance", "money", 0xFF43A047),
    Category("health", "Health", "heart", 0xFFE53935),
    Category("work", "Work", "work", 0xFF8D6E63),
    Category("nutrition", "Nutrition", "restaurant", 0xFFFB8C00),
    Category("home", "Home", "home", 0xFFFFB300),
    Category("outdoor", "Outdoor", "terrain", 0xFF7CB342),
    Category("task", "Task", "task", 0xFFD81B60),
    Category("other", "Other", "category", 0xFF78909C),
)

const val FALLBACK_CATEGORY = "other"

data class Habit(
    val id: Long,
    val name: String,
    val description: String = "",
    val categoryId: String = FALLBACK_CATEGORY,
    val type: EvalType = EvalType.YES_NO,
    /** Target for NUMERIC (in [unit]) and TIMER (in minutes). */
    val goal: Double = 1.0,
    val unit: String = "",
    val checklist: List<String> = emptyList(),
    val frequency: Frequency = Frequency(),
    val startDay: Long,
    val endDay: Long? = null,
    /** Minutes after midnight, or null for no reminder. */
    val reminder: Int? = null,
    val priority: Int = 1,
    /** Recurring tasks work like habits but are listed under Tasks and have no statistics. */
    val isTask: Boolean = false,
    /**
     * Per-day value: 1.0 for a ticked yes/no habit, the amount for numeric,
     * minutes for timer, and a bit mask of ticked items for checklist.
     */
    val entries: Map<Long, Double> = emptyMap(),
) {
    fun value(day: Long): Double = entries[day] ?: 0.0

    fun isActive(day: Long): Boolean = day >= startDay && (endDay == null || day <= endDay)

    fun isScheduled(day: Long): Boolean {
        if (!isActive(day)) return false
        val date = LocalDate.ofEpochDay(day)
        return when (frequency.type) {
            FreqType.DAILY, FreqType.TIMES_PER_WEEK -> true
            FreqType.WEEK_DAYS -> date.dayOfWeek.value in frequency.days
            FreqType.MONTH_DAYS -> date.dayOfMonth in frequency.days
            FreqType.EVERY_N_DAYS -> (day - startDay) % frequency.count.coerceAtLeast(1) == 0L
        }
    }

    fun checkedCount(day: Long): Int {
        val mask = value(day).toLong()
        return checklist.indices.count { mask and (1L shl it) != 0L }
    }

    fun isComplete(day: Long): Boolean = when (type) {
        EvalType.YES_NO -> value(day) >= 1.0
        EvalType.NUMERIC, EvalType.TIMER -> value(day) >= goal
        EvalType.CHECKLIST -> checklist.isNotEmpty() && checkedCount(day) == checklist.size
    }

    /** 0..1, used for the partial progress ring. */
    fun progress(day: Long): Float = when (type) {
        EvalType.YES_NO -> if (isComplete(day)) 1f else 0f
        EvalType.NUMERIC, EvalType.TIMER -> if (goal <= 0) 1f else (value(day) / goal).toFloat().coerceIn(0f, 1f)
        EvalType.CHECKLIST -> if (checklist.isEmpty()) 0f else checkedCount(day).toFloat() / checklist.size
    }

    private fun weekStart(day: Long): Long = day - (LocalDate.ofEpochDay(day).dayOfWeek.value - 1)

    private fun completionsIn(from: Long, to: Long): Int =
        (maxOf(from, startDay)..to).count { isComplete(it) }

    /** Consecutive completed scheduled days. An unfinished today doesn't break the streak yet. */
    fun currentStreak(today: Long): Int {
        if (frequency.type == FreqType.TIMES_PER_WEEK) {
            var streak = completionsIn(weekStart(today), today)
            var week = weekStart(today) - 7
            while (week + 6 >= startDay) {
                val done = completionsIn(week, week + 6)
                if (done < frequency.count) break
                streak += done
                week -= 7
            }
            return streak
        }
        var day = if (isComplete(today)) today else today - 1
        var streak = 0
        while (day >= startDay) {
            if (isScheduled(day)) {
                if (isComplete(day)) streak++ else break
            }
            day--
        }
        return streak
    }

    fun bestStreak(today: Long): Int {
        var best = 0
        var run = 0
        if (frequency.type == FreqType.TIMES_PER_WEEK) {
            var week = weekStart(startDay)
            while (week <= today) {
                val done = completionsIn(week, minOf(week + 6, today))
                val weekOver = week + 6 < today
                run = if (weekOver && done < frequency.count) 0 else run + done
                best = maxOf(best, run)
                week += 7
            }
            return best
        }
        for (day in startDay..today) {
            if (!isScheduled(day)) continue
            if (isComplete(day)) {
                run++
                best = maxOf(best, run)
            } else if (day < today) {
                run = 0
            }
        }
        return best
    }

    /** Share of scheduled days completed so far. Today only counts once it's done. */
    fun successRate(today: Long): Float {
        val last = minOf(today, endDay ?: today)
        if (last < startDay) return 0f
        if (frequency.type == FreqType.TIMES_PER_WEEK) {
            var got = 0
            var wanted = 0
            var week = weekStart(startDay)
            while (week <= last) {
                val done = completionsIn(week, minOf(week + 6, last))
                val weekOver = week + 6 < today
                if (weekOver || done >= frequency.count) {
                    got += minOf(done, frequency.count)
                    wanted += frequency.count
                }
                week += 7
            }
            return if (wanted == 0) 0f else got.toFloat() / wanted
        }
        var done = 0
        var total = 0
        for (day in startDay..last) {
            if (!isScheduled(day)) continue
            if (isComplete(day)) {
                done++
                total++
            } else if (day < today) {
                total++
            }
        }
        return if (total == 0) 0f else done.toFloat() / total
    }

    fun completionsBetween(from: Long, to: Long): Int = entries.keys.count { it in from..to && isComplete(it) }

    fun frequencyLabel(): String = when (frequency.type) {
        FreqType.DAILY -> "Every day"
        FreqType.WEEK_DAYS -> if (frequency.days.size == 7) "Every day" else
            frequency.days.sorted().joinToString(", ") { WeekDayShort[it - 1] }
        FreqType.TIMES_PER_WEEK -> "${frequency.count} times per week"
        FreqType.MONTH_DAYS -> "Days " + frequency.days.sorted().joinToString(", ") + " of the month"
        FreqType.EVERY_N_DAYS -> if (frequency.count == 1) "Every day" else "Every ${frequency.count} days"
    }
}

val WeekDayShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

data class Task(
    val id: Long,
    val name: String,
    val note: String = "",
    val categoryId: String = "task",
    val day: Long,
    val priority: Int = 1,
    val done: Boolean = false,
)

data class AppData(
    val habits: List<Habit> = emptyList(),
    val tasks: List<Task> = emptyList(),
    /** Only user-made categories; the defaults live in code. */
    val customCategories: List<Category> = emptyList(),
) {
    val categories: List<Category> get() = DefaultCategories + customCategories

    fun category(id: String): Category =
        customCategories.find { it.id == id }
            ?: DefaultCategories.find { it.id == id }
            ?: DefaultCategories.last()
}

fun todayEpochDay(): Long = LocalDate.now().toEpochDay()

fun formatNumber(value: Double): String =
    if (value == Math.floor(value)) value.toLong().toString() else "%.1f".format(value)
