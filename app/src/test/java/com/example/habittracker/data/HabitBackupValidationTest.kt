package com.example.habittracker.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.Room
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HabitBackupValidationTest {
    private lateinit var database: HabitDatabase
    private lateinit var preferences: android.content.SharedPreferences

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, HabitDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        preferences = context.getSharedPreferences("habit_backup_test", Context.MODE_PRIVATE)
    }

    @After
    fun tearDown() {
        database.close()
        preferences.edit().clear().apply()
    }

    @Test
    fun validBackupRestoresHabitsAndCompletions() = runTest {
        val json = """
            {
              "backupVersion": 1,
              "settings": {"themeMode": "dark", "defaultsCreated": true},
              "habits": [
                {"id": 7, "name": "Read", "doneToday": true, "icon": "📚", "category": "Study", "color": "blue", "frequency": "Daily"}
              ],
              "completions": [
                {"habitId": 7, "date": "2026-09-08"}
              ]
            }
        """.trimIndent()

        val result = restoreHabitBackup(database, preferences, json)

        assertEquals(1, result.habitCount)
        assertEquals(1, result.completionCount)
        assertEquals("dark", result.themeMode)
        assertEquals(1, database.habitDao().getAll().size)
        assertEquals(1, database.habitCompletionDao().getAll().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateHabitIdsAreRejected() = runTest {
        restoreHabitBackup(database, preferences, """
            {"backupVersion":1,"habits":[{"id":1,"name":"A"},{"id":1,"name":"B"}],"completions":[]}
        """.trimIndent())
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidCompletionDateIsRejected() = runTest {
        restoreHabitBackup(database, preferences, """
            {"backupVersion":1,"habits":[{"id":1,"name":"A"}],"completions":[{"habitId":1,"date":"09-08-2026"}]}
        """.trimIndent())
    }

    @Test(expected = IllegalArgumentException::class)
    fun orphanCompletionIsRejected() = runTest {
        restoreHabitBackup(database, preferences, """
            {"backupVersion":1,"habits":[{"id":1,"name":"A"}],"completions":[{"habitId":99,"date":"2026-09-08"}]}
        """.trimIndent())
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsupportedBackupVersionIsRejected() = runTest {
        restoreHabitBackup(database, preferences, """
            {"backupVersion":2,"habits":[],"completions":[]}
        """.trimIndent())
    }
}
