package audio

import kotlinx.serialization.Serializable

@Serializable
data class ReactivityConfig(
    val bassSensitivity: Float = 1.0f,
    val midSensitivity: Float = 1.0f,
    val highSensitivity: Float = 1.0f,
    val beatThreshold: Float = 1.0f,
    val smoothingFactor: Float = 1.0f,
    val centroidInfluence: Float = 0.5f,
    val onsetSensitivity: Float = 1.0f,
    val energySmoothing: Float = 0.15f
) {
    fun applyBandSensitivity(magnitudes: FloatArray, bandCount: Int): FloatArray {
        if (bassSensitivity == 1.0f && midSensitivity == 1.0f && highSensitivity == 1.0f) {
            return magnitudes
        }
        val third = bandCount / 3
        val result = magnitudes.copyOf()
        for (i in result.indices) {
            val multiplier = when {
                i < third -> bassSensitivity
                i < third * 2 -> midSensitivity
                else -> highSensitivity
            }
            // Scale around the -80dB floor: shift to 0-based, multiply, shift back
            val normalized = ((result[i] + 80f) / 80f).coerceIn(0f, 1f)
            val scaled = (normalized * multiplier).coerceIn(0f, 1f)
            result[i] = scaled * 80f - 80f
        }
        return result
    }
}
