package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.example.ui.screens.DebtsScreen
import com.example.ui.theme.SmartVaultTheme
import com.example.ui.utils.AppStrings
import com.example.ui.utils.AppText
import com.example.ui.utils.PasswordGenerator
import com.example.data.firestore.CloudSyncStatus
import com.example.data.model.VaultItemType
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalizationTest {
    @get:Rule val compose = createComposeRule()

    @Before fun setUp() {
        AppText.initialize(ApplicationProvider.getApplicationContext())
        AppText.setLanguage("ar")
    }

    @After fun reset() {
        ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("smart_vault_user_prefs", Context.MODE_PRIVATE)
            .edit().remove("app_language").commit()
        AppText.setLanguage("ar")
    }

    @Test fun englishCatalogHasNoUntranslatedArabicOrUnexpandedTokens() {
        val args = Array<Any?>(20) { "ARG$it" }
        val fields = R.string::class.java.fields.filter {
            it.name.startsWith("text_") || it.name.startsWith("label_") || it.name == "app_name"
        }
        assertTrue(fields.size > 1000)
        for (field in fields) {
            val english = AppText.textFor("en", field.getInt(null), *args)
            assertFalse(field.name, Regex("\\{\\d+\\}").containsMatchIn(english))
            // A language selector intentionally displays Arabic's native name.
            if (field.name != "label_arabic")
                assertFalse(field.name + ": " + english, Regex("[\\u0600-\\u06ff]").containsMatchIn(english))
            assertTrue(AppText.textFor("ar", field.getInt(null), *args).isNotEmpty())
        }
    }

    @Test fun switchingLanguageRecomposesTheOpenDebtScreen() {
        compose.setContent {
            SmartVaultTheme {
                DebtsScreen(emptyList(), onBack = {}, onAddDebt = { _, _, _, _ -> },
                    onRecordPayment = { _, _, _ -> }, onDeleteDebt = {})
            }
        }
        compose.onNodeWithText("إدارة الديون (لي / عليّ)").assertIsDisplayed()
        compose.runOnIdle { AppText.setLanguage("en") }
        compose.onNodeWithText("Debt management (owed to me / I owe)").assertIsDisplayed()
        compose.runOnIdle { AppText.setLanguage("ar") }
        compose.onNodeWithText("إدارة الديون (لي / عليّ)").assertIsDisplayed()
    }

    @Test fun labelsAndDatesFollowLanguageWithoutChangingStoredValues() {
        val storedGoldType = "سبيكة"
        val status = CloudSyncStatus.CONNECTED
        val strength = PasswordGenerator.PasswordStrength.STRONG
        AppText.setLanguage("en")
        assertEquals("Bullion", AppText.goldType(storedGoldType))
        assertEquals("سبيكة", storedGoldType)
        assertEquals("Connected to cloud", status.labelAr)
        assertEquals("Strong", strength.labelAr)
        assertEquals("Password", VaultItemType.PASSWORD.localizedTitle)
        assertEquals("EGP", AppText.currency("ج.م"))
        assertEquals("Home", AppStrings("en").navHome)
        assertEquals("January", SimpleDateFormat("MMMM", AppText.locale).format(Date(0)))
        AppText.setLanguage("ar")
        assertEquals("متصل بالسحابة", status.labelAr)
        assertEquals("قوية", strength.labelAr)
        assertEquals("سبيكة", AppText.goldType(storedGoldType))
    }

    @Test fun missingResourcesAndArgumentsAreNotSilentlyHidden() {
        assertThrows(android.content.res.Resources.NotFoundException::class.java) {
            AppText.textFor("en", 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            AppText.textFor("en", R.string.text_9e5ba8f6616f)
        }
    }

    @Test fun persistedLanguageIsRestoredAndArgumentsAreNotTranslated() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("smart_vault_user_prefs", Context.MODE_PRIVATE)
            .edit().putString("app_language", "en").commit()
        AppText.initialize(context)
        assertEquals("en", AppText.language)
        val userText = "حسابي {1} $100"
        assertTrue(AppText.text(R.string.text_9e5ba8f6616f, userText, "500", "EGP").contains(userText))
        assertEquals("تصنيف خاص", AppStrings("en").translateCategory("تصنيف خاص"))
    }
}
