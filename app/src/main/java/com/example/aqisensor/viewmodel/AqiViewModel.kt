package com.example.aqisensor.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aqisensor.data.model.SensorSnapshot
import com.example.aqisensor.data.model.SensorSnapshot.Companion.MAX_HISTORY
import com.example.aqisensor.data.repository.AqiRepository
import com.example.aqisensor.datastore.IpAddressDataStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

// ─── US EPA PM2.5 → AQI Calculation ───────────────────────────────────────

/**
 * US EPA PM2.5 AQI breakpoint table.
 *
 * Each row:  (BPLo, BPHi, ILo, IHi)
 *   BPLo / BPHi = concentration breakpoints (µg/m³)
 *   ILo  / IHi  = corresponding AQI breakpoints
 */
private data class AqiBreakpoint(
    val bpLo: Double,
    val bpHi: Double,
    val aqiLo: Int,
    val aqiHi: Int,
)

private val PM25_BREAKPOINTS = listOf(
    AqiBreakpoint(  0.0,  12.0,   0,  50),
    AqiBreakpoint( 12.1,  35.4,  51, 100),
    AqiBreakpoint( 35.5,  55.4, 101, 150),
    AqiBreakpoint( 55.5, 150.4, 151, 200),
    AqiBreakpoint(150.5, 250.4, 201, 300),
    AqiBreakpoint(250.5, 500.0, 301, 500),
)

/**
 * Converts a PM2.5 concentration (µg/m³) to a US EPA AQI value
 * using the standard linear interpolation formula:
 *
 *   Ip = (IHi − ILo) / (BPHi − BPLo) × (Cp − BPLo) + ILo
 *
 * Concentrations above 500 µg/m³ are clamped to AQI 500.
 * Negative values return AQI 0.
 */
fun calculateAqi(pm25: Double): Int {
    if (pm25 < 0.0) return 0
    // Truncate to one decimal place per EPA spec
    val cp = (pm25 * 10).toInt() / 10.0
    for (bp in PM25_BREAKPOINTS) {
        if (cp <= bp.bpHi) {
            val aqi = ((bp.aqiHi - bp.aqiLo).toDouble() / (bp.bpHi - bp.bpLo)) *
                    (cp - bp.bpLo) + bp.aqiLo
            return aqi.roundToInt()
        }
    }
    return 500 // above the highest breakpoint
}

/**
 * Returns the EPA category label for a given AQI value.
 */
fun aqiCategoryFromValue(aqi: Int): String = when {
    aqi <= 50  -> "Good"
    aqi <= 100 -> "Moderate"
    aqi <= 150 -> "Unhealthy (SG)"
    aqi <= 200 -> "Unhealthy"
    aqi <= 300 -> "Very Unhealthy"
    else       -> "Hazardous"
}

// ─── UI State ──────────────────────────────────────────────────────────────

/** Sensor readings parsed from the device. */
data class SensorData(
    val tempC: Double       = 0.0,
    val humidityPct: Double = 0.0,
    /** Raw dust concentration in µg/m³ from the sensor. */
    val dustUgM3: Double    = 0.0,
    /** Average dust concentration in µg/m³ calculated from the rolling history. */
    val avgDustUgM3: Double = 0.0,
    /** AQI calculated on the phone from avgDustUgM3. */
    val aqi: Int            = 0,
    /** EPA category derived from the calculated AQI. */
    val category: String    = "—",
    val fanOn: Boolean      = true,
)

/**
 * Change vs. the previous reading. Null means there is no previous reading yet.
 * Positive = went up, negative = went down, zero = unchanged.
 */
data class SensorDelta(
    val tempC: Double?,
    val humidityPct: Double?,
    val dustUgM3: Double?,
)


/** The complete UI state tree consumed by Composable screens. */
sealed interface UiState {
    /** First launch: no IP saved yet. */
    data object NeedsIp : UiState

    /** Connected and polling successfully. */
    data class Connected(
        val ip: String,
        val sensors: SensorData,
        /** Rolling history ring (newest last), capped at [MAX_HISTORY]. */
        val history: List<SensorSnapshot>,
        /** Delta vs. the previous reading. Null fields = first reading. */
        val delta: SensorDelta,
    ) : UiState

    /** Three consecutive failures — device unreachable. */
    data class Error(
        val ip: String,
        val message: String,
    ) : UiState

    /** Initial loading state before the first poll completes. */
    data class Loading(val ip: String) : UiState
}

// ─── ViewModel ─────────────────────────────────────────────────────────────

/**
 * Central ViewModel that manages:
 * - IP address persistence (DataStore)
 * - Repository lifecycle (recreated when IP changes)
 * - 2-second polling loop (lifecycle-aware via [startPolling] / [stopPolling])
 * - Error state after 3 consecutive failures
 * - On-device rolling history (phone-side only — no extra ESP8266 load)
 * - AQI calculation from raw PM2.5 µg/m³ using US EPA formula
 */
class AqiViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStore = IpAddressDataStore(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading(""))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var repository: AqiRepository? = null
    private var pollJob: Job? = null
    private var consecutiveFailures = 0
    private val maxFailures = 3

    /**
     * Rolling history buffer — stored on the phone only.
     * The ESP8266 never sees this; we simply accumulate every poll result here.
     */
    private val historyBuffer = ArrayDeque<SensorSnapshot>(MAX_HISTORY + 1)

    // ── Current saved IP (nullable while DataStore loads) ──────────────────
    private val _savedIp = MutableStateFlow<String?>(null)

    init {
        // Observe the DataStore IP and re-initialise whenever it changes
        viewModelScope.launch {
            dataStore.deviceIpFlow.collect { ip ->
                _savedIp.value = ip
                if (ip.isNullOrBlank()) {
                    _uiState.value = UiState.NeedsIp
                    stopPolling()
                } else {
                    initRepository(ip)
                }
            }
        }
    }

    private fun initRepository(ip: String) {
        if (repository == null) {
            repository = AqiRepository(ip)
        } else {
            repository!!.updateIp(ip)
        }
        // Clear history when switching device / IP
        historyBuffer.clear()
        _uiState.value = UiState.Loading(ip)
        consecutiveFailures = 0
        // Start polling immediately when IP is ready — fixes the race where
        // LaunchedEffect fires before DataStore finishes loading the saved IP.
        stopPolling()
        startPolling()
    }

    // ─── Lifecycle control (called from UI layer) ──────────────────────────

    /** Begin (or resume) the 2-second poll loop. */
    fun startPolling() {
        val repo = repository ?: return
        val ip   = _savedIp.value ?: return
        if (pollJob?.isActive == true) return

        pollJob = viewModelScope.launch {
            while (isActive) {
                try {
                    val status = repo.getStatus()
                    consecutiveFailures = 0

                    // ── History accumulation (phone-only, zero ESP8266 impact) ──
                    val previous = historyBuffer.lastOrNull()
                    val snapshot = SensorSnapshot(
                        timestampMs  = System.currentTimeMillis(),
                        tempC        = status.tempC,
                        humidityPct  = status.humidityPct,
                        dustUgM3     = status.dustUgM3,
                    )
                    historyBuffer.addLast(snapshot)
                    if (historyBuffer.size > MAX_HISTORY) historyBuffer.removeFirst()

                    val avgDust = historyBuffer.map { it.dustUgM3 }.average()

                    // ── AQI calculated on the phone from average dust ───────
                    val aqi = calculateAqi(avgDust)
                    val category = aqiCategoryFromValue(aqi)

                    val sensors = SensorData(
                        tempC       = status.tempC,
                        humidityPct = status.humidityPct,
                        dustUgM3    = status.dustUgM3,
                        avgDustUgM3 = avgDust,
                        aqi         = aqi,
                        category    = category,
                        fanOn       = status.fanOn,
                    )

                    // ── Delta vs previous reading ──────────────────────────────
                    val delta = SensorDelta(
                        tempC       = previous?.let { sensors.tempC - it.tempC },
                        humidityPct = previous?.let { sensors.humidityPct - it.humidityPct },
                        dustUgM3    = previous?.let { sensors.dustUgM3 - it.dustUgM3 },
                    )

                    _uiState.value = UiState.Connected(
                        ip      = ip,
                        sensors = sensors,
                        history = historyBuffer.toList(),
                        delta   = delta,
                    )
                } catch (e: CancellationException) {
                    throw e // don't swallow coroutine cancellation
                } catch (e: Exception) {
                    consecutiveFailures++
                    if (consecutiveFailures >= maxFailures) {
                        _uiState.value = UiState.Error(
                            ip      = _savedIp.value ?: "",
                            message = e.localizedMessage ?: "Unknown error",
                        )
                    }
                }
                delay(2_000)
            }
        }
    }

    /** Cancel the poll loop (call when app is backgrounded). */
    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    // ─── Control Toggles ───────────────────────────────────────────────────

    fun toggleFan(on: Boolean) {
        val repo = repository ?: return
        val currentState = _uiState.value
        if (currentState is UiState.Connected) {
            _uiState.value = currentState.copy(
                sensors = currentState.sensors.copy(fanOn = on)
            )
        }
        viewModelScope.launch {
            try {
                repo.toggleFan(on)
                refreshStatus()
            } catch (_: Exception) { }
        }
    }

    private suspend fun refreshStatus() {
        val repo = repository ?: return
        try {
            val status = repo.getStatus()
            val avgDust = if (historyBuffer.isNotEmpty()) {
                historyBuffer.map { it.dustUgM3 }.average()
            } else {
                status.dustUgM3
            }
            val aqi = calculateAqi(avgDust)
            val category = aqiCategoryFromValue(aqi)
            val currentState = _uiState.value
            if (currentState is UiState.Connected) {
                _uiState.value = currentState.copy(
                    sensors = currentState.sensors.copy(
                        tempC       = status.tempC,
                        humidityPct = status.humidityPct,
                        dustUgM3    = status.dustUgM3,
                        avgDustUgM3 = avgDust,
                        aqi         = aqi,
                        category    = category,
                        fanOn       = status.fanOn,
                    )
                )
            }
        } catch (_: Exception) { }
    }

    // ─── User actions ──────────────────────────────────────────────────────

    /**
     * Save a new IP, update the repository, and restart polling.
     * Triggers DataStore write → collect in [init] handles the rest.
     */
    fun saveIp(ip: String) {
        viewModelScope.launch {
            dataStore.saveIp(ip)
        }
    }

    /** Reset failure count and force a new poll attempt. */
    fun retry() {
        consecutiveFailures = 0
        stopPolling()
        startPolling()
    }
}
