package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun SpectrumVisualizer(
    magnitudes: FloatArray,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val barCount = magnitudes.size
        if (barCount == 0) return@Canvas

        val barWidth = size.width / barCount
        val maxHeight = size.height

        for (i in 0 until barCount) {
            // Normalize dB range (-80..0) to 0..1
            val normalized = (magnitudes[i] + 80f) / 80f
            val barHeight = (normalized.coerceIn(0f, 1f) * maxHeight)

            if (barHeight < 1f) continue

            // Frequency-based hue: low freq = red (0), mid = green (120), high = blue (240)
            val hue = (i.toFloat() / barCount) * 270f
            // Amplitude controls brightness: louder = brighter
            val lightness = 0.3f + normalized * 0.4f
            val baseColor = Color.hsl(hue, 0.85f, lightness)
            val tipColor = Color.hsl(hue, 0.95f, (lightness + 0.2f).coerceAtMost(0.9f))

            val x = i * barWidth
            val y = maxHeight - barHeight

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(tipColor, baseColor),
                    startY = y,
                    endY = maxHeight
                ),
                topLeft = Offset(x, y),
                size = Size(barWidth - 1f, barHeight)
            )
        }
    }
}
