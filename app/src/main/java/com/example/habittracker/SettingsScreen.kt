package com.example.habittracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.HabitDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val PREFS_NAME = "habit_tracker"
private const val THEME_KEY = "theme_mode"

internal enum class AppThemeMode(val storageValue: String, val label: String) {
    System("system", "System"),
    Light("light", "Light"),
    Dark("dark", "Dark");

    companion object {
        fun fromStorage(value: String?): AppThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: System
    }
}

@Composable
fun SettingsScreen(
    themeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { HabitDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Personalize the app and manage your data.",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        item {
            SettingsCard(title = "Appearance") {
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose how Habit Tracker looks.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeMode.entries.forEach { option ->
                        if (option == themeMode) {
                            Button(
                                onClick = { onThemeModeChanged(option) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(option.label)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onThemeModeChanged(option) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(option.label)
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsCard(title = "Data") {
                Text(
                    text = "Reset all habits and completion history.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { showResetDialog = true }) {
                    Text("Reset all data")
                }
            }
        }

        item {
            SettingsCard(title = "About") {
                Text(
                    text = "Habit Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Build habits. Track progress. Stay consistent.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset all data?") },
            text = {
                Text("This will permanently delete every habit and its completion history. This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        scope.launch(Dispatchers.IO) {
                            database.habitCompletionDao().deleteAll()
                            database.habitDao().deleteAll()
                            context.getSharedPreferences(PREFS_NAME, 0)
                                .edit()
                                .putBoolean("defaults_created", false)
                                .apply()
                        }
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
