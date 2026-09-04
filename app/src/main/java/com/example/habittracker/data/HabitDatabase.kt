package com.example.habittracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitCompletionDao(): HabitCompletionDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS habit_completions (
                        habitId INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        PRIMARY KEY(habitId, date)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_habit_completions_date " +
                        "ON habit_completions(date)"
                )

                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO habit_completions(habitId, date)
                    SELECT id, ? FROM habits WHERE doneToday = 1
                    """.trimIndent(),
                    arrayOf(today)
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN icon TEXT NOT NULL DEFAULT '✓'")
                db.execSQL("ALTER TABLE habits ADD COLUMN category TEXT NOT NULL DEFAULT 'General'")
                db.execSQL("ALTER TABLE habits ADD COLUMN color TEXT NOT NULL DEFAULT 'green'")
                db.execSQL("ALTER TABLE habits ADD COLUMN frequency TEXT NOT NULL DEFAULT 'Daily'")
            }
        }

        @Volatile
        private var INSTANCE: HabitDatabase? = null

        fun getInstance(context: Context): HabitDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabitDatabase::class.java,
                    "habit_tracker.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
