package com.example.aqisensor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aqisensor.ui.theme.AccentGreen
import com.example.aqisensor.ui.theme.AccentRed
import com.example.aqisensor.ui.theme.JetBrainsMono

/**
 * A single sensor readout cell — displays a large monospace number and a unit label.
 *
 * Always shows live data at full opacity (toggle logic removed).
 *
 * An optional [delta] badge is shown below the unit label:
 * - Positive delta → red arrow ↑ +X
 * - Negative delta → green arrow ↓ X
 * - Zero / null   → nothing shown
 *
 * @param label     Short sensor name, e.g. "TEMP", "HUM", "DUST"
 * @param value     Formatted string value, e.g. "24.6", "57", "18"
 * @param unit      Unit string, e.g. "°C", "%", "μg/m³"
 * @param accentColor  The accent tint for this readout's value
 * @param delta     Change vs. previous reading (null = no previous reading)
 * @param modifier  Optional modifier for the outer column
 */
@Composable
fun SensorReadout(
    label: String,
    value: String,
    unit: String,
    accentColor: Color = AccentGreen,
    delta: Double? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Unit label — small caps style
        Text(
            text  = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(6.dp))

        // Large monospace numeric value with subtle background tint
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.08f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text  = value,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = JetBrainsMono,
                    fontSize   = 38.sp,
                    lineHeight = 42.sp,
                ),
                color = accentColor,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(Modifier.height(4.dp))

        // Unit text
        Text(
            text  = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )

        // ── Delta badge ────────────────────────────────────────────────────
        val threshold = 0.05
        if (delta != null && Math.abs(delta) >= threshold) {
            val isUp = delta > 0
            val arrow = if (isUp) "↑" else "↓"
            val sign  = if (isUp) "+" else ""
            val formatted = when {
                Math.abs(delta) < 10 -> "$sign%.1f".format(delta)
                else                  -> "$sign${delta.toInt()}"
            }
            val badgeColor: Color = if (isUp) AccentRed else AccentGreen

            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(badgeColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text  = "$arrow $formatted",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily    = JetBrainsMono,
                        fontSize      = 10.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = badgeColor,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            // Reserve the same height so layout doesn't shift when badge appears
            Spacer(Modifier.height(20.dp))
        }
    }
}
