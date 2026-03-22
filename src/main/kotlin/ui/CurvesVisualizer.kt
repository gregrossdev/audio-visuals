package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import audio.AudioFeatures
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

// Musical interval ratios for Lissajous (D-13.2)
private val INTERVALS = arrayOf(
    1 to 2,   // octave
    2 to 3,   // fifth
    3 to 4,   // fourth
    3 to 5,   // major sixth
    4 to 5,   // major third
    5 to 6,   // minor third
)

private class CurvesState {
    var time = 0.0
    var ratioIndex = 0
    var beatCooldown = 0
    var burstTimer = 0f
    // Rose petal count — smoothed to avoid jitter
    var currentK = 3f
}

@Composable
fun CurvesVisualizer(
    magnitudes: FloatArray,
    theme: ColorTheme,
    isBeat: Boolean,
    config: CurvesConfig = CurvesConfig(),
    audioFeatures: AudioFeatures? = null,
    modifier: Modifier = Modifier
) {
    val state = remember { CurvesState() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w == 0f || h == 0f) return@Canvas

        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(w, h) * 0.38f
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

        // Advance time (D-13.6)
        state.time += 0.02 * config.speedMultiplier * (1f + energy * 2f)

        // Beat → pattern transition (D-13.6)
        if (state.beatCooldown > 0) state.beatCooldown--
        if (isBeatEvent && state.beatCooldown <= 0) {
            state.ratioIndex = (state.ratioIndex + 1) % INTERVALS.size
            state.beatCooldown = 10
            state.burstTimer = 1f
        }
        if (state.burstTimer > 0f) state.burstTimer *= 0.92f

        // Trail fade — semi-transparent background overlay (D-13.5)
        drawRect(
            color = Color.Black.copy(alpha = config.trailAlpha),
            topLeft = Offset.Zero,
            size = Size(w, h)
        )

        val points = config.pointCount
        val burstBoost = state.burstTimer.coerceIn(0f, 1f)

        when (config.curveMode) {
            CurveMode.LISSAJOUS -> {
                // Lissajous: x = A·sin(a·t + δ), y = B·sin(b·t) (D-13.2)
                val (a, b) = INTERVALS[state.ratioIndex]
                val ampA = (0.3f + bassEnergy * 0.4f) * radius
                val ampB = (0.3f + highsEnergy * 0.4f) * radius
                val delta = centroid * PI.toFloat()
                val tBase = state.time.toFloat()

                var prevX = cx + ampA * sin(a * tBase + delta)
                var prevY = cy + ampB * sin(b * tBase)

                for (i in 1 until points) {
                    val frac = i.toFloat() / points
                    val t = tBase + frac * TWO_PI * 2f
                    val px = cx + ampA * sin(a * t + delta)
                    val py = cy + ampB * sin(b * t)

                    val hue = theme.hueStart + frac * (theme.hueEnd - theme.hueStart) * config.colorCycleSpeed + centroid * 30f
                    val lightness = (theme.lightnessMin + 0.3f + burstBoost * 0.15f).coerceIn(0f, 1f)
                    val color = Color.hsl(hue.mod(360f), theme.saturation, lightness)

                    // Glow
                    drawLine(
                        color = color.copy(alpha = 0.3f * theme.glowAlpha),
                        start = Offset(prevX, prevY),
                        end = Offset(px, py),
                        strokeWidth = config.lineWidth * 3f,
                        cap = StrokeCap.Round,
                        blendMode = BlendMode.Screen
                    )
                    // Main
                    drawLine(
                        color = color.copy(alpha = 0.85f),
                        start = Offset(prevX, prevY),
                        end = Offset(px, py),
                        strokeWidth = config.lineWidth,
                        cap = StrokeCap.Round
                    )
                    prevX = px
                    prevY = py
                }
            }

            CurveMode.ROSE -> {
                // Rose: r = a·cos(k·θ) (D-13.3)
                // Smooth k toward target to avoid jitter
                val targetK = 2f + floor(bassEnergy * 6f).coerceIn(0f, 6f)
                state.currentK += (targetK - state.currentK) * 0.05f
                val k = state.currentK

                val baseAmp = (0.3f + energy * 0.3f) * radius
                val tOffset = state.time.toFloat() * 0.3f

                // Draw 2 nested roses: outer (bass-driven), inner (mids-driven)
                val roses = listOf(
                    Triple(k, baseAmp, 0f),
                    Triple(k + 1f, baseAmp * (0.5f + midsEnergy * 0.3f), 0.4f)
                )

                for ((roseK, amp, hueFrac) in roses) {
                    val petals = if (roseK.toInt() % 2 == 0) TWO_PI * roseK.toInt() else TWO_PI
                    var prevX = Float.NaN
                    var prevY = Float.NaN

                    for (i in 0 until points) {
                        val frac = i.toFloat() / points
                        val theta = frac * petals + tOffset
                        val r = amp * cos(roseK * theta)
                        val px = cx + r * cos(theta)
                        val py = cy + r * sin(theta)

                        if (!prevX.isNaN()) {
                            val hue = theme.hueStart + (frac + hueFrac) * (theme.hueEnd - theme.hueStart) * config.colorCycleSpeed + centroid * 20f
                            val lightness = (theme.lightnessMin + 0.25f + burstBoost * 0.15f).coerceIn(0f, 1f)
                            val color = Color.hsl(hue.mod(360f), theme.saturation, lightness)

                            drawLine(
                                color = color.copy(alpha = 0.3f * theme.glowAlpha),
                                start = Offset(prevX, prevY),
                                end = Offset(px, py),
                                strokeWidth = config.lineWidth * 2.5f,
                                cap = StrokeCap.Round,
                                blendMode = BlendMode.Screen
                            )
                            drawLine(
                                color = color.copy(alpha = 0.8f),
                                start = Offset(prevX, prevY),
                                end = Offset(px, py),
                                strokeWidth = config.lineWidth,
                                cap = StrokeCap.Round
                            )
                        }
                        prevX = px
                        prevY = py
                    }
                }
            }

            CurveMode.SPIROGRAPH -> {
                // Hypotrochoid: x = (R-r)·cos(t) + d·cos((R-r)/r·t) (D-13.4)
                val bigR = 5f + bassEnergy * 3f
                val smallR = 2f + midsEnergy * 2f
                val d = 1f + highsEnergy * 2f
                val diff = bigR - smallR
                val ratio = diff / smallR.coerceAtLeast(0.1f)
                val scale = radius / (bigR + d + 1f)
                val tBase = state.time.toFloat()

                var prevX = cx + (diff * cos(tBase) + d * cos(ratio * tBase)) * scale
                var prevY = cy + (diff * sin(tBase) - d * sin(ratio * tBase)) * scale

                for (i in 1 until points) {
                    val frac = i.toFloat() / points
                    val t = tBase + frac * TWO_PI * 4f
                    val px = cx + (diff * cos(t) + d * cos(ratio * t)) * scale
                    val py = cy + (diff * sin(t) - d * sin(ratio * t)) * scale

                    val hue = theme.hueStart + frac * (theme.hueEnd - theme.hueStart) * config.colorCycleSpeed + centroid * 25f
                    val lightness = (theme.lightnessMin + 0.3f + burstBoost * 0.15f).coerceIn(0f, 1f)
                    val color = Color.hsl(hue.mod(360f), theme.saturation, lightness)

                    drawLine(
                        color = color.copy(alpha = 0.3f * theme.glowAlpha),
                        start = Offset(prevX, prevY),
                        end = Offset(px, py),
                        strokeWidth = config.lineWidth * 2.5f,
                        cap = StrokeCap.Round,
                        blendMode = BlendMode.Screen
                    )
                    drawLine(
                        color = color.copy(alpha = 0.85f),
                        start = Offset(prevX, prevY),
                        end = Offset(px, py),
                        strokeWidth = config.lineWidth,
                        cap = StrokeCap.Round
                    )
                    prevX = px
                    prevY = py
                }
            }
        }
    }
}
