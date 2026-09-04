package com.example.habittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.data.HabitCompletionEntity
import com.example.habittracker.data.HabitDatabase
import com.example.habittracker.data.HabitEntity
import com.example.habittracker.ui.theme.HabitTrackerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class AppTab { Home, History, Settings }

private const val PREFS_NAME = "habit_tracker"
private const val THEME_KEY = "theme_mode"

private val habitIcons = listOf("✓", "💧", "🏃", "📚", "🧘", "💪", "🥗", "💤")
private val habitCategories = listOf("General", "Health", "Fitness", "Study", "Mind")
private val habitColors = listOf("green", "blue", "orange", "purple")
private val habitFrequencies = listOf("Daily", "Weekdays", "Weekly")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = remember { getSharedPreferences(PREFS_NAME, MODE_PRIVATE) }
            var themeMode by remember {
                mutableStateOf(AppThemeMode.fromStorage(preferences.getString(THEME_KEY, null)))
            }
            val darkTheme = when (themeMode) {
                AppThemeMode.System -> androidx.compose.foundation.isSystemInDarkTheme()
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }
            HabitTrackerTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HabitApp(
                        themeMode = themeMode,
                        onThemeModeChanged = { newMode ->
                            themeMode = newMode
                            preferences.edit().putString(THEME_KEY, newMode.storageValue).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitApp(
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit
) {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == AppTab.Home,
                    onClick = { selectedTab = AppTab.Home },
                    icon = { Text("✓") },
                    label = { Text("Today") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.History,
                    onClick = { selectedTab = AppTab.History },
                    icon = { Text("▦") },
                    label = { Text("History") }
                )
                NavigationBarItem(
                    selected = selectedTab == AppTab.Settings,
                    onClick = { selectedTab = AppTab.Settings },
                    icon = { Text("⚙") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedTab) {
                AppTab.Home -> HabitHomeScreen()
                AppTab.History -> HistoryScreen()
                AppTab.Settings -> SettingsScreen(themeMode, onThemeModeChanged)
            }
        }
    }
}

@Composable
private fun HabitHomeScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { HabitDatabase.getInstance(context) }
    val habitDao = database.habitDao()
    val completionDao = database.habitCompletionDao()
    val habits by habitDao.observeHabits().collectAsStateWithLifecycle(initialValue = emptyList())
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val completedIds by completionDao.observeCompletedHabitIds(today)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val scope = rememberCoroutineScope()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        val preferences = context.getSharedPreferences(PREFS_NAME, 0)
        if (!preferences.getBoolean("defaults_created", false)) {
            if (habitDao.observeHabits().first().isEmpty()) {
                habitDao.insert(HabitEntity(name = "Drink Water", icon = "💧", category = "Health"))
                habitDao.insert(HabitEntity(name = "Walk 30 Minutes", icon = "🏃", category = "Fitness"))
            }
            preferences.edit().putBoolean("defaults_created", true).apply()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text("Good day 👋", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Your habits",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
    ) { innerPadding ->
        if (habits.isEmpty()) {
            EmptyHabits(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                onAdd = { showAddDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { TodaySummary(habits.size, completedIds.size) }
                items(habits, key = { it.id }) { habit ->
                    val isCompleted = habit.id in completedIds
                    HabitCard(
                        habit = habit,
                        doneToday = isCompleted,
                        onToggle = {
                            scope.launch {
                                if (isCompleted) {
                                    completionDao.deleteForDate(habit.id, today)
                                } else {
                                    completionDao.insert(HabitCompletionEntity(habit.id, today))
                                }
                            }
                        },
                        onEdit = { editingHabit = habit },
                        onDelete = {
                            scope.launch {
                                completionDao.deleteForHabit(habit.id)
                                habitDao.delete(habit)
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        HabitFormDialog(
            existingHabit = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, icon, category, color, frequency ->
                scope.launch {
                    habitDao.insert(
                        HabitEntity(
                            name = name,
                            icon = icon,
                            category = category,
                            color = color,
                            frequency = frequency
                        )
                    )
                }
                showAddDialog = false
            }
        )
    }

    editingHabit?.let { habit ->
        HabitFormDialog(
            existingHabit = habit,
            onDismiss = { editingHabit = null },
            onSave = { name, icon, category, color, frequency ->
                scope.launch {
                    habitDao.update(
                        habit.copy(
                            name = name,
                            icon = icon,
                            category = category,
                            color = color,
                            frequency = frequency
                        )
                    )
                }
                editingHabit = null
            }
        )
    }
}

@Composable
private fun TodaySummary(total: Int, completed: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text("$completed of $total habits completed", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun HabitCard(
    habit: HabitEntity,
    doneToday: Boolean,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val accent = habitAccentColor(habit.color)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 5.dp, height = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
            )
            Spacer(modifier = Modifier.size(12.dp))
            BoxIndicator(doneToday, accent)
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${habit.icon} ${habit.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${habit.category} • ${habit.frequency} • " +
                        if (doneToday) "Completed today" else "Not completed yet",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Checkbox(checked = doneToday, onCheckedChange = { onToggle() })
            TextButton(onClick = onEdit) { Text("Edit") }
            TextButton(onClick = { showDeleteDialog = true }) { Text("Delete") }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete habit?") },
            text = { Text("\"${habit.name}\" and its history will be removed.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun BoxIndicator(done: Boolean, accent: Color) {
    val color = if (done) accent else MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(color))
}

private fun habitAccentColor(color: String): Color = when (color.lowercase(Locale.US)) {
    "blue" -> Color(0xFF4F7FD4)
    "orange" -> Color(0xFFD47A2C)
    "purple" -> Color(0xFF8758C7)
    else -> Color(0xFF2E8B68)
}

@Composable
private fun EmptyHabits(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No habits yet", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Add your first habit and start building your routine.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onAdd) { Text("Add Habit") }
    }
}

@Composable
private fun HabitFormDialog(
    existingHabit: HabitEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(existingHabit?.name.orEmpty()) }
    var icon by remember { mutableStateOf(existingHabit?.icon ?: habitIcons.first()) }
    var category by remember { mutableStateOf(existingHabit?.category ?: habitCategories.first()) }
    var color by remember { mutableStateOf(existingHabit?.color ?: habitColors.first()) }
    var frequency by remember { mutableStateOf(existingHabit?.frequency ?: habitFrequencies.first()) }
    val trimmedName = name.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingHabit == null) "Add Habit" else "Edit Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    habitIcons.take(4).forEach { option ->
                        ChoiceButton(option, option == icon) { icon = option }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    habitIcons.drop(4).forEach { option ->
                        ChoiceButton(option, option == icon) { icon = option }
                    }
                }
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    habitCategories.take(3).forEach { option ->
                        ChoiceButton(option, option == category) { category = option }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    habitCategories.drop(3).forEach { option ->
                        ChoiceButton(option, option == category) { category = option }
                    }
                }
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    habitColors.forEach { option ->
                        ChoiceButton(option.replaceFirstChar { it.uppercase() }, option == color) { color = option }
                    }
                }
                Text("Frequency", style = MaterialTheme.typography.labelLarge)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    habitFrequencies.forEach { option ->
                        ChoiceButton(option, option == frequency) { frequency = option }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(trimmedName, icon, category, color, frequency) },
                enabled = trimmedName.isNotEmpty()
            ) { Text(if (existingHabit == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RowScope.ChoiceButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.weight(1f)) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f)) { Text(label) }
    }
}
