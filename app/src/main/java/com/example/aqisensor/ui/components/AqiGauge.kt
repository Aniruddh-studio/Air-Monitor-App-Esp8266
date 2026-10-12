package com.example.aqisensor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aqisensor.ui.theme.JetBrainsMono
import com.example.aqisensor.ui.theme.aqiCategory
import com.example.aqisensor.ui.theme.aqiColor

/**
 * A circular AQI gauge inspired by the reference design.
 *
 * Displays a 240° arc that fills proportionally to the AQI value (0–500 scale).
 * The interior shows "AQI" label, the numeric value, and the category text.
 *
 * @param aqi           The current AQI integer value (0..500)
 * @param size          Diameter of the gauge
 * @param strokeWidth   Thickness of the arc stroke
 * @param modifier      Optional modifier
 */
@Composable
fun AqiGauge(
    aqi: Int,
    size: Dp = 220.dp,
    strokeWidth: Dp = 14.dp,
    modifier: Modifier = Modifier,
) {
    val maxAqi = 500f
    val targetFraction = (aqi.coerceIn(0, 500) / maxAqi).coerceIn(0f, 1f)
    val color = aqiColor(aqi)
    val category = aqiCategory(aqi)

    // Animate sweep fraction on value change
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "aqiSweep",
    )

    // Track background color
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        // Arc canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val padding = stroke / 2f
            val arcSize = Size(
                width = this.size.width - stroke,
                height = this.size.height - stroke,
            )
            val topLeft = Offset(padding, padding)

            // The arc spans 240° starting from 150° (bottom-left) to 30° (bottom-right)
            val startAngle = 150f
            val totalSweep = 240f

            // Background track
            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = totalSweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            // Foreground progress
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = totalSweep * animatedFraction,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        // Centre text
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "AQI",
                style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 2.sp,
                    fontSize = 13.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "$aqi",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = JetBrainsMono,
                    fontSize = 48.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = color,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = category,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = color,
            )
        }
    }
}
