package com.example.aqisensor.data.network

import com.example.aqisensor.data.model.DeviceStatus
import com.example.aqisensor.data.model.ToggleRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface mapping the ESP8266's HTTP API.
 *
 * Base URL is constructed dynamically from the user-supplied IP address,
 * so a fresh Retrofit instance is created whenever the IP changes.
 *
 * Note: POST /toggle/display was removed — the OLED display has been
 * taken off the hardware.
 */
interface AqiApiService {

    /**
     * Poll device state every 2 seconds.
     * Returns all sensor readings + toggle states.
     */
    @GET("status")
    suspend fun getStatus(): DeviceStatus

    /**
     * Toggle the fan on or off.
     */
    @POST("toggle/fan")
    suspend fun toggleFan(@Body body: ToggleRequest)
}
