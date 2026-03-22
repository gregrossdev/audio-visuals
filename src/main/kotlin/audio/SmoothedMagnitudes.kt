package audio

class SmoothedMagnitudes(
    private val decayRate: Float = 2.0f
) {
    private var previous: FloatArray? = null

    fun smooth(current: FloatArray, decayMultiplier: Float = 1.0f): FloatArray {
        val prev = previous
        if (prev == null || prev.size != current.size) {
            previous = current.copyOf()
            return current
        }
        val result = FloatArray(current.size) { i ->
            maxOf(current[i], prev[i] - decayRate * decayMultiplier)
        }
        previous = result
        return result
    }
}
