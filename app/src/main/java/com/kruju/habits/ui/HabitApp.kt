package com.kruju.habits.ui

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kruju.habits.HabitViewModel

@Composable
fun HabitApp(vm: HabitViewModel) {
    val data by vm.data.collectAsState()
    val today by vm.today.collectAsState()
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }

    val open = openId?.let { id -> data.habits.find { it.id == id } }
    if (open != null) {
        BackHandler { openId = null }
        DetailScreen(
            habit = open,
            today = today,
            onBack = { openId = null },
            onToggle = { day -> vm.toggle(open.id, day) },
            onEdit = { editingId = open.id },
            onDelete = {
                openId = null
                vm.delete(open.id)
            },
        )
    } else {
        TodayScreen(
            data = data,
            today = today,
            vm = vm,
            onOpen = { openId = it.id },
            onAdd = { adding = true },
        )
    }

    if (adding) {
        HabitDialog(
            initial = null,
            onDismiss = { adding = false },
            onSave = { name, emoji, color ->
                vm.add(name, emoji, color)
                adding = false
            },
        )
    }
    data.habits.find { it.id == editingId }?.let { habit ->
        HabitDialog(
            initial = habit,
            onDismiss = { editingId = null },
            onSave = { name, emoji, color ->
                vm.edit(habit.id, name, emoji, color)
                editingId = null
            },
        )
    }
}

fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
