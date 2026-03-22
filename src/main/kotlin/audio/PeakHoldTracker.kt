package audio

class PeakHoldTracker(
    private val holdFrames: Int = 30,
    private val decayRate: Float = 1.0f
) {
    private var peaks: FloatArray? = null
    private var holdCounters: IntArray? = null

    fun update(current: FloatArray): FloatArray {
        val p = peaks
        val h = holdCounters
        if (p == null || h == null || p.size != current.size) {
            peaks = current.copyOf()
            holdCounters = IntArray(current.size) { holdFrames }
            return current
        }

        val result = FloatArray(current.size)
        for (i in current.indices) {
            if (current[i] >= p[i]) {
                result[i] = current[i]
                h[i] = holdFrames
            } else if (h[i] > 0) {
                result[i] = p[i]
                h[i]--
            } else {
                result[i] = (p[i] - decayRate).coerceAtLeast(-80f)
            }
        }
        peaks = result
        return result
    }
}
