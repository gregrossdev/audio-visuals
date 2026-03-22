package audio

import kotlin.math.max
import kotlin.math.sqrt

data class BandEnergies(
    val subBass: Float,
    val bass: Float,
    val mids: Float,
    val highs: Float
)

class BandEnergyAnalyzer(
    sampleRate: Int = 44100,
    fftSize: Int = 1024
) {
    private val binResolution = sampleRate.toFloat() / fftSize

    private val subBassRange = hzToBin(20f)..hzToBin(80f)
    private val bassRange = hzToBin(80f)..hzToBin(300f)
    private val midsRange = hzToBin(300f)..hzToBin(2000f)
    private val highsRange = hzToBin(2000f)..hzToBin(20000f)

    private val rollingMax = FloatArray(4) { 0f }
    private val decayFactor = 0.999f

    private fun hzToBin(frequency: Float): Int = (frequency / binResolution).toInt()

    fun update(linearMagnitudes: FloatArray): BandEnergies {
        val rawEnergies = floatArrayOf(
            rms(linearMagnitudes, subBassRange),
            rms(linearMagnitudes, bassRange),
            rms(linearMagnitudes, midsRange),
            rms(linearMagnitudes, highsRange)
        )

        for (i in rollingMax.indices) {
            rollingMax[i] = max(rollingMax[i] * decayFactor, rawEnergies[i])
        }

        val normalized = FloatArray(4) { i ->
            if (rollingMax[i] < 1e-8f) 0f
            else (rawEnergies[i] / rollingMax[i]).coerceIn(0f, 1f)
        }

        return BandEnergies(
            subBass = normalized[0],
            bass = normalized[1],
            mids = normalized[2],
            highs = normalized[3]
        )
    }

    private fun rms(magnitudes: FloatArray, range: IntRange): Float {
        val clampedRange = range.first.coerceAtLeast(0)..range.last.coerceAtMost(magnitudes.size - 1)
        val count = clampedRange.last - clampedRange.first + 1
        if (count <= 0) return 0f

        var sum = 0f
        for (i in clampedRange) {
            sum += magnitudes[i] * magnitudes[i]
        }
        return sqrt(sum / count)
    }
}
