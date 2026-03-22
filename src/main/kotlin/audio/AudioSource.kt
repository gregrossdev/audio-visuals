package audio

import kotlinx.coroutines.flow.StateFlow

interface AudioSource {
    val samples: StateFlow<FloatArray>
    var gain: Float
    fun pause()
    fun resume()
    fun stop()
}
