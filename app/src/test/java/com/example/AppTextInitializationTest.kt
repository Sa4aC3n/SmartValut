package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.utils.AppText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class AppTextInitializationTest {
    @Test fun missingInitializationIsReportedInsteadOfReturningBlankText() {
        val error = assertThrows(IllegalStateException::class.java) {
            AppText.text(R.string.app_name)
        }
        assertTrue(error.message!!.contains("AppText must be initialized"))
        AppText.initialize(ApplicationProvider.getApplicationContext())
        assertEquals("Smart Vault", AppText.textFor("en", R.string.app_name))
    }
}
