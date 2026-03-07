import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import audio.AudioCapture
import audio.FFTProcessor
import ui.SpectrumVisualizer

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Audio Visuals",
        state = rememberWindowState(width = 1024.dp, height = 600.dp)
    ) {
        App()
    }
}

@Composable
fun App() {
    val audioCapture = remember { AudioCapture() }
    val fftProcessor = remember { FFTProcessor() }
    val scope = rememberCoroutineScope()

    val magnitudes by fftProcessor.magnitudes.collectAsState()

    LaunchedEffect(Unit) {
        audioCapture.start(scope)
        fftProcessor.start(scope, audioCapture)
    }

    DisposableEffect(Unit) {
        onDispose {
            fftProcessor.stop()
            audioCapture.stop()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        SpectrumVisualizer(magnitudes = magnitudes)
    }
}
