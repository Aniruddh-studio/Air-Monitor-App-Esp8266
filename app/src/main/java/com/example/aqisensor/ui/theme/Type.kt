package com.example.aqisensor.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

// ─── Google Fonts provider ─────────────────────────────────────────────────
// Uses the Downloadable Fonts API — no TTF bundled, keeps APK lean.
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage   = "com.google.android.gms",
    certificates      = com.example.aqisensor.R.array.com_google_android_gms_fonts_certs,
)

// ─── JetBrains Mono — numeric readouts ────────────────────────────────────
private val jetBrainsMonoFont = GoogleFont("JetBrains Mono")

val JetBrainsMono = FontFamily(
    Font(googleFont = jetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = jetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = jetBrainsMonoFont, fontProvider = provider, weight = FontWeight.Bold),
)

// ─── Inter — labels and body text ─────────────────────────────────────────
private val interFont = GoogleFont("Inter")

val Inter = FontFamily(
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = interFont, fontProvider = provider, weight = FontWeight.Bold),
)

// ─── Typography scale ──────────────────────────────────────────────────────
val AqiTypography = Typography(
    // Sensor number readout — large mono
    displayLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize   = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp,
    ),
    // Section headers
    headlineMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 18.sp,
        lineHeight = 24.sp,
    ),
    // Toggle row labels
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize   = 15.sp,
        lineHeight = 20.sp,
    ),
    // Toggle row descriptions
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    // Sensor unit labels
    labelSmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize   = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
    // Air quality category
    labelMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),
)
