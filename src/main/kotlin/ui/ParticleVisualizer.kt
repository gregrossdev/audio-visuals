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
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import audio.AudioFeatures
import kotlin.random.Random

private class Particle {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var life = 0f
    var hue = 0f
    var size = 3f
    var alive = false
}

private class ParticlePool(val maxParticles: Int = 800) {
    val particles = Array(maxParticles) { Particle() }
    private var nextIndex = 0

    fun spawn(
        cx: Float, cy: Float,
        speed: Float, angle: Float,
        hue: Float, size: Float
    ) {
        val p = particles[nextIndex]
        p.x = cx
        p.y = cy
        p.vx = cos(angle) * speed
        p.vy = sin(angle) * speed
        p.life = 1f
        p.hue = hue
        p.size = size
        p.alive = true
        nextIndex = (nextIndex + 1) % maxParticles
    }
}

@Composable
fun ParticleVisualizer(
    magnitudes: FloatArray,
    theme: ColorTheme,
    isBeat: Boolean,
    config: ParticleConfig = ParticleConfig(),
    audioFeatures: AudioFeatures? = null,
    modifier: Modifier = Modifier
) {
    val pool = remember(config.maxParticles) { ParticlePool(config.maxParticles) }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val minDim = min(w, h)
        val sizeScale = minDim * 0.003f   // proportional particle size unit
        val speedScale = minDim * 0.003f   // proportional speed unit
        val bandCount = magnitudes.size
        if (bandCount == 0) return@Canvas

        // Reactive background
        val energy = magnitudes.map { ((it + 80f) / 80f).coerceIn(0f, 1f) }.average().toFloat()
        val bgAlpha = (energy * 0.25f).coerceIn(0f, 0.25f)
        if (bgAlpha > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.hsl(theme.backgroundHue, 0.6f, 0.12f).copy(alpha = bgAlpha),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.5f
                ),
                radius = w * 0.5f,
                center = Offset(cx, cy)
            )
        }

        // Use AudioFeatures for band energies and onsets, fallback to manual calc
        val be = audioFeatures?.bandEnergies
        val onsets = audioFeatures?.onsets
        val globalEnergy = audioFeatures?.energy ?: energy
        val energyScale = 0.5f + globalEnergy * 1.5f

        val bassEnergy = be?.bass ?: run {
            val third = bandCount / 3
            var s = 0f; for (i in 0 until third) s += ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f); s / third
        }
        val midEnergy = be?.mids ?: run {
            val third = bandCount / 3
            var s = 0f; for (i in third until third * 2) s += ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f); s / third
        }
        val highEnergy = be?.highs ?: run {
            val third = bandCount / 3
            var s = 0f; for (i in third * 2 until bandCount) s += ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f); s / (bandCount - third * 2).coerceAtLeast(1)
        }

        // Spawn particles — use per-band onsets for burst spawning, energy for continuous
        val bassSpawn = (bassEnergy * 4).toInt().coerceAtMost(config.bassSpawnRate)
        val midSpawn = (midEnergy * 3).toInt().coerceAtMost(config.midSpawnRate)
        val highSpawn = (highEnergy * 3).toInt().coerceAtMost(config.highSpawnRate)

        // Bass: large, slow, low hue
        for (k in 0 until bassSpawn) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            pool.spawn(cx, cy, speed = (config.bassSpeedMin + bassEnergy * config.bassSpeedScale) * speedScale, angle = angle,
                hue = theme.hueStart, size = (config.bassSizeMin + bassEnergy * config.bassSizeScale) * sizeScale * energyScale)
        }
        // Sub-bass onset burst — big particles
        if (onsets?.subBass == true) {
            for (k in 0 until config.beatBurstCount / 2) {
                val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
                pool.spawn(cx, cy, speed = (2f + Random.nextFloat() * 3f) * speedScale, angle = angle,
                    hue = theme.hueStart, size = (6f + Random.nextFloat() * 4f) * sizeScale * energyScale)
            }
        }
        // Mids: medium
        for (k in 0 until midSpawn) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val midHue = theme.hueStart + (theme.hueEnd - theme.hueStart) * 0.5f
            pool.spawn(cx, cy, speed = (config.midSpeedMin + midEnergy * config.midSpeedScale) * speedScale, angle = angle,
                hue = midHue, size = (config.midSizeMin + midEnergy * config.midSizeScale) * sizeScale * energyScale)
        }
        // Mids onset burst
        if (onsets?.mids == true) {
            for (k in 0 until config.beatBurstCount / 3) {
                val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
                val midHue = theme.hueStart + (theme.hueEnd - theme.hueStart) * 0.5f
                pool.spawn(cx, cy, speed = (2f + Random.nextFloat() * 4f) * speedScale, angle = angle,
                    hue = midHue, size = (3f + Random.nextFloat() * 3f) * sizeScale * energyScale)
            }
        }
        // Highs: small, fast, high hue
        for (k in 0 until highSpawn) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            pool.spawn(cx, cy, speed = (config.highSpeedMin + highEnergy * config.highSpeedScale) * speedScale, angle = angle,
                hue = theme.hueEnd, size = (config.highSizeMin + highEnergy * config.highSizeScale) * sizeScale * energyScale)
        }
        // Highs onset — sparkles
        if (onsets?.highs == true) {
            for (k in 0 until config.beatBurstCount / 4) {
                val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
                pool.spawn(cx, cy, speed = (4f + Random.nextFloat() * 6f) * speedScale, angle = angle,
                    hue = theme.hueEnd, size = (1.5f + Random.nextFloat() * 2f) * sizeScale)
            }
        }

        // Bass onset burst (replaces old isBeat burst)
        if (onsets?.bass == true || (onsets == null && isBeat)) {
            for (k in 0 until config.beatBurstCount) {
                val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
                val burstHue = theme.hueStart + Random.nextFloat() * (theme.hueEnd - theme.hueStart)
                pool.spawn(cx, cy, speed = (3f + Random.nextFloat() * 5f) * speedScale, angle = angle,
                    hue = burstHue, size = (4f + Random.nextFloat() * 5f) * sizeScale * energyScale)
            }
        }

        // Update and draw particles
        for (p in pool.particles) {
            if (!p.alive) continue

            // Update position
            p.x += p.vx
            p.y += p.vy

            // Slight drag
            p.vx *= config.drag
            p.vy *= config.drag

            // Decay life
            p.life -= config.lifeDecay
            if (p.life <= 0f) {
                p.alive = false
                continue
            }

            // Off-screen check
            val marginX = w * 0.05f
            val marginY = h * 0.05f
            if (p.x < -marginX || p.x > w + marginX || p.y < -marginY || p.y > h + marginY) {
                p.alive = false
                continue
            }

            val alpha = p.life.coerceIn(0f, 1f)
            val lightness = theme.lightnessMin + alpha * theme.lightnessScale
            val color = Color.hsl(p.hue.mod(360f), theme.saturation, lightness.coerceIn(0f, 1f))

            // Glow pass
            drawCircle(
                color = color.copy(alpha = alpha * theme.glowAlpha),
                radius = p.size * 2.5f,
                center = Offset(p.x, p.y),
                blendMode = BlendMode.Screen
            )

            // Main particle
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = p.size,
                center = Offset(p.x, p.y)
            )
        }
    }
}
