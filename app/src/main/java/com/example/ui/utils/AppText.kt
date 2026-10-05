package com.example.ui.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/** Shared language for Compose, dialogs, reports and background workers. */
object AppText {
    private var currentLanguage by mutableStateOf("ar")
    val language: String get() = currentLanguage
    private lateinit var arabic: Resources
    private lateinit var english: Resources
    private val argumentPattern = Regex("\\{(\\d+)\\}")

    fun initialize(context: Context) {
        fun resources(code: String): Resources {
            val config = Configuration(context.resources.configuration)
            config.setLocale(Locale.forLanguageTag(code))
            return context.createConfigurationContext(config).resources
        }
        arabic = resources("ar")
        english = resources("en")
        setLanguage(context.getSharedPreferences("smart_vault_user_prefs", Context.MODE_PRIVATE)
            .getString("app_language", "ar") ?: "ar")
    }

    fun setLanguage(code: String) {
        currentLanguage = if (code == "en") "en" else "ar"
    }

    val locale: Locale get() = Locale.forLanguageTag(language)

    /** Display-only mappings: callers retain the original database/filter values. */
    fun goldType(value: String): String = when (value) {
        "سبيكة" -> text(com.example.R.string.text_c15fe79b40d5)
        "مشغولات" -> text(com.example.R.string.text_25e01e43fe84)
        "عملة ذهبية" -> text(com.example.R.string.text_acf535a08f2a)
        "جنيه ذهب" -> text(com.example.R.string.text_add83a5f7f2d)
        "أخرى" -> text(com.example.R.string.text_83b4b55adb2a)
        else -> value
    }

    fun currency(value: String): String = if (language != "en") value else when (value) {
        "ج.م", "جنيه", "جنيه مصري" -> "EGP"
        "ر.س", "ريال", "ريال سعودي" -> "SAR"
        "د.إ", "درهم", "درهم إماراتي" -> "AED"
        "دولار", "دولار أمريكي" -> "USD"
        "يورو" -> "EUR"
        "د.ك" -> "KWD"
        "د.ب" -> "BHD"
        "ر.ع" -> "OMR"
        "ر.ق" -> "QAR"
        else -> value
    }

    fun text(@StringRes id: Int, vararg arguments: Any?): String =
        textFor(language, id, *arguments)

    fun textFor(code: String, @StringRes id: Int, vararg arguments: Any?): String {
        val template = (if (code == "en") english else arabic).getString(id)
        // Never interpret or translate user-provided arguments.
        return argumentPattern.replace(template) { match ->
            val index = match.groupValues[1].toInt()
            require(index < arguments.size) { "Missing translation argument $index" }
            arguments[index].toString()
        }
    }
}
