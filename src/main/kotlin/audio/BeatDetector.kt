package audio

class BeatDetector(
    private val bassBands: Int = 4,
    private val threshold: Float = 1.5f,
    private val cooldownFrames: Int = 8,
    private val historySize: Int = 30
) {
    private val history = FloatArray(historySize)
    private var historyIndex = 0
    private var historyCount = 0
    private var cooldown = 0

    fun update(magnitudes: FloatArray, thresholdMultiplier: Float = 1.0f): Boolean {
        if (magnitudes.isEmpty()) return false

        // Compute bass energy from first N bands
        val bands = minOf(bassBands, magnitudes.size)
        var bassEnergy = 0f
        for (i in 0 until bands) {
            bassEnergy += ((magnitudes[i] + 80f) / 80f).coerceIn(0f, 1f)
        }
        bassEnergy /= bands

        // Update rolling history
        history[historyIndex] = bassEnergy
        historyIndex = (historyIndex + 1) % historySize
        if (historyCount < historySize) historyCount++

        // Compute rolling average
        var sum = 0f
        for (i in 0 until historyCount) sum += history[i]
        val average = sum / historyCount

        // Cooldown management
        if (cooldown > 0) {
            cooldown--
            return false
        }

        // Beat detected if current energy exceeds average by threshold
        val effectiveThreshold = threshold * thresholdMultiplier
        val isBeat = bassEnergy > average * effectiveThreshold && bassEnergy > 0.15f
        if (isBeat) cooldown = cooldownFrames
        return isBeat
    }
}
