package com.kruju.habits.ui

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kruju.habits.data.EvalType
import com.kruju.habits.data.Habit
import com.kruju.habits.data.formatNumber
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Logs a value for numeric, timer and checklist habits on one day. */
@Composable
fun EntryDialog(habit: Habit, day: Long, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    val dateLabel = LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEE, d MMM"))
    var result by remember { mutableStateOf(habit.value(day)) }
    // Lives here rather than in TimerEntry so pressing OK while the stopwatch runs still counts that time.
    val runningSince = remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = {
            Column {
                Text(habit.name, fontWeight = FontWeight.Bold)
                Text(dateLabel, style = MaterialTheme.typography.bodyMedium, color = Palette.TextSoft)
            }
        },
        text = {
            when (habit.type) {
                EvalType.CHECKLIST -> ChecklistEntry(habit, result.toLong()) { result = it.toDouble() }
                EvalType.TIMER -> TimerEntry(habit, result, runningSince) { result = it }
                else -> NumericEntry(habit, result) { result = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val live = runningSince.value?.let { (SystemClock.elapsedRealtime() - it) / 60_000.0 } ?: 0.0
                onSave(result + live)
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.TextSoft) } },
    )
}

@Composable
private fun NumericEntry(habit: Habit, value: Double, onChange: (Double) -> Unit) {
    var text by remember { mutableStateOf(if (value == 0.0) "" else formatNumber(value)) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = {
                val next = ((text.toDoubleOrNull() ?: 0.0) - 1).coerceAtLeast(0.0)
                text = formatNumber(next)
                onChange(next)
            }) { Icon(Icons.Filled.Remove, contentDescription = "Less") }
            OutlinedTextField(
                value = text,
                onValueChange = { input ->
                    text = input.filter { it.isDigit() || it == '.' }.take(9)
                    onChange(text.toDoubleOrNull() ?: 0.0)
                },
                singleLine = true,
                placeholder = { Text("0") },
                textStyle = MaterialTheme.typography.headlineSmall.copy(textAlign = TextAlign.Center),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .width(140.dp)
                    .padding(horizontal = 8.dp),
            )
            FilledTonalIconButton(onClick = {
                val next = (text.toDoubleOrNull() ?: 0.0) + 1
                text = formatNumber(next)
                onChange(next)
            }) { Icon(Icons.Filled.Add, contentDescription = "More") }
        }
        Text(
            "Goal: at least ${formatNumber(habit.goal)} ${habit.unit}".trim(),
            color = Palette.TextSoft,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun TimerEntry(habit: Habit, minutes: Double, running: MutableState<Long?>, onChange: (Double) -> Unit) {
    var runningSince by running
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(runningSince) {
        while (runningSince != null) {
            now = SystemClock.elapsedRealtime()
            delay(250)
        }
    }
    val liveMs = runningSince?.let { now - it } ?: 0L
    val shownSeconds = (minutes * 60).toLong() + liveMs / 1000

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "%02d:%02d:%02d".format(shownSeconds / 3600, shownSeconds / 60 % 60, shownSeconds % 60),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = if (runningSince != null) Palette.Accent else Palette.Text,
        )
        Text("Goal: ${formatNumber(habit.goal)} min", color = Palette.TextSoft)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = {
                val started = runningSince
                if (started == null) {
                    runningSince = SystemClock.elapsedRealtime()
                } else {
                    onChange(minutes + (SystemClock.elapsedRealtime() - started) / 60_000.0)
                    runningSince = null
                }
            }) {
                Icon(
                    if (runningSince == null) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (runningSince == null) "Start" else "Pause",
                )
            }
            OutlinedButton(onClick = { onChange((minutes - 5).coerceAtLeast(0.0)) }, enabled = runningSince == null) { Text("−5 min") }
            OutlinedButton(onClick = { onChange(minutes + 5) }, enabled = runningSince == null) { Text("+5 min") }
        }
    }
}

@Composable
private fun ChecklistEntry(habit: Habit, mask: Long, onChange: (Long) -> Unit) {
    Column {
        habit.checklist.forEachIndexed { index, item ->
            val bit = 1L shl index
            val checked = mask and bit != 0L
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onChange(mask xor bit) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = checked, onCheckedChange = { onChange(mask xor bit) })
                Text(item, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
