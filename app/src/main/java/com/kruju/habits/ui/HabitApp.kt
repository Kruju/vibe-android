package com.kruju.habits.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kruju.habits.HabitViewModel
import com.kruju.habits.data.EvalType
import com.kruju.habits.data.Habit
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private data class TabItem(val title: String, val icon: ImageVector)

private val Tabs = listOf(
    TabItem("Today", Icons.Filled.Today),
    TabItem("Habits", Icons.Filled.WorkspacePremium),
    TabItem("Tasks", Icons.Filled.TaskAlt),
    TabItem("Categories", Icons.Filled.GridView),
    TabItem("Timer", Icons.Filled.Timer),
)

private const val TAB_TODAY = 0
private const val TAB_HABITS = 1
private const val TAB_TASKS = 2
private const val TAB_CATEGORIES = 3

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitApp(vm: HabitViewModel) {
    val data by vm.data.collectAsState()
    val today by vm.today.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawer = rememberDrawerState(DrawerValue.Closed)

    var tab by rememberSaveable { mutableIntStateOf(TAB_TODAY) }
    var selectedDay by rememberSaveable { mutableStateOf(today) }
    var detailId by rememberSaveable { mutableStateOf<Long?>(null) }
    var wizardOpen by rememberSaveable { mutableStateOf(false) }
    var wizardEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    var wizardIsTask by rememberSaveable { mutableStateOf(false) }
    var entryHabitId by rememberSaveable { mutableStateOf<Long?>(null) }
    var entryDay by rememberSaveable { mutableStateOf(today) }
    var taskDialogOpen by rememberSaveable { mutableStateOf(false) }
    var taskEditId by rememberSaveable { mutableStateOf<Long?>(null) }
    var addSheet by rememberSaveable { mutableStateOf(false) }
    var backupDialog by rememberSaveable { mutableStateOf(false) }
    var aboutDialog by rememberSaveable { mutableStateOf(false) }

    fun onHabitStatus(habit: Habit, day: Long) {
        if (habit.type == EvalType.YES_NO) {
            vm.toggleYesNo(habit.id, day)
        } else {
            entryHabitId = habit.id
            entryDay = day
        }
    }

    fun openWizard(isTask: Boolean, editId: Long? = null) {
        wizardIsTask = isTask
        wizardEditId = editId
        wizardOpen = true
    }

    fun openTask(id: Long?) {
        taskEditId = id
        taskDialogOpen = true
    }

    val detailHabit = detailId?.let { id -> data.habits.find { it.id == id } }
    val wizardHabit = wizardEditId?.let { id -> data.habits.find { it.id == id } }

    when {
        wizardOpen -> HabitWizard(
            data = data,
            initial = wizardHabit,
            isTask = wizardHabit?.isTask ?: wizardIsTask,
            today = today,
            onClose = { wizardOpen = false },
            onSave = {
                vm.saveHabit(it)
                wizardOpen = false
            },
        )

        detailHabit != null -> {
            BackHandler { detailId = null }
            HabitDetailScreen(
                data = data,
                habit = detailHabit,
                today = today,
                onBack = { detailId = null },
                onStatus = ::onHabitStatus,
                onEdit = { openWizard(detailHabit.isTask, detailHabit.id) },
                onDelete = {
                    detailId = null
                    vm.deleteHabit(detailHabit.id)
                },
            )
        }

        else -> {
            BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
            BackHandler(enabled = !drawer.isOpen && tab != TAB_TODAY) { tab = TAB_TODAY }
            ModalNavigationDrawer(
                drawerState = drawer,
                drawerContent = {
                    AppDrawer(
                        today = today,
                        currentTab = tab,
                        onTab = {
                            tab = it
                            scope.launch { drawer.close() }
                        },
                        onBackup = {
                            backupDialog = true
                            scope.launch { drawer.close() }
                        },
                        onAbout = {
                            aboutDialog = true
                            scope.launch { drawer.close() }
                        },
                    )
                },
            ) {
                Scaffold(
                    containerColor = Palette.Background,
                    topBar = {
                        TopAppBar(
                            title = { Text(Tabs[tab].title, fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawer.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = "Menu")
                                }
                            },
                            actions = {
                                if (tab == TAB_TODAY && selectedDay != today) {
                                    TextButton(onClick = { selectedDay = today }) { Text("TODAY") }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Palette.Background),
                        )
                    },
                    bottomBar = {
                        NavigationBar(containerColor = Palette.Surface) {
                            Tabs.forEachIndexed { index, item ->
                                NavigationBarItem(
                                    selected = tab == index,
                                    onClick = { tab = index },
                                    icon = { Icon(item.icon, contentDescription = null) },
                                    label = { Text(item.title, maxLines = 1) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Palette.Accent,
                                        selectedTextColor = Palette.Accent,
                                        indicatorColor = Palette.AccentDim.copy(alpha = 0.5f),
                                        unselectedIconColor = Palette.TextSoft,
                                        unselectedTextColor = Palette.TextSoft,
                                    ),
                                )
                            }
                        }
                    },
                    floatingActionButton = {
                        if (tab <= TAB_TASKS) {
                            FloatingActionButton(
                                onClick = { if (tab == TAB_HABITS) openWizard(isTask = false) else addSheet = true },
                                containerColor = Palette.Accent,
                                contentColor = Color.White,
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Add")
                            }
                        }
                    },
                ) { padding ->
                    when (tab) {
                        TAB_TODAY -> TodayScreen(
                            data = data,
                            today = today,
                            selected = selectedDay,
                            onSelect = { selectedDay = it },
                            onHabitStatus = ::onHabitStatus,
                            onHabitClick = { detailId = it.id },
                            onTaskToggle = { vm.toggleTask(it.id) },
                            onTaskClick = { openTask(it.id) },
                            contentPadding = padding,
                        )
                        TAB_HABITS -> HabitsScreen(data, today, onOpen = { detailId = it.id }, contentPadding = padding)
                        TAB_TASKS -> TasksScreen(
                            data = data,
                            today = today,
                            onTaskToggle = { vm.toggleTask(it.id) },
                            onTaskClick = { openTask(it.id) },
                            onRecurringClick = { detailId = it.id },
                            contentPadding = padding,
                        )
                        TAB_CATEGORIES -> CategoriesScreen(data, vm, padding)
                        else -> TimerScreen(vm, padding)
                    }
                }
            }
        }
    }

    if (addSheet) {
        ModalBottomSheet(onDismissRequest = { addSheet = false }, containerColor = Palette.Surface) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                AddOption(
                    Icons.Filled.WorkspacePremium,
                    "Habit",
                    "An activity that repeats over time, with tracking and statistics.",
                ) {
                    addSheet = false
                    openWizard(isTask = false)
                }
                AddOption(
                    Icons.Filled.Repeat,
                    "Recurring task",
                    "An activity that repeats over time, without statistics.",
                ) {
                    addSheet = false
                    openWizard(isTask = true)
                }
                AddOption(
                    Icons.Filled.TaskAlt,
                    "Task",
                    "A one-off activity for a single day.",
                ) {
                    addSheet = false
                    openTask(null)
                }
            }
        }
    }

    entryHabitId?.let { id -> data.habits.find { it.id == id } }?.let { habit ->
        EntryDialog(
            habit = habit,
            day = entryDay,
            onDismiss = { entryHabitId = null },
            onSave = {
                vm.setEntry(habit.id, entryDay, it)
                entryHabitId = null
            },
        )
    }

    if (taskDialogOpen) {
        val task = taskEditId?.let { id -> data.tasks.find { it.id == id } }
        TaskDialog(
            data = data,
            initial = task,
            defaultDay = if (tab == TAB_TODAY) maxOf(selectedDay, today) else today,
            onDismiss = { taskDialogOpen = false },
            onSave = {
                vm.saveTask(it)
                taskDialogOpen = false
            },
            onDelete = task?.let {
                {
                    vm.deleteTask(it.id)
                    taskDialogOpen = false
                }
            },
        )
    }

    if (backupDialog) BackupDialog(vm, onDismiss = { backupDialog = false })

    if (aboutDialog) {
        AlertDialog(
            onDismissRequest = { aboutDialog = false },
            containerColor = Palette.Surface,
            title = { Text("Habit Streaks") },
            text = {
                Text(
                    "A habit tracker, to-do list and timer that works fully offline. " +
                        "Everything is stored on this phone only. Use Backup to move your data to another device."
                )
            },
            confirmButton = { TextButton(onClick = { aboutDialog = false }) { Text("OK") } },
        )
    }
}

