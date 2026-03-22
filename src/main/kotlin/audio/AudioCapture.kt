package audio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.*

class AudioCapture(
    private val sampleRate: Float = 44100f,
    private val sampleSize: Int = 1024
) : AudioSource {
    private val format = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        sampleRate,
        16,
        1,
        2,
        sampleRate,
        false // little-endian
    )

    private val _samples = MutableStateFlow(FloatArray(sampleSize))
    override val samples: StateFlow<FloatArray> = _samples.asStateFlow()

    override var gain: Float = 1.0f

    private var line: TargetDataLine? = null
    private var captureJob: Job? = null

    fun start(scope: CoroutineScope, mixerInfo: Mixer.Info? = null) {
        val info = DataLine.Info(TargetDataLine::class.java, format)
        val dataLine = if (mixerInfo != null) {
            AudioSystem.getMixer(mixerInfo).getLine(info) as TargetDataLine
        } else {
            AudioSystem.getLine(info) as TargetDataLine
        }
        val bufferBytes = sampleSize * 2 // 16-bit = 2 bytes per sample
        dataLine.open(format, bufferBytes * 2)
        dataLine.start()
        line = dataLine

        captureJob = scope.launch(Dispatchers.IO) {
            val byteBuffer = ByteArray(bufferBytes)
            while (isActive) {
                val bytesRead = dataLine.read(byteBuffer, 0, bufferBytes)
                if (bytesRead > 0) {
                    _samples.value = bytesToFloats(byteBuffer, bytesRead)
                }
            }
        }
    }

    override fun stop() {
        captureJob?.cancel()
        captureJob = null
        line?.stop()
        line?.close()
        line = null
    }

    override fun pause() {
        line?.stop()
    }

    override fun resume() {
        line?.start()
    }

    private fun bytesToFloats(bytes: ByteArray, length: Int): FloatArray {
        val shortBuffer = ByteBuffer.wrap(bytes, 0, length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        val sampleCount = length / 2
        val currentGain = gain
        val floats = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            floats[i] = (shortBuffer.get(i) / 32768f * currentGain).coerceIn(-1f, 1f)
        }
        return floats
    }

    companion object {
        fun availableDevices(): List<Mixer.Info> {
            val targetInfo = DataLine.Info(TargetDataLine::class.java, null)
            return AudioSystem.getMixerInfo().filter { mixerInfo ->
                try {
                    val mixer = AudioSystem.getMixer(mixerInfo)
                    mixer.targetLineInfo.any { it is DataLine.Info && it.lineClass == TargetDataLine::class.java }
                } catch (_: Exception) {
                    false
                }
            }
        }
    }
}
