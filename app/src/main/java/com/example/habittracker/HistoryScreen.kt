package com.example.habittracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.habittracker.data.HabitDatabase
import com.example.habittracker.data.HabitEntity
import com.example.habittracker.data.bestStreakForRange
import com.example.habittracker.data.completionRateForRange
import com.example.habittracker.data.expandedQueryStart
import com.example.habittracker.data.formatStatsDate
import com.example.habittracker.data.monthCalendar
import com.example.habittracker.data.monthEnd
import com.example.habittracker.data.monthStart
import com.example.habittracker.data.yearEnd
import com.example.habittracker.data.yearStart
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private enum class HistoryView { Month, Year }

@Composable
fun HistoryScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { HabitDatabase.getInstance(context) }
    val habits by database.habitDao().observeHabits()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedHabitId by remember { mutableIntStateOf(-1) }
    var monthOffset by remember { mutableIntStateOf(0) }
    var yearOffset by remember { mutableIntStateOf(0) }
    var historyView by remember { mutableStateOf(HistoryView.Month) }

    val selectedHabit = habits.firstOrNull { it.id == selectedHabitId } ?: habits.firstOrNull()

    LaunchedEffect(habits) {
        if (selectedHabitId == -1 && habits.isNotEmpty()) {
            selectedHabitId = habits.first().id
        } else if (habits.none { it.id == selectedHabitId }) {
            selectedHabitId = habits.firstOrNull()?.id ?: -1
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
        )

        if (selectedHabit == null) {
            EmptyHistory()
            return@Column
        }

        HabitPicker(
            habits = habits,
            selectedHabit = selectedHabit,
            onSelected = { selectedHabitId = it }
        )

        Spacer(modifier = Modifier.height(10.dp))
        HistoryViewPicker(historyView) { historyView = it }
        Spacer(modifier = Modifier.height(8.dp))

        when (historyView) {
            HistoryView.Month -> MonthHistory(
                database = database,
                habit = selectedHabit,
                monthOffset = monthOffset,
                onPrevious = { monthOffset-- },
                onNext = { monthOffset++ }
            )
            HistoryView.Year -> YearHistory(
                database = database,
                habit = selectedHabit,
                yearOffset = yearOffset,
                onPrevious = { yearOffset-- },
                onNext = { yearOffset++ }
            )
        }
    }
}

@Composable
private fun HistoryViewPicker(selected: HistoryView, onSelected: (HistoryView) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryModeButton("Month", selected == HistoryView.Month, Modifier.weight(1f)) {
            onSelected(HistoryView.Month)
        }
        HistoryModeButton("Year", selected == HistoryView.Year, Modifier.weight(1f)) {
            onSelected(HistoryView.Year)
        }
    }
}

