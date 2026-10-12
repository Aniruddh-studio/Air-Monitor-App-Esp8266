package com.example.aqisensor.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aqisensor.R
import com.example.aqisensor.data.model.SensorSnapshot
import com.example.aqisensor.ui.components.AqiGauge
import com.example.aqisensor.ui.components.SensorHistoryChart
import com.example.aqisensor.ui.theme.*
import com.example.aqisensor.viewmodel.SensorData
import com.example.aqisensor.viewmodel.SensorDelta
import com.example.aqisensor.viewmodel.UiState
import java.util.Calendar

/**
 * Main analytics dashboard screen.
 *
 * Layout (top → bottom):
 * 1. Greeting header with settings icon
 * 2. Circular AQI gauge
 * 3. "Indoor Air Quality" label
 * 4. Three sensor metric cards (Temperature, Humidity, PM2.5)
 * 5. History graph card (preserved from original)
 */
@Composable
fun DashboardScreen(
    uiState: UiState,
    onRetry: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleFan: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        when (uiState) {
            is UiState.NeedsIp -> Unit // handled by NavController in MainActivity

            is UiState.Loading -> LoadingContent(onOpenSettings = onOpenSettings)

            is UiState.Error -> ErrorContent(
                message        = uiState.message,
                onRetry        = onRetry,
                onOpenSettings = onOpenSettings,
            )

            is UiState.Connected -> ConnectedContent(
                sensors        = uiState.sensors,
                history        = uiState.history,
                delta          = uiState.delta,
                onOpenSettings = onOpenSettings,
                onToggleFan    = onToggleFan,
            )
        }
    }
}

// ─── Loading ───────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        // Settings button at top-right
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        // Centered loading indicator
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = AccentGreen)
            Spacer(Modifier.height(16.dp))
            Text(
                text  = "Connecting…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Error ─────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp)) {
        // Settings button at top-right
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector        = Icons.Default.WifiOff,
                contentDescription = null,
                modifier           = Modifier.size(64.dp),
                tint               = AccentRed,
            )
            Text(
                text      = stringResource(R.string.error_cant_reach),
                style     = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text      = stringResource(R.string.error_hint),
                style     = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onRetry,
                colors  = ButtonDefaults.buttonColors(containerColor = AccentGreen),
            ) {
                Text(
                    text  = stringResource(R.string.action_retry),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

// ─── Connected — main dashboard ───────────────────────────────────────────

@Composable
private fun ConnectedContent(
    sensors: SensorData,
    history: List<SensorSnapshot>,
    delta: SensorDelta,
    onOpenSettings: () -> Unit,
    onToggleFan: (Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))

        // ── Greeting header ────────────────────────────────────────────────
        GreetingHeader(onOpenSettings = onOpenSettings)

        Spacer(Modifier.height(24.dp))

        // ── AQI Gauge ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AqiGauge(
                aqi = sensors.aqi,
                size = 230.dp,
                strokeWidth = 16.dp,
            )
        }

        Spacer(Modifier.height(8.dp))

        // ── "Indoor Air Quality" label ─────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "🌿",
                fontSize = 16.sp,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Indoor Air Quality",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Sensor metric cards (2x2 grid) ───────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                icon = Icons.Default.Thermostat,
                value = "%.1f°C".format(sensors.tempC),
                label = "Temperature",
                iconTint = ChartTemp,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                icon = Icons.Default.WaterDrop,
                value = "%.0f%%".format(sensors.humidityPct),
                label = "Humidity",
                iconTint = ChartHumidity,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MetricCard(
                icon = Icons.Default.Air,
                value = pmLevel(sensors.avgDustUgM3),
                label = "PM2.5",
                iconTint = aqiColor(sensors.aqi),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                icon = Icons.Default.Cloud,
                value = "%.1f".format(sensors.avgDustUgM3),
                label = "µg/m³",
                iconTint = ChartDust,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── Device Controls Card (Fan Control) ─────────────────────────────
        DeviceControlsCard(
            sensors     = sensors,
            onToggleFan = onToggleFan,
        )

        Spacer(Modifier.height(20.dp))

        // ── History graph card ─────────────────────────────────────────────
        HistoryCard(history = history)

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Greeting header ──────────────────────────────────────────────────────

@Composable
private fun GreetingHeader(onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "${greeting()},",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Breathe Better",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "🌿",
                    fontSize = 20.sp,
                )
            }
        }

        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ─── Metric card ──────────────────────────────────────────────────────────

@Composable
private fun MetricCard(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )

            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = JetBrainsMono,
                    fontSize = 16.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}

// ─── Device Controls Card ─────────────────────────────────────────────────

@Composable
private fun DeviceControlsCard(
    sensors: SensorData,
    onToggleFan: (Boolean) -> Unit,
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text  = "AIR PURIFIER",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            // Main Air Purifier Control Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (sensors.fanOn) AccentGreen.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (sensors.fanOn) AccentGreen.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Air,
                            contentDescription = "Air Purifier Control",
                            tint = if (sensors.fanOn) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Column {
                        Text(
                            text = "Air Purifier",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (sensors.fanOn) "PURIFIER ON — Active Air Filtration" else "PURIFIER OFF — Standby",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (sensors.fanOn) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    }
                }

                Switch(
                    checked = sensors.fanOn,
                    onCheckedChange = onToggleFan,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = AccentGreen,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }
    }
}

// ─── History graph card ───────────────────────────────────────────────────

@Composable
private fun HistoryCard(
    history: List<SensorSnapshot>,
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text  = "HISTORY",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val pointCount = history.size
                val seconds    = pointCount * 2
                val label = when {
                    pointCount == 0  -> "no data yet"
                    seconds < 60     -> "${seconds}s"
                    else             -> "${seconds / 60}m ${seconds % 60}s"
                }
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 6.dp),
                color    = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            SensorHistoryChart(
                history  = history,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────

/** Returns a time-appropriate greeting. */
private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else      -> "Good Evening"
    }
}

/**
 * Maps PM2.5 dust value to a human-readable level for the metric card.
 */
private fun pmLevel(dust: Double): String = when {
    dust <= 12.0  -> "Low"
    dust <= 35.4  -> "Med"
    dust <= 55.4  -> "High"
    else          -> "V.High"
}
