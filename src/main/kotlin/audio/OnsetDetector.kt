package audio

import kotlin.math.max

data class OnsetEvents(
    val subBass: Boolean,
    val bass: Boolean,
    val mids: Boolean,
    val highs: Boolean
)

class OnsetDetector(
    sampleRate: Int = 44100,
    fftSize: Int = 1024
) {
    private val binResolution = sampleRate.toFloat() / fftSize

    private val bandRanges = arrayOf(
        hzToBin(20f)..hzToBin(80f),
        hzToBin(80f)..hzToBin(300f),
        hzToBin(300f)..hzToBin(2000f),
        hzToBin(2000f)..hzToBin(20000f)
    )

    private val historySize = 30
    private val fluxHistory = Array(4) { FloatArray(historySize) }
    private val historyIndex = IntArray(4)
    private val historyCount = IntArray(4)

    private val cooldownFrames = 6
    private val cooldowns = IntArray(4)

    private var previousMagnitudes: FloatArray? = null

    private fun hzToBin(frequency: Float): Int = (frequency / binResolution).toInt()

    fun update(linearMagnitudes: FloatArray, sensitivityMultiplier: Float = 1.0f): OnsetEvents {
        val prev = previousMagnitudes
        previousMagnitudes = linearMagnitudes.copyOf()

        if (prev == null) {
            return OnsetEvents(subBass = false, bass = false, mids = false, highs = false)
        }

        val onsets = BooleanArray(4)

        for (band in 0 until 4) {
            val range = bandRanges[band]
            val clampedRange = range.first.coerceAtLeast(0)..range.last.coerceAtMost(linearMagnitudes.size - 1)

            var flux = 0f
            for (i in clampedRange) {
                flux += max(0f, linearMagnitudes[i] - prev[i])
            }

            fluxHistory[band][historyIndex[band]] = flux
            historyIndex[band] = (historyIndex[band] + 1) % historySize
            if (historyCount[band] < historySize) historyCount[band]++

            var sum = 0f
            for (i in 0 until historyCount[band]) {
                sum += fluxHistory[band][i]
            }
            val rollingAverage = sum / historyCount[band]

            if (cooldowns[band] > 0) {
                cooldowns[band]--
            }

            if (cooldowns[band] == 0 &&
                flux > rollingAverage * 2.0f * sensitivityMultiplier &&
                flux > 0.001f
            ) {
                onsets[band] = true
                cooldowns[band] = cooldownFrames
            }
        }

        return OnsetEvents(
            subBass = onsets[0],
            bass = onsets[1],
            mids = onsets[2],
            highs = onsets[3]
        )
    }
}
