package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import audio.AudioFeatures
import audio.OpenSimplex2

private const val SEED = 137L

private class TerrainState {
    var zOffset = 0.0
    var timeOffset = 0.0
    var beatPulse = 0f
}

@Composable
fun TerrainVisualizer(
    magnitudes: FloatArray,
    theme: ColorTheme,
    isBeat: Boolean,
    config: TerrainConfig = TerrainConfig(),
    audioFeatures: AudioFeatures? = null,
    modifier: Modifier = Modifier
) {
    val state = remember { TerrainState() }
    // Pre-allocate projection arrays
    val screenXArr = remember(config.gridCols, config.gridRows) {
        FloatArray(config.gridCols * config.gridRows)
    }
    val screenYArr = remember(config.gridCols, config.gridRows) {
        FloatArray(config.gridCols * config.gridRows)
    }
    val heightArr = remember(config.gridCols, config.gridRows) {
        FloatArray(config.gridCols * config.gridRows)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w == 0f || h == 0f) return@Canvas

        val cols = config.gridCols
        val rows = config.gridRows
        val bandCount = magnitudes.size

        // Extract audio features
        val be = audioFeatures?.bandEnergies
        val onsets = audioFeatures?.onsets
        val energy = audioFeatures?.energy ?: if (bandCount > 0) {
            magnitudes.map { ((it + 80f) / 80f).coerceIn(0f, 1f) }.average().toFloat()
        } else 0f
        val centroid = audioFeatures?.spectralCentroid ?: 0.5f
        val bassEnergy = be?.bass ?: 0f
        val midsEnergy = be?.mids ?: 0f
        val highsEnergy = be?.highs ?: 0f

        val isBeatEvent = onsets?.bass == true || onsets?.subBass == true || (onsets == null && isBeat)

        // Advance scroll and time (D-12.4)
        val effectiveSpeed = config.scrollSpeed * (1f + energy * 2f)
        state.zOffset += effectiveSpeed.toDouble()
        state.timeOffset += 0.005

        // Beat pulse (D-12.5)
        if (isBeatEvent) state.beatPulse = 0.3f
        if (state.beatPulse > 0f) state.beatPulse *= 0.9f

        val centerX = w / 2f
        val centerY = h * 0.75f // Camera looks slightly downward
        val rowSpacing = 1.0
        val colSpacing = w / (cols - 1).toFloat()

        // Compute heights and project to screen (D-12.1, D-12.3)
        for (row in 0 until rows) {
            val worldZ = row.toDouble() * rowSpacing + state.zOffset

            for (col in 0 until cols) {
                val idx = row * cols + col

                // World X centered on 0
                val worldXNorm = (col.toFloat() / (cols - 1).toFloat()) - 0.5f
                val worldX = worldXNorm * 10f

                // Base height from noise
                var height = OpenSimplex2.noise3_ImproveXY(
                    SEED,
                    worldX.toDouble() * config.noiseScale,
                    worldZ * config.noiseScale,
                    state.timeOffset
                ) * config.heightScale

                // Blend in magnitudes — map column to frequency bin (D-12.3)
                if (bandCount > 0) {
                    val bin = (col * bandCount / cols).coerceIn(0, bandCount - 1)
                    val mag = ((magnitudes[bin] + 80f) / 80f).coerceIn(0f, 1f)
                    height += mag * config.heightScale * 0.5f
                }

                // Band-specific boost (D-12.5)
                val colFraction = col.toFloat() / cols
                when {
                    colFraction < 0.25f -> height *= 1f + bassEnergy * 0.8f
                    colFraction < 0.6f -> height *= 1f + midsEnergy * 0.4f
                    else -> height *= 1f + highsEnergy * 0.3f
                }

                // Beat pulse
                height *= 1f + state.beatPulse

                heightArr[idx] = height

                // Perspective projection (D-12.1)
                val depth = (rows - row).toFloat() // Far rows have higher depth
                val depthFactor = config.focalLength / (depth * 8f + config.focalLength)

                screenXArr[idx] = centerX + worldXNorm * w * 0.8f * depthFactor
                screenYArr[idx] = centerY - depth * 12f * depthFactor - height * depthFactor * config.cameraTilt
            }
        }

        // Render back-to-front (D-12.6)
        for (row in 0 until rows) {
            // Distance fog alpha
            val depthNorm = row.toFloat() / rows
            val fogAlpha = if (depthNorm < config.fogStart) 1f
            else if (depthNorm > config.fogEnd) 0f
            else 1f - (depthNorm - config.fogStart) / (config.fogEnd - config.fogStart)

            if (fogAlpha < 0.02f) continue

            for (col in 0 until cols) {
                val idx = row * cols + col
                val sx = screenXArr[idx]
                val sy = screenYArr[idx]
                val ht = heightArr[idx]

                // Height-based color (D-12.5 centroid → color)
                val heightNorm = ((ht / config.heightScale + 1f) / 2f).coerceIn(0f, 1f)
                val hue = theme.hueStart + heightNorm * (theme.hueEnd - theme.hueStart) + centroid * 20f
                val lightness = (theme.lightnessMin + heightNorm * theme.lightnessScale).coerceIn(0f, 1f)
                val color = Color.hsl(hue.mod(360f), theme.saturation, lightness)
                val alpha = fogAlpha * 0.85f

                // Horizontal line to next column
                if (col < cols - 1) {
                    val nextIdx = idx + 1
                    val nextSx = screenXArr[nextIdx]
                    val nextSy = screenYArr[nextIdx]

                    // Glow for peaks
                    if (heightNorm > 0.6f) {
                        drawLine(
                            color = color.copy(alpha = alpha * theme.glowAlpha * 0.4f),
                            start = Offset(sx, sy),
                            end = Offset(nextSx, nextSy),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round,
                            blendMode = BlendMode.Screen
                        )
                    }

                    drawLine(
                        color = color.copy(alpha = alpha),
                        start = Offset(sx, sy),
                        end = Offset(nextSx, nextSy),
                        strokeWidth = 1.2f,
                        cap = StrokeCap.Round
                    )
                }

                // Depth line to next row
                if (row < rows - 1) {
                    val nextRowIdx = (row + 1) * cols + col
                    val nextRowSx = screenXArr[nextRowIdx]
                    val nextRowSy = screenYArr[nextRowIdx]
                    val nextRowFogNorm = (row + 1).toFloat() / rows
                    val nextRowFogAlpha = if (nextRowFogNorm < config.fogStart) 1f
                    else if (nextRowFogNorm > config.fogEnd) 0f
                    else 1f - (nextRowFogNorm - config.fogStart) / (config.fogEnd - config.fogStart)
                    val depthAlpha = (fogAlpha * 0.5f + nextRowFogAlpha * 0.5f) * 0.4f

                    if (depthAlpha > 0.02f) {
                        drawLine(
                            color = color.copy(alpha = depthAlpha),
                            start = Offset(sx, sy),
                            end = Offset(nextRowSx, nextRowSy),
                            strokeWidth = 0.8f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}
