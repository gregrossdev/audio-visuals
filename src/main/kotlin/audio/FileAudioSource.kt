package audio

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.*

class FileAudioSource(
    private val sampleSize: Int = 1024
) : AudioSource {

    private val targetFormat = AudioFormat(
        AudioFormat.Encoding.PCM_SIGNED,
        44100f,
        16,
        1,
        2,
        44100f,
        false
    )

    private val _samples = MutableStateFlow(FloatArray(sampleSize))
    override val samples: StateFlow<FloatArray> = _samples.asStateFlow()

    override var gain: Float = 1.0f

    @Volatile
    private var paused: Boolean = false
    private var playbackJob: Job? = null
    private var outputLine: SourceDataLine? = null
    private var audioStream: AudioInputStream? = null

    fun start(scope: CoroutineScope, file: File) {
        stop()

        val fileStream = AudioSystem.getAudioInputStream(file)
        val sourceFormat = fileStream.format

        // Step 1: Decode compressed formats (MP3/FLAC) to PCM at source sample rate/channels
        val decodedStream = if (sourceFormat.encoding != AudioFormat.Encoding.PCM_SIGNED) {
            val decodedFormat = AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                sourceFormat.sampleRate,
                16,
                sourceFormat.channels,
                sourceFormat.channels * 2,
                sourceFormat.sampleRate,
                false
            )
            AudioSystem.getAudioInputStream(decodedFormat, fileStream)
        } else {
            fileStream
        }

        // Step 2: Convert sample rate/channels to our target format if needed
        val decodedFmt = decodedStream.format
        val pcmStream = if (decodedFmt.sampleRate != targetFormat.sampleRate ||
            decodedFmt.channels != targetFormat.channels
        ) {
            AudioSystem.getAudioInputStream(targetFormat, decodedStream)
        } else {
            decodedStream
        }
        audioStream = pcmStream

        val lineInfo = DataLine.Info(SourceDataLine::class.java, targetFormat)
        val line = AudioSystem.getLine(lineInfo) as SourceDataLine
        val bufferBytes = sampleSize * 2
        line.open(targetFormat, bufferBytes * 4)
        line.start()
        outputLine = line

        paused = false
        playbackJob = scope.launch(Dispatchers.IO) {
            val byteBuffer = ByteArray(bufferBytes)
            try {
                while (isActive) {
                    while (paused && isActive) {
                        delay(50)
                    }
                    if (!isActive) break
                    val bytesRead = pcmStream.read(byteBuffer, 0, bufferBytes)
                    if (bytesRead <= 0) break
                    _samples.value = bytesToFloats(byteBuffer, bytesRead)
                    line.write(byteBuffer, 0, bytesRead)
                }
            } finally {
                withContext(NonCancellable) {
                    line.drain()
                    line.stop()
                    line.close()
                    pcmStream.close()
                }
            }
        }
    }

    override fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        outputLine = null
        audioStream = null
    }

    override fun pause() {
        paused = true
        outputLine?.stop()
    }

    override fun resume() {
        outputLine?.start()
        paused = false
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
}
