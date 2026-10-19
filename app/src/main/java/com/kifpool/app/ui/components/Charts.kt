package com.kifpool.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kifpool.app.util.Fmt

data class DonutSlice(val color: Color, val value: Float)

@Composable
fun DonutChart(
    slices: List<DonutSlice>,
    modifier: Modifier = Modifier,
    stroke: Dp = 26.dp,
    gapDegrees: Float = 2.5f,
    centerContent: @Composable () -> Unit = {}
) {
    var played by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "donut"
    )
    LaunchedEffect(slices) { played = true }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val total = slices.sumOf { it.value.toDouble() }.toFloat()
            if (total <= 0f) return@Canvas
            val strokePx = stroke.toPx()
            val inset = strokePx / 2f + 2.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            var start = -90f
            slices.forEach { s ->
                val sweep = (s.value / total) * 360f * progress
                val visible = (sweep - gapDegrees).coerceAtLeast(0.4f)
                drawArc(
                    color = s.color,
                    startAngle = start,
                    sweepAngle = visible,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Butt)
                )
                start += sweep
            }
        }
        centerContent()
    }
}

@Composable
fun BarChart(
    values: List<Float>,
    barColor: Color,
    highlightColor: Color,
    gridColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier
) {
    var played by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "bars"
    )
    LaunchedEffect(values) { played = true }

    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val maxV = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val maxRef = maxV
        val n = values.size
        val labelH = 18.dp.toPx()
        val chartH = size.height - labelH
        val slot = size.width / n
        val barW = slot * 0.55f

        values.forEachIndexed { i, v ->
            val h = (v / maxRef) * chartH * progress
            if (h > 0f) {
                val x = i * slot + (slot - barW) / 2f
                val color = if (v >= maxRef) highlightColor else barColor
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, chartH - h),
                    size = Size(barW, h),
                    cornerRadius = CornerRadius(barW / 3f, barW / 3f)
                )
            }
        }

        drawLine(
            color = gridColor,
            start = Offset(0f, chartH),
            end = Offset(size.width, chartH),
            strokeWidth = 1.dp.toPx()
        )

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(
                (labelColor.alpha * 255f).toInt(),
                (labelColor.red * 255f).toInt(),
                (labelColor.green * 255f).toInt(),
                (labelColor.blue * 255f).toInt()
            )
            textSize = labelH * 0.75f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val step = if (n <= 10) 2 else 5
        for (d in 0 until n) {
            if (d % step == 0 || d == n - 1) {
                drawContext.canvas.nativeCanvas.drawText(
                    Fmt.fa(d + 1),
                    d * slot + slot / 2f,
                    size.height - labelH * 0.15f,
                    paint
                )
            }
        }
    }
}
