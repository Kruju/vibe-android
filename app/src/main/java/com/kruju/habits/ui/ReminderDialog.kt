package com.kruju.habits.ui

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kruju.habits.data.ReminderSettings
import com.kruju.habits.data.Reminders
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ReminderDialog(current: ReminderSettings, onDismiss: () -> Unit, onSave: (ReminderSettings) -> Unit) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(current.enabled) }
    var hour by remember { mutableIntStateOf(current.hour) }
    var minute by remember { mutableIntStateOf(current.minute) }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            enabled = false
            toast(context, "Notifications are blocked. You can allow them in system settings.")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily reminder") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Get a nudge when you still have habits left to check off.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Remind me", style = MaterialTheme.typography.titleMedium)
                    Switch(
                        checked = enabled,
                        onCheckedChange = { on ->
                            enabled = on
                            if (on && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canNotify(context)) {
                                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
                OutlinedButton(
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                hour = h
                                minute = m
                            },
                            hour,
                            minute,
                            DateFormat.is24HourFormat(context),
                        ).show()
                    },
                ) {
                    Text("At " + LocalTime.of(hour, minute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(ReminderSettings(enabled, hour, minute)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
