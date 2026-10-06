package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.utils.AppLanguageProvider
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ActivityResultLocalizationTest {
    @get:Rule val compose = createComposeRule()

    private fun findActivity(context: Context): Activity? {
        var current = context
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return current as? Activity
    }

    @Test fun launchersRemainConnectedToActivityAcrossLanguageChanges() {
        val language = mutableStateOf("ar")
        var localizedContext: Context? = null
        var hostActivity: Activity? = null
        var direction: LayoutDirection? = null
        var result: String? = null
        var launcher: ActivityResultLauncher<String>? = null
        val contract = object : ActivityResultContract<String, String>() {
            override fun createIntent(context: Context, input: String) = Intent()
            override fun parseResult(resultCode: Int, intent: Intent?) = "unexpected external result"
            override fun getSynchronousResult(context: Context, input: String) = SynchronousResult(input)
        }
        compose.setContent {
            val activity = findActivity(LocalContext.current)
            AppLanguageProvider(language.value) {
                val context = LocalContext.current
                val owner = LocalActivityResultRegistryOwner.current
                val layout = LocalLayoutDirection.current
                // Same permission registration used unconditionally by MainAppContent at startup.
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
                // Also cover backup export and receipt import, without opening external UI.
                rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {}
                rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {}
                val registered = rememberLauncherForActivityResult(contract) { result = it }
                SideEffect {
                    assertNotNull(activity)
                    assertSame(activity, owner)
                    hostActivity = activity
                    localizedContext = context
                    direction = layout
                    launcher = registered
                }
            }
        }
        for (code in listOf("ar", "en", "ar")) {
            compose.runOnIdle { language.value = code }
            compose.waitForIdle()
            compose.runOnIdle {
                assertSame(hostActivity, findActivity(requireNotNull(localizedContext)))
                assertEquals(code, localizedContext!!.resources.configuration.locales[0].language)
                assertEquals(if (code == "en") LayoutDirection.Ltr else LayoutDirection.Rtl, direction)
                assertEquals(if (code == "en") "Smart Vault" else "الخزنة الذكية", localizedContext!!.getString(R.string.app_name))
                launcher!!.launch("result-$code")
            }
            compose.waitForIdle()
            compose.runOnIdle { assertEquals("result-$code", result) }
        }
    }
}
