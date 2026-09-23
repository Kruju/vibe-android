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
        private const val VERSION = 2

        fun toJson(data: AppData): String = JSONObject()
            .put("version", VERSION)
            .put("habits", JSONArray(data.habits.map { habitToJson(it) }))
            .put("tasks", JSONArray(data.tasks.map { taskToJson(it) }))
            .put("categories", JSONArray(data.customCategories.map { categoryToJson(it) }))
            .toString(1)

        /** Throws on anything that isn't a Habit Streaks backup. */
        fun fromJson(text: String): AppData {
            val root = JSONObject(text)
            if (root.optInt("version", 1) < 2) return fromV1(root)
            return AppData(
                habits = root.getJSONArray("habits").objects().map { habitFromJson(it) },
                tasks = (root.optJSONArray("tasks") ?: JSONArray()).objects().map { taskFromJson(it) },
                customCategories = (root.optJSONArray("categories") ?: JSONArray()).objects().map { categoryFromJson(it) },
            )
        }

        private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

        private fun JSONArray.ints(): List<Int> = (0 until length()).map { getInt(it) }

        private fun habitToJson(h: Habit): JSONObject {
            val entries = JSONObject()
            for ((day, value) in h.entries) entries.put(day.toString(), value)
            return JSONObject()
                .put("id", h.id)
                .put("name", h.name)
                .put("description", h.description)
                .put("category", h.categoryId)
                .put("type", h.type.name)
                .put("goal", h.goal)
                .put("unit", h.unit)
                .put("checklist", JSONArray(h.checklist))
                .put(
                    "frequency",
                    JSONObject()
                        .put("type", h.frequency.type.name)
                        .put("days", JSONArray(h.frequency.days.sorted()))
                        .put("count", h.frequency.count)
                )
                .put("start", h.startDay)
                .put("end", h.endDay ?: JSONObject.NULL)
                .put("reminder", h.reminder ?: JSONObject.NULL)
                .put("priority", h.priority)
                .put("isTask", h.isTask)
                .put("entries", entries)
        }

        private fun habitFromJson(o: JSONObject): Habit {
            val f = o.optJSONObject("frequency") ?: JSONObject()
            val entriesJson = o.optJSONObject("entries") ?: JSONObject()
            val entries = entriesJson.keys().asSequence().associate { it.toLong() to entriesJson.getDouble(it) }
            val checklist = o.optJSONArray("checklist") ?: JSONArray()
            return Habit(
                id = o.getLong("id"),
                name = o.getString("name"),
                description = o.optString("description"),
                categoryId = o.optString("category", FALLBACK_CATEGORY),
                type = enumValueOrNull<EvalType>(o.optString("type")) ?: EvalType.YES_NO,
                goal = o.optDouble("goal", 1.0),
                unit = o.optString("unit"),
                checklist = (0 until checklist.length()).map { checklist.getString(it) },
                frequency = Frequency(
                    type = enumValueOrNull<FreqType>(f.optString("type")) ?: FreqType.DAILY,
                    days = (f.optJSONArray("days") ?: JSONArray()).ints().toSet(),
                    count = f.optInt("count", 1).coerceAtLeast(1),
                ),
                startDay = o.optLong("start", todayEpochDay()),
                endDay = if (o.isNull("end")) null else o.optLong("end"),
                reminder = if (o.isNull("reminder")) null else o.optInt("reminder"),
                priority = o.optInt("priority", 1),
                isTask = o.optBoolean("isTask", false),
                entries = entries,
            )
        }

        private fun taskToJson(t: Task) = JSONObject()
            .put("id", t.id)
            .put("name", t.name)
            .put("note", t.note)
            .put("category", t.categoryId)
            .put("day", t.day)
            .put("priority", t.priority)
            .put("done", t.done)

        private fun taskFromJson(o: JSONObject) = Task(
            id = o.getLong("id"),
            name = o.getString("name"),
            note = o.optString("note"),
            categoryId = o.optString("category", "task"),
            day = o.optLong("day", todayEpochDay()),
            priority = o.optInt("priority", 1),
            done = o.optBoolean("done", false),
        )

        private fun categoryToJson(c: Category) = JSONObject()
            .put("id", c.id)
            .put("name", c.name)
            .put("icon", c.icon)
            .put("color", c.color)

        private fun categoryFromJson(o: JSONObject) = Category(
            id = o.getString("id"),
            name = o.getString("name"),
            icon = o.optString("icon", "category"),
            color = o.optLong("color", 0xFF78909C),
            custom = true,
        )

        /** Version 1 had simple yes/no habits with an emoji and one global reminder. */
        private fun fromV1(root: JSONObject): AppData {
            val reminder = root.optJSONObject("reminder")
            val reminderMinutes = if (reminder != null && reminder.optBoolean("enabled")) {
                reminder.optInt("hour", 20) * 60 + reminder.optInt("minute", 0)
            } else {
                null
            }
            val habits = root.getJSONArray("habits").objects().map { h ->
                val done = h.optJSONArray("done") ?: JSONArray()
                val days = (0 until done.length()).map { done.getLong(it) }
                val created = h.optLong("created", todayEpochDay())
                Habit(
                    id = h.getLong("id"),
                    name = h.getString("name"),
                    startDay = minOf(created, days.minOrNull() ?: created),
                    reminder = reminderMinutes,
                    entries = days.associateWith { 1.0 },
                )
            }
            return AppData(habits = habits)
        }

        private inline fun <reified T : Enum<T>> enumValueOrNull(name: String): T? =
            enumValues<T>().firstOrNull { it.name == name }
    }
}
