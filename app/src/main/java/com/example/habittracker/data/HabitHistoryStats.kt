package com.example.habittracker.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val DATE_PATTERN = "yyyy-MM-dd"

private val statsFormatter = SimpleDateFormat(DATE_PATTERN, Locale.US)

fun scheduledDatesInRange(
    frequency: String,
    start: Calendar,
    end: Calendar
): List<String> {
    val result = mutableListOf<String>()
    val cursor = start.clone() as Calendar
    cursor.timeZone = start.timeZone

    if (frequency.equals("weekly", ignoreCase = true)) {
        val weeks = linkedSetOf<String>()
        while (!cursor.after(end)) {
            weeks += weekStart(cursor)
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        return weeks.toList()
    }

    while (!cursor.after(end)) {
        val scheduled = !frequency.equals("weekdays", ignoreCase = true) ||
            cursor.get(Calendar.DAY_OF_WEEK) in Calendar.MONDAY..Calendar.FRIDAY
        if (scheduled) result += statsFormatter.format(cursor.time)
        cursor.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
}

fun completionRateForRange(
    frequency: String,
    start: Calendar,
    end: Calendar,
    completedDates: List<String>
): Int {
    val scheduled = scheduledDatesInRange(frequency, start, end)
    if (scheduled.isEmpty()) return 0

    val completed = completedDates.toSet()
    val completedOpportunities = if (frequency.equals("weekly", ignoreCase = true)) {
        completedDates.map { parseDate(it) }
            .filterNotNull()
            .map { weekStart(it) }
            .toSet()
            .count { it in scheduled }
    } else {
        scheduled.count { it in completed }
    }

    return (completedOpportunities * 100 / scheduled.size).coerceIn(0, 100)
}

fun completedOpportunityCount(
    frequency: String,
    start: Calendar,
    end: Calendar,
    completedDates: List<String>
): Int {
    val scheduled = scheduledDatesInRange(frequency, start, end).toSet()
    if (scheduled.isEmpty()) return 0

    return if (frequency.equals("weekly", ignoreCase = true)) {
        completedDates.mapNotNull { parseDate(it) }
            .map { weekStart(it) }
            .toSet()
            .count { it in scheduled }
    } else {
        completedDates.count { it in scheduled }
    }
}

fun bestStreakForRange(
    frequency: String,
    start: Calendar,
    end: Calendar,
    completedDates: List<String>
): Int {
    val scheduled = scheduledDatesInRange(frequency, start, end)
    if (scheduled.isEmpty()) return 0

    val completedOpportunities = if (frequency.equals("weekly", ignoreCase = true)) {
        completedDates.mapNotNull { parseDate(it) }
            .map { weekStart(it) }
            .toSet()
    } else {
        completedDates.toSet()
    }

    var best = 0
    var current = 0
    scheduled.forEach { opportunity ->
        if (opportunity in completedOpportunities) {
            current++
            if (current > best) best = current
        } else {
            current = 0
        }
    }
    return best
}

fun monthCalendar(monthOffset: Int): Calendar = Calendar.getInstance().apply {
    add(Calendar.MONTH, monthOffset)
    set(Calendar.DAY_OF_MONTH, 1)
}

fun monthStart(month: Calendar): Calendar = (month.clone() as Calendar).apply {
    set(Calendar.DAY_OF_MONTH, 1)
}

fun monthEnd(month: Calendar): Calendar = (month.clone() as Calendar).apply {
    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
}

fun yearStart(yearOffset: Int): Calendar = Calendar.getInstance().apply {
    add(Calendar.YEAR, yearOffset)
    set(Calendar.MONTH, Calendar.JANUARY)
    set(Calendar.DAY_OF_MONTH, 1)
}

fun yearEnd(year: Calendar): Calendar = (year.clone() as Calendar).apply {
    set(Calendar.MONTH, Calendar.DECEMBER)
    set(Calendar.DAY_OF_MONTH, 31)
}

fun expandedQueryStart(start: Calendar, frequency: String): Calendar =
    (start.clone() as Calendar).apply {
        if (frequency.equals("weekly", ignoreCase = true)) {
            add(Calendar.DAY_OF_YEAR, -6)
        }
    }

fun formatStatsDate(calendar: Calendar): String = statsFormatter.format(calendar.time)

private fun weekStart(date: Calendar): String {
    val monday = date.clone() as Calendar
    monday.firstDayOfWeek = Calendar.MONDAY
    monday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    return statsFormatter.format(monday.time)
}

private fun parseDate(value: String): Calendar? = runCatching {
    val date = statsFormatter.parse(value) ?: return null
    Calendar.getInstance().apply { time = date }
}.getOrNull()
