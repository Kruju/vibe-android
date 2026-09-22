package com.kruju.habits

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import com.kruju.habits.data.AppData
import com.kruju.habits.data.Habit
import com.kruju.habits.data.HabitStore
import com.kruju.habits.data.ReminderSettings
import com.kruju.habits.data.Reminders
import com.kruju.habits.data.todayEpochDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HabitViewModel(app: Application) : AndroidViewModel(app) {
    private val store = HabitStore(app)

    private val _data = MutableStateFlow(store.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val _today = MutableStateFlow(todayEpochDay())
    val today: StateFlow<Long> = _today.asStateFlow()

    /** Called on resume so the "Today" screen rolls over after midnight. */
    fun refreshToday() {
        _today.value = todayEpochDay()
    }

    // The file is a few KB at most, so writing on the calling thread is fine and keeps saves in order.
    private fun update(change: (AppData) -> AppData) {
        val next = change(_data.value)
        _data.value = next
        store.save(next)
    }

    private fun updateHabit(id: Long, change: (Habit) -> Habit) = update { data ->
        data.copy(habits = data.habits.map { if (it.id == id) change(it) else it })
    }

    fun toggle(id: Long, day: Long) = updateHabit(id) { h ->
        h.copy(doneDays = if (day in h.doneDays) h.doneDays - day else h.doneDays + day)
    }

    fun add(name: String, emoji: String, color: Int) = update { data ->
        val habit = Habit(
            id = System.currentTimeMillis(),
            name = name.trim(),
            emoji = emoji,
            color = color,
            createdDay = _today.value,
        )
        data.copy(habits = data.habits + habit)
    }

    fun edit(id: Long, name: String, emoji: String, color: Int) = updateHabit(id) {
        it.copy(name = name.trim(), emoji = emoji, color = color)
    }

    fun delete(id: Long) = update { data -> data.copy(habits = data.habits.filterNot { it.id == id }) }


    fun setReminder(settings: ReminderSettings) {
        update { it.copy(reminder = settings) }
        Reminders.schedule(getApplication(), settings)
    }

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
        update { imported }
        Reminders.schedule(getApplication(), imported.reminder)
        true
    } catch (e: Exception) {
        false
    }
}
