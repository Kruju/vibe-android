package com.kruju.habits.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kruju.habits.HabitViewModel
import com.kruju.habits.data.AppData
import com.kruju.habits.data.Habit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    data: AppData,
    today: Long,
    vm: HabitViewModel,
    onOpen: (Habit) -> Unit,
    onAdd: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var showReminder by remember { mutableStateOf(false) }
    var confirmImport by remember { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(today)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) toast(context, if (vm.exportTo(uri)) "Backup saved" else "Couldn't save the backup")
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            toast(context, if (vm.importFrom(uri)) "Backup restored" else "That file isn't a Habit Streaks backup")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Today")
                        Text(
                            date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showReminder = true }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Daily reminder")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Export backup") },
                                onClick = {
                                    menuOpen = false
                                    exportLauncher.launch("habit-streaks-$date.json")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Import backup") },
                                onClick = {
                                    menuOpen = false
                                    confirmImport = true
                                },
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New habit") },
            )
        },
    ) { padding ->
        if (data.habits.isEmpty()) {
            EmptyState(Modifier.padding(padding))
        } else {
            val doneCount = data.habits.count { it.isDone(today) }
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 8.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { ProgressCard(doneCount, data.habits.size) }
                items(data.habits, key = { it.id }) { habit ->
                    HabitRow(
                        habit = habit,
                        today = today,
                        onToggle = { vm.toggle(habit.id, today) },
                        onClick = { onOpen(habit) },
                    )
                }
            }
        }
    }

    if (showReminder) {
        ReminderDialog(
            current = data.reminder,
            onDismiss = { showReminder = false },
            onSave = {
                vm.setReminder(it)
                showReminder = false
            },
        )
    }
    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Import backup?") },
            text = { Text("Your current habits and history will be replaced by the ones in the backup file.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    importLauncher.launch(arrayOf("*/*"))
                }) { Text("Choose file") }
            },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ProgressCard(done: Int, total: Int) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                if (done == total) "All done today! 🎉" else "$done of $total done",
                style = MaterialTheme.typography.titleMedium,
            )
            LinearProgressIndicator(
                progress = { done.toFloat() / total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
            )
        }
    }
}

@Composable
private fun HabitRow(habit: Habit, today: Long, onToggle: () -> Unit, onClick: () -> Unit) {
    val color = habitColor(habit.color)
    val done = habit.isDone(today)
    val streak = habit.currentStreak(today)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (done) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(habit.emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                WeekDots(habit, today, color)
            }
            if (streak > 0) {
                Text(
                    "🔥 $streak",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
            CheckButton(done, color, onToggle)
        }
    }
}

@Composable
private fun WeekDots(habit: Habit, today: Long, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (day in today - 6..today) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    LocalDate.ofEpochDay(day).dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (habit.isDone(day)) color else MaterialTheme.colorScheme.outlineVariant)
                )
            }
        }
    }
}

@Composable
private fun CheckButton(done: Boolean, color: Color, onToggle: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (done) color else Color.Transparent)
            .border(2.dp, color, CircleShape)
            .clickable {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White)
    }
}

@Composable
private fun EmptyState(modifier: Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🌱", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("Start your first habit", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap “New habit”, then check it off every day to build a streak.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