@Composable
private fun HistoryModeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    if (selected) {
        TextButton(onClick = onClick, modifier = modifier) {
            Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    } else {
        TextButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
private fun MonthHistory(
    database: HabitDatabase,
    habit: HabitEntity,
    monthOffset: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val month = remember(monthOffset) { monthCalendar(monthOffset) }
    val start = remember(month.timeInMillis) { monthStart(month) }
    val end = remember(month.timeInMillis) { monthEnd(month) }
    val queryStart = remember(month.timeInMillis, habit.frequency) {
        expandedQueryStart(start, habit.frequency)
    }
    val startDate = formatStatsDate(queryStart)
    val endDate = formatStatsDate(end)
    val completedDates by database.habitCompletionDao()
        .observeCompletedDates(habit.id, startDate, endDate)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val monthData = remember(month.timeInMillis) { buildMonthData(month) }
    val visibleCompletedDates = completedDates.toSet()
    val completionRate = completionRateForRange(habit.frequency, start, end, completedDates)
    val bestStreak = bestStreakForRange(habit.frequency, start, end, completedDates)
    val completedCount = completedOpportunityCountForMonth(habit, start, end, completedDates)

    MonthHeader(
        monthOffset = monthOffset,
        onPrevious = onPrevious,
        onNext = onNext
    )

    CalendarLegend()

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(monthData, key = { it.key }) { day ->
            CalendarDay(
                day = day,
                completed = day.date in visibleCompletedDates
            )
        }
    }

    MonthStatsCard(
        completedCount = completedCount,
        completionRate = completionRate,
        bestStreak = bestStreak,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun YearHistory(
    database: HabitDatabase,
    habit: HabitEntity,
    yearOffset: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val year = remember(yearOffset) { yearStart(yearOffset) }
    val end = remember(year.timeInMillis) { yearEnd(year) }
    val queryStart = remember(year.timeInMillis, habit.frequency) {
        expandedQueryStart(year, habit.frequency)
    }
    val startDate = formatStatsDate(queryStart)
    val endDate = formatStatsDate(end)
    val completedDates by database.habitCompletionDao()
        .observeCompletedDatesForYear(habit.id, startDate, endDate)
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val yearRate = completionRateForRange(habit.frequency, year, end, completedDates)
    val yearBestStreak = bestStreakForRange(habit.frequency, year, end, completedDates)
    val yearCompleted = completedOpportunityCountForRange(habit, year, end, completedDates)
    val yearScheduled = scheduledOpportunityCount(habit.frequency, year, end)
    val yearTitle = remember(year.timeInMillis) {
        SimpleDateFormat("yyyy", Locale.getDefault()).format(year.time)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPrevious) { Text("‹") }
        Text(yearTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onNext) { Text("›") }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            StatItem("Completion", "$yearRate%", Modifier.weight(1f))
            StatItem("Completed", "$yearCompleted/$yearScheduled", Modifier.weight(1f))
            StatItem("Best streak", "$yearBestStreak", Modifier.weight(1f))
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (monthIndex in 0..11) {
            item(key = "month-$monthIndex") {
                val month = (year.clone() as Calendar).apply { set(Calendar.MONTH, monthIndex) }
                val monthStart = monthStart(month)
                val monthEnd = monthEnd(month)
                val rate = completionRateForRange(habit.frequency, monthStart, monthEnd, completedDates)
                val completed = completedOpportunityCountForRange(habit, monthStart, monthEnd, completedDates)
                val best = bestStreakForRange(habit.frequency, monthStart, monthEnd, completedDates)
                val title = SimpleDateFormat("MMMM", Locale.getDefault()).format(month.time)
                YearMonthCard(title, rate, completed, best)
            }
        }
    }
}

@Composable
private fun YearMonthCard(title: String, rate: Int, completed: Int, bestStreak: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("$completed days", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.size(12.dp))
            Text("$rate%", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.size(12.dp))
            Text("🔥 $bestStreak", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MonthStatsCard(
    completedCount: Int,
    completionRate: Int,
    bestStreak: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier.fillMaxWidth().then(modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            StatItem("Completed", "$completedCount", Modifier.weight(1f))
            StatItem("Success", "$completionRate%", Modifier.weight(1f))
            StatItem("Best streak", "$bestStreak", Modifier.weight(1f))
        }
    }
}

private fun completedOpportunityCountForMonth(
    habit: HabitEntity,
    start: Calendar,
    end: Calendar,
    completedDates: List<String>
): Int = completedOpportunityCountForRange(habit, start, end, completedDates)

private fun completedOpportunityCountForRange(
    habit: HabitEntity,
    start: Calendar,
    end: Calendar,
    completedDates: List<String>
): Int {
    val scheduled = com.example.habittracker.data.scheduledDatesInRange(habit.frequency, start, end).toSet()
    if (scheduled.isEmpty()) return 0
    return if (habit.frequency.equals("weekly", ignoreCase = true)) {
        completedDates.mapNotNull { parseStatsDate(it) }
            .map { weekStartForStats(it) }
            .toSet()
            .count { it in scheduled }
    } else {
        completedDates.count { it in scheduled }
    }
}

private fun scheduledOpportunityCount(frequency: String, start: Calendar, end: Calendar): Int =
    com.example.habittracker.data.scheduledDatesInRange(frequency, start, end).size

private fun parseStatsDate(value: String): Calendar? = runCatching {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value) ?: return null
    Calendar.getInstance().apply { time = date }
}.getOrNull()

private fun weekStartForStats(date: Calendar): String {
    val monday = date.clone() as Calendar
    monday.firstDayOfWeek = Calendar.MONDAY
    monday.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(monday.time)
}

@Composable
private fun HabitPicker(
    habits: List<HabitEntity>,
    selectedHabit: HabitEntity,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { expanded = true },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Habit", style = MaterialTheme.typography.labelMedium)
                    Text(selectedHabit.name, fontWeight = FontWeight.SemiBold)
                }
                Text("Change", color = MaterialTheme.colorScheme.primary)
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            habits.forEach { habit ->
                DropdownMenuItem(
                    text = { Text(habit.name) },
                    onClick = {
                        onSelected(habit.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(monthOffset: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    val month = remember(monthOffset) { monthCalendar(monthOffset) }
    val title = remember(month.timeInMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onPrevious) { Text("‹") }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        TextButton(onClick = onNext) { Text("›") }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach {
            Text(it, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CalendarDay(day: CalendarDayData, completed: Boolean) {
    if (day.dayOfMonth == 0) {
        Spacer(modifier = Modifier.size(42.dp))
        return
    }

    val background = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (completed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = textColor,
            fontWeight = if (completed) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("No habits to show", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Add a habit first, then its history will appear here.")
    }
}

private data class CalendarDayData(val key: String, val date: String, val dayOfMonth: Int)

private fun buildMonthData(month: Calendar): List<CalendarDayData> {
    val first = month.clone() as Calendar
    first.set(Calendar.DAY_OF_MONTH, 1)
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val result = mutableListOf<CalendarDayData>()

    repeat(firstDayOfWeek) { index -> result += CalendarDayData("empty-$index", "", 0) }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    for (day in 1..daysInMonth) {
        val date = first.clone() as Calendar
        date.set(Calendar.DAY_OF_MONTH, day)
        val formatted = formatter.format(date.time)
        result += CalendarDayData(formatted, formatted, day)
    }

    while (result.size % 7 != 0) {
        val index = result.size
        result += CalendarDayData("empty-$index", "", 0)
    }
    return result
}
