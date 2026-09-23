package com.kruju.habits.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kruju.habits.data.AppData
import com.kruju.habits.data.Habit
import com.kruju.habits.data.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun HabitsScreen(data: AppData, today: Long, onOpen: (Habit) -> Unit, contentPadding: PaddingValues) {
    val habits = data.habits.filterNot { it.isTask }
    if (habits.isEmpty()) {
        EmptyState(
            Icons.Filled.WorkspacePremium,
            "No habits yet",
            "Tap + to create your first habit and start a streak.",
            Modifier.padding(contentPadding),
        )
        return
    }
    RecurringList(data, habits, today, onOpen, contentPadding, showStats = true)
}

@Composable
private fun RecurringList(
    data: AppData,
    items: List<Habit>,
    today: Long,
    onOpen: (Habit) -> Unit,
    contentPadding: PaddingValues,
    showStats: Boolean,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 88.dp,
        ),
    ) {
        items(items, key = { it.id }) { habit ->
            ItemRow(
                category = data.category(habit.categoryId),
                title = habit.name,
                onClick = { onOpen(habit) },
                subtitle = {
                    Text(habit.frequencyLabel(), style = MaterialTheme.typography.bodySmall, color = Palette.TextSoft)
                },
                trailing = {
                    if (showStats) {
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = Palette.Accent,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text("${habit.currentStreak(today)}", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "${(habit.successRate(today) * 100).roundToInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = Palette.TextSoft,
                            )
                        }
                    }
                },
            )
            RowDivider()
        }
    }
}

@Composable
fun TasksScreen(
    data: AppData,
    today: Long,
    onTaskToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    onRecurringClick: (Habit) -> Unit,
    contentPadding: PaddingValues,
) {
    var recurring by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(top = contentPadding.calculateTopPadding())) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Pill("Single tasks", !recurring, { recurring = false })
            Pill("Recurring tasks", recurring, { recurring = true })
        }
        val inner = PaddingValues(bottom = contentPadding.calculateBottomPadding())
        if (recurring) {
            val items = data.habits.filter { it.isTask }
            if (items.isEmpty()) {
                EmptyState(Icons.Filled.Repeat, "No recurring tasks", "Recurring tasks repeat on a schedule but have no streaks.", Modifier.padding(inner))
            } else {
                RecurringList(data, items, today, onRecurringClick, inner, showStats = false)
            }
        } else {
            SingleTaskList(data, today, onTaskToggle, onTaskClick, inner)
        }
    }
}

@Composable
private fun SingleTaskList(
    data: AppData,
    today: Long,
    onToggle: (Task) -> Unit,
    onClick: (Task) -> Unit,
    contentPadding: PaddingValues,
) {
    if (data.tasks.isEmpty()) {
        EmptyState(Icons.Filled.CheckCircle, "No tasks", "Single tasks are one-off to-dos with a date.", Modifier.padding(contentPadding))
        return
    }
    val pending = data.tasks.filterNot { it.done }.sortedWith(compareBy<Task> { it.day }.thenByDescending { it.priority })
    val done = data.tasks.filter { it.done }.sortedByDescending { it.day }
    val format = DateTimeFormatter.ofPattern("EEE, d MMM")

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 88.dp),
    ) {
        for ((title, list) in listOf("Pending" to pending, "Completed" to done)) {
            if (list.isEmpty()) continue
            item(key = title) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Palette.Accent,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            items(list, key = { it.id }) { task ->
                ItemRow(
                    category = data.category(task.categoryId),
                    title = task.name,
                    onClick = { onClick(task) },
                    dim = task.done,
                    subtitle = {
                        val overdue = !task.done && task.day < today
                        Text(
                            when (task.day) {
                                today -> "Today"
                                today + 1 -> "Tomorrow"
                                today - 1 -> "Yesterday"
                                else -> LocalDate.ofEpochDay(task.day).format(format)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overdue) Palette.Fail else Palette.TextSoft,
                        )
                        if (task.priority == 2) Tag("High priority", Palette.Accent)
                    },
                    trailing = {
                        StatusCircle(0f, task.done, missed = !task.done && task.day < today, onClick = { onToggle(task) })
                    },
                )
                RowDivider()
            }
        }
    }
}
