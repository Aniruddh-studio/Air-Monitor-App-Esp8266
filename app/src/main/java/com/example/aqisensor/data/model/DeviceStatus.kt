package com.example.aqisensor.data.model

import com.google.gson.annotations.SerializedName

/**
 * Mirrors the JSON payload returned by GET /status on the ESP8266 device.
 *
 * {
 *   "fan": true,
 *   "tempC": 23.8,
 *   "humidityPct": 73.0,
 *   "dustUgM3": 28.45,
 *   "wifi": true
 * }
 */
data class DeviceStatus(
    @SerializedName("fan")         val fanOn: Boolean = true,
    @SerializedName("tempC")       val tempC: Double,
    @SerializedName("humidityPct") val humidityPct: Double,
    @SerializedName("dustUgM3")    val dustUgM3: Double,
    @SerializedName("wifi")        val wifi: Boolean = true,
)