@Composable
private fun AppDrawer(
    today: Long,
    currentTab: Int,
    onTab: (Int) -> Unit,
    onBackup: () -> Unit,
    onAbout: () -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = Palette.Surface) {
        Column(Modifier.padding(24.dp)) {
            Text("Habit Streaks", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Palette.Accent)
            Text(
                LocalDate.ofEpochDay(today).format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),
                color = Palette.TextSoft,
            )
        }
        HorizontalDivider(color = Palette.Divider)
        Spacer(Modifier.height(8.dp))
        val itemColors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = Palette.AccentDim.copy(alpha = 0.5f),
            selectedIconColor = Palette.Accent,
            selectedTextColor = Palette.Accent,
            unselectedContainerColor = Color.Transparent,
        )
        Tabs.forEachIndexed { index, item ->
            NavigationDrawerItem(
                label = { Text(item.title) },
                icon = { Icon(item.icon, contentDescription = null) },
                selected = index == currentTab,
                onClick = { onTab(index) },
                colors = itemColors,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Palette.Divider)
        Spacer(Modifier.height(8.dp))
        NavigationDrawerItem(
            label = { Text("Backup & restore") },
            icon = { Icon(Icons.Filled.Backup, contentDescription = null) },
            selected = false,
            onClick = onBackup,
            colors = itemColors,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        NavigationDrawerItem(
            label = { Text("About") },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            selected = false,
            onClick = onAbout,
            colors = itemColors,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun AddOption(icon: ImageVector, title: String, text: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Palette.AccentDim),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Palette.Accent)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Palette.TextSoft)
        }
    }
}

@Composable
private fun BackupDialog(vm: HabitViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var confirmImport by rememberSaveable { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) toast(context, if (vm.exportTo(uri)) "Backup saved" else "Couldn't save the backup")
        onDismiss()
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) toast(context, if (vm.importFrom(uri)) "Backup restored" else "That file isn't a Habit Streaks backup")
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.Surface,
        title = { Text(if (confirmImport) "Replace your data?" else "Backup & restore") },
        text = {
            Text(
                if (confirmImport) {
                    "Everything in the app will be replaced by the contents of the backup file."
                } else {
                    "Save all your habits, tasks and history to a file, or restore them from one. " +
                        "Keep the file somewhere safe, like your cloud drive."
                }
            )
        },
        confirmButton = {
            if (confirmImport) {
                TextButton(onClick = { importLauncher.launch(arrayOf("*/*")) }) { Text("Choose file") }
            } else {
                TextButton(onClick = { exportLauncher.launch("habit-streaks-${LocalDate.now()}.json") }) { Text("Export") }
            }
        },
        dismissButton = {
            if (confirmImport) {
                TextButton(onClick = onDismiss) { Text("Cancel", color = Palette.TextSoft) }
            } else {
                TextButton(onClick = { confirmImport = true }) { Text("Import") }
            }
        },
    )
}
