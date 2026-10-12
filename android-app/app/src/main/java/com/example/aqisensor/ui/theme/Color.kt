package com.example.aqisensor.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Light mode palette ────────────────────────────────────────────────────
val PanelLight       = Color(0xFFF7F5EF) // warm off-white panel
val BackgroundLight  = Color(0xFFEDEAE2) // parchment background
val SurfaceVarLight  = Color(0xFFE8E5DB)
val OnPanelLight     = Color(0xFF1C1B1A)
val OutlineLight     = Color(0xFFCCC9BF)

// ─── Dark mode palette ─────────────────────────────────────────────────────
val PanelDark        = Color(0xFF1B2024) // deep charcoal panel
val BackgroundDark   = Color(0xFF14181B) // near-black background
val SurfaceVarDark   = Color(0xFF242B30)
val OnPanelDark      = Color(0xFFE8E4DA)
val OutlineDark      = Color(0xFF3A4249)

// ─── Accent / status tokens ────────────────────────────────────────────────
val AccentGreen      = Color(0xFF6FDCA0) // Good (0–50)
val AccentYellow     = Color(0xFFFFD54F) // Moderate (51–100)
val AccentOrange     = Color(0xFFFF9800) // Unhealthy for Sensitive Groups (101–150)
val AccentRed        = Color(0xFFE2645A) // Bad / Unhealthy (151–200)
val AccentPurple     = Color(0xFF9C27B0) // Very Bad / Very Unhealthy (201–300)
val AccentMaroon     = Color(0xFF7E0023) // Hazardous (301–500)
val AccentAmber      = Color(0xFFE8B04B) // off / standby (kept for non-AQI uses)

// ─── Chart / analytics accent colours ──────────────────────────────────────
val ChartTemp        = Color(0xFFFF7043) // warm orange-red for temperature
val ChartHumidity    = Color(0xFF4FC3F7) // cool sky-blue for humidity
val ChartDust        = Color(0xFFE8B04B) // amber for dust (matches AccentAmber)

// ─── AQI category helpers ──────────────────────────────────────────────────

/**
 * Maps the device-reported category string to a UI colour.
 *
 * | AQI     | Level                             | Color  | App Display |
 * |--------:|-----------------------------------|--------|-------------|
 * |   0–50  | Good                              | Green  | Good        |
 * |  51–100 | Moderate                          | Yellow | Moderate    |
 * | 101–150 | Unhealthy for Sensitive Groups    | Orange | Unhealthy   |
 * | 151–200 | Unhealthy                         | Red    | Bad         |
 * | 201–300 | Very Unhealthy                    | Purple | Very Bad    |
 * | 301–500 | Hazardous                         | Maroon | Hazardous   |
 */
fun categoryColor(category: String): Color = when (category.lowercase()) {
    "good"                              -> AccentGreen
    "moderate"                          -> AccentYellow
    "unhealthy", "unhealthy for sensitive groups" -> AccentOrange
    "bad"                               -> AccentRed
    "very bad", "very unhealthy"        -> AccentPurple
    "hazardous"                         -> AccentMaroon
    else                                -> AccentAmber
}

/**
 * Maps a raw AQI integer (typically derived from dust PM2.5 µg/m³)
 * to the display category label used in the app UI.
 */
fun aqiCategory(aqi: Int): String = when {
    aqi <= 50  -> "Good"
    aqi <= 100 -> "Moderate"
    aqi <= 150 -> "Unhealthy"
    aqi <= 200 -> "Bad"
    aqi <= 300 -> "Very Bad"
    else       -> "Hazardous"
}

/**
 * Maps a raw AQI integer to its colour.
 */
fun aqiColor(aqi: Int): Color = when {
    aqi <= 50  -> AccentGreen
    aqi <= 100 -> AccentYellow
    aqi <= 150 -> AccentOrange
    aqi <= 200 -> AccentRed
    aqi <= 300 -> AccentPurple
    else       -> AccentMaroon
}
