package com.example.habittracker

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.habittracker.data.HabitDatabase
import com.example.habittracker.data.exportHabitBackup
import com.example.habittracker.data.restoreHabitBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val PREFS_NAME = "habit_tracker"
private const val DEFAULTS_CREATED_KEY = "defaults_created"
private const val FEEDBACK_SUBJECT = "Habit Tracker Feedback"
private const val ISSUE_URL = "https://github.com/menx61694-star/habit_tracker/issues"

enum class AppThemeMode(val storageValue: String, val label: String) {
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
    val context = LocalContext.current
    val database = remember { HabitDatabase.getInstance(context) }
    val preferences = remember {
        context.getSharedPreferences(PREFS_NAME, 0)
    }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }
    var feedback by rememberSaveable { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val backup = withContext(Dispatchers.IO) {
                    exportHabitBackup(database, preferences)
                }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.writer(Charsets.UTF_8).use { writer -> writer.write(backup) }
                    } ?: error("Could not open the selected file")
                }
                snackbarHostState.showSnackbar("Backup exported successfully")
            } catch (error: Exception) {
                snackbarHostState.showSnackbar(
                    error.message ?: "Backup export failed"
                )
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val backup = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.reader(Charsets.UTF_8).readText()
                    } ?: error("Could not read the selected file")
                }
                val result = withContext(Dispatchers.IO) {
                    restoreHabitBackup(database, preferences, backup)
                }
                result.themeMode?.let { onThemeModeChanged(AppThemeMode.fromStorage(it)) }
                snackbarHostState.showSnackbar(
                    "Restored ${result.habitCount} habits and ${result.completionCount} completions"
                )
            } catch (error: Exception) {
                snackbarHostState.showSnackbar(
                    error.message ?: "Backup restore failed"
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
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
                    text = "Personalize the app, back up your habits, and manage your data.",
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
                SettingsCard(title = "Backup & restore") {
                    Text(
                        text = "Save your habits and completion history as a JSON backup, then restore it on this or another device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val stamp = SimpleDateFormat("yyyyMMdd-HHmm", Locale.US)
                                    .format(Date())
                                exportLauncher.launch("HabitTracker-backup-$stamp.json")
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Export")
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Import")
                        }
                    }
                }
            }

            item {
                SettingsCard(title = "Feedback") {
                    Text(
                        text = "Tell us what should be improved or what is working well.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = feedback,
                        onValueChange = { feedback = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        label = { Text("Your feedback") },
                        placeholder = { Text("Write your suggestion or report a problem…") }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val message = feedback.trim()
                                if (message.isNotEmpty()) {
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_SUBJECT, FEEDBACK_SUBJECT)
                                            putExtra(Intent.EXTRA_TEXT, message)
                                        }
                                        context.startActivity(
                                            Intent.createChooser(shareIntent, "Send feedback with")
                                        )
                                    } catch (_: ActivityNotFoundException) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("No app is available to send feedback")
                                        }
                                    }
                                }
                            },
                            enabled = feedback.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share feedback")
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(ISSUE_URL))
                                    )
                                } catch (_: ActivityNotFoundException) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Could not open issue tracker")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Report issue")
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Version ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
                            preferences.edit()
                                .putBoolean(DEFAULTS_CREATED_KEY, false)
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
