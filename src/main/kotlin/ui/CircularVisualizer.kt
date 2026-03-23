package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CircularVisualizer(magnitudes: FloatArray, theme: ColorTheme, peaks: FloatArray? = null, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = magnitudes.size
        if (barCount == 0) return@Canvas

        val cx = size.width / 2f
        val cy = size.height / 2f
        val minDim = min(size.width, size.height)
        val baseRadius = minDim * 0.15f
        val maxBarLength = minDim * 0.35f

        // Reactive background
        val energy = magnitudes.map { ((it + 80f) / 80f).coerceIn(0f, 1f) }.average().toFloat()
        val bgAlpha = (energy * 0.3f).coerceIn(0f, 0.3f)
        if (bgAlpha > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.hsl(theme.backgroundHue, 0.6f, 0.15f).copy(alpha = bgAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = minDim * 0.5f
                ),
                radius = minDim * 0.5f,
                center = Offset(cx, cy)
            )
        }

        val strokeUnit = minDim * 0.003f   // proportional stroke width unit

        // Inner circle anchor
        drawCircle(
            color = Color.hsl(theme.backgroundHue, 0.1f, 0.25f),
            radius = baseRadius,
            center = Offset(cx, cy),
            style = Stroke(width = strokeUnit)
        )

        val step = maxOf(1, barCount / 128)
        for (j in 0 until barCount / step) {
            val i = j * step
            val normalized = ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f)
            if (normalized < 0.01f) continue

            val angle = (j.toFloat() / (barCount / step)) * 360f
            val radians = Math.toRadians(angle.toDouble() - 90.0)
            val cosA = cos(radians).toFloat()
            val sinA = sin(radians).toFloat()

            val startX = cx + baseRadius * cosA
            val startY = cy + baseRadius * sinA
            val barLength = normalized * maxBarLength
            val endX = cx + (baseRadius + barLength) * cosA
            val endY = cy + (baseRadius + barLength) * sinA

            val color = theme.barColor(i, barCount, normalized)

            // Glow pass
            drawLine(
                color = color.copy(alpha = theme.glowAlpha),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeUnit * 2f,
                blendMode = BlendMode.Screen
            )

            // Main bar
            drawLine(
                color = color,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeUnit
            )

            // Peak tick
            if (peaks != null && i < peaks.size) {
                val peakNorm = ((peaks[i] + 80f) / 80f).coerceIn(0f, 1f)
                if (peakNorm > 0.02f) {
                    val peakLen = peakNorm * maxBarLength
                    val peakR = baseRadius + peakLen
                    val tickHalf = minDim * 0.003f
                    val tickStart = Offset(cx + (peakR - tickHalf) * cosA, cy + (peakR - tickHalf) * sinA)
                    val tickEnd = Offset(cx + (peakR + tickHalf) * cosA, cy + (peakR + tickHalf) * sinA)
                    drawLine(
                        color = theme.peakColor(i, barCount),
                        start = tickStart,
                        end = tickEnd,
                        strokeWidth = strokeUnit
                    )
                }
            }
        }
    }
}
