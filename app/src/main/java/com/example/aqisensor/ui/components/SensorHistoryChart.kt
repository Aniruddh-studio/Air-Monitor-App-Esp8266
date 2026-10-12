package com.example.aqisensor.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aqisensor.data.model.SensorSnapshot
import com.example.aqisensor.ui.theme.ChartDust
import com.example.aqisensor.ui.theme.JetBrainsMono
import androidx.compose.ui.text.font.FontWeight
import java.util.Locale

/**
 * Renders a live single-line chart for dust (PM2.5 / AQI).
 *
 * The chart is driven entirely by [history] — a rolling list of [SensorSnapshot]
 * that lives only on the phone. No extra network requests are made.
 *
 * The line animates in smoothly when new data arrives.
 *
 * @param history  The ordered list of snapshots (oldest first, newest last)
 */
@Composable
fun SensorHistoryChart(
    history: List<SensorSnapshot>,
    modifier: Modifier = Modifier,
) {
    // ── Animation state must be declared before ANY early return ────────────
    val progress = remember { Animatable(0f) }
    LaunchedEffect(history.size) {
        if (history.size <= 1) progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(600))
    }
    val pct by progress.asState()

    if (history.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Waiting for data…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    // ── Legend ──────────────────────────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendPill("PM2.5", ChartDust)
    }

    val gridColor     = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
    val labelColor    = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    val textMeasurer  = rememberTextMeasurer()
    val labelStyle    = MaterialTheme.typography.labelSmall.copy(
        fontFamily    = JetBrainsMono,
        fontSize      = 8.sp,
        letterSpacing = 0.sp,
        color         = labelColor,
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
    ) {
        val w = size.width
        val h = size.height
        val padL = 36f   // space for Y-axis labels
        val padR = 8f
        val padT = 12f
        val padB = 12f
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        // ── Dashed horizontal grid lines (5 bands) ─────────────────────────
        val gridLines = 5
        repeat(gridLines + 1) { i ->
            val y = padT + chartH * (i / gridLines.toFloat())
            drawLine(
                color       = gridColor,
                start       = Offset(padL, y),
                end         = Offset(w - padR, y),
                strokeWidth = 1f,
                pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f),
            )
        }

        // ── Draw dust series ─────────────────────────────────────────────────
        drawSeriesEnhanced(history, pct, w, h, padL, padR, padT, padB, ChartDust, textMeasurer, labelStyle) { it.dustUgM3.toFloat() }
    }

    // ── Summary stats row (Min, Current, Max) ──────────────────────────────
    if (history.isNotEmpty()) {
        val minVal = history.minOf { it.dustUgM3 }
        val currentVal = history.last().dustUgM3
        val maxVal = history.maxOf { it.dustUgM3 }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatItem(
                label = "Min",
                value = "%.1f".format(minVal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            StatItem(
                label = "Current",
                value = "%.1f".format(currentVal),
                color = ChartDust,
                modifier = Modifier.weight(1f),
            )
            StatItem(
                label = "Max",
                value = "%.1f".format(maxVal),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// ─── Canvas helper — enhanced series drawing ───────────────────────────────

private fun DrawScope.drawSeriesEnhanced(
    history: List<SensorSnapshot>,
    revealPct: Float,
    w: Float,
    h: Float,
    padL: Float,
    padR: Float,
    padT: Float,
    padB: Float,
    lineColor: Color,
    textMeasurer: TextMeasurer,
    labelStyle: TextStyle,
    valueOf: (SensorSnapshot) -> Float,
) {
    if (history.size < 2) return
    val chartW = w - padL - padR
    val chartH = h - padT - padB

    val values = history.map(valueOf)
    val minV = values.min()
    val maxV = values.max()
    val range = if (maxV - minV < 0.01f) 1f else maxV - minV

    // How many points to draw (animated reveal)
    val count = (history.size * revealPct).toInt().coerceAtLeast(2)
    val slice = history.takeLast(count)
    val sliceValues = slice.map(valueOf)

    fun xOf(idx: Int) = padL + chartW * idx.toFloat() / (count - 1).coerceAtLeast(1)
    fun yOf(v: Float) = padT + chartH * (1f - (v - minV) / range)

    // ── Y-axis labels (min / max) ───────────────────────────────────────────
    val maxLabel = if (maxV >= 100) "${maxV.toInt()}" else "%.1f".format(maxV)
    val minLabel = if (minV >= 100) "${minV.toInt()}" else "%.1f".format(minV)
    val maxResult = textMeasurer.measure(maxLabel, labelStyle)
    val minResult = textMeasurer.measure(minLabel, labelStyle)
    drawText(maxResult, topLeft = Offset(padL - maxResult.size.width - 4f, padT - maxResult.size.height / 2f))
    drawText(minResult, topLeft = Offset(padL - minResult.size.width - 4f, padT + chartH - minResult.size.height / 2f))

    // ── Fill path (gradient under the line) ──────────────────────────────────
    val fillPath = Path().apply {
        moveTo(xOf(0), h - padB)
        lineTo(xOf(0), yOf(sliceValues[0]))
        sliceValues.forEachIndexed { i, v ->
            if (i == 0) return@forEachIndexed
            val x0 = xOf(i - 1); val y0 = yOf(sliceValues[i - 1])
            val x1 = xOf(i);     val y1 = yOf(v)
            val cx = (x0 + x1) / 2f
            cubicTo(cx, y0, cx, y1, x1, y1)
        }
        lineTo(xOf(count - 1), h - padB)
        close()
    }
    drawPath(
        path  = fillPath,
        brush = Brush.verticalGradient(
            colors    = listOf(lineColor.copy(alpha = 0.30f), lineColor.copy(alpha = 0f)),
            startY    = padT,
            endY      = h - padB,
        ),
    )

    // ── Stroke path ──────────────────────────────────────────────────────────
    val strokePath = Path().apply {
        moveTo(xOf(0), yOf(sliceValues[0]))
        sliceValues.forEachIndexed { i, v ->
            if (i == 0) return@forEachIndexed
            val x0 = xOf(i - 1); val y0 = yOf(sliceValues[i - 1])
            val x1 = xOf(i);     val y1 = yOf(v)
            val cx = (x0 + x1) / 2f
            cubicTo(cx, y0, cx, y1, x1, y1)
        }
    }
    drawPath(
        path  = strokePath,
        color = lineColor,
        style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // ── Latest value dot with glow ───────────────────────────────────────────
    val lastX = xOf(count - 1)
    val lastY = yOf(sliceValues.last())
    // Outer glow
    drawCircle(color = lineColor.copy(alpha = 0.25f), radius = 10f, center = Offset(lastX, lastY))
    // Core dot
    drawCircle(color = lineColor, radius = 5.5f, center = Offset(lastX, lastY))
    // Inner highlight
    drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 2.5f, center = Offset(lastX, lastY))
}

// ─── Legend pill ────────────────────────────────────────────────────────────

@Composable
private fun LegendPill(label: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color)
        }
        Spacer(Modifier.width(5.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily    = JetBrainsMono,
                fontSize      = 9.sp,
                letterSpacing = 1.sp,
            ),
            color = color,
        )
    }
}

// ─── Stat item (Min / Current / Max summary) ───────────────────────────────

@Composable
private fun StatItem(
    label: String,
    value: String,
    unit: String = "µg/m³",
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            ),
            color = color.copy(alpha = 0.75f),
        )
        Spacer(Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                ),
                color = color,
            )
            Spacer(Modifier.width(2.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                ),
                color = color.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
    }
}
