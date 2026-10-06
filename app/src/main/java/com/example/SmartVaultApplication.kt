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
            if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("com.smartsafe.app")
                    .setApiKey("AIzaSyFallbackKeyForSafeAppOperation000")
                    .setProjectId("smartsafe-local")
                    .build()
                com.google.firebase.FirebaseApp.initializeApp(this, options)
            }
        } catch (e: Exception) {
            Log.w("SmartVaultApp", "Firebase safe init fallback: ${e.message}")
        }
    }
}

