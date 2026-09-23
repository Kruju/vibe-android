package com.kruju.habits.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kruju.habits.data.AppData
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
fun HabitDetailScreen(
    data: AppData,
    habit: Habit,
    today: Long,
    onBack: () -> Unit,
    onStatus: (Habit, Long) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val category = data.category(habit.categoryId)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    val tabs = if (habit.isTask) listOf("Calendar") else listOf("Calendar", "Statistics")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(habit.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Palette.Background),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryBadge(category, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(category.name, color = category.composeColor(), fontWeight = FontWeight.SemiBold)
                    Text(habit.frequencyLabel(), style = MaterialTheme.typography.bodySmall, color = Palette.TextSoft)
                }
            }
            if (tabs.size > 1) {
                TabRow(
                    selectedTabIndex = tab,
                    containerColor = Palette.Background,
                    contentColor = Palette.Accent,
                    indicator = { positions ->
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(positions[tab]), color = Palette.Accent)
                    },
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = tab == index,
                            onClick = { tab = index },
                            text = { Text(title.uppercase(), fontWeight = FontWeight.SemiBold) },
                            selectedContentColor = Palette.Accent,
                            unselectedContentColor = Palette.TextSoft,
                        )
                    }
                }
            }
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (tab == 0) {
                    CalendarCard(habit, today, onStatus)
                    if (habit.description.isNotBlank()) {
                        Text(habit.description, color = Palette.TextSoft)
                    }
                    Text(
                        "Tap a day to mark it. Green is done, red is a missed day.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.TextFaint,
                    )
                } else {
                    Statistics(habit, today)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Palette.Surface,
            title = { Text("Delete “${habit.name}”?") },
            text = { Text("Its whole history will be removed. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Delete", color = Palette.Fail) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = Palette.TextSoft) } },
        )
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .padding(14.dp)
    ) { content() }
}

@Composable
private fun CalendarCard(habit: Habit, today: Long, onStatus: (Habit, Long) -> Unit) {
    val thisMonth = YearMonth.from(LocalDate.ofEpochDay(today))
    var month by remember { mutableStateOf(thisMonth) }
    val offset = month.atDay(1).dayOfWeek.value - 1
    val length = month.lengthOfMonth()
    val weeks = (offset + length + 6) / 7

    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { month = month.plusMonths(1) }, enabled = month < thisMonth) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }
        Row {
            for (dow in DayOfWeek.values()) {
                Text(
                    dow.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextSoft,
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
                            CalendarDay(habit, day, today, dayOfMonth) { onStatus(habit, day) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDay(habit: Habit, day: Long, today: Long, number: Int, onClick: () -> Unit) {
    val done = habit.isComplete(day)
    val scheduled = habit.isScheduled(day)
    val missed = scheduled && !done && day < today
    val progress = habit.progress(day)
    val clickable = day <= today && habit.isActive(day)
    Box(
        Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(
                when {
                    done -> Palette.Success
                    missed -> Palette.Fail.copy(alpha = 0.18f)
                    else -> Color.Transparent
                }
            )
            .clickable(enabled = clickable, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (!done && progress > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 2.dp.toPx()
                drawArc(
                    Palette.Accent, -90f, 360f * progress, false,
                    Offset(stroke / 2, stroke / 2), Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            "$number",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
            color = when {
                done -> Color.White
                missed -> Palette.Fail
                day == today -> Palette.Accent
                !scheduled || day > today -> Palette.TextFaint
                else -> Palette.Text
            },
        )
    }
}

@Composable
private fun Statistics(habit: Habit, today: Long) {
    val rate = habit.successRate(today)
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val stroke = 10.dp.toPx()
                    val arc = Size(size.width - stroke, size.height - stroke)
                    drawArc(Palette.Card, 0f, 360f, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke))
                    drawArc(
                        Palette.Accent, -90f, 360f * rate, false, Offset(stroke / 2, stroke / 2), arc,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                Text("${(rate * 100).roundToInt()}%", fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Habit score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Share of scheduled days you completed since you started.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Palette.TextSoft,
                )
            }
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StreakTile(Icons.Filled.LocalFireDepartment, "${habit.currentStreak(today)}", "Current streak", Modifier.weight(1f))
        StreakTile(Icons.Filled.EmojiEvents, "${habit.bestStreak(today)}", "Best streak", Modifier.weight(1f))
    }

    val date = LocalDate.ofEpochDay(today)
    val weekStart = today - (date.dayOfWeek.value - 1)
    val monthStart = date.withDayOfMonth(1).toEpochDay()
    val yearStart = date.withDayOfYear(1).toEpochDay()
    Card {
        Text("Times completed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row {
            CountCell("This week", habit.completionsBetween(weekStart, today), Modifier.weight(1f))
            CountCell("This month", habit.completionsBetween(monthStart, today), Modifier.weight(1f))
            CountCell("This year", habit.completionsBetween(yearStart, today), Modifier.weight(1f))
            CountCell("Total", habit.completionsBetween(Long.MIN_VALUE, today), Modifier.weight(1f))
        }
    }

    Card {
        Text("Last 6 months", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        MonthlyBars(habit, today)
    }
}

@Composable
private fun StreakTile(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Palette.Surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Palette.Accent, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.TextSoft)
        }
    }
}

@Composable
private fun CountCell(label: String, count: Int, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Palette.Accent)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Palette.TextSoft, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MonthlyBars(habit: Habit, today: Long) {
    val current = YearMonth.from(LocalDate.ofEpochDay(today))
    val months = (5 downTo 0).map { current.minusMonths(it.toLong()) }
    val counts = months.map { m -> habit.completionsBetween(m.atDay(1).toEpochDay(), m.atEndOfMonth().toEpochDay()) }
    val max = (counts.maxOrNull() ?: 0).coerceAtLeast(1)

    Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        months.forEachIndexed { index, month ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${counts[index]}", style = MaterialTheme.typography.labelSmall, color = Palette.TextSoft)
                Canvas(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    val h = size.height * counts[index] / max
                    drawRoundRect(Palette.Card, cornerRadius = CornerRadius(6.dp.toPx()))
                    if (h > 0f) {
                        drawRoundRect(
                            Palette.Accent,
                            topLeft = Offset(0f, size.height - h),
                            size = Size(size.width, h),
                            cornerRadius = CornerRadius(6.dp.toPx()),
                        )
                    }
                }
                Text(
                    month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextSoft,
                )
            }
        }
    }
}
