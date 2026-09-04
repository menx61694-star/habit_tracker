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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HistoryScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val database = remember { HabitDatabase.getInstance(context) }
    val habits by database.habitDao().observeHabits()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedHabitId by remember { mutableIntStateOf(-1) }
    var monthOffset by remember { mutableIntStateOf(0) }

    val selectedHabit = habits.firstOrNull { it.id == selectedHabitId } ?: habits.firstOrNull()

    androidx.compose.runtime.LaunchedEffect(habits) {
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

        Spacer(modifier = Modifier.height(16.dp))

        MonthHeader(
            monthOffset = monthOffset,
            onPrevious = { monthOffset-- },
            onNext = { monthOffset++ }
        )

        val month = remember(monthOffset) { Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) } }
        val monthData = remember(month.timeInMillis) { buildMonthData(month) }
        val startDate = monthData.first().date
        val endDate = monthData.last().date
        val completedDates by database.habitCompletionDao()
            .observeCompletedDates(selectedHabit.id, startDate, endDate)
            .collectAsStateWithLifecycle(initialValue = emptyList())

        CalendarLegend()

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(monthData, key = { it.key }) { day ->
                CalendarDay(
                    day = day,
                    completed = day.date in completedDates
                )
            }
        }

        val completedCount = completedDates.size
        val totalDays = monthData.count { it.dayOfMonth > 0 }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("This month", fontWeight = FontWeight.SemiBold)
                    Text("$completedCount completed days")
                }
                Text(
                    text = if (totalDays == 0) "0%" else "${completedCount * 100 / totalDays}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
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

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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
private fun MonthHeader(
    monthOffset: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val month = remember(monthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
    }
    val title = remember(month.timeInMillis) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(month.time)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month")
        }
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next month")
        }
    }
}

@Composable
private fun CalendarLegend() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach {
            Text(
                text = it,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CalendarDay(day: CalendarDayData, completed: Boolean) {
    if (day.dayOfMonth == 0) {
        Spacer(modifier = Modifier.size(42.dp))
        return
    }

    val background = if (completed) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (completed) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(background),
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

private data class CalendarDayData(
    val key: String,
    val date: String,
    val dayOfMonth: Int
)

private fun buildMonthData(month: Calendar): List<CalendarDayData> {
    val first = month.clone() as Calendar
    first.set(Calendar.DAY_OF_MONTH, 1)
    val daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
    val result = mutableListOf<CalendarDayData>()

    repeat(firstDayOfWeek) { index ->
        result += CalendarDayData("empty-$index", "", 0)
    }

    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    for (day in 1..daysInMonth) {
        val date = first.clone() as Calendar
        date.set(Calendar.DAY_OF_MONTH, day)
        result += CalendarDayData(
            key = formatter.format(date.time),
            date = formatter.format(date.time),
            dayOfMonth = day
        )
    }

    while (result.size % 7 != 0) {
        val index = result.size
        result += CalendarDayData("empty-$index", "", 0)
    }
    return result
}
