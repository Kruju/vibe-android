package com.kruju.habits.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Keeps everything in a single JSON file in the app's private storage. Nothing leaves the phone. */
class HabitStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "habits.json"))

    @Synchronized
    fun load(): AppData = try {
        fromJson(String(file.readFully(), Charsets.UTF_8))
    } catch (e: Exception) {
        AppData()
    }

    @Synchronized
    fun save(data: AppData) {
        val out = file.startWrite()
        try {
            out.write(toJson(data).toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    companion object {
        private const val VERSION = 1

        fun toJson(data: AppData): String {
            val habits = JSONArray()
            for (h in data.habits) {
                habits.put(
                    JSONObject()
                        .put("id", h.id)
                        .put("name", h.name)
                        .put("emoji", h.emoji)
                        .put("color", h.color)
                        .put("created", h.createdDay)
                        .put("done", JSONArray(h.doneDays.sorted()))
                )
            }
            val reminder = JSONObject()
                .put("enabled", data.reminder.enabled)
                .put("hour", data.reminder.hour)
                .put("minute", data.reminder.minute)
            return JSONObject()
                .put("version", VERSION)
                .put("habits", habits)
                .put("reminder", reminder)
                .toString(2)
        }

        /** Throws on anything that isn't a Habit Streaks backup. */
        fun fromJson(text: String): AppData {
            val root = JSONObject(text)
            val habitsJson = root.getJSONArray("habits")
            val habits = (0 until habitsJson.length()).map { i ->
                val h = habitsJson.getJSONObject(i)
                val done = h.optJSONArray("done") ?: JSONArray()
                Habit(
                    id = h.getLong("id"),
                    name = h.getString("name"),
                    emoji = h.optString("emoji", "✅"),
                    color = h.optInt("color", 0),
                    createdDay = h.optLong("created", todayEpochDay()),
                    doneDays = (0 until done.length()).map { done.getLong(it) }.toSet(),
                )
            }
            val r = root.optJSONObject("reminder")
            val reminder = if (r == null) ReminderSettings() else ReminderSettings(
                enabled = r.optBoolean("enabled", false),
                hour = r.optInt("hour", 20).coerceIn(0, 23),
                minute = r.optInt("minute", 0).coerceIn(0, 59),
            )
            return AppData(habits, reminder)
        }
    }
}
