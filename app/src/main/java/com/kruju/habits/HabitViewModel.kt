package com.kruju.habits

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import com.kruju.habits.data.AppData
import com.kruju.habits.data.Category
import com.kruju.habits.data.EvalType
import com.kruju.habits.data.FALLBACK_CATEGORY
import com.kruju.habits.data.Habit
import com.kruju.habits.data.HabitStore
import com.kruju.habits.data.Reminders
import com.kruju.habits.data.Task
import com.kruju.habits.data.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Stopwatch/countdown state. Times come from elapsedRealtime so the timer keeps going while the app is closed. */
data class TimerState(
    val countdown: Boolean = false,
    val durationMs: Long = 25 * 60_000L,
    val runningSince: Long? = null,
    val accumulatedMs: Long = 0,
) {
    val running: Boolean get() = runningSince != null

    fun elapsed(now: Long = SystemClock.elapsedRealtime()): Long =
        accumulatedMs + (runningSince?.let { now - it } ?: 0)
}

class HabitViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HabitStore(app)

    private val _data = MutableStateFlow(store.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _today = MutableStateFlow(todayEpochDay())
    val today: StateFlow<Long> = _today.asStateFlow()

    private val _timer = MutableStateFlow(TimerState())
    val timer: StateFlow<TimerState> = _timer.asStateFlow()

    init {
        // Re-arm on launch too, e.g. after an update or data migration.
        Reminders.scheduleAll(app, _data.value)
    }

    /** Called on resume so "today" rolls over after midnight. */
    fun refreshToday() {
        _today.value = todayEpochDay()
    }

    // The file is small, so writing on the calling thread is fine and keeps saves in order.
    private fun update(change: (AppData) -> AppData) {
        val next = change(_data.value)
        _data.value = next
        store.save(next)
    }

    private fun updateHabit(id: Long, change: (Habit) -> Habit) = update { data ->
        data.copy(habits = data.habits.map { if (it.id == id) change(it) else it })
    }

    // Habits and recurring tasks

    fun saveHabit(habit: Habit) {
        update { data ->
            val exists = data.habits.any { it.id == habit.id }
            data.copy(habits = if (exists) data.habits.map { if (it.id == habit.id) habit else it } else data.habits + habit)
        }
        Reminders.schedule(getApplication(), habit)
    }

    fun deleteHabit(id: Long) {
        update { data -> data.copy(habits = data.habits.filterNot { it.id == id }) }
        Reminders.cancel(getApplication(), id)
    }

    fun setEntry(id: Long, day: Long, value: Double) = updateHabit(id) { h ->
        h.copy(entries = if (value <= 0.0) h.entries - day else h.entries + (day to value))
    }

    /** One-tap action from lists: yes/no toggles; other types are handled by dialogs. */
    fun toggleYesNo(id: Long, day: Long) = updateHabit(id) { h ->
        if (h.type != EvalType.YES_NO) h
        else h.copy(entries = if (h.isComplete(day)) h.entries - day else h.entries + (day to 1.0))
    }

    // Single tasks

    fun saveTask(task: Task) = update { data ->
        val exists = data.tasks.any { it.id == task.id }
        data.copy(tasks = if (exists) data.tasks.map { if (it.id == task.id) task else it } else data.tasks + task)
    }

    fun toggleTask(id: Long) = update { data ->
        data.copy(tasks = data.tasks.map { if (it.id == id) it.copy(done = !it.done) else it })
    }

    fun deleteTask(id: Long) = update { data -> data.copy(tasks = data.tasks.filterNot { it.id == id }) }

    // Categories

    fun saveCategory(category: Category) = update { data ->
        val exists = data.customCategories.any { it.id == category.id }
        data.copy(
            customCategories = if (exists) data.customCategories.map { if (it.id == category.id) category else it }
            else data.customCategories + category
        )
    }

    /** Anything in a deleted category moves to "Other". */
    fun deleteCategory(id: String) = update { data ->
        data.copy(
            customCategories = data.customCategories.filterNot { it.id == id },
            habits = data.habits.map { if (it.categoryId == id) it.copy(categoryId = FALLBACK_CATEGORY) else it },
            tasks = data.tasks.map { if (it.categoryId == id) it.copy(categoryId = FALLBACK_CATEGORY) else it },
        )
    }

    // Timer

    fun setTimerMode(countdown: Boolean) {
        _timer.value = TimerState(countdown = countdown, durationMs = _timer.value.durationMs)
    }

    fun setCountdownDuration(ms: Long) {
        _timer.value = _timer.value.copy(durationMs = ms.coerceIn(60_000L, 5 * 3_600_000L), accumulatedMs = 0, runningSince = null)
    }

    fun startPauseTimer() {
        val t = _timer.value
        val now = SystemClock.elapsedRealtime()
        _timer.value = if (t.running) t.copy(accumulatedMs = t.elapsed(now), runningSince = null) else t.copy(runningSince = now)
    }

    fun resetTimer() {
        _timer.value = _timer.value.copy(accumulatedMs = 0, runningSince = null)
    }

    // Backup

    fun exportTo(uri: Uri): Boolean = try {
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use {
            it.write(HabitStore.toJson(_data.value).toByteArray(Charsets.UTF_8))
        }
        true
    } catch (e: Exception) {
        false
    }

    fun importFrom(uri: Uri): Boolean = try {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)!!.use {
            it.readBytes().toString(Charsets.UTF_8)
        }
        val imported = HabitStore.fromJson(text)
        _data.value.habits.forEach { Reminders.cancel(getApplication(), it.id) }
        update { imported }
        Reminders.scheduleAll(getApplication(), imported)
        true
    } catch (e: Exception) {
        false
    }
}
