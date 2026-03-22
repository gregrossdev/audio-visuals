package audio

import kotlin.math.log2
import kotlin.math.roundToInt

class LogBinMapper(
    private val fftSize: Int = 1024,
    private val sampleRate: Float = 44100f,
    private val minFreq: Float = 20f,
    private val maxFreq: Float = 20000f,
    private val bandsPerOctave: Int = 6
) {
    private val nyquist = sampleRate / 2f
    private val binWidth = nyquist / (fftSize / 2)

    val bandCount: Int
    val bandFrequencies: FloatArray
    private val bandBinRanges: Array<IntRange>

    init {
        val octaves = log2(maxFreq / minFreq)
        bandCount = (octaves * bandsPerOctave).roundToInt()
        bandFrequencies = FloatArray(bandCount)
        bandBinRanges = Array(bandCount) { IntRange.EMPTY }

        for (b in 0 until bandCount) {
            val freqLow = minFreq * Math.pow(2.0, b.toDouble() / bandsPerOctave).toFloat()
            val freqHigh = minFreq * Math.pow(2.0, (b + 1).toDouble() / bandsPerOctave).toFloat()
            bandFrequencies[b] = (freqLow + freqHigh) / 2f

            val binLow = (freqLow / binWidth).roundToInt().coerceIn(0, fftSize / 2 - 1)
            val binHigh = (freqHigh / binWidth).roundToInt().coerceIn(binLow, fftSize / 2 - 1)
            bandBinRanges[b] = binLow..binHigh
        }
    }

    fun map(linearMagnitudes: FloatArray): FloatArray {
        return FloatArray(bandCount) { b ->
            val range = bandBinRanges[b]
            if (range.isEmpty()) {
                -80f
            } else {
                var sum = 0f
                var count = 0
                for (i in range) {
                    if (i < linearMagnitudes.size) {
                        sum += linearMagnitudes[i]
                        count++
                    }
                }
                if (count > 0) sum / count else -80f
            }
        }
    }
}
