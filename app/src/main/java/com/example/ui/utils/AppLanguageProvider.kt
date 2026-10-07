package com.example.ui.utils

import android.content.res.Configuration
import android.view.ContextThemeWrapper
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import java.util.Locale

/**
 * Applies the in-app locale without breaking Activity-backed Compose owners.
 *
 * rememberLauncherForActivityResult() resolves its registry owner from
 * LocalActivityResultRegistryOwner, which can otherwise fall back to
 * LocalContext. Replacing LocalContext with a localized ContextThemeWrapper
 * can make that fallback fail on real devices. Capture and validate the owner
 * before replacing the context, then explicitly re-provide it.
 */
@Composable
fun AppLanguageProvider(language: String, content: @Composable () -> Unit) {
    val baseContext = LocalContext.current
    val baseConfiguration = LocalConfiguration.current

    // Must be resolved from the Activity-backed composition before LocalContext
    // is replaced with the localized wrapper.
    val activityResultRegistryOwner = requireNotNull(LocalActivityResultRegistryOwner.current) {
        "AppLanguageProvider requires an ActivityResultRegistryOwner from the host Activity"
    }

    val localizedContext = remember(baseContext, baseConfiguration, language) {
        ContextThemeWrapper(baseContext, 0).apply {
            applyOverrideConfiguration(Configuration(baseConfiguration).apply {
                setLocale(Locale.forLanguageTag(language))
            })
        }
    }

    val strings = remember(language) {
        AppText.setLanguage(language)
        AppStrings(language)
    }

    CompositionLocalProvider(
        LocalActivityResultRegistryOwner provides activityResultRegistryOwner,
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides if (language == "en") LayoutDirection.Ltr else LayoutDirection.Rtl,
        LocalAppLanguage provides language,
        LocalStrings provides strings,
        content = content
    )
}
