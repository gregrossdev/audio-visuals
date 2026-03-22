package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun WaveformVisualizer(magnitudes: FloatArray, theme: ColorTheme, peaks: FloatArray? = null, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val totalBins = magnitudes.size
        if (totalBins == 0) return@Canvas

        val step = 4
        val pointCount = totalBins / step
        if (pointCount < 2) return@Canvas

        val w = size.width
        val h = size.height

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
                    center = Offset(w / 2f, h / 2f),
                    radius = w * 0.6f
                ),
                radius = w * 0.6f,
                center = Offset(w / 2f, h / 2f)
            )
        }

        val path = Path()
        val glowPath = Path()
        val colorStops = mutableListOf<Pair<Float, Color>>()

        // Pre-compute points
        val xs = FloatArray(pointCount)
        val ys = FloatArray(pointCount)
        for (j in 0 until pointCount) {
            val i = j * step
            val normalized = (magnitudes[i] + 80f) / 80f
            val clamped = normalized.coerceIn(0f, 1f)
            xs[j] = (j.toFloat() / (pointCount - 1)) * w
            ys[j] = h - (clamped * h)
            colorStops.add((j.toFloat() / (pointCount - 1)) to theme.barColor(i, totalBins, clamped))
        }

        // Catmull-Rom spline → cubic Bézier
        path.moveTo(xs[0], ys[0])
        glowPath.moveTo(xs[0], ys[0])
        for (j in 0 until pointCount - 1) {
            val p0x = xs[maxOf(j - 1, 0)]
            val p0y = ys[maxOf(j - 1, 0)]
            val p1x = xs[j]
            val p1y = ys[j]
            val p2x = xs[j + 1]
            val p2y = ys[j + 1]
            val p3x = xs[minOf(j + 2, pointCount - 1)]
            val p3y = ys[minOf(j + 2, pointCount - 1)]

            val cp1x = p1x + (p2x - p0x) / 6f
            val cp1y = p1y + (p2y - p0y) / 6f
            val cp2x = p2x - (p3x - p1x) / 6f
            val cp2y = p2y - (p3y - p1y) / 6f

            path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2x, p2y)
            glowPath.cubicTo(cp1x, cp1y, cp2x, cp2y, p2x, p2y)
        }

        // Glow pass — stroke the outline thickly at low alpha
        drawPath(
            path = glowPath,
            brush = Brush.horizontalGradient(colorStops = colorStops.toTypedArray()),
            style = Stroke(width = 6f),
            alpha = theme.glowAlpha,
            blendMode = BlendMode.Screen
        )

        // Close path down to bottom for fill
        path.lineTo(w, h)
        path.lineTo(0f, h)
        path.close()

        drawPath(
            path = path,
            brush = Brush.horizontalGradient(colorStops = colorStops.toTypedArray()),
            style = Fill
        )

        // Peak dots
        if (peaks != null) {
            val peakStep = if (totalBins > 128) 4 else 1
            val peakPointCount = totalBins / peakStep
            for (j in 0 until peakPointCount) {
                val i = j * peakStep
                if (i >= peaks.size) break
                val peakNorm = ((peaks[i] + 80f) / 80f).coerceIn(0f, 1f)
                if (peakNorm < 0.02f) continue
                val x = (j.toFloat() / (peakPointCount - 1)) * w
                val y = h - (peakNorm * h)
                drawCircle(
                    color = theme.peakColor(i, totalBins),
                    radius = 2f,
                    center = Offset(x, y)
                )
            }
        }
    }
}
