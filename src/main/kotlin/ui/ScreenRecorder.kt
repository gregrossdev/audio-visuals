package ui

import kotlinx.coroutines.*
import java.awt.Rectangle
import java.awt.Robot
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ScreenRecorder {
    private var process: Process? = null
    private var captureJob: Job? = null
    private var startTimeMs: Long = 0L
    private var outputPath: String? = null

    var isRecording: Boolean = false
        private set

    val elapsedSeconds: Int
        get() = if (isRecording) ((System.currentTimeMillis() - startTimeMs) / 1000).toInt() else 0

    fun isAvailable(): Boolean {
        return try {
            val p = ProcessBuilder("which", "ffmpeg")
                .redirectErrorStream(true)
                .start()
            val result = p.waitFor()
            result == 0
        } catch (e: Exception) {
            false
        }
    }

    fun start(window: java.awt.Window, scope: CoroutineScope) {
        if (isRecording) return

        val dir = File("recordings")
        dir.mkdirs()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"))
        val file = File(dir, "recording-$timestamp.mp4")
        outputPath = file.path

        val bounds = window.bounds
        val width = bounds.width
        val height = bounds.height

        // Ensure even dimensions for H.264
        val w = if (width % 2 == 0) width else width - 1
        val h = if (height % 2 == 0) height else height - 1

        process = ProcessBuilder(
            "ffmpeg", "-y",
            "-f", "rawvideo",
            "-pixel_format", "bgr24",
            "-video_size", "${w}x${h}",
            "-framerate", "30",
            "-i", "pipe:0",
            "-c:v", "libx264",
            "-preset", "ultrafast",
            "-pix_fmt", "yuv420p",
            "-crf", "23",
            file.absolutePath
        ).redirectErrorStream(true).start()

        isRecording = true
        startTimeMs = System.currentTimeMillis()

        val robot = Robot()
        val captureRect = Rectangle(bounds.x, bounds.y, w, h)
        val ffmpegOut = process!!.outputStream

        captureJob = scope.launch(Dispatchers.IO) {
            try {
                val bgrImage = BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR)
                val bgrGraphics = bgrImage.createGraphics()

                while (isActive) {
                    val frameStart = System.currentTimeMillis()

                    val capture = robot.createScreenCapture(captureRect)
                    // Convert to BGR format for ffmpeg
                    bgrGraphics.drawImage(capture, 0, 0, null)
                    val pixels = (bgrImage.raster.dataBuffer as DataBufferByte).data

                    try {
                        ffmpegOut.write(pixels)
                        ffmpegOut.flush()
                    } catch (e: Exception) {
                        // ffmpeg process may have died
                        break
                    }

                    // Target ~30fps
                    val elapsed = System.currentTimeMillis() - frameStart
                    val sleepMs = (33 - elapsed).coerceAtLeast(1)
                    delay(sleepMs)
                }

                bgrGraphics.dispose()
            } catch (e: CancellationException) {
                // Normal shutdown
            }
        }
    }

    fun stop(): String? {
        if (!isRecording) return null

        isRecording = false
        captureJob?.cancel()
        captureJob = null

        try {
            process?.outputStream?.close()
            process?.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        process = null
        return outputPath
    }
}
