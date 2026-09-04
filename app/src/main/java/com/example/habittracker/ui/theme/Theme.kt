package com.example.habittracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = HabitGreen,
    secondary = HabitBlue,
    tertiary = HabitOrange
)

private val DarkColors = darkColorScheme(
    primary = HabitGreenLight,
    secondary = HabitBlueLight,
    tertiary = HabitOrangeLight
)

@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HabitTypography,
        content = content
    )
}
