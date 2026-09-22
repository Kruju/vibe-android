package com.kruju.habits.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kruju.habits.data.Habit

private val Emojis = listOf(
    "💧", "📖", "🏃", "🧘", "💪",
    "🥗", "😴", "🦷", "💊", "🚶",
    "🎸", "✍️", "🧹", "💻", "🌱",
    "📵", "☕", "🙏", "🧠", "💰",
)

@Composable
fun HabitDialog(initial: Habit?, onDismiss: () -> Unit, onSave: (name: String, emoji: String, color: Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var emoji by rememberSaveable { mutableStateOf(initial?.emoji ?: Emojis.first()) }
    var color by rememberSaveable { mutableIntStateOf(initial?.color ?: 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New habit" else "Edit habit") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(40) },
                    label = { Text("Name") },
                    placeholder = { Text("e.g. Drink water") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                for (row in Emojis.chunked(5)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (option in row) {
                            Box(
                                Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (option == emoji) habitColor(color).copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable { emoji = option },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(option, fontSize = 22.sp)
                            }
                        }
                    }
                }
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HabitColors.forEachIndexed { index, option ->
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(option)
                                .clickable { color = index },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (index == color) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, emoji, color) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
