package com.example.habittracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitCompletionDao {
    @Query("SELECT habitId FROM habit_completions WHERE date = :date")
    fun observeCompletedHabitIds(date: String): Flow<List<Int>>

    @Query(
        "SELECT date FROM habit_completions " +
            "WHERE habitId = :habitId AND date BETWEEN :startDate AND :endDate " +
            "ORDER BY date ASC"
    )
    fun observeCompletedDates(
        habitId: Int,
        startDate: String,
        endDate: String
    ): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND date = :date")
    suspend fun deleteForDate(habitId: Int, date: String)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId")
    suspend fun deleteForHabit(habitId: Int)

    @Query("DELETE FROM habit_completions")
    suspend fun deleteAll()
}
