package com.kruju.habits.ui

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ThumbsUpDown
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kruju.habits.data.AppData
import com.kruju.habits.data.EvalType
import com.kruju.habits.data.FreqType
import com.kruju.habits.data.Frequency
import com.kruju.habits.data.Habit
import com.kruju.habits.data.Reminders
import com.kruju.habits.data.WeekDayShort
import com.kruju.habits.data.formatNumber
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val STEP_CATEGORY = 0
private const val STEP_EVALUATION = 1
private const val STEP_DEFINE = 2
private const val STEP_FREQUENCY = 3
private const val STEP_WHEN = 4

/** Step-by-step creation (and editing) of a habit or recurring task. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitWizard(
    data: AppData,
    initial: Habit?,
    isTask: Boolean,
    today: Long,
    onClose: () -> Unit,
    onSave: (Habit) -> Unit,
) {
    val context = LocalContext.current
    val noun = if (isTask) "task" else "habit"
    var step by rememberSaveable { mutableIntStateOf(if (initial == null) STEP_CATEGORY else STEP_DEFINE) }

    var categoryId by rememberSaveable { mutableStateOf(initial?.categoryId ?: if (isTask) "task" else "other") }
    var type by rememberSaveable { mutableStateOf(initial?.type ?: EvalType.YES_NO) }
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var description by rememberSaveable { mutableStateOf(initial?.description ?: "") }
    var goal by rememberSaveable { mutableStateOf(initial?.takeIf { it.type != EvalType.YES_NO }?.goal?.let { formatNumber(it) } ?: "") }
    var unit by rememberSaveable { mutableStateOf(initial?.unit ?: "") }
    val checklist = remember { mutableStateListOf<String>().apply { addAll(initial?.checklist.orEmpty()) } }
    var freqType by rememberSaveable { mutableStateOf(initial?.frequency?.type ?: FreqType.DAILY) }
    val weekDays = remember {
        mutableStateListOf<Int>().apply {
            addAll(initial?.frequency?.takeIf { it.type == FreqType.WEEK_DAYS }?.days ?: (1..5).toSet())
        }
    }
    val monthDays = remember {
        mutableStateListOf<Int>().apply {
            addAll(initial?.frequency?.takeIf { it.type == FreqType.MONTH_DAYS }?.days ?: setOf(1))
        }
    }
    var timesPerWeek by rememberSaveable {
        mutableIntStateOf(initial?.frequency?.takeIf { it.type == FreqType.TIMES_PER_WEEK }?.count ?: 3)
    }
    var everyN by rememberSaveable {
        mutableIntStateOf(initial?.frequency?.takeIf { it.type == FreqType.EVERY_N_DAYS }?.count ?: 2)
    }
    var startDay by rememberSaveable { mutableStateOf(initial?.startDay ?: today) }
    var endDay by rememberSaveable { mutableStateOf(initial?.endDay) }
    var reminder by rememberSaveable { mutableStateOf(initial?.reminder) }
    var priority by rememberSaveable { mutableIntStateOf(initial?.priority ?: 1) }

    val canContinue = when (step) {
        STEP_DEFINE -> name.isNotBlank() && when (type) {
            EvalType.NUMERIC, EvalType.TIMER -> (goal.toDoubleOrNull() ?: 0.0) > 0
            EvalType.CHECKLIST -> checklist.isNotEmpty()
            EvalType.YES_NO -> true
        }
        STEP_FREQUENCY -> when (freqType) {
            FreqType.WEEK_DAYS -> weekDays.isNotEmpty()
            FreqType.MONTH_DAYS -> monthDays.isNotEmpty()
            else -> true
        }
        else -> true
    }

    fun build() = Habit(
        id = initial?.id ?: System.currentTimeMillis(),
        name = name.trim(),
        description = description.trim(),
        categoryId = categoryId,
        type = type,
        goal = if (type == EvalType.YES_NO || type == EvalType.CHECKLIST) 1.0 else goal.toDouble(),
        unit = if (type == EvalType.NUMERIC) unit.trim() else "",
        checklist = if (type == EvalType.CHECKLIST) checklist.toList() else emptyList(),
        frequency = when (freqType) {
            FreqType.WEEK_DAYS -> Frequency(freqType, days = weekDays.toSet())
            FreqType.MONTH_DAYS -> Frequency(freqType, days = monthDays.toSet())
            FreqType.TIMES_PER_WEEK -> Frequency(freqType, count = timesPerWeek)
            FreqType.EVERY_N_DAYS -> Frequency(freqType, count = everyN)
            FreqType.DAILY -> Frequency()
        },
        startDay = startDay,
        endDay = endDay?.takeIf { it >= startDay },
        reminder = reminder,
        priority = priority,
        isTask = isTask,
        entries = initial?.entries ?: emptyMap(),
    )

    fun back() {
        if (step == STEP_CATEGORY || (initial != null && step == STEP_DEFINE)) onClose() else step--
    }
    BackHandler { back() }

    val title = when (step) {
        STEP_CATEGORY -> "Select a category"
        STEP_EVALUATION -> "How do you want to evaluate your progress?"
        STEP_DEFINE -> "Define your $noun"
        STEP_FREQUENCY -> "How often do you want to do it?"
        else -> "When do you want to do it?"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Close") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Palette.Background),
            )
        },
        bottomBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.Surface)
                    .navigationBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { back() }) {
                    Text(if (step == STEP_CATEGORY || (initial != null && step == STEP_DEFINE)) "CANCEL" else "BACK", color = Palette.TextSoft)
                }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                    for (i in 0..STEP_WHEN) {
                        Box(
                            Modifier
                                .padding(3.dp)
                                .size(if (i == step) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (i <= step) Palette.Accent else Palette.TextFaint)
                        )
                    }
                }
                TextButton(
                    enabled = canContinue,
                    onClick = { if (step == STEP_WHEN) onSave(build()) else step++ },
                ) {
                    Text(if (step == STEP_WHEN) "SAVE" else "NEXT", fontWeight = FontWeight.Bold)
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (step) {
                STEP_CATEGORY -> CategoryStep(data, categoryId) {
                    categoryId = it
                    step = STEP_EVALUATION
                }
                STEP_EVALUATION -> EvaluationStep(type) {
                    type = it
                    step = STEP_DEFINE
                }
                STEP_DEFINE -> Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it.take(60) },
                        label = { Text(if (isTask) "Task" else "Habit") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    when (type) {
                        EvalType.NUMERIC -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            NumberField(goal, { goal = it }, "Goal (at least)", Modifier.weight(1f))
                            OutlinedTextField(
                                value = unit,
                                onValueChange = { unit = it.take(16) },
                                label = { Text("Unit") },
                                placeholder = { Text("e.g. glasses") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        EvalType.TIMER -> NumberField(goal, { goal = it }, "Goal in minutes", Modifier.fillMaxWidth())
                        EvalType.CHECKLIST -> ChecklistEditor(checklist)
                        EvalType.YES_NO -> Unit
                    }
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(200) },
                        label = { Text("Description (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                STEP_FREQUENCY -> Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                ) {
                    FrequencyOption("Every day", freqType == FreqType.DAILY) { freqType = FreqType.DAILY }
                    FrequencyOption("Specific days of the week", freqType == FreqType.WEEK_DAYS) { freqType = FreqType.WEEK_DAYS }
                    if (freqType == FreqType.WEEK_DAYS) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            WeekDayShort.forEachIndexed { index, label ->
                                val day = index + 1
                                DayChip(label.take(2), day in weekDays) {
                                    if (day in weekDays) weekDays.remove(day) else weekDays.add(day)
                                }
                            }
                        }
                    }
                    FrequencyOption("Some days per week", freqType == FreqType.TIMES_PER_WEEK) { freqType = FreqType.TIMES_PER_WEEK }
                    if (freqType == FreqType.TIMES_PER_WEEK) {
                        Stepper(timesPerWeek, 1..6, "days per week") { timesPerWeek = it }
                    }
                    FrequencyOption("Specific days of the month", freqType == FreqType.MONTH_DAYS) { freqType = FreqType.MONTH_DAYS }
                    if (freqType == FreqType.MONTH_DAYS) {
                        Column(Modifier.padding(horizontal = 16.dp)) {
                            for (row in (1..31).chunked(7)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    for (day in row) {
                                        DayChip("$day", day in monthDays) {
                                            if (day in monthDays) monthDays.remove(day) else monthDays.add(day)
                                        }
                                    }
                                    repeat(7 - row.size) { Spacer(Modifier.size(40.dp)) }
                                }
                            }
                        }
                    }
                    FrequencyOption("Repeat every few days", freqType == FreqType.EVERY_N_DAYS) { freqType = FreqType.EVERY_N_DAYS }
                    if (freqType == FreqType.EVERY_N_DAYS) {
                        Stepper(everyN, 2..60, "days") { everyN = it }
                    }
                }
                else -> WhenStep(
                    startDay = startDay,
                    onStart = { startDay = it },
                    endDay = endDay,
                    onEnd = { endDay = it },
                    reminder = reminder,
                    onReminder = { reminder = it },
                    priority = priority,
                    onPriority = { priority = it },
                    today = today,
                    context = context,
                )
            }
        }
    }
}

@Composable
private fun CategoryStep(data: AppData, selected: String, onPick: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(data.categories, key = { it.id }) { category ->
            val isSelected = category.id == selected
            Row(
                Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.Card)
                    .border(1.5.dp, if (isSelected) Palette.Accent else Color.Transparent, RoundedCornerShape(14.dp))
                    .clickable { onPick(category.id) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryBadge(category, 36.dp)
                Spacer(Modifier.width(10.dp))
                Text(category.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EvaluationStep(selected: EvalType, onPick: (EvalType) -> Unit) {
    val options: List<Triple<EvalType, ImageVector, Pair<String, String>>> = listOf(
        Triple(EvalType.YES_NO, Icons.Filled.ThumbsUpDown, "With a yes or no" to "Record whether you succeed with the activity or not"),
        Triple(EvalType.NUMERIC, Icons.Filled.Pin, "With a numeric value" to "Set a daily goal or limit, e.g. 8 glasses of water"),
        Triple(EvalType.TIMER, Icons.Filled.Timer, "With a timer" to "Set a time goal and track it with a stopwatch"),
        Triple(EvalType.CHECKLIST, Icons.Filled.Checklist, "With a checklist" to "Evaluate your activity with a list of sub-items"),
    )
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        for ((type, icon, texts) in options) {
            val isSelected = type == selected
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Palette.AccentDim else Palette.Card)
                    .clickable { onPick(type) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Palette.Accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(texts.first.uppercase(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(2.dp))
                    Text(texts.second, style = MaterialTheme.typography.bodySmall, color = Palette.TextSoft)
                }
            }
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { input -> onChange(input.filter { it.isDigit() || it == '.' }.take(8)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

@Composable
private fun ChecklistEditor(items: MutableList<String>) {
    var newItem by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Checklist", style = MaterialTheme.typography.labelLarge, color = Palette.TextSoft)
        items.forEachIndexed { index, item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Palette.Card)
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${index + 1}.  $item", Modifier.weight(1f))
                IconButton(onClick = { items.removeAt(index) }) {
                    Icon(Icons.Filled.Close, contentDescription = "Remove item", tint = Palette.TextSoft)
                }
            }
        }
        if (items.size < 20) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newItem,
                    onValueChange = { newItem = it.take(60) },
                    label = { Text("New item") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    enabled = newItem.isNotBlank(),
                    onClick = {
                        items.add(newItem.trim())
                        newItem = ""
                    },
                ) { Icon(Icons.Filled.Add, contentDescription = "Add item", tint = Palette.Accent) }
            }
        }
    }
}

@Composable
private fun FrequencyOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun DayChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .padding(vertical = 3.dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) Palette.Accent else Palette.Card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (selected) Color.White else Palette.TextSoft, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Stepper(value: Int, range: IntRange, label: String, onChange: (Int) -> Unit) {
    Row(
        Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(onClick = { onChange((value - 1).coerceIn(range)) }) {
            Icon(Icons.Filled.Remove, contentDescription = "Fewer")
        }
        Text("$value", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 16.dp))
        FilledTonalIconButton(onClick = { onChange((value + 1).coerceIn(range)) }) {
            Icon(Icons.Filled.Add, contentDescription = "More")
        }
        Text(label, color = Palette.TextSoft, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun WhenStep(
    startDay: Long,
    onStart: (Long) -> Unit,
    endDay: Long?,
    onEnd: (Long?) -> Unit,
    reminder: Int?,
    onReminder: (Int?) -> Unit,
    priority: Int,
    onPriority: (Int) -> Unit,
    today: Long,
    context: android.content.Context,
) {
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) toast(context, "Notifications are blocked, so reminders won't show.")
    }

    fun pickDate(initial: Long, onPicked: (Long) -> Unit) {
        val date = LocalDate.ofEpochDay(initial)
        DatePickerDialog(
            context,
            { _, y, m, d -> onPicked(LocalDate.of(y, m + 1, d).toEpochDay()) },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth,
        ).show()
    }

    fun pickTime(initial: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, h, m -> onPicked(h * 60 + m) },
            initial / 60,
            initial % 60,
            DateFormat.is24HourFormat(context),
        ).show()
    }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        SettingRow(
            Icons.Filled.Event,
            "Start date",
            LocalDate.ofEpochDay(startDay).format(dateFormat),
            onClick = { pickDate(startDay, onStart) },
        )
        RowDivider()
        SettingRow(
            Icons.Filled.EventBusy,
            "End date",
            endDay?.let { LocalDate.ofEpochDay(it).format(dateFormat) } ?: "No end date",
            onClick = { if (endDay != null) pickDate(endDay, onEnd) },
        ) {
            Switch(
                checked = endDay != null,
                onCheckedChange = { on -> if (on) pickDate(maxOf(startDay, today) + 30, onEnd) else onEnd(null) },
            )
        }
        RowDivider()
        SettingRow(
            Icons.Filled.Notifications,
            "Reminder",
            reminder?.let { LocalTime.of(it / 60, it % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) } ?: "Off",
            onClick = { if (reminder != null) pickTime(reminder, onReminder) },
        ) {
            Switch(
                checked = reminder != null,
                onCheckedChange = { on ->
                    if (on) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canNotify(context)) {
                            permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        pickTime(20 * 60, onReminder)
                    } else {
                        onReminder(null)
                    }
                },
            )
        }
        RowDivider()
        SettingRow(Icons.Filled.Flag, "Priority", priorityLabel(priority), onClick = {}) {}
        Row(
            Modifier.padding(start = 70.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (p in 0..2) Pill(priorityLabel(p), priority == p, { onPriority(p) })
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Palette.AccentDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Accent)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.bodySmall, color = Palette.Accent)
        }
        trailing()
    }
}
