package com.kruju.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kruju.habits.data.AppData
import com.kruju.habits.data.EvalType
import com.kruju.habits.data.Habit
import com.kruju.habits.data.Task
import com.kruju.habits.data.formatNumber
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private const val DAYS_BACK = 60L
private const val DAYS_AHEAD = 30L

@Composable
fun TodayScreen(
    data: AppData,
    today: Long,
    selected: Long,
    onSelect: (Long) -> Unit,
    onHabitStatus: (Habit, Long) -> Unit,
    onHabitClick: (Habit) -> Unit,
    onTaskToggle: (Task) -> Unit,
    onTaskClick: (Task) -> Unit,
    contentPadding: PaddingValues,
) {
    val habits = data.habits.filter { it.isScheduled(selected) }.sortedByDescending { it.priority }
    val tasks = data.tasks.filter { it.day == selected }.sortedWith(compareBy<Task> { it.done }.thenByDescending { it.priority })

    Column(Modifier.padding(top = contentPadding.calculateTopPadding())) {
        DateStrip(today, selected, onSelect)
        if (habits.isEmpty() && tasks.isEmpty()) {
            EmptyState(
                Icons.Filled.EventAvailable,
                "Nothing scheduled",
                "Tap + to add a habit or a task for this day.",
                Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
            )
        } else LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 88.dp),
        ) {
            items(habits, key = { "h${it.id}" }) { habit ->
                HabitDayRow(habit, selected, today, onHabitStatus, onHabitClick, data)
                RowDivider()
            }
            items(tasks, key = { "t${it.id}" }) { task ->
                ItemRow(
                    category = data.category(task.categoryId),
                    title = task.name,
                    onClick = { onTaskClick(task) },
                    dim = task.done,
                    subtitle = { Tag("Task", Palette.Accent) },
                    trailing = {
                        StatusCircle(
                            progress = 0f,
                            done = task.done,
                            missed = !task.done && selected < today,
                            onClick = { onTaskToggle(task) },
                        )
                    },
                )
                RowDivider()
            }
        }
    }
}

@Composable
private fun HabitDayRow(
    habit: Habit,
    day: Long,
    today: Long,
    onStatus: (Habit, Long) -> Unit,
    onClick: (Habit) -> Unit,
    data: AppData,
) {
    val done = habit.isComplete(day)
    ItemRow(
        category = data.category(habit.categoryId),
        title = habit.name,
        onClick = { onClick(habit) },
        subtitle = {
            Tag(if (habit.isTask) "Recurring task" else "Habit", Palette.Accent)
            progressText(habit, day)?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = Palette.TextSoft)
            }
        },
        trailing = {
            StatusCircle(
                progress = habit.progress(day),
                done = done,
                missed = !done && day < today,
                onClick = { if (day <= today) onStatus(habit, day) },
            )
        },
    )
}

fun progressText(habit: Habit, day: Long): String? = when (habit.type) {
    EvalType.YES_NO -> null
    EvalType.NUMERIC -> "${formatNumber(habit.value(day))}/${formatNumber(habit.goal)} ${habit.unit}".trim()
    EvalType.TIMER -> "${formatNumber(Math.floor(habit.value(day)))}/${formatNumber(habit.goal)} min"
    EvalType.CHECKLIST -> "${habit.checkedCount(day)}/${habit.checklist.size}"
}

@Composable
private fun DateStrip(today: Long, selected: Long, onSelect: (Long) -> Unit) {
    val days = (today - DAYS_BACK..today + DAYS_AHEAD).toList()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (DAYS_BACK - 3).toInt())
    LaunchedEffect(today) {
        state.scrollToItem((DAYS_BACK - 3).toInt().coerceAtLeast(0))
    }
    LazyRow(
        state = state,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(days, key = { it }) { day ->
            val date = LocalDate.ofEpochDay(day)
            val isSelected = day == selected
            Column(
                Modifier
                    .width(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSelected) Palette.Accent else Palette.Card)
                    .clickable { onSelect(day) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) Color.White else Palette.TextSoft,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else Palette.Text,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                day != today -> Color.Transparent
                                isSelected -> Color.White
                                else -> Palette.Accent
                            }
                        )
                )
            }
        }
    }
}
