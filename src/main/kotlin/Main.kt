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
import ui.ControlBar
import ui.SpectrumVisualizer
import javax.sound.sampled.Mixer

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

    var devices by remember { mutableStateOf(emptyList<Mixer.Info>()) }
    var selectedDevice by remember { mutableStateOf<Mixer.Info?>(null) }
    var gain by remember { mutableStateOf(1.0f) }
    var isPaused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        devices = AudioCapture.availableDevices()
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

            ControlBar(
                devices = devices,
                selectedDevice = selectedDevice,
                onDeviceSelected = { device ->
                    selectedDevice = device
                    fftProcessor.stop()
                    audioCapture.stop()
                    audioCapture.gain = gain
                    audioCapture.start(scope, device)
                    fftProcessor.start(scope, audioCapture)
                },
                gain = gain,
                onGainChange = { newGain ->
                    gain = newGain
                    audioCapture.gain = newGain
                },
                isPaused = isPaused,
                onPauseToggle = {
                    isPaused = !isPaused
                    if (isPaused) {
                        audioCapture.pause()
                    } else {
                        audioCapture.resume()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}
