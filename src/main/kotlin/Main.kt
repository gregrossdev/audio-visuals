import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import audio.*
import ui.*
import java.awt.Rectangle
import java.awt.Robot
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import javax.sound.sampled.Mixer
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Audio Visuals",
        state = rememberWindowState(width = 1024.dp, height = 600.dp),
        onKeyEvent = { false }
    ) {
        App(window)
    }
}

fun takeScreenshot(window: java.awt.Window): String? {
    return try {
        val dir = File("screenshots")
        dir.mkdirs()
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss"))
        val file = File(dir, "screenshot-$timestamp.png")
        val robot = Robot()
        val bounds = window.bounds
        val capture = robot.createScreenCapture(Rectangle(bounds.x, bounds.y, bounds.width, bounds.height))
        ImageIO.write(capture, "png", file)
        file.path
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App(window: java.awt.Window) {
    var screenshotStatus by remember { mutableStateOf<String?>(null) }

    // Clear screenshot status after 3 seconds
    LaunchedEffect(screenshotStatus) {
        if (screenshotStatus != null) {
            kotlinx.coroutines.delay(3000)
            screenshotStatus = null
        }
    }

    val doScreenshot = {
        val path = takeScreenshot(window)
        screenshotStatus = if (path != null) "Saved: $path" else "Screenshot failed"
    }

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

    // Auto-hide control bar state
    var controlBarVisible by remember { mutableStateOf(true) }
    var lastMouseMove by remember { mutableStateOf(System.currentTimeMillis()) }
    var containerHeight by remember { mutableStateOf(0) }
    var pointerY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val bottomThresholdPx = with(density) { 80.dp.toPx() }

    // Auto-hide timer: hide after 3 seconds of inactivity
    LaunchedEffect(lastMouseMove) {
        kotlinx.coroutines.delay(3000)
        controlBarVisible = false
    }

    AudioVisualsTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onGloballyPositioned { containerHeight = it.size.height }
                .onPointerEvent(PointerEventType.Move) { event ->
                    val position = event.changes.firstOrNull()?.position
                    if (position != null) {
                        pointerY = position.y
                        lastMouseMove = System.currentTimeMillis()
                        // Show control bar when mouse is near bottom edge
                        if (containerHeight > 0 && pointerY > containerHeight - bottomThresholdPx) {
                            controlBarVisible = true
                        } else if (!controlBarVisible) {
                            // Also show briefly on any mouse movement
                            controlBarVisible = true
                        }
                    }
                }
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        event.key == Key.S &&
                        event.isMetaPressed
                    ) {
                        doScreenshot()
                        true
                    } else false
                }
        ) {
            val activeTheme = themePreset.theme

            // Render all enabled layers back-to-front (full window)
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
                            layers = preset.layers.take(5)
                            themePreset = preset.themePreset
                            reactivityConfig = preset.reactivity
                            selectedLayerId = preset.layers.firstOrNull()?.id ?: ""
                        }
                    },
                    onDeletePreset = { name ->
                        PresetManager.delete(name)
                        presetNames = PresetManager.list()
                    },
                    isVisible = settingsPanelOpen
                )
            }

            // Screenshot status overlay
            if (screenshotStatus != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = screenshotStatus ?: "",
                        fontSize = 13.sp,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.7f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }

            // Auto-hide control bar at bottom
            AnimatedVisibility(
                visible = controlBarVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
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
                    onScreenshot = { doScreenshot() },
                    settingsOpen = settingsPanelOpen,
                    onSettingsToggle = { settingsPanelOpen = !settingsPanelOpen },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF1A1A1A).copy(alpha = 0.9f))
                )
            }
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
            VisualizationMode.CURVES -> CurvesVisualizer(
                magnitudes = magnitudes,
                theme = theme,
                isBeat = isBeat,
                config = layer.config as? CurvesConfig ?: CurvesConfig(),
                audioFeatures = audioFeatures
            )
        }
    }
}
