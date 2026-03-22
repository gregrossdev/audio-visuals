import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import audio.*
import ui.*
import java.io.File
import javax.sound.sampled.Mixer
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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
    val fileSource = remember { FileAudioSource() }
    val fftProcessor = remember { FFTProcessor() }
    val smoother = remember { SmoothedMagnitudes() }
    val logSmoother = remember { SmoothedMagnitudes() }
    val peakTracker = remember { PeakHoldTracker() }
    val beatDetector = remember { BeatDetector() }
    val bandEnergyAnalyzer = remember { BandEnergyAnalyzer() }
    val centroidTracker = remember { SpectralCentroidTracker() }
    val onsetDetector = remember { OnsetDetector() }
    val energyEnvelope = remember { EnergyEnvelope() }
    val scope = rememberCoroutineScope()

    val rawMagnitudes by fftProcessor.magnitudes.collectAsState()
    val rawLogMagnitudes by fftProcessor.logMagnitudes.collectAsState()
    val rawLinearMagnitudes by fftProcessor.linearMagnitudes.collectAsState()

    var sourceMode by remember { mutableStateOf(SourceMode.MIC) }
    var logScale by remember { mutableStateOf(true) }
    var themePreset by remember { mutableStateOf(ThemePreset.SPECTRUM) }
    var devices by remember { mutableStateOf(emptyList<Mixer.Info>()) }
    var selectedDevice by remember { mutableStateOf<Mixer.Info?>(null) }
    var gain by remember { mutableStateOf(1.0f) }
    var isPaused by remember { mutableStateOf(false) }
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileName by remember { mutableStateOf<String?>(null) }

    // Layers & settings
    var settingsPanelOpen by remember { mutableStateOf(false) }
    var layers by remember { mutableStateOf(listOf(defaultLayer())) }
    var selectedLayerId by remember { mutableStateOf(layers.first().id) }
    var reactivityConfig by remember { mutableStateOf(ReactivityConfig()) }
    var presetNames by remember { mutableStateOf(PresetManager.list()) }

    val activeSource: AudioSource = if (sourceMode == SourceMode.MIC) audioCapture else fileSource
    val selectedRaw = if (logScale) rawLogMagnitudes else rawMagnitudes

    // Apply reactivity-adjusted smoothing
    val smoothed = remember(selectedRaw, reactivityConfig.smoothingFactor) {
        if (logScale) logSmoother.smooth(selectedRaw, reactivityConfig.smoothingFactor)
        else smoother.smooth(selectedRaw, reactivityConfig.smoothingFactor)
    }

    // Apply band sensitivity
    val magnitudes = remember(smoothed, reactivityConfig) {
        reactivityConfig.applyBandSensitivity(smoothed, smoothed.size)
    }

    val peaks = remember(magnitudes) { peakTracker.update(magnitudes) }
    val isBeat = remember(magnitudes, reactivityConfig.beatThreshold) {
        beatDetector.update(magnitudes, reactivityConfig.beatThreshold)
    }
    val bandFrequencies = if (logScale) fftProcessor.logBinMapper.bandFrequencies else null

    // Advanced audio features from linear magnitudes
    val audioFeatures = remember(rawLinearMagnitudes, reactivityConfig.onsetSensitivity) {
        AudioFeatures(
            bandEnergies = bandEnergyAnalyzer.update(rawLinearMagnitudes),
            spectralCentroid = centroidTracker.update(rawLinearMagnitudes),
            onsets = onsetDetector.update(rawLinearMagnitudes, reactivityConfig.onsetSensitivity),
            energy = energyEnvelope.update(rawLinearMagnitudes)
        )
    }

    LaunchedEffect(Unit) {
        devices = AudioCapture.availableDevices()
        audioCapture.start(scope)
        fftProcessor.start(scope, audioCapture)
    }

    DisposableEffect(Unit) {
        onDispose {
            fftProcessor.stop()
            audioCapture.stop()
            fileSource.stop()
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
                val activeTheme = themePreset.theme

                // Render all enabled layers back-to-front
                layers.filter { it.enabled }.forEach { layer ->
                    key(layer.id) {
                        LayerRenderer(
                            layer = layer,
                            magnitudes = magnitudes,
                            theme = activeTheme,
                            isBeat = isBeat,
                            peaks = peaks,
                            bandFrequencies = bandFrequencies,
                            audioFeatures = audioFeatures
                        )
                    }
                }

                // Settings panel overlay
                if (settingsPanelOpen) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        SettingsPanel(
                            layers = layers,
                            selectedLayerId = selectedLayerId,
                            onLayersChanged = { layers = it },
                            onSelectedLayerChanged = { selectedLayerId = it },
                            reactivity = reactivityConfig,
                            onReactivityChanged = { reactivityConfig = it },
                            presetNames = presetNames,
                            onSavePreset = { name ->
                                PresetManager.save(Preset(name, layers, themePreset, reactivityConfig))
                                presetNames = PresetManager.list()
                            },
                            onLoadPreset = { name ->
                                val preset = PresetManager.load(name)
                                if (preset != null) {
                                    layers = preset.layers
                                    themePreset = preset.themePreset
                                    reactivityConfig = preset.reactivity
                                    selectedLayerId = preset.layers.firstOrNull()?.id ?: ""
                                }
                            },
                            onDeletePreset = { name ->
                                PresetManager.delete(name)
                                presetNames = PresetManager.list()
                            }
                        )
                    }
                }
            }

            ControlBar(
                sourceMode = sourceMode,
                onSourceModeChanged = { newMode ->
                    if (newMode == sourceMode) return@ControlBar
                    fftProcessor.stop()
                    activeSource.stop()
                    isPaused = false
                    sourceMode = newMode
                    when (newMode) {
                        SourceMode.MIC -> {
                            audioCapture.gain = gain
                            audioCapture.start(scope, selectedDevice)
                            fftProcessor.start(scope, audioCapture)
                        }
                        SourceMode.FILE -> {
                            if (selectedFile != null) {
                                fileSource.gain = gain
                                fileSource.start(scope, selectedFile!!)
                                fftProcessor.start(scope, fileSource)
                            }
                        }
                    }
                },
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
                fileName = fileName,
                onOpenFile = {
                    val chooser = JFileChooser()
                    chooser.fileFilter = FileNameExtensionFilter(
                        "Audio Files (*.wav, *.mp3, *.flac)", "wav", "mp3", "flac"
                    )
                    val result = chooser.showOpenDialog(null)
                    if (result == JFileChooser.APPROVE_OPTION) {
                        val file = chooser.selectedFile
                        selectedFile = file
                        fileName = file.name
                        fftProcessor.stop()
                        fileSource.stop()
                        if (sourceMode == SourceMode.MIC) {
                            audioCapture.stop()
                            sourceMode = SourceMode.FILE
                        }
                        isPaused = false
                        fileSource.gain = gain
                        fileSource.start(scope, file)
                        fftProcessor.start(scope, fileSource)
                    }
                },
                layerCount = layers.size,
                layerSummary = if (layers.size == 1) layers.first().vizMode.name.lowercase()
                    .replaceFirstChar { it.uppercase() } else "${layers.size} layers",
                logScale = logScale,
                onLogScaleChanged = { logScale = it },
                themePreset = themePreset,
                onThemeChanged = { themePreset = it },
                gain = gain,
                onGainChange = { newGain ->
                    gain = newGain
                    activeSource.gain = newGain
                },
                isPaused = isPaused,
                onPauseToggle = {
                    isPaused = !isPaused
                    if (isPaused) activeSource.pause() else activeSource.resume()
                },
                settingsOpen = settingsPanelOpen,
                onSettingsToggle = { settingsPanelOpen = !settingsPanelOpen },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
fun LayerRenderer(
    layer: VisualizerLayer,
    magnitudes: FloatArray,
    theme: ColorTheme,
    isBeat: Boolean,
    peaks: FloatArray,
    bandFrequencies: FloatArray?,
    audioFeatures: AudioFeatures
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = layer.opacity
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        when (layer.vizMode) {
            VisualizationMode.BARS -> SpectrumVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                peaks = peaks,
                bandFrequencies = bandFrequencies
            )
            VisualizationMode.WAVEFORM -> WaveformVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                peaks = peaks
            )
            VisualizationMode.CIRCULAR -> CircularVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                peaks = peaks
            )
            VisualizationMode.PARTICLES -> ParticleVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                isBeat = isBeat,
                config = layer.config as? ParticleConfig ?: ParticleConfig(),
                audioFeatures = audioFeatures
            )
            VisualizationMode.KALEIDOSCOPE -> KaleidoscopeVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                isBeat = isBeat,
                config = layer.config as? KaleidoscopeConfig ?: KaleidoscopeConfig(),
                audioFeatures = audioFeatures
            )
            VisualizationMode.TRAILS -> TrailsVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                isBeat = isBeat,
                config = layer.config as? TrailsConfig ?: TrailsConfig(),
                audioFeatures = audioFeatures
            )
            VisualizationMode.MANDALA -> MandalaVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                isBeat = isBeat,
                config = layer.config as? MandalaConfig ?: MandalaConfig(),
                audioFeatures = audioFeatures
            )
            VisualizationMode.FLOW_FIELD -> FlowFieldVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                isBeat = isBeat,
                config = layer.config as? FlowFieldConfig ?: FlowFieldConfig(),
                audioFeatures = audioFeatures
            )
            VisualizationMode.TERRAIN -> TerrainVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                isBeat = isBeat,
                config = layer.config as? TerrainConfig ?: TerrainConfig(),
                audioFeatures = audioFeatures
            )
        }
    }
}
