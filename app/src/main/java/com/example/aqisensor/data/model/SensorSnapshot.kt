package com.example.aqisensor.data.model

/**
 * A single point in the on-device history ring buffer.
 *
 * Stored entirely in ViewModel memory — never sent to or from the ESP8266.
 * The ring buffer is capped at [MAX_HISTORY] entries (≈ 2 minutes at 2 s polling).
 */
data class SensorSnapshot(
    /** System clock millis when this reading was received. */
    val timestampMs: Long,
    val tempC: Double,
    val humidityPct: Double,
    /** Raw dust concentration in µg/m³ from the sensor. */
    val dustUgM3: Double,
) {
    companion object {
        /** Maximum points kept. 60 × 2 s = 2-minute rolling window. */
        const val MAX_HISTORY = 60
    }
}
