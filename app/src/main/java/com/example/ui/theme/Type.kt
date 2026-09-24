package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.example.R

val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val TajawalFont = GoogleFont("Tajawal")

val TajawalFontFamily = FontFamily(
    Font(googleFont = TajawalFont, fontProvider = provider, weight = FontWeight.Light),
    Font(googleFont = TajawalFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = TajawalFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = TajawalFont, fontProvider = provider, weight = FontWeight.Bold),
    Font(googleFont = TajawalFont, fontProvider = provider, weight = FontWeight.ExtraBold)
)

private val defaultTypography = Typography()

val Typography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = TajawalFontFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = TajawalFontFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = TajawalFontFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = TajawalFontFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = TajawalFontFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = TajawalFontFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = TajawalFontFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = TajawalFontFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = TajawalFontFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = TajawalFontFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = TajawalFontFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = TajawalFontFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = TajawalFontFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = TajawalFontFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = TajawalFontFamily)
)
