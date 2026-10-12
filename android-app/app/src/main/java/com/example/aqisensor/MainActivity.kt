package com.example.aqisensor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.aqisensor.ui.screen.DashboardScreen
import com.example.aqisensor.ui.screen.IpSetupScreen
import com.example.aqisensor.ui.screen.SettingsDialog
import com.example.aqisensor.ui.screen.SplashScreen
import com.example.aqisensor.ui.theme.AqiSensorTheme
import com.example.aqisensor.viewmodel.AqiViewModel
import com.example.aqisensor.viewmodel.UiState

/**
 * Single-Activity entry point.
 *
 * Responsibilities:
 * - Edge-to-edge layout
 * - Observing lifecycle to start/stop polling (battery-safe)
 * - Routing between IpSetupScreen and DashboardScreen based on [UiState]
 * - Hosting the SettingsDialog overlay
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AqiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AqiSensorTheme {
                AqiSensorApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AqiSensorApp(viewModel: AqiViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // ── Splash screen ──────────────────────────────────────────────────────
    if (showSplash) {
        SplashScreen(onFinished = { showSplash = false })
        return
    }

    // ── Lifecycle-aware polling ─────────────────────────────────────────────
    // Starts polling when STARTED (foreground), cancels on STOPPED (background).
    // This is the primary battery-drain guard.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.startPolling()
            // The suspend block here stays active while the lifecycle is STARTED.
            // When it drops below STARTED (app backgrounded), the coroutine is
            // cancelled, which in turn cancels the poll job.
            kotlinx.coroutines.awaitCancellation()
        }
    }

    // ── Screen routing ─────────────────────────────────────────────────────
    when (uiState) {
        is UiState.NeedsIp -> {
            IpSetupScreen(
                onSave = { ip ->
                    viewModel.saveIp(ip)
                },
            )
        }

        else -> {
            DashboardScreen(
                uiState        = uiState,
                onRetry        = viewModel::retry,
                onOpenSettings = { showSettings = true },
                onToggleFan    = viewModel::toggleFan,
            )
        }
    }

    // ── Settings dialog overlay ────────────────────────────────────────────
    if (showSettings) {
        val currentIp = when (val s = uiState) {
            is UiState.Connected -> s.ip
            is UiState.Error     -> s.ip
            is UiState.Loading   -> s.ip
            else                 -> ""
        }
        SettingsDialog(
            currentIp = currentIp,
            onSave    = { newIp ->
                viewModel.saveIp(newIp)
                showSettings = false
            },
            onDismiss = { showSettings = false },
        )
    }
}

