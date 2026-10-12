package com.example.aqisensor.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.aqisensor.ui.theme.categoryColor

/**
 * A pulsing coloured dot + air quality category label with pill background.
 *
 * The dot colour animates between values as the category changes,
 * and a subtle pulse glow radiates outward to indicate live data.
 *
 * @param category  The category string returned by the device, e.g. "Good"
 * @param modifier  Optional modifier for the outer Row
 */
@Composable
fun StatusDot(
    category: String,
    modifier: Modifier = Modifier,
) {
    val dotColor by animateColorAsState(
        targetValue = categoryColor(category),
        animationSpec = tween(500),
        label = "statusDotColor",
    )

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulseGlow")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue  = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseRadius",
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(dotColor.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        // Pulsing dot with glow
        Box(
            modifier = Modifier
                .size(14.dp)
                .drawBehind {
                    // Outer pulse glow ring
                    drawCircle(
                        color  = dotColor.copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                    )
                }
                .clip(CircleShape)
                .background(dotColor),
        )

        Spacer(Modifier.width(10.dp))

        // Category text — bolder
        Text(
            text  = category,
            style = MaterialTheme.typography.labelMedium,
            color = dotColor,
        )
    }
}
