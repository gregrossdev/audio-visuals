package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import audio.AudioFeatures

private class TrailsState(val historySize: Int = 40, val pointsPerFrame: Int = 30) {
    val history = Array(historySize) { FloatArray(0) }
    var writeIndex = 0
    var frameCount = 0

    fun push(magnitudes: FloatArray) {
        // Downsample to pointsPerFrame
        val step = maxOf(1, magnitudes.size / pointsPerFrame)
        val sampled = FloatArray(pointsPerFrame) { j ->
            val i = (j * step).coerceAtMost(magnitudes.size - 1)
            ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f)
        }
        history[writeIndex] = sampled
        writeIndex = (writeIndex + 1) % historySize
        if (frameCount < historySize) frameCount++
    }

    fun getLayer(age: Int): FloatArray? {
        if (age >= frameCount) return null
        val index = (writeIndex - 1 - age + historySize * 2) % historySize
        return history[index]
    }
}

@Composable
fun TrailsVisualizer(
    magnitudes: FloatArray,
    theme: ColorTheme,
    isBeat: Boolean,
    config: TrailsConfig = TrailsConfig(),
    audioFeatures: AudioFeatures? = null,
    modifier: Modifier = Modifier
) {
    val state = remember(config.historySize, config.pointsPerFrame) {
        TrailsState(config.historySize, config.pointsPerFrame)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val bandCount = magnitudes.size
        if (bandCount == 0) return@Canvas

        val w = size.width
        val h = size.height

        // Push current frame
        state.push(magnitudes)

        // Reactive background
        val energy = magnitudes.map { ((it + 80f) / 80f).coerceIn(0f, 1f) }.average().toFloat()
        val bgAlpha = (energy * 0.2f).coerceIn(0f, 0.2f)
        if (bgAlpha > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.hsl(theme.backgroundHue, 0.6f, 0.1f).copy(alpha = bgAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w / 2f, h * 0.3f),
                    radius = w * 0.6f
                ),
                radius = w * 0.6f,
                center = Offset(w / 2f, h * 0.3f)
            )
        }

        val layers = state.frameCount
        val maxVerticalSpread = h * config.verticalSpread
        val topMargin = h * config.topMargin

        // Draw oldest to newest (back to front)
        for (age in layers - 1 downTo 0) {
            val data = state.getLayer(age) ?: continue
            val pointCount = data.size
            if (pointCount < 2) continue

            val ageFraction = age.toFloat() / state.historySize
            val alpha = (1f - ageFraction).coerceIn(0f, 1f) * if (age == 0 && isBeat) 1f else 0.8f
            if (alpha < 0.02f) continue

            // Vertical offset — older layers shift down
            val yOffset = topMargin + ageFraction * maxVerticalSpread

            // Pre-compute points
            val xs = FloatArray(pointCount)
            val ys = FloatArray(pointCount)
            for (j in 0 until pointCount) {
                xs[j] = (j.toFloat() / (pointCount - 1)) * w
                ys[j] = yOffset + (1f - data[j]) * h * 0.25f
            }

            // Build smooth path via Catmull-Rom
            val path = Path()
            path.moveTo(xs[0], ys[0])
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
            }

            // Close path to bottom for fill
            path.lineTo(w, yOffset + h * 0.25f)
            path.lineTo(0f, yOffset + h * 0.25f)
            path.close()

            // Color shifts with age — centroid shifts hue (bass=warm, treble=cool)
            val centroid = audioFeatures?.spectralCentroid ?: 0.5f
            val centroidHueShift = (centroid - 0.5f) * 60f // shift ±30 degrees based on brightness
            val trailEnergy = audioFeatures?.energy ?: energy
            val hueShift = ageFraction * config.hueFadeSpeed
            val colorStops = Array(pointCount) { j ->
                val fraction = j.toFloat() / (pointCount - 1)
                // Blend toward background hue with age, offset by centroid
                val agedHue = theme.hueStart + (theme.hueEnd - theme.hueStart) * fraction + centroidHueShift
                val blendedHue = agedHue * (1f - hueShift) + theme.backgroundHue * hueShift
                val energyBoost = if (age == 0) trailEnergy * 0.15f else 0f
                val lightness = theme.lightnessMin + data[j] * theme.lightnessScale * (1f - ageFraction * config.lightnessFade) + energyBoost
                fraction to Color.hsl(
                    blendedHue.mod(360f),
                    theme.saturation * (1f - ageFraction * config.saturationFade),
                    lightness.coerceIn(0f, 1f)
                ).copy(alpha = alpha)
            }

            drawPath(
                path = path,
                brush = Brush.horizontalGradient(colorStops = colorStops),
                style = Fill
            )
        }
    }
}
