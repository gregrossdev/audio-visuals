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

    private val _linearMagnitudes = MutableStateFlow(FloatArray(magnitudeBins))
    val linearMagnitudes: StateFlow<FloatArray> = _linearMagnitudes.asStateFlow()

    val logBinMapper = LogBinMapper(fftSize)
    private val _logMagnitudes = MutableStateFlow(FloatArray(logBinMapper.bandCount))
    val logMagnitudes: StateFlow<FloatArray> = _logMagnitudes.asStateFlow()

    private var processJob: Job? = null

    fun start(scope: CoroutineScope, source: AudioSource) {
        processJob = scope.launch(Dispatchers.Default) {
            source.samples.collect { samples ->
                if (samples.size >= fftSize) {
                    val (dbMags, linMags) = processFFT(samples)
                    _magnitudes.value = dbMags
                    _linearMagnitudes.value = linMags
                    _logMagnitudes.value = logBinMapper.map(dbMags)
                }
            }
        }
    }

    fun stop() {
        processJob?.cancel()
        processJob = null
    }

    private fun processFFT(samples: FloatArray): Pair<FloatArray, FloatArray> {
        // Apply Hann window and convert to double for JTransforms
        val windowed = DoubleArray(fftSize) { i ->
            (samples[i] * hannWindow[i]).toDouble()
        }

        // In-place real FFT
        fft.realForward(windowed)

        // Extract magnitudes
        val dbResult = FloatArray(magnitudeBins)
        val linearResult = FloatArray(magnitudeBins)
        for (i in 0 until magnitudeBins) {
            val real = windowed[2 * i]
            val imag = windowed[2 * i + 1]
            val magnitude = sqrt(real * real + imag * imag).toFloat()

            linearResult[i] = magnitude

            // Convert to dB scale, clamp floor at -80 dB
            val db = if (magnitude > 0f) 20.0 * log10(magnitude.toDouble()) else -80.0
            dbResult[i] = db.coerceIn(-80.0, 0.0).toFloat()
        }
        return Pair(dbResult, linearResult)
    }
}
