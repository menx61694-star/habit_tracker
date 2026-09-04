package com.example.habittracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val doneToday: Boolean = false,
    val icon: String = "✓",
    val category: String = "General",
    val color: String = "green",
    val frequency: String = "Daily"
)
