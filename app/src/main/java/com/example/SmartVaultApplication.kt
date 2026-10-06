package com.example

import android.app.Application
import android.util.Log
import com.example.ui.utils.AppText
import com.google.firebase.FirebaseApp

class SmartVaultApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppText.initialize(this)
        validateFirebaseInDebug()
    }

    private fun validateFirebaseInDebug() {
        if (BuildConfig.DEBUG && FirebaseApp.getApps(this).isNotEmpty()) {
            val options = FirebaseApp.getInstance().options
            val projectId = options.projectId
            if (projectId != "smartsafe-cd443") {
                Log.e("SmartVaultApp", "[SECURITY] Invalid Firebase projectId in DEBUG: expected 'smartsafe-cd443', got '$projectId'")
            }
        }
    }
}


