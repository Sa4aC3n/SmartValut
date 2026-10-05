package com.example

import android.app.Application
import com.example.ui.utils.AppText

class SmartVaultApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppText.initialize(this)
    }
}
