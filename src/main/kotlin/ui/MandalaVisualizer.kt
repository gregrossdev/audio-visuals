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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import audio.AudioFeatures
import kotlin.math.*

private class MandalaState {
    var rotation = 0f
    var pulseScale = 1f
    var directionMultiplier = 1f
    var smoothedHighs = 0f
    var smoothedBass = 0f
    var smoothedMids = 0f
    var smoothedCentroid = 0.5f
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
    val state = remember { MandalaState() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val minDim = min(w, h)
        val bandCount = magnitudes.size
        if (bandCount == 0) return@Canvas

        // --- Audio smoothing ---
        val bass = audioFeatures?.bandEnergies?.bass ?: run {
            magnitudes.take(bandCount / 4).map { ((it + 80f) / 80f).coerceIn(0f, 1f) }
                .average().toFloat()
        }
        val mids = audioFeatures?.bandEnergies?.mids ?: run {
            magnitudes.drop(bandCount / 4).take(bandCount / 2).map { ((it + 80f) / 80f).coerceIn(0f, 1f) }
                .average().toFloat()
        }
        val highs = audioFeatures?.bandEnergies?.highs ?: run {
            magnitudes.drop(bandCount * 3 / 4).map { ((it + 80f) / 80f).coerceIn(0f, 1f) }
                .average().toFloat()
        }
        val centroid = audioFeatures?.spectralCentroid ?: 0.5f

        state.smoothedBass += (bass - state.smoothedBass) * 0.1f
        state.smoothedMids += (mids - state.smoothedMids) * 0.1f
        state.smoothedHighs += (highs - state.smoothedHighs) * 0.08f
        state.smoothedCentroid += (centroid - state.smoothedCentroid) * 0.05f

        // Beat direction flip
        if (isBeat) {
            state.directionMultiplier = -state.directionMultiplier
        }

        // Pulse scale from bass
        state.pulseScale = 0.9f + state.smoothedBass * 0.3f * config.pulseIntensity

        // Rotation update
        state.rotation += config.innerRotationSpeed * state.directionMultiplier *
                (0.5f + state.smoothedMids)

        // --- Reactive background ---
        val energy = (state.smoothedBass + state.smoothedMids + state.smoothedHighs) / 3f
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

        // --- Compute hue offset from centroid ---
        val hueOffset = state.smoothedCentroid * 60f

        // --- Flower of Life circle centers ---
        val baseR = minDim * 0.12f * state.pulseScale
        val allCenters = mutableListOf<Pair<Offset, Int>>() // center, ring level

        // Ring 0: center circle
        allCenters.add(Pair(Offset(cx, cy), 0))

        // Build Flower of Life rings
        // Each ring n places circles at distance n*R from center at specific angles
        // Ring 1: 6 circles at 60deg intervals, distance R
        // Ring 2: 12 circles (6 at distance 2R at 30deg offsets, 6 at distance R*sqrt(3) at 60deg)
        // General approach: use hex grid positions
        val ringCentersSet = mutableSetOf<Long>() // quantized positions for dedup
        fun quantize(offset: Offset): Long {
            val qx = (offset.x * 10f).toLong()
            val qy = (offset.y * 10f).toLong()
            return qx * 100000L + qy
        }
        ringCentersSet.add(quantize(Offset(cx, cy)))

        val maxRings = config.rings.coerceIn(1, 8)
        for (ring in 1..maxRings) {
            // Hex grid ring: walk around the hexagonal ring at distance `ring`
            // A hex ring of radius n has 6*n positions
            val positions = 6 * ring
            for (i in 0 until positions) {
                // Determine hex coordinates using cube coordinate ring walking
                val side = i / ring        // which of 6 sides (0-5)
                val offset = i % ring      // position along that side

                // Start direction for each side
                val startAngle = side * (PI / 3.0)
                val stepAngle = startAngle + (PI / 3.0)

                // Start point of this side at distance ring*R from center
                val startX = cx + (ring * baseR * cos(startAngle - PI / 6.0)).toFloat()
                val startY = cy + (ring * baseR * sin(startAngle - PI / 6.0)).toFloat()

                // Step along this side
                val dx = (baseR * cos(stepAngle - PI / 6.0)).toFloat()
                val dy = (baseR * sin(stepAngle - PI / 6.0)).toFloat()

                val px = startX + offset * dx
                val py = startY + offset * dy
                val pos = Offset(px, py)
                val key = quantize(pos)
                if (key !in ringCentersSet) {
                    ringCentersSet.add(key)
                    allCenters.add(Pair(pos, ring))
                }
            }
        }

        // --- Draw Flower of Life ---
        rotate(degrees = state.rotation, pivot = Offset(cx, cy)) {
            // Draw circles for each center
            for ((center, ring) in allCenters) {
                val ringFraction = ring.toFloat() / maxRings.coerceAtLeast(1)
                val alphaBase = (1f - ringFraction * 0.6f).coerceIn(0.1f, 1f)
                val circleAlpha = alphaBase * (0.4f + state.smoothedBass * 0.6f)

                val circleColor = theme.barColor(
                    ((ringFraction + hueOffset / 360f) * bandCount).toInt().coerceIn(0, bandCount - 1),
                    bandCount,
                    0.5f + state.smoothedMids * 0.5f
                )

                // Glow effect on main circles
                drawCircle(
                    color = circleColor.copy(alpha = (circleAlpha * theme.glowAlpha * 0.5f).coerceIn(0f, 1f)),
                    radius = baseR * 1.15f,
                    center = center,
                    style = Stroke(width = minDim * 0.004f),
                    blendMode = BlendMode.Screen
                )

                // Main circle outline
                drawCircle(
                    color = circleColor.copy(alpha = circleAlpha.coerceIn(0f, 1f)),
                    radius = baseR,
                    center = center,
                    style = Stroke(width = minDim * 0.002f + state.smoothedBass * minDim * 0.002f)
                )
            }

            // --- Recursive fractal detail at each circle center ---
            val effectiveDetail = (state.smoothedHighs * config.detailLevel).coerceIn(0.5f, config.detailLevel.toFloat()).toInt().coerceIn(1, 4)

            for ((center, ring) in allCenters) {
                if (ring == 0) continue // skip center for fractals

                val ringFraction = ring.toFloat() / maxRings.coerceAtLeast(1)
                val fractalColor = theme.barColor(
                    ((ringFraction + hueOffset / 360f) * bandCount).toInt().coerceIn(0, bandCount - 1),
                    bandCount,
                    0.3f + state.smoothedHighs * 0.7f
                )

                drawFractalDetail(
                    center = center,
                    radius = baseR * 0.4f,
                    depth = effectiveDetail,
                    maxDepth = effectiveDetail,
                    color = fractalColor,
                    minDim = minDim
                )
            }

            // --- String-art web connections ---
            if (config.showConnections && allCenters.size > 1) {
                val connectionStep = max(2, (6 - state.smoothedMids * 4).toInt())
                val connectionColor = theme.barColor(
                    (bandCount / 2),
                    bandCount,
                    0.3f + state.smoothedMids * 0.5f
                ).copy(alpha = (state.smoothedMids * config.connectionAlpha).coerceIn(0f, 0.6f))

                // Connect centers across rings
                for (i in allCenters.indices) {
                    if (i % connectionStep != 0) continue
                    val (c1, ring1) = allCenters[i]
                    for (j in (i + connectionStep) until allCenters.size step connectionStep) {
                        val (c2, ring2) = allCenters[j]
                        // Only connect across different rings
                        if (ring1 != ring2) {
                            drawLine(
                                color = connectionColor,
                                start = c1,
                                end = c2,
                                strokeWidth = minDim * 0.001f
                            )
                        }
                    }
                }
            }

            // --- Outer ring rotation effect ---
            // Draw additional rotating ring outlines at outerRotationSpeed
            val outerRotAngle = state.rotation * config.outerRotationSpeed / config.innerRotationSpeed.coerceAtLeast(0.01f)
            rotate(degrees = outerRotAngle - state.rotation, pivot = Offset(cx, cy)) {
                for (ring in 1..maxRings) {
                    val ringRadius = ring * baseR
                    val ringAlpha = (0.15f - ring * 0.02f).coerceIn(0.02f, 0.15f) *
                            (0.5f + state.smoothedMids * 0.5f)
                    val ringColor = theme.barColor(
                        ((ring.toFloat() / maxRings + hueOffset / 360f) * bandCount).toInt().coerceIn(0, bandCount - 1),
                        bandCount,
                        0.4f
                    )
                    drawCircle(
                        color = ringColor.copy(alpha = ringAlpha.coerceIn(0f, 1f)),
                        radius = ringRadius,
                        center = Offset(cx, cy),
                        style = Stroke(width = minDim * 0.001f)
                    )
                }
            }
        }
    }
}

/**
 * Recursively draw smaller circles around a parent center to create fractal detail.
 */
private fun DrawScope.drawFractalDetail(
    center: Offset,
    radius: Float,
    depth: Int,
    maxDepth: Int,
    color: Color,
    minDim: Float
) {
    if (depth <= 0 || radius < minDim * 0.003f) return

    val alphaScale = (depth.toFloat() / maxDepth).coerceIn(0.1f, 0.8f)
    val subCount = if (depth > 2) 3 else 6

    for (i in 0 until subCount) {
        val angle = (i.toFloat() / subCount) * 2f * PI.toFloat()
        val sx = center.x + radius * cos(angle)
        val sy = center.y + radius * sin(angle)
        val subCenter = Offset(sx, sy)

        drawCircle(
            color = color.copy(alpha = (alphaScale * 0.5f).coerceIn(0f, 1f)),
            radius = radius,
            center = subCenter,
            style = Stroke(width = minDim * 0.001f)
        )

        // Recurse
        drawFractalDetail(
            center = subCenter,
            radius = radius * 0.4f,
            depth = depth - 1,
            maxDepth = maxDepth,
            color = color,
            minDim = minDim
        )
    }
}
