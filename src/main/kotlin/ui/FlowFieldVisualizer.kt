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
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private const val TWO_PI = (2.0 * PI).toFloat()
private const val SEED = 42L

private class FlowParticle {
    var x = 0f
    var y = 0f
    var prevX = 0f
    var prevY = 0f
    var vx = 0f
    var vy = 0f
    var life = 0f
    var hue = 0f
    var size = 1.5f
    var alive = false
}

private class FlowParticlePool(val count: Int) {
    val particles = Array(count) { FlowParticle() }
    private var nextIndex = 0

    fun spawn(width: Float, height: Float) {
        val p = particles[nextIndex]
        // Spawn at random edge
        when (Random.nextInt(4)) {
            0 -> { p.x = Random.nextFloat() * width; p.y = 0f }
            1 -> { p.x = Random.nextFloat() * width; p.y = height }
            2 -> { p.x = 0f; p.y = Random.nextFloat() * height }
            3 -> { p.x = width; p.y = Random.nextFloat() * height }
        }
        p.prevX = p.x
        p.prevY = p.y
        p.vx = 0f
        p.vy = 0f
        p.life = 1f
        p.size = 1.5f
        p.alive = true
        nextIndex = (nextIndex + 1) % count
    }

    fun initAll(width: Float, height: Float) {
        for (p in particles) {
            p.x = Random.nextFloat() * width
            p.y = Random.nextFloat() * height
            p.prevX = p.x
            p.prevY = p.y
            p.vx = 0f
            p.vy = 0f
            p.life = Random.nextFloat()
            p.alive = true
        }
    }
}

private class FlowFieldState {
    var timeOffset = 0.0
    var burstTimer = 0f
    var initialized = false
}

@Composable
fun FlowFieldVisualizer(
    magnitudes: FloatArray,
    theme: ColorTheme,
    isBeat: Boolean,
    config: FlowFieldConfig = FlowFieldConfig(),
    audioFeatures: AudioFeatures? = null,
    modifier: Modifier = Modifier
) {
    val pool = remember(config.particleCount) { FlowParticlePool(config.particleCount) }
    val state = remember { FlowFieldState() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w == 0f || h == 0f) return@Canvas

        // Initialize particles spread across canvas on first frame
        if (!state.initialized) {
            pool.initAll(w, h)
            state.initialized = true
        }

        val bandCount = magnitudes.size

        // Extract audio features with fallbacks
        val be = audioFeatures?.bandEnergies
        val onsets = audioFeatures?.onsets
        val energy = audioFeatures?.energy ?: if (bandCount > 0) {
            magnitudes.map { ((it + 80f) / 80f).coerceIn(0f, 1f) }.average().toFloat()
        } else 0f
        val centroid = audioFeatures?.spectralCentroid ?: 0.5f
        val bassEnergy = be?.bass ?: if (bandCount > 0) {
            val third = bandCount / 3
            var s = 0f; for (i in 0 until third) s += ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f); s / third
        } else 0f

        val isBeatEvent = onsets?.bass == true || onsets?.subBass == true || (onsets == null && isBeat)
        val isOnset = onsets?.let { it.mids || it.highs } ?: false

        // Update time — energy drives speed, beat causes discontinuity (D-11.4)
        val effectiveSpeed = config.fieldSpeed * (1f + energy * 2f)
        if (isBeatEvent) {
            state.timeOffset += config.beatTimeJump.toDouble()
        } else {
            state.timeOffset += effectiveSpeed.toDouble()
        }

        // Centroid drives turbulence — maps to noise scale range (D-11.4)
        val effectiveNoiseScale = config.turbulenceMin + centroid * (config.turbulenceMax - config.turbulenceMin)

        // Update burst timer for onset color effect
        if (isOnset) state.burstTimer = 1f
        if (state.burstTimer > 0f) state.burstTimer -= 0.067f // ~15 frames decay

        // Respawn dead particles
        val respawnRate = (5 + (energy * 15).toInt()).coerceAtMost(30)
        for (i in 0 until respawnRate) {
            val idx = Random.nextInt(pool.count)
            val p = pool.particles[idx]
            if (!p.alive || p.life <= 0f) {
                pool.spawn(w, h)
            }
        }

        // Update particles
        val hueRange = theme.hueEnd - theme.hueStart
        val baseSizeMin = 1.0f
        val baseSizeScale = 3.0f

        for (p in pool.particles) {
            if (!p.alive) continue

            // Save previous position for trail line (D-11.5)
            p.prevX = p.x
            p.prevY = p.y

            // Sample noise at particle position — direct per-particle sampling (D-11.3)
            val noiseVal = OpenSimplex2.noise3_ImproveXY(
                SEED,
                (p.x * effectiveNoiseScale).toDouble(),
                (p.y * effectiveNoiseScale).toDouble(),
                state.timeOffset
            )
            val angle = noiseVal * TWO_PI

            // Update velocity with noise direction + drag
            p.vx = (p.vx + cos(angle) * config.particleSpeed) * config.particleDrag
            p.vy = (p.vy + sin(angle) * config.particleSpeed) * config.particleDrag

            // Update position
            p.x += p.vx
            p.y += p.vy

            // Bass drives particle size (D-11.4)
            p.size = baseSizeMin + bassEnergy * baseSizeScale

            // Velocity-angle hue mapping (D-11.7)
            val velAngle = atan2(p.vy, p.vx)
            val hueNorm = (velAngle / TWO_PI + 0.5f)
            p.hue = theme.hueStart + hueNorm * hueRange + centroid * 30f

            // Decay life
            p.life -= 0.003f

            // Wrap around edges instead of dying (keeps density)
            if (p.x < -10f) { p.x += w + 20f; p.prevX = p.x }
            if (p.x > w + 10f) { p.x -= w + 20f; p.prevX = p.x }
            if (p.y < -10f) { p.y += h + 20f; p.prevY = p.y }
            if (p.y > h + 10f) { p.y -= h + 20f; p.prevY = p.y }

            // Respawn if life depleted
            if (p.life <= 0f) {
                pool.spawn(w, h)
            }
        }

        // Render — two passes: glow then main (D-11.5)
        val burstBoost = state.burstTimer.coerceIn(0f, 1f)

        for (p in pool.particles) {
            if (!p.alive) continue

            val alpha = (p.life * 0.8f).coerceIn(0f, 1f)
            val saturation = (theme.saturation + burstBoost * 0.2f).coerceIn(0f, 1f)
            val lightness = (theme.lightnessMin + alpha * theme.lightnessScale + burstBoost * 0.15f).coerceIn(0f, 1f)
            val color = Color.hsl(p.hue.mod(360f), saturation, lightness)

            val from = Offset(p.prevX, p.prevY)
            val to = Offset(p.x, p.y)

            // Glow pass
            drawLine(
                color = color.copy(alpha = alpha * theme.glowAlpha * 0.5f),
                start = from,
                end = to,
                strokeWidth = p.size * 3f,
                cap = StrokeCap.Round,
                blendMode = BlendMode.Screen
            )

            // Main trail line
            drawLine(
                color = color.copy(alpha = alpha),
                start = from,
                end = to,
                strokeWidth = p.size,
                cap = StrokeCap.Round
            )
        }
    }
}
