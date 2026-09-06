package com.example.habittracker.data

import android.content.SharedPreferences
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

private const val BACKUP_VERSION = 1
private const val PREF_THEME_MODE = "theme_mode"
private const val PREF_DEFAULTS_CREATED = "defaults_created"

suspend fun exportHabitBackup(
    database: HabitDatabase,
    preferences: SharedPreferences
): String {
    val (habits, completions) = database.withTransaction {
        database.habitDao().getAll() to database.habitCompletionDao().getAll()
    }

    val root = JSONObject()
        .put("backupVersion", BACKUP_VERSION)
        .put("exportedAt", Date().time)
        .put(
            "settings",
            JSONObject()
                .put("themeMode", preferences.getString(PREF_THEME_MODE, "system"))
                .put("defaultsCreated", preferences.getBoolean(PREF_DEFAULTS_CREATED, false))
        )

    val habitArray = JSONArray()
    habits.forEach { habit ->
        habitArray.put(
            JSONObject()
                .put("id", habit.id)
                .put("name", habit.name)
                .put("doneToday", habit.doneToday)
                .put("icon", habit.icon)
                .put("category", habit.category)
                .put("color", habit.color)
                .put("frequency", habit.frequency)
        )
    }

    val completionArray = JSONArray()
    completions.forEach { completion ->
        completionArray.put(
            JSONObject()
                .put("habitId", completion.habitId)
                .put("date", completion.date)
        )
    }

    root.put("habits", habitArray)
    root.put("completions", completionArray)
    return root.toString(2)
}

data class BackupRestoreResult(
    val habitCount: Int,
    val completionCount: Int,
    val themeMode: String?,
    val defaultsCreated: Boolean?
)

suspend fun restoreHabitBackup(
    database: HabitDatabase,
    preferences: SharedPreferences,
    json: String
): BackupRestoreResult {
    val root = JSONObject(json)
    require(root.optInt("backupVersion", -1) == BACKUP_VERSION) {
        "Unsupported backup version"
    }

    val habitArray = root.optJSONArray("habits")
        ?: throw IllegalArgumentException("Backup is missing habits")
    val completionArray = root.optJSONArray("completions")
        ?: throw IllegalArgumentException("Backup is missing completions")

    val habits = buildList(habitArray.length()) {
        for (index in 0 until habitArray.length()) {
            val item = habitArray.getJSONObject(index)
            val name = item.getString("name").trim()
            require(name.isNotEmpty()) { "Backup contains a habit with an empty name" }
            add(
                HabitEntity(
                    id = item.getInt("id"),
                    name = name,
                    doneToday = item.optBoolean("doneToday", false),
                    icon = item.optString("icon", "✓"),
                    category = item.optString("category", "General"),
                    color = item.optString("color", "green"),
                    frequency = item.optString("frequency", "Daily")
                )
            )
        }
    }

    val completions = buildList(completionArray.length()) {
        for (index in 0 until completionArray.length()) {
            val item = completionArray.getJSONObject(index)
            val date = item.getString("date")
            require(date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                "Backup contains an invalid completion date"
            }
            add(
                HabitCompletionEntity(
                    habitId = item.getInt("habitId"),
                    date = date
                )
            )
        }
    }

    val habitIds = habits.map { it.id }
    require(habitIds.size == habitIds.toSet().size) {
        "Backup contains duplicate habit IDs"
    }
    require(completions.all { it.habitId in habitIds.toSet() }) {
        "Backup contains a completion for a missing habit"
    }

    val settings = root.optJSONObject("settings")
    val themeMode = settings?.optString("themeMode")?.takeIf { it.isNotBlank() }
    val defaultsCreated = settings?.optBoolean("defaultsCreated")

    database.withTransaction {
        database.habitCompletionDao().deleteAll()
        database.habitDao().deleteAll()
        if (habits.isNotEmpty()) database.habitDao().insertAll(habits)
        if (completions.isNotEmpty()) database.habitCompletionDao().insertAll(completions)
    }

    preferences.edit().apply {
        if (themeMode != null) putString(PREF_THEME_MODE, themeMode)
        if (defaultsCreated != null) putBoolean(PREF_DEFAULTS_CREATED, defaultsCreated)
    }.apply()

    return BackupRestoreResult(
        habitCount = habits.size,
        completionCount = completions.size,
        themeMode = themeMode,
        defaultsCreated = defaultsCreated
    )
}
