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
- Firebase SDK foundation for Analytics, Crashlytics, Authentication, and Cloud Firestore

## Tech stack
- Kotlin
- Jetpack Compose + Material 3
- Room
- Firebase Android BoM
- Firebase Analytics, Crashlytics, Authentication, and Cloud Firestore
- Kotlin coroutines and Flow
- Gradle / Android Gradle Plugin

## Firebase setup
The repository keeps Firebase configuration out of source control until a Firebase project is connected.

1. Create or select a Firebase project in the Firebase console.
2. Register the Android app using the application ID `com.example.habittracker`.
3. Download `google-services.json`.
4. Put the file at `app/google-services.json`.
5. Re-sync/build the project.

The Gradle configuration automatically enables the Google Services and Crashlytics plugins when `app/google-services.json` exists. Until then, the project still builds in CI without a Firebase project attached.

## Build
Open the repository as an Android Studio project and sync Gradle. The GitHub Actions workflow also builds the debug APK on pushes and pull requests targeting `main`.
