# Habit Tracker

Native Android Habit Tracker built with Kotlin and Jetpack Compose.

## Current features
- Add, edit, and delete habits
- Daily, weekday, and weekly frequencies
- Completion history stored locally with Room
- Month and year history with completion rate and streak statistics
- Light, dark, and system themes
- Refined Material 3 typography and color palette
- JSON export/import backup for habits and completion history
- Feedback sharing and issue reporting from Settings
- Reset all local data
- Android Auto Backup enabled in the application manifest

## Tech stack
- Kotlin
- Jetpack Compose + Material 3
- Room
- Kotlin coroutines and Flow
- Gradle / Android Gradle Plugin

## Build
Open the repository as an Android Studio project and sync Gradle. The GitHub Actions workflow also builds the debug APK on pushes and pull requests targeting `main`.
