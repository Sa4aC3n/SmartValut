package com.example.ui.utils

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/** Keep the Activity in the ContextWrapper chain for activity results and platform dialogs. */
@Composable
fun AppLanguageProvider(language: String, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val localizedContext = remember(baseContext, baseConfiguration, language) {
        ContextThemeWrapper(baseContext, 0).apply {
            applyOverrideConfiguration(Configuration(baseConfiguration).apply {
                setLocale(Locale.forLanguageTag(language))
            })
        }
    }
    val strings = remember(language) { AppStrings(language) }
    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl,
        LocalAppLanguage provides language,
        LocalStrings provides strings,
        content = content
    )
}
