package com.example.aqisensor.data.repository

import com.example.aqisensor.data.model.DeviceStatus
import com.example.aqisensor.data.model.ToggleRequest
import com.example.aqisensor.data.network.AqiApiService
import com.example.aqisensor.data.network.NetworkModule

/**
 * Repository that wraps the network layer.
 *
 * Constructed with an [ipAddress]; call [updateIp] to swap the target device.
 * All public functions are suspend functions — call from a coroutine scope.
 *
 * Errors propagate as exceptions to the caller (the ViewModel), which
 * catches them and maps them to UI state.
 */
class AqiRepository(ipAddress: String) {

    private var api: AqiApiService = NetworkModule.createApiService(ipAddress)

    /** Replace the target device IP and rebuild the API client. */
    fun updateIp(newIp: String) {
        api = NetworkModule.createApiService(newIp)
    }

    // ─── Read ──────────────────────────────────────────────────────────────

    /**
     * Fetch the current device status.
     * @throws Exception on network/timeout/parse error
     */
    suspend fun getStatus(): DeviceStatus = api.getStatus()

    // ─── Write ─────────────────────────────────────────────────────────────

    suspend fun toggleFan(on: Boolean) = api.toggleFan(ToggleRequest(on))
}
