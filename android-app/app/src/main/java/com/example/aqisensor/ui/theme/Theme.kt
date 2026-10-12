package com.example.aqisensor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Light colour scheme ───────────────────────────────────────────────────
private val LightColors = lightColorScheme(
    primary            = AccentGreen,
    onPrimary          = Color(0xFF003824),
    primaryContainer   = Color(0xFF9FF5C1),
    onPrimaryContainer = Color(0xFF00210F),
    secondary          = AccentAmber,
    onSecondary        = Color(0xFF3C2800),
    secondaryContainer = Color(0xFFFFDF9E),
    onSecondaryContainer = Color(0xFF261900),
    error              = AccentRed,
    onError            = Color(0xFF690005),
    background         = BackgroundLight,
    onBackground       = OnPanelLight,
    surface            = PanelLight,
    onSurface          = OnPanelLight,
    surfaceVariant     = SurfaceVarLight,
    onSurfaceVariant   = Color(0xFF4A4742),
    outline            = OutlineLight,
)

// ─── Dark colour scheme ────────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary            = AccentGreen,
    onPrimary          = Color(0xFF003824),
    primaryContainer   = Color(0xFF005229),
    onPrimaryContainer = Color(0xFF9FF5C1),
    secondary          = AccentAmber,
    onSecondary        = Color(0xFF3C2800),
    secondaryContainer = Color(0xFF564300),
    onSecondaryContainer = Color(0xFFFFDF9E),
    error              = AccentRed,
    onError            = Color(0xFF690005),
    background         = BackgroundDark,
    onBackground       = OnPanelDark,
    surface            = PanelDark,
    onSurface          = OnPanelDark,
    surfaceVariant     = SurfaceVarDark,
    onSurfaceVariant   = Color(0xFFCCC5B8),
    outline            = OutlineDark,
)

@Composable
fun AqiSensorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = AqiTypography,
        content     = content,
    )
}
