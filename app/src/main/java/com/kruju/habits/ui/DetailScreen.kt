package com.kruju.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kruju.habits.data.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    habit: Habit,
    today: Long,
    onBack: () -> Unit,
    onToggle: (Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = habitColor(habit.color)
    val thisMonth = YearMonth.from(LocalDate.ofEpochDay(today))
    var month by remember { mutableStateOf(thisMonth) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${habit.emoji}  ${habit.name}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("🔥 ${habit.currentStreak(today)}", "Current streak", Modifier.weight(1f))
                StatCard("🏆 ${habit.bestStreak()}", "Best streak", Modifier.weight(1f))
                StatCard(
                    "${(habit.completionRate(today, 30) * 100).roundToInt()}%",
                    "Last 30 days",
                    Modifier.weight(1f),
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { month = month.minusMonths(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                        }
                        Text(
                            month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < thisMonth) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                        }
                    }
                    MonthGrid(habit, month, today, color, onToggle)
                }
            }

            Text(
                "Tap any past day to fix your history. Total check-ins: ${habit.doneDays.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete habit?") },
            text = { Text("“${habit.name}” and all of its history will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier) {
    Card(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, maxLines = 1)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthGrid(habit: Habit, month: YearMonth, today: Long, color: Color, onToggle: (Long) -> Unit) {
    val offset = month.atDay(1).dayOfWeek.value - 1 // Monday-first
    val length = month.lengthOfMonth()
    val weeks = (offset + length + 6) / 7

    Column {
        Row {
            for (dow in DayOfWeek.values()) {
                Text(
                    dow.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        for (week in 0 until weeks) {
            Row {
                for (col in 0 until 7) {
                    val dayOfMonth = week * 7 + col - offset + 1
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (dayOfMonth in 1..length) {
                            val day = month.atDay(dayOfMonth).toEpochDay()
                            DayCell(dayOfMonth, habit.isDone(day), day == today, day > today, color) {
                                onToggle(day)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(number: Int, done: Boolean, isToday: Boolean, future: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(if (done) color else Color.Transparent)
            .then(if (isToday) Modifier.border(2.dp, color, CircleShape) else Modifier)
            .clickable(enabled = !future, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "$number",
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                done -> Color.White
                future -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}
