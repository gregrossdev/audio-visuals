package audio

import kotlin.math.max
import kotlin.math.sqrt

class EnergyEnvelope(
    private val attackCoeff: Float = 0.15f,
    private val releaseCoeff: Float = 0.05f
) {
    private var smoothed = 0f
    private var rollingMax = 0.0001f

    fun update(linearMagnitudes: FloatArray): Float {
        // RMS of full spectrum
        var sumSq = 0f
        for (mag in linearMagnitudes) {
            sumSq += mag * mag
        }
        val rms = sqrt(sumSq / linearMagnitudes.size)

        // Auto-gain via rolling max
        if (rms > rollingMax) {
            rollingMax = rms
        } else {
            rollingMax *= 0.999f
            if (rollingMax < 0.0001f) rollingMax = 0.0001f
        }

        val normalized = (rms / rollingMax).coerceIn(0f, 1f)

        // Attack/release smoothing
        val coeff = if (normalized > smoothed) attackCoeff else releaseCoeff
        smoothed += coeff * (normalized - smoothed)

        return smoothed.coerceIn(0f, 1f)
    }
}
