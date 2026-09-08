package com.example.habittracker.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class HabitHistoryStatsTest {
    private fun range(startDay: Int, endDay: Int): Pair<Calendar, Calendar> {
        val start = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, startDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = start.clone() as Calendar
        end.set(Calendar.DAY_OF_MONTH, endDay)
        return start to end
    }

    @Test
    fun dailyScheduleIncludesEveryDay() {
        val (start, end) = range(1, 7)
        assertEquals(7, scheduledDatesInRange("daily", start, end).size)
    }

    @Test
    fun weekdayScheduleExcludesWeekend() {
        val (start, end) = range(7, 13)
        assertEquals(5, scheduledDatesInRange("weekdays", start, end).size)
    }

    @Test
    fun completionRateUsesOnlyScheduledDays() {
        val (start, end) = range(7, 13)
        val completed = listOf("2026-09-07", "2026-09-08", "2026-09-12")
        assertEquals(40, completionRateForRange("weekdays", start, end, completed))
    }

    @Test
    fun bestStreakCountsConsecutiveScheduledOpportunities() {
        val (start, end) = range(7, 13)
        val completed = listOf("2026-09-07", "2026-09-08", "2026-09-10", "2026-09-11")
        assertEquals(2, bestStreakForRange("weekdays", start, end, completed))
    }
}
