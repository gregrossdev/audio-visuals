package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import audio.AudioFeatures
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private class MandalaState(rings: Int) {
    val ringAngles = FloatArray(rings)
    var pulseScale = 1f
}

@Composable
fun MandalaVisualizer(
    magnitudes: FloatArray,
    theme: ColorTheme,
    isBeat: Boolean,
    config: MandalaConfig = MandalaConfig(),
    audioFeatures: AudioFeatures? = null,
    modifier: Modifier = Modifier
) {
    val state = remember(config.rings) { MandalaState(config.rings) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val minDim = min(w, h)
        val bandCount = magnitudes.size
        if (bandCount == 0) return@Canvas

        // Compute overall and per-band energy
        val energy = magnitudes.map { ((it + 80f) / 80f).coerceIn(0f, 1f) }.average().toFloat()
        val bandEnergies = FloatArray(config.rings) { ring ->
            val bandStart = (ring.toFloat() / config.rings * bandCount).toInt()
            val bandEnd = ((ring + 1).toFloat() / config.rings * bandCount).toInt().coerceAtMost(bandCount)
            var sum = 0f
            var count = 0
            for (i in bandStart until bandEnd) {
                sum += ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f)
                count++
            }
            if (count > 0) sum / count else 0f
        }

        // Reactive background
        val bgAlpha = (energy * 0.25f).coerceIn(0f, 0.25f)
        if (bgAlpha > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.hsl(theme.backgroundHue, 0.5f, 0.1f).copy(alpha = bgAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = minDim * 0.55f
                ),
                radius = minDim * 0.55f,
                center = Offset(cx, cy)
            )
        }

        // Beat pulse — driven by energy envelope when available
        val pulseEnergy = audioFeatures?.energy ?: energy
        val targetPulse = if (isBeat) 1f + config.pulseIntensity * (0.5f + pulseEnergy) else 1f
        state.pulseScale += (targetPulse - state.pulseScale) * 0.2f

        // Ring spacing
        val innerRadius = minDim * 0.08f
        val outerRadius = minDim * 0.42f
        val ringSpacing = if (config.rings > 1) (outerRadius - innerRadius) / (config.rings - 1) else 0f

        // Per-band energy from AudioFeatures for ring scaling
        val featureBandEnergies = audioFeatures?.bandEnergies

        for (ring in 0 until config.rings) {
            val ringEnergy = bandEnergies[ring]
            // Scale ring radius using musically accurate band energies
            val bandScale = when {
                featureBandEnergies == null -> 1f
                ring < config.rings / 4 -> 0.8f + featureBandEnergies.subBass * 0.4f
                ring < config.rings / 2 -> 0.8f + featureBandEnergies.bass * 0.4f
                ring < config.rings * 3 / 4 -> 0.8f + featureBandEnergies.mids * 0.4f
                else -> 0.8f + featureBandEnergies.highs * 0.4f
            }
            val ringRadius = (innerRadius + ring * ringSpacing) * bandScale

            // Update rotation — inner rings slower, outer faster
            val speedFraction = ring.toFloat() / config.rings.coerceAtLeast(1)
            val speed = config.innerRotationSpeed + (config.outerRotationSpeed - config.innerRotationSpeed) * speedFraction
            val direction = if (ring % 2 == 0) 1f else -1f
            state.ringAngles[ring] += speed * direction * (0.5f + ringEnergy)

            val elements = config.elementsPerRing
            val angleStep = 360f / elements
            val ringHueFraction = ring.toFloat() / config.rings
            val ringColor = theme.barColor(
                (ringHueFraction * bandCount).toInt().coerceIn(0, bandCount - 1),
                bandCount,
                ringEnergy
            )

            rotate(degrees = state.ringAngles[ring], pivot = Offset(cx, cy)) {
                val elementRadius = ringRadius * state.pulseScale

                // Draw elements
                for (e in 0 until elements) {
                    val angle = Math.toRadians((e * angleStep).toDouble())
                    val ex = cx + elementRadius * cos(angle).toFloat()
                    val ey = cy + elementRadius * sin(angle).toFloat()

                    val elementSize = (3f + ringEnergy * minDim * 0.02f) * state.pulseScale
                    val elementAlpha = (0.4f + ringEnergy * 0.6f).coerceIn(0f, 1f)

                    // Glow
                    drawCircle(
                        color = ringColor.copy(alpha = elementAlpha * theme.glowAlpha),
                        radius = elementSize * 2.5f,
                        center = Offset(ex, ey),
                        blendMode = BlendMode.Screen
                    )

                    // Shape
                    when (config.shapeType) {
                        MandalaShape.CIRCLE -> {
                            drawCircle(
                                color = ringColor.copy(alpha = elementAlpha),
                                radius = elementSize,
                                center = Offset(ex, ey)
                            )
                        }
                        MandalaShape.DIAMOND -> {
                            val path = Path().apply {
                                moveTo(ex, ey - elementSize)
                                lineTo(ex + elementSize, ey)
                                lineTo(ex, ey + elementSize)
                                lineTo(ex - elementSize, ey)
                                close()
                            }
                            drawPath(path, ringColor.copy(alpha = elementAlpha))
                        }
                        MandalaShape.TRIANGLE -> {
                            val path = Path().apply {
                                moveTo(ex, ey - elementSize)
                                lineTo(ex + elementSize * 0.866f, ey + elementSize * 0.5f)
                                lineTo(ex - elementSize * 0.866f, ey + elementSize * 0.5f)
                                close()
                            }
                            drawPath(path, ringColor.copy(alpha = elementAlpha))
                        }
                        MandalaShape.LINE -> {
                            val lineAngle = Math.atan2(
                                (ey - cy).toDouble(),
                                (ex - cx).toDouble()
                            )
                            val lx = elementSize * cos(lineAngle).toFloat()
                            val ly = elementSize * sin(lineAngle).toFloat()
                            drawLine(
                                color = ringColor.copy(alpha = elementAlpha),
                                start = Offset(ex - lx, ey - ly),
                                end = Offset(ex + lx, ey + ly),
                                strokeWidth = 2f + ringEnergy * 2f
                            )
                        }
                    }

                    // Connection lines between adjacent elements
                    if (config.showConnections && elements > 1) {
                        val nextAngle = Math.toRadians(((e + 1) * angleStep).toDouble())
                        val nx = cx + elementRadius * cos(nextAngle).toFloat()
                        val ny = cy + elementRadius * sin(nextAngle).toFloat()

                        drawLine(
                            color = ringColor.copy(alpha = ringEnergy * config.connectionAlpha),
                            start = Offset(ex, ey),
                            end = Offset(nx, ny),
                            strokeWidth = 1f + ringEnergy
                        )
                    }
                }

                // Ring outline circle
                if (ringEnergy > 0.1f) {
                    drawCircle(
                        color = ringColor.copy(alpha = ringEnergy * 0.2f),
                        radius = elementRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1f + ringEnergy)
                    )
                }
            }
        }
    }
}
