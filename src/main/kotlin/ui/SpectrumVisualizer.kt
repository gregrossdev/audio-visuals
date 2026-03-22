package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas

@Composable
fun SpectrumVisualizer(
    magnitudes: FloatArray,
    theme: ColorTheme,
    peaks: FloatArray? = null,
    bandFrequencies: FloatArray? = null,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = magnitudes.size
        if (barCount == 0) return@Canvas

        val barWidth = size.width / barCount
        val maxHeight = size.height
        val labelHeight = if (bandFrequencies != null) 16f else 0f
        val drawHeight = maxHeight - labelHeight

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
                    center = Offset(size.width / 2f, drawHeight / 2f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = Offset(size.width / 2f, drawHeight / 2f)
            )
        }

        for (i in 0 until barCount) {
            val normalized = (magnitudes[i] + 80f) / 80f
            val barHeight = (normalized.coerceIn(0f, 1f) * drawHeight)

            if (barHeight < 1f) continue

            val baseColor = theme.barColor(i, barCount, normalized)
            val tip = theme.tipColor(i, barCount, normalized)

            val x = i * barWidth
            val y = drawHeight - barHeight

            // Glow pass
            val glowPad = 2f
            drawRect(
                color = baseColor.copy(alpha = theme.glowAlpha),
                topLeft = Offset(x - glowPad, y - glowPad),
                size = Size(barWidth - 1f + glowPad * 2, barHeight + glowPad),
                blendMode = BlendMode.Screen
            )

            // Main bar
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(tip, baseColor),
                    startY = y,
                    endY = drawHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth - 1f, barHeight)
            )

            // Peak indicator
            if (peaks != null && i < peaks.size) {
                val peakNorm = ((peaks[i] + 80f) / 80f).coerceIn(0f, 1f)
                val peakY = drawHeight - (peakNorm * drawHeight)
                if (peakNorm > 0.01f) {
                    drawLine(
                        color = theme.peakColor(i, barCount),
                        start = Offset(x, peakY),
                        end = Offset(x + barWidth - 1f, peakY),
                        strokeWidth = 1.5f
                    )
                }
            }
        }

        // Frequency labels
        if (bandFrequencies != null) {
            val labelFreqs = floatArrayOf(50f, 100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f, 20000f)
            val labelTexts = arrayOf("50", "100", "200", "500", "1k", "2k", "5k", "10k", "20k")

            drawIntoCanvas { canvas ->
                val skiaPaint = org.jetbrains.skia.Paint().apply {
                    color = 0x80B0B0B0.toInt()
                }
                val skiaFont = org.jetbrains.skia.Font(null, 10f)

                for (k in labelFreqs.indices) {
                    val freq = labelFreqs[k]
                    var closestBand = 0
                    var closestDist = Float.MAX_VALUE
                    for (b in bandFrequencies.indices) {
                        val dist = kotlin.math.abs(bandFrequencies[b] - freq)
                        if (dist < closestDist) {
                            closestDist = dist
                            closestBand = b
                        }
                    }
                    val x = (closestBand.toFloat() / barCount) * size.width
                    canvas.nativeCanvas.drawString(
                        labelTexts[k], x, maxHeight - 2f, skiaFont, skiaPaint
                    )
                }
            }
        }
    }
}
