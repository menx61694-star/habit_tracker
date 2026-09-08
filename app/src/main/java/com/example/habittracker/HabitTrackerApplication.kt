package com.example.habittracker

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class HabitTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Firebase is optional for the open-source build. If no
        // google-services.json is configured, leave the local app working.
        val firebaseApp = FirebaseApp.initializeApp(this) ?: return
        val appCheck = FirebaseAppCheck.getInstance(firebaseApp)

        if (BuildConfig.DEBUG) {
            appCheck.installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            appCheck.installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }
    }
}
