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
) {
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
    val samples: StateFlow<FloatArray> = _samples.asStateFlow()

    private var line: TargetDataLine? = null
    private var captureJob: Job? = null

    fun start(scope: CoroutineScope) {
        val dataLine = AudioSystem.getLine(DataLine.Info(TargetDataLine::class.java, format)) as TargetDataLine
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

    fun stop() {
        captureJob?.cancel()
        captureJob = null
        line?.stop()
        line?.close()
        line = null
    }

    private fun bytesToFloats(bytes: ByteArray, length: Int): FloatArray {
        val shortBuffer = ByteBuffer.wrap(bytes, 0, length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .asShortBuffer()
        val sampleCount = length / 2
        val floats = FloatArray(sampleCount)
        for (i in 0 until sampleCount) {
            floats[i] = shortBuffer.get(i) / 32768f
        }
        return floats
    }
}
