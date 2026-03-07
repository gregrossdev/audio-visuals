import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import audio.AudioCapture
import audio.FFTProcessor
import ui.AudioVisualsTheme
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

    AudioVisualsTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                SpectrumVisualizer(magnitudes = magnitudes)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Control bar — populated in batch 2.3
            }
        }
    }
}
