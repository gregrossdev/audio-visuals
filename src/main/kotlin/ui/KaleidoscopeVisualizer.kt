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

        // --- Proportional radius boundaries for 3 depth layers ---
        val innerRadiusStart = minDim * 0.08f
        val innerRadiusEnd = minDim * 0.22f
        val midRadiusStart = minDim * 0.22f
        val midRadiusEnd = minDim * 0.38f
        val outerRadiusStart = minDim * 0.38f
        val outerRadiusEnd = minDim * 0.48f

        // --- Proportional stroke widths ---
        val strokeThin = minDim * 0.003f
        val strokeMedium = minDim * 0.005f

        // Compute energy
        val energy = magnitudes.map { ((it + 80f) / 80f).coerceIn(0f, 1f) }.average().toFloat()
        val bassEnergy = run {
            val bands = minOf(4, bandCount)
            var sum = 0f
            for (i in 0 until bands) sum += ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f)
            sum / bands
        }

        // Band energy breakdown for layered reactivity
        val highEnergy = run {
            val start = (bandCount * 3) / 4
            val end = bandCount
            if (start >= end) energy
            else {
                var sum = 0f
                for (i in start until end) sum += ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f)
                sum / (end - start)
            }
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

                        // === INNER LAYER — reacts to highs, small detailed shapes ===
                        val innerShapeCount = minOf(bandCount, config.shapesPerWedge + 4)
                        for (j in 0 until innerShapeCount) {
                            val t = j.toFloat() / innerShapeCount
                            val bandIndex = ((bandCount * 3 / 4) + (t * bandCount / 4).toInt())
                                .coerceIn(0, bandCount - 1)
                            val normalized = ((magnitudes[bandIndex] + 80f) / 80f).coerceIn(0f, 1f)
                            if (normalized < 0.05f) continue

                            val radius = innerRadiusStart + t * (innerRadiusEnd - innerRadiusStart)
                            val wedgeAngle = Math.toRadians(
                                (-segmentAngle / 2.0 + t * segmentAngle * 0.8)
                            )

                            val shapeX = cx + radius * cos(wedgeAngle).toFloat()
                            val shapeY = cy + radius * sin(wedgeAngle).toFloat()
                            val shapeSize = normalized * minDim * 0.02f * (0.8f + highEnergy * 0.4f)

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

                        // === MIDDLE LAYER — reacts to mids, arcs and triangles ===
                        val midShapeCount = minOf(bandCount, config.shapesPerWedge)
                        for (j in 0 until midShapeCount) {
                            val t = j.toFloat() / midShapeCount
                            val bandIndex = ((bandCount / 4) + (t * bandCount / 2).toInt())
                                .coerceIn(0, bandCount - 1)
                            val normalized = ((magnitudes[bandIndex] + 80f) / 80f).coerceIn(0f, 1f)
                            if (normalized < 0.05f) continue

                            val radius = midRadiusStart + t * (midRadiusEnd - midRadiusStart)
                            val wedgeAngle = Math.toRadians(
                                (-segmentAngle / 2.0 + t * segmentAngle * 0.8)
                            )

                            val shapeX = cx + radius * cos(wedgeAngle).toFloat()
                            val shapeY = cy + radius * sin(wedgeAngle).toFloat()
                            val shapeSize = normalized * minDim * 0.03f * (0.8f + midsScale * 0.4f)

                            val color = theme.barColor(bandIndex, bandCount, normalized)

                            // Draw small triangles for variety
                            val triHalf = shapeSize * 0.8f
                            val p1 = Offset(shapeX, shapeY - triHalf)
                            val p2 = Offset(shapeX - triHalf, shapeY + triHalf * 0.6f)
                            val p3 = Offset(shapeX + triHalf, shapeY + triHalf * 0.6f)

                            val triPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(p1.x, p1.y)
                                lineTo(p2.x, p2.y)
                                lineTo(p3.x, p3.y)
                                close()
                            }
                            drawPath(
                                path = triPath,
                                color = color.copy(alpha = normalized * 0.7f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeThin
                                )
                            )

                            // Glow behind triangle
                            drawCircle(
                                color = color.copy(alpha = normalized * theme.glowAlpha * 0.5f),
                                radius = shapeSize * 1.5f,
                                center = Offset(shapeX, shapeY)
                            )
                        }

                        // === OUTER LAYER — reacts to bass, large arc sweeps ===
                        val outerArcRadius = outerRadiusStart + bassEnergy * (outerRadiusEnd - outerRadiusStart)
                        val outerArcAlpha = (bassEnergy * 2f).coerceIn(0f, 1f)
                        if (outerArcAlpha > 0.01f) {
                            val arcAngleStart = -segmentAngle / 2f + 2f
                            val arcSweep = (segmentAngle - 4f) * bassEnergy

                            drawArc(
                                color = theme.barColor(0, bandCount, bassEnergy)
                                    .copy(alpha = outerArcAlpha * 0.5f),
                                startAngle = arcAngleStart,
                                sweepAngle = arcSweep,
                                useCenter = false,
                                topLeft = Offset(cx - outerArcRadius, cy - outerArcRadius),
                                size = Size(outerArcRadius * 2, outerArcRadius * 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeMedium + bassEnergy * strokeMedium
                                )
                            )
                        }

                        // Connecting lines at outer edge
                        val outerLineCount = minOf(3, bandCount / 4)
                        for (j in 0 until outerLineCount) {
                            val t = j.toFloat() / outerLineCount
                            val bandIndex = (t * bandCount / 4).toInt().coerceIn(0, bandCount - 1)
                            val normalized = ((magnitudes[bandIndex] + 80f) / 80f).coerceIn(0f, 1f)
                            if (normalized < 0.1f) continue

                            val angle1 = Math.toRadians(
                                (-segmentAngle / 2.0 + t * segmentAngle * 0.6)
                            )
                            val angle2 = Math.toRadians(
                                (-segmentAngle / 2.0 + (t + 0.3f) * segmentAngle * 0.6)
                            )
                            val r = outerRadiusStart + normalized * (outerRadiusEnd - outerRadiusStart) * 0.5f

                            drawLine(
                                color = theme.barColor(bandIndex, bandCount, normalized)
                                    .copy(alpha = normalized * 0.4f),
                                start = Offset(
                                    cx + r * cos(angle1).toFloat(),
                                    cy + r * sin(angle1).toFloat()
                                ),
                                end = Offset(
                                    cx + r * cos(angle2).toFloat(),
                                    cy + r * sin(angle2).toFloat()
                                ),
                                strokeWidth = strokeThin
                            )
                        }

                        // Arc lines connecting shapes — smooth alpha transition
                        val arcAlpha = (energy * 2f).coerceIn(0f, 1f)
                        if (arcAlpha > 0.01f) {
                            val arcRadius = midRadiusStart + energy * (midRadiusEnd - midRadiusStart)
                            val arcAngleStart = -segmentAngle / 2f + 2f
                            val arcSweep = (segmentAngle - 4f) * energy

                            drawArc(
                                color = theme.barColor(0, bandCount, energy)
                                    .copy(alpha = arcAlpha * 0.6f),
                                startAngle = arcAngleStart,
                                sweepAngle = arcSweep,
                                useCenter = false,
                                topLeft = Offset(cx - arcRadius, cy - arcRadius),
                                size = Size(arcRadius * 2, arcRadius * 2),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = strokeThin + energy * strokeMedium
                                )
                            )
                        }

                        // Radial lines from center — driven by bass, smooth alpha
                        val radialAlpha = (bassEnergy * 2f).coerceIn(0f, 1f)
                        if (radialAlpha > 0.01f) {
                            val lineLen = innerRadiusStart + bassEnergy * (outerRadiusEnd - innerRadiusStart)
                            val lineAngle = Math.toRadians(0.0)
                            val endX = cx + lineLen * cos(lineAngle).toFloat()
                            val endY = cy + lineLen * sin(lineAngle).toFloat()

                            drawLine(
                                color = theme.peakColor(0, bandCount)
                                    .copy(alpha = radialAlpha * 0.5f),
                                start = Offset(cx, cy),
                                end = Offset(endX, endY),
                                strokeWidth = strokeThin + bassEnergy * strokeMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
