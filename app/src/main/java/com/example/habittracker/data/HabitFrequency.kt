package com.example.habittracker.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val DATE_PATTERN = "yyyy-MM-dd"

fun isHabitScheduledToday(frequency: String, date: Date = Date()): Boolean {
    val calendar = Calendar.getInstance().apply { time = date }
    return when (frequency.lowercase(Locale.US)) {
        "weekdays" -> calendar.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY
        else -> true
    }
}

fun currentWeekDateRange(date: Date = Date()): Pair<String, String> {
    val calendar = Calendar.getInstance().apply {
        time = date
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    }
    val start = formatDate(calendar.time)
    calendar.add(Calendar.DAY_OF_YEAR, 6)
    return start to formatDate(calendar.time)
}

fun formatDate(date: Date): String =
    SimpleDateFormat(DATE_PATTERN, Locale.US).format(date)
