package audio

class SpectralCentroidTracker(
    sampleRate: Int = 44100,
    fftSize: Int = 1024,
    private val smoothingAlpha: Float = 0.3f
) {
    private val nyquist = sampleRate / 2
    private val binWidth = nyquist.toFloat() / (fftSize / 2)
    private var smoothedValue = 0f
    private var hasValue = false

    fun update(linearMagnitudes: FloatArray): Float {
        var weightedSum = 0f
        var magnitudeSum = 0f

        for (i in linearMagnitudes.indices) {
            val binFrequency = i * binWidth
            weightedSum += binFrequency * linearMagnitudes[i]
            magnitudeSum += linearMagnitudes[i]
        }

        if (magnitudeSum < 0.0001f) {
            return smoothedValue
        }

        val centroid = (weightedSum / magnitudeSum) / nyquist

        smoothedValue = if (hasValue) {
            smoothingAlpha * centroid + (1f - smoothingAlpha) * smoothedValue
        } else {
            hasValue = true
            centroid
        }

        return smoothedValue
    }
}
