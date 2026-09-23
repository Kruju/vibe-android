package com.kruju.habits.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kruju.habits.data.AppData
import com.kruju.habits.data.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun TaskDialog(
    data: AppData,
    initial: Task?,
    defaultDay: Long,
    onDismiss: () -> Unit,
    onSave: (Task) -> Unit,
    onDelete: (() -> Unit)?,
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var note by rememberSaveable { mutableStateOf(initial?.note ?: "") }
    var categoryId by rememberSaveable { mutableStateOf(initial?.categoryId ?: "task") }
    var day by rememberSaveable { mutableLongStateOf(initial?.day ?: defaultDay) }
    var priority by rememberSaveable { mutableIntStateOf(initial?.priority ?: 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = { Text(if (initial == null) "New task" else "Edit task") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Task") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Category", style = MaterialTheme.typography.labelLarge, color = Palette.TextSoft)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(data.categories, key = { it.id }) { category ->
                        val selected = category.id == categoryId
                        Column(
                            Modifier
                                .width(68.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Palette.Card)
                                .border(1.5.dp, if (selected) Palette.Accent else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { categoryId = category.id }
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CategoryBadge(category, 32.dp)
                            Text(
                                category.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        val date = LocalDate.ofEpochDay(day)
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> day = LocalDate.of(y, m + 1, d).toEpochDay() },
                            date.year,
                            date.monthValue - 1,
                            date.dayOfMonth,
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Event, contentDescription = null)
                    Text(
                        "  " + LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text("Priority", style = MaterialTheme.typography.labelLarge, color = Palette.TextSoft)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (p in 0..2) Pill(priorityLabel(p), priority == p, { priority = p })
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(200) },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        Task(
                            id = initial?.id ?: System.currentTimeMillis(),
                            name = name.trim(),
                            note = note.trim(),
                            categoryId = categoryId,
                            day = day,
                            priority = priority,
                            done = initial?.done ?: false,
                        )
                    )
                },
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) { Text("Delete", color = Palette.Fail) }
                TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.TextSoft) }
            }
        },
    )
}
