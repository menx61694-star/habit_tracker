package com.example.habittracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = HabitGreen,
    onPrimary = HabitSurfaceLight,
    primaryContainer = ColorTokens.HabitGreenContainerLight,
    onPrimaryContainer = ColorTokens.HabitGreenOnContainerLight,
    secondary = HabitBlue,
    onSecondary = HabitSurfaceLight,
    secondaryContainer = ColorTokens.HabitBlueContainerLight,
    onSecondaryContainer = ColorTokens.HabitBlueOnContainerLight,
    tertiary = HabitOrange,
    onTertiary = HabitSurfaceLight,
    background = HabitBackgroundLight,
    onBackground = ColorTokens.HabitOnBackgroundLight,
    surface = HabitSurfaceLight,
    onSurface = ColorTokens.HabitOnSurfaceLight,
    surfaceVariant = HabitSurfaceVariantLight,
    onSurfaceVariant = HabitOnSurfaceVariantLight
)

private val DarkColors = darkColorScheme(
    primary = HabitGreenLight,
    onPrimary = ColorTokens.HabitGreenOnPrimaryDark,
    primaryContainer = ColorTokens.HabitGreenContainerDark,
    onPrimaryContainer = ColorTokens.HabitGreenOnContainerDark,
    secondary = HabitBlueLight,
    onSecondary = ColorTokens.HabitBlueOnPrimaryDark,
    secondaryContainer = ColorTokens.HabitBlueContainerDark,
    onSecondaryContainer = ColorTokens.HabitBlueOnContainerDark,
    tertiary = HabitOrangeLight,
    onTertiary = ColorTokens.HabitOrangeOnPrimaryDark,
    background = HabitBackgroundDark,
    onBackground = ColorTokens.HabitOnBackgroundDark,
    surface = HabitSurfaceDark,
    onSurface = ColorTokens.HabitOnSurfaceDark,
    surfaceVariant = HabitSurfaceVariantDark,
    onSurfaceVariant = HabitOnSurfaceVariantDark
)

@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HabitTypography,
        content = content
    )
}

private object ColorTokens {
    val HabitGreenContainerLight = androidx.compose.ui.graphics.Color(0xFFD7F0E3)
    val HabitGreenOnContainerLight = androidx.compose.ui.graphics.Color(0xFF0D3B27)
    val HabitBlueContainerLight = androidx.compose.ui.graphics.Color(0xFFDCE7FA)
    val HabitBlueOnContainerLight = androidx.compose.ui.graphics.Color(0xFF10284F)
    val HabitOrangeOnPrimaryDark = androidx.compose.ui.graphics.Color(0xFF2B1707)
    val HabitGreenContainerDark = androidx.compose.ui.graphics.Color(0xFF214936)
    val HabitGreenOnContainerDark = androidx.compose.ui.graphics.Color(0xFFB9E9CF)
    val HabitBlueContainerDark = androidx.compose.ui.graphics.Color(0xFF293C63)
    val HabitBlueOnContainerDark = androidx.compose.ui.graphics.Color(0xFFC5D5FF)
    val HabitBlueOnPrimaryDark = androidx.compose.ui.graphics.Color(0xFF10284F)
    val HabitOnBackgroundLight = androidx.compose.ui.graphics.Color(0xFF17201A)
    val HabitOnSurfaceLight = androidx.compose.ui.graphics.Color(0xFF17201A)
    val HabitOnBackgroundDark = androidx.compose.ui.graphics.Color(0xFFE8EEE9)
    val HabitOnSurfaceDark = androidx.compose.ui.graphics.Color(0xFFE8EEE9)
}
