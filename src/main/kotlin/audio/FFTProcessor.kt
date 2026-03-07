package audio

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class FFTProcessor(
    private val fftSize: Int = 1024
) {
    private val fft = DoubleFFT_1D(fftSize)
    private val hannWindow = FloatArray(fftSize) { i ->
        (0.5 * (1 - kotlin.math.cos(2.0 * Math.PI * i / (fftSize - 1)))).toFloat()
    }
    private val magnitudeBins = fftSize / 2

    private val _magnitudes = MutableStateFlow(FloatArray(magnitudeBins))
    val magnitudes: StateFlow<FloatArray> = _magnitudes.asStateFlow()

    private var processJob: Job? = null

    fun start(scope: CoroutineScope, audioCapture: AudioCapture) {
        processJob = scope.launch(Dispatchers.Default) {
            audioCapture.samples.collect { samples ->
                if (samples.size >= fftSize) {
                    _magnitudes.value = processFFT(samples)
                }
            }
        }
    }

    fun stop() {
        processJob?.cancel()
        processJob = null
    }

    private fun processFFT(samples: FloatArray): FloatArray {
        // Apply Hann window and convert to double for JTransforms
        val windowed = DoubleArray(fftSize) { i ->
            (samples[i] * hannWindow[i]).toDouble()
        }

        // In-place real FFT
        fft.realForward(windowed)

        // Extract magnitudes and convert to dB
        val result = FloatArray(magnitudeBins)
        for (i in 0 until magnitudeBins) {
            val real = windowed[2 * i]
            val imag = windowed[2 * i + 1]
            val magnitude = sqrt(real * real + imag * imag)

            // Convert to dB scale, clamp floor at -80 dB
            val db = if (magnitude > 0.0) 20.0 * log10(magnitude) else -80.0
            result[i] = db.coerceIn(-80.0, 0.0).toFloat()
        }
        return result
    }
}
