package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import audio.AudioFeatures
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private class KaleidoscopeState {
    var rotationAngle = 0f
    var rotationSpeed = 0.3f
    var breathScale = 1f
}

@Composable
fun KaleidoscopeVisualizer(
    magnitudes: FloatArray,
    theme: ColorTheme,
    isBeat: Boolean,
    config: KaleidoscopeConfig = KaleidoscopeConfig(),
    audioFeatures: AudioFeatures? = null,
    modifier: Modifier = Modifier
) {
    val state = remember { KaleidoscopeState() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val minDim = min(w, h)
        val bandCount = magnitudes.size
        if (bandCount == 0) return@Canvas

        // Compute energy
        val energy = magnitudes.map { ((it + 80f) / 80f).coerceIn(0f, 1f) }.average().toFloat()
        val bassEnergy = run {
            val bands = minOf(4, bandCount)
            var sum = 0f
            for (i in 0 until bands) sum += ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f)
            sum / bands
        }

        // Use spectral centroid to influence rotation speed (bright sounds = faster)
        val centroidBoost = (audioFeatures?.spectralCentroid ?: 0f) * config.energyRotationScale * 0.5f
        val midsScale = audioFeatures?.bandEnergies?.mids ?: energy

        // Update rotation — speed driven by energy + centroid, beat spikes it
        state.rotationSpeed = config.baseRotationSpeed + energy * config.energyRotationScale + centroidBoost
        if (isBeat) state.rotationSpeed += config.beatSpeedBoost
        state.rotationAngle += state.rotationSpeed

        // Breath scale — bass makes it pulse
        val targetBreath = config.breathMin + bassEnergy * config.breathScale
        state.breathScale += (targetBreath - state.breathScale) * config.breathEasing

        // Reactive background — sweep gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.hsl(theme.backgroundHue, 0.5f, 0.08f).copy(alpha = 0.4f + energy * 0.3f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = minDim * 0.6f
            ),
            radius = minDim * 0.6f,
            center = Offset(cx, cy)
        )

        val segments = config.segments
        val segmentAngle = 360f / segments

        // Draw rotated segments
        rotate(degrees = state.rotationAngle, pivot = Offset(cx, cy)) {
            scale(scale = state.breathScale, pivot = Offset(cx, cy)) {
                for (seg in 0 until segments) {
                    rotate(degrees = seg * segmentAngle, pivot = Offset(cx, cy)) {
                        // Draw shapes in one wedge, driven by frequency bands
                        val shapesPerWedge = minOf(bandCount, config.shapesPerWedge)
                        for (j in 0 until shapesPerWedge) {
                            val bandIndex = (j.toFloat() / shapesPerWedge * bandCount).toInt()
                                .coerceIn(0, bandCount - 1)
                            val normalized = ((magnitudes[bandIndex] + 80f) / 80f).coerceIn(0f, 1f)
                            if (normalized < 0.05f) continue

                            val radius = minDim * 0.08f + (j.toFloat() / shapesPerWedge) * minDim * 0.35f
                            val wedgeAngle = Math.toRadians(
                                (-segmentAngle / 2.0 + (j.toFloat() / shapesPerWedge) * segmentAngle * 0.8)
                            )

                            val shapeX = cx + radius * cos(wedgeAngle).toFloat()
                            val shapeY = cy + radius * sin(wedgeAngle).toFloat()
                            val shapeSize = normalized * minDim * 0.04f * (0.8f + midsScale * 0.4f) + 2f

                            val color = theme.barColor(bandIndex, bandCount, normalized)

                            // Glow
                            drawCircle(
                                color = color.copy(alpha = normalized * theme.glowAlpha),
                                radius = shapeSize * 2f,
                                center = Offset(shapeX, shapeY)
                            )

                            // Shape
                            drawCircle(
                                color = color.copy(alpha = normalized * 0.9f),
                                radius = shapeSize,
                                center = Offset(shapeX, shapeY)
                            )
                        }

                        // Arc lines connecting shapes — driven by energy
                        if (energy > 0.15f) {
                            val arcRadius = minDim * 0.15f + energy * minDim * 0.2f
                            val arcAngleStart = -segmentAngle / 2f + 2f
                            val arcSweep = (segmentAngle - 4f) * energy

                            drawArc(
                                color = theme.barColor(0, bandCount, energy)
                                    .copy(alpha = energy * 0.6f),
                                startAngle = arcAngleStart,
                                sweepAngle = arcSweep,
                                useCenter = false,
                                topLeft = Offset(cx - arcRadius, cy - arcRadius),
                                size = Size(arcRadius * 2, arcRadius * 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.5f + energy * 2f
                                )
                            )
                        }

                        // Radial lines from center — driven by bass
                        if (bassEnergy > 0.2f) {
                            val lineLen = minDim * 0.1f + bassEnergy * minDim * 0.3f
                            val lineAngle = Math.toRadians(0.0)
                            val endX = cx + lineLen * cos(lineAngle).toFloat()
                            val endY = cy + lineLen * sin(lineAngle).toFloat()

                            drawLine(
                                color = theme.peakColor(0, bandCount)
                                    .copy(alpha = bassEnergy * 0.5f),
                                start = Offset(cx, cy),
                                end = Offset(endX, endY),
                                strokeWidth = 1f + bassEnergy * 2f
                            )
                        }
                    }
                }
            }
        }
    }
}
