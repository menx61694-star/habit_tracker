package com.example.habittracker.data

import androidx.room.Entity

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "date"]
)
data class HabitCompletionEntity(
    val habitId: Int,
    val date: String
)
