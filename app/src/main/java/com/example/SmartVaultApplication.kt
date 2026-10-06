package com.example

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.ui.utils.AppText

class SmartVaultApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        AppText.initialize(this)
        initFirebaseSafely()
    }

    private fun initFirebaseSafely() {
        try {
            // Load only the real options generated from google-services.json.
            // Missing configuration is supported as local-only mode, not a fake project.
            if (com.google.firebase.FirebaseApp.initializeApp(this) == null) {
                Log.w("SmartVaultApp", "Firebase configuration missing; cloud services disabled")
            }
        } catch (e: Exception) {
            Log.w("SmartVaultApp", "Firebase initialization failed; cloud services disabled", e)
        }
    }
}

