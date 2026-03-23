package ui

import audio.AudioFeatures
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

data class ColorTheme(
    val hueStart: Float,
    val hueEnd: Float,
    val saturation: Float,
    val peakSaturation: Float,
    val lightnessMin: Float,
    val lightnessScale: Float,
    val peakLightness: Float,
    val glowAlpha: Float,
    val backgroundHue: Float
) {
    fun barColor(index: Int, count: Int, normalized: Float): Color {
        val hue = hueStart + (index.toFloat() / count) * (hueEnd - hueStart)
        val lightness = lightnessMin + normalized * lightnessScale
        return Color.hsl(hue.mod(360f), saturation, lightness.coerceIn(0f, 1f))
    }

    fun tipColor(index: Int, count: Int, normalized: Float): Color {
        val hue = hueStart + (index.toFloat() / count) * (hueEnd - hueStart)
        val lightness = lightnessMin + normalized * lightnessScale + 0.2f
        return Color.hsl(hue.mod(360f), peakSaturation, lightness.coerceAtMost(0.9f))
    }

    fun peakColor(index: Int, count: Int): Color {
        val hue = hueStart + (index.toFloat() / count) * (hueEnd - hueStart)
        return Color.hsl(hue.mod(360f), peakSaturation, peakLightness)
    }
}

object DynamicColorTheme {
    private var smoothedCentroid = 0.5f
    private var smoothedEnergy = 0f
    private val smoothFactor = 0.08f

    fun compute(audioFeatures: AudioFeatures, isBeat: Boolean): ColorTheme {
        smoothedCentroid += (audioFeatures.spectralCentroid - smoothedCentroid) * smoothFactor
        smoothedEnergy += (audioFeatures.energy - smoothedEnergy) * smoothFactor * 2

        // Map centroid to hue: low=warm(0-60), mid=green/cyan(90-200), high=blue/purple(220-300)
        val baseHue = smoothedCentroid * 300f
        val hueSpread = 60f + smoothedEnergy * 30f

        // Energy modulates saturation and lightness
        val sat = 0.7f + smoothedEnergy * 0.3f
        val lightnessScale = 0.3f + smoothedEnergy * 0.2f
        val beatBoost = if (isBeat) 0.15f else 0f

        return ColorTheme(
            hueStart = baseHue,
            hueEnd = baseHue + hueSpread,
            saturation = sat.coerceAtMost(1f),
            peakSaturation = (sat + 0.1f).coerceAtMost(1f),
            lightnessMin = 0.25f + beatBoost,
            lightnessScale = lightnessScale + beatBoost,
            peakLightness = (0.75f + beatBoost).coerceAtMost(0.95f),
            glowAlpha = 0.2f + smoothedEnergy * 0.1f,
            backgroundHue = baseHue
        )
    }
}

@Serializable
enum class ThemePreset(val label: String, val theme: ColorTheme) {
    SPECTRUM("Spectrum", ColorTheme(
        hueStart = 0f, hueEnd = 270f,
        saturation = 0.85f, peakSaturation = 0.95f,
        lightnessMin = 0.3f, lightnessScale = 0.4f,
        peakLightness = 0.8f, glowAlpha = 0.2f,
        backgroundHue = 260f
    )),
    NEON("Neon", ColorTheme(
        hueStart = 180f, hueEnd = 320f,
        saturation = 0.95f, peakSaturation = 1.0f,
        lightnessMin = 0.4f, lightnessScale = 0.35f,
        peakLightness = 0.85f, glowAlpha = 0.25f,
        backgroundHue = 250f
    )),
    MONOCHROME("Mono", ColorTheme(
        hueStart = 220f, hueEnd = 220f,
        saturation = 0.6f, peakSaturation = 0.7f,
        lightnessMin = 0.2f, lightnessScale = 0.5f,
        peakLightness = 0.75f, glowAlpha = 0.2f,
        backgroundHue = 220f
    )),
    WARM("Warm", ColorTheme(
        hueStart = 0f, hueEnd = 60f,
        saturation = 0.9f, peakSaturation = 0.95f,
        lightnessMin = 0.3f, lightnessScale = 0.4f,
        peakLightness = 0.8f, glowAlpha = 0.2f,
        backgroundHue = 20f
    )),
    OCEAN("Ocean", ColorTheme(
        hueStart = 180f, hueEnd = 240f,
        saturation = 0.8f, peakSaturation = 0.9f,
        lightnessMin = 0.25f, lightnessScale = 0.45f,
        peakLightness = 0.8f, glowAlpha = 0.2f,
        backgroundHue = 210f
    )),
    FIRE("Fire", ColorTheme(
        hueStart = 0f, hueEnd = 40f,
        saturation = 0.95f, peakSaturation = 1.0f,
        lightnessMin = 0.3f, lightnessScale = 0.4f,
        peakLightness = 0.85f, glowAlpha = 0.25f,
        backgroundHue = 10f
    )),
    REACTIVE("Reactive", ColorTheme(
        hueStart = 0f, hueEnd = 270f,
        saturation = 0.85f, peakSaturation = 0.95f,
        lightnessMin = 0.3f, lightnessScale = 0.4f,
        peakLightness = 0.8f, glowAlpha = 0.2f,
        backgroundHue = 260f
    ))
}
