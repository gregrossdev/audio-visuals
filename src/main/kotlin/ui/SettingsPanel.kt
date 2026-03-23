package ui

import audio.ReactivityConfig
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsPanel(
    layers: List<VisualizerLayer>,
    selectedLayerId: String,
    onLayersChanged: (List<VisualizerLayer>) -> Unit,
    onSelectedLayerChanged: (String) -> Unit,
    reactivity: ReactivityConfig,
    onReactivityChanged: (ReactivityConfig) -> Unit,
    presetNames: List<String>,
    onSavePreset: (String) -> Unit,
    onLoadPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val selectedLayer = layers.find { it.id == selectedLayerId }
    val panelShape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
    Column(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFF1A1A1A).copy(alpha = 0.75f), panelShape)
            .border(1.dp, Color.White.copy(alpha = 0.08f), panelShape)
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", fontSize = 16.sp, color = Color.White)

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Presets section
        PresetSection(
            presetNames = presetNames,
            onSavePreset = onSavePreset,
            onLoadPreset = onLoadPreset,
            onDeletePreset = onDeletePreset
        )

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Layers section
        SectionHeader("Layers")

        layers.forEachIndexed { index, layer ->
            LayerRow(
                layer = layer,
                isSelected = layer.id == selectedLayerId,
                canDelete = layers.size > 1,
                canMoveUp = index > 0,
                canMoveDown = index < layers.size - 1,
                onSelect = { onSelectedLayerChanged(layer.id) },
                onModeChanged = { mode ->
                    val newConfig = defaultConfigForMode(mode)
                    onLayersChanged(layers.map {
                        if (it.id == layer.id) it.copy(vizMode = mode, config = newConfig) else it
                    })
                    onSelectedLayerChanged(layer.id)
                },
                onToggleEnabled = {
                    onLayersChanged(layers.map {
                        if (it.id == layer.id) it.copy(enabled = !it.enabled) else it
                    })
                },
                onDelete = {
                    val newLayers = layers.filter { it.id != layer.id }
                    onLayersChanged(newLayers)
                    if (selectedLayerId == layer.id) {
                        onSelectedLayerChanged(newLayers.first().id)
                    }
                },
                onMoveUp = {
                    val mutable = layers.toMutableList()
                    val temp = mutable[index]
                    mutable[index] = mutable[index - 1]
                    mutable[index - 1] = temp
                    onLayersChanged(mutable)
                },
                onMoveDown = {
                    val mutable = layers.toMutableList()
                    val temp = mutable[index]
                    mutable[index] = mutable[index + 1]
                    mutable[index + 1] = temp
                    onLayersChanged(mutable)
                }
            )
        }

        // Add layer button
        OutlinedButton(
            onClick = {
                val newLayer = defaultLayer()
                onLayersChanged(layers + newLayer)
                onSelectedLayerChanged(newLayer.id)
            },
            modifier = Modifier.fillMaxWidth().height(32.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
            enabled = layers.size < 5,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("+ Add Layer", fontSize = 11.sp)
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Selected layer settings
        if (selectedLayer != null) {
            SectionHeader("Selected Layer")

            // Opacity slider
            LabeledSlider("Opacity", selectedLayer.opacity, 0f, 1f) { newOpacity ->
                onLayersChanged(layers.map {
                    if (it.id == selectedLayerId) it.copy(opacity = newOpacity) else it
                })
            }

            // Blend mode dropdown
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Blend", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    FilledTonalButton(
                        onClick = { expanded = true },
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(selectedLayer.blendMode.label, fontSize = 11.sp)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        LayerBlendMode.entries.forEach { blend ->
                            DropdownMenuItem(
                                text = { Text(blend.label, fontSize = 12.sp) },
                                onClick = {
                                    onLayersChanged(layers.map {
                                        if (it.id == selectedLayerId) it.copy(blendMode = blend) else it
                                    })
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Per-visualizer config for selected layer
            LayerConfigSection(selectedLayer) { newConfig ->
                onLayersChanged(layers.map {
                    if (it.id == selectedLayerId) it.copy(config = newConfig) else it
                })
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

        // Audio Reactivity section
        SectionHeader("Audio Reactivity")
        LabeledSlider("Bass", reactivity.bassSensitivity, 0.5f, 3.0f) {
            onReactivityChanged(reactivity.copy(bassSensitivity = it))
        }
        LabeledSlider("Mids", reactivity.midSensitivity, 0.5f, 3.0f) {
            onReactivityChanged(reactivity.copy(midSensitivity = it))
        }
        LabeledSlider("Highs", reactivity.highSensitivity, 0.5f, 3.0f) {
            onReactivityChanged(reactivity.copy(highSensitivity = it))
        }
        LabeledSlider("Beat Sens.", reactivity.beatThreshold, 0.5f, 3.0f) {
            onReactivityChanged(reactivity.copy(beatThreshold = it))
        }
        LabeledSlider("Smoothing", reactivity.smoothingFactor, 0.2f, 3.0f) {
            onReactivityChanged(reactivity.copy(smoothingFactor = it))
        }
        LabeledSlider("Centroid", reactivity.centroidInfluence, 0.0f, 1.0f) {
            onReactivityChanged(reactivity.copy(centroidInfluence = it))
        }
        LabeledSlider("Onset Sens.", reactivity.onsetSensitivity, 0.5f, 3.0f) {
            onReactivityChanged(reactivity.copy(onsetSensitivity = it))
        }
        LabeledSlider("Energy Smooth", reactivity.energySmoothing, 0.05f, 0.5f) {
            onReactivityChanged(reactivity.copy(energySmoothing = it))
        }
        LabeledSlider("Trail Fade", reactivity.trailFade, 0f, 0.15f) {
            onReactivityChanged(reactivity.copy(trailFade = it))
        }
    }
    }
}

@Composable
private fun LayerRow(
    layer: VisualizerLayer,
    isSelected: Boolean,
    canDelete: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onSelect: () -> Unit,
    onToggleEnabled: () -> Unit,
    onModeChanged: (VisualizationMode) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent
            )
            .padding(vertical = 2.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Enable/disable checkbox
        Checkbox(
            checked = layer.enabled,
            onCheckedChange = { onToggleEnabled() },
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = Color.White.copy(alpha = 0.3f)
            )
        )

        // Layer name — click to select + pick visualization mode
        Box(modifier = Modifier.weight(1f)) {
            var modeExpanded by remember { mutableStateOf(false) }
            TextButton(
                onClick = {
                    onSelect()
                    modeExpanded = true
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.fillMaxWidth().height(28.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        layer.vizMode.name.lowercase().replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        "\u25BE",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }
            }
            DropdownMenu(expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                VisualizationMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                color = if (mode == layer.vizMode) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onModeChanged(mode)
                            modeExpanded = false
                        }
                    )
                }
            }
        }

        // Reorder buttons
        if (canMoveUp) {
            TextButton(
                onClick = onMoveUp,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(24.dp)
            ) {
                Text("\u25B2", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            Spacer(Modifier.size(24.dp))
        }

        if (canMoveDown) {
            TextButton(
                onClick = onMoveDown,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(24.dp)
            ) {
                Text("\u25BC", fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
            }
        } else {
            Spacer(Modifier.size(24.dp))
        }

        // Delete button
        if (canDelete) {
            TextButton(
                onClick = onDelete,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(24.dp)
            ) {
                Text("\u2715", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun LayerConfigSection(layer: VisualizerLayer, onConfigChanged: (VisualizerConfig) -> Unit) {
    when (val config = layer.config) {
        is ParticleConfig -> ParticleSettings(config) { onConfigChanged(it) }
        is KaleidoscopeConfig -> KaleidoscopeSettings(config) { onConfigChanged(it) }
        is TrailsConfig -> TrailsSettings(config) { onConfigChanged(it) }
        is MandalaConfig -> MandalaSettings(config) { onConfigChanged(it) }
        is FlowFieldConfig -> FlowFieldSettings(config) { onConfigChanged(it) }
        is TerrainConfig -> TerrainSettings(config) { onConfigChanged(it) }
        is CurvesConfig -> CurvesSettings(config) { onConfigChanged(it) }
        else -> {
            Text(
                "No configurable parameters for this visualizer.",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

fun defaultConfigForMode(mode: VisualizationMode): VisualizerConfig = when (mode) {
    VisualizationMode.BARS -> BarsConfig()
    VisualizationMode.WAVEFORM -> WaveformConfig()
    VisualizationMode.CIRCULAR -> CircularConfig()
    VisualizationMode.PARTICLES -> ParticleConfig()
    VisualizationMode.KALEIDOSCOPE -> KaleidoscopeConfig()
    VisualizationMode.TRAILS -> TrailsConfig()
    VisualizationMode.MANDALA -> MandalaConfig()
    VisualizationMode.FLOW_FIELD -> FlowFieldConfig()
    VisualizationMode.TERRAIN -> TerrainConfig()
    VisualizationMode.CURVES -> CurvesConfig()
}

@Composable
private fun PresetSection(
    presetNames: List<String>,
    onSavePreset: (String) -> Unit,
    onLoadPreset: (String) -> Unit,
    onDeletePreset: (String) -> Unit
) {
    SectionHeader("Presets")

    var saveName by remember { mutableStateOf("") }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = saveName,
            onValueChange = { saveName = it },
            placeholder = { Text("Preset name", fontSize = 11.sp) },
            modifier = Modifier.weight(1f).height(40.dp),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                focusedBorderColor = Color.White.copy(alpha = 0.5f)
            )
        )
        FilledTonalButton(
            onClick = {
                if (saveName.isNotBlank()) {
                    onSavePreset(saveName.trim())
                    saveName = ""
                }
            },
            contentPadding = PaddingValues(horizontal = 10.dp),
            modifier = Modifier.height(32.dp),
            enabled = saveName.isNotBlank()
        ) {
            Text("Save", fontSize = 11.sp)
        }
    }

    if (presetNames.isNotEmpty()) {
        var loadExpanded by remember { mutableStateOf(false) }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { loadExpanded = true },
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    Text("Load Preset", fontSize = 11.sp)
                }
                DropdownMenu(
                    expanded = loadExpanded,
                    onDismissRequest = { loadExpanded = false }
                ) {
                    presetNames.forEach { name ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(name, fontSize = 12.sp)
                                    TextButton(
                                        onClick = {
                                            onDeletePreset(name)
                                            loadExpanded = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp)
                                    ) {
                                        Text("Del", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            },
                            onClick = {
                                onLoadPreset(name)
                                loadExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticleSettings(config: ParticleConfig, onChange: (ParticleConfig) -> Unit) {
    SectionHeader("Particles")
    LabeledSlider("Max Particles", config.maxParticles.toFloat(), 100f, 2000f, isInt = true) {
        onChange(config.copy(maxParticles = it.toInt()))
    }
    LabeledSlider("Bass Spawn", config.bassSpawnRate.toFloat(), 1f, 12f, isInt = true) {
        onChange(config.copy(bassSpawnRate = it.toInt()))
    }
    LabeledSlider("Mid Spawn", config.midSpawnRate.toFloat(), 1f, 12f, isInt = true) {
        onChange(config.copy(midSpawnRate = it.toInt()))
    }
    LabeledSlider("High Spawn", config.highSpawnRate.toFloat(), 1f, 12f, isInt = true) {
        onChange(config.copy(highSpawnRate = it.toInt()))
    }
    LabeledSlider("Beat Burst", config.beatBurstCount.toFloat(), 5f, 60f, isInt = true) {
        onChange(config.copy(beatBurstCount = it.toInt()))
    }
    LabeledSlider("Drag", config.drag, 0.95f, 0.999f) {
        onChange(config.copy(drag = it))
    }
    LabeledSlider("Life Decay", config.lifeDecay, 0.005f, 0.05f) {
        onChange(config.copy(lifeDecay = it))
    }
}

@Composable
private fun KaleidoscopeSettings(config: KaleidoscopeConfig, onChange: (KaleidoscopeConfig) -> Unit) {
    SectionHeader("Kaleidoscope")
    LabeledSlider("Segments", config.segments.toFloat(), 3f, 16f, isInt = true) {
        onChange(config.copy(segments = it.toInt()))
    }
    LabeledSlider("Shapes/Wedge", config.shapesPerWedge.toFloat(), 4f, 24f, isInt = true) {
        onChange(config.copy(shapesPerWedge = it.toInt()))
    }
    LabeledSlider("Rotation Speed", config.baseRotationSpeed, 0f, 1.0f) {
        onChange(config.copy(baseRotationSpeed = it))
    }
    LabeledSlider("Energy Scale", config.energyRotationScale, 0f, 4.0f) {
        onChange(config.copy(energyRotationScale = it))
    }
    LabeledSlider("Beat Boost", config.beatSpeedBoost, 0f, 8.0f) {
        onChange(config.copy(beatSpeedBoost = it))
    }
    LabeledSlider("Breath", config.breathScale, 0f, 0.5f) {
        onChange(config.copy(breathScale = it))
    }
}

@Composable
private fun TrailsSettings(config: TrailsConfig, onChange: (TrailsConfig) -> Unit) {
    SectionHeader("Trails")
    LabeledSlider("History Depth", config.historySize.toFloat(), 10f, 80f, isInt = true) {
        onChange(config.copy(historySize = it.toInt()))
    }
    LabeledSlider("Points/Frame", config.pointsPerFrame.toFloat(), 10f, 60f, isInt = true) {
        onChange(config.copy(pointsPerFrame = it.toInt()))
    }
    LabeledSlider("V. Spread", config.verticalSpread, 0.3f, 1.0f) {
        onChange(config.copy(verticalSpread = it))
    }
    LabeledSlider("Hue Fade", config.hueFadeSpeed, 0f, 1.0f) {
        onChange(config.copy(hueFadeSpeed = it))
    }
    LabeledSlider("Sat. Fade", config.saturationFade, 0f, 0.8f) {
        onChange(config.copy(saturationFade = it))
    }
}

@Composable
private fun MandalaSettings(config: MandalaConfig, onChange: (MandalaConfig) -> Unit) {
    SectionHeader("Mandala")
    LabeledSlider("Rings", config.rings.toFloat(), 2f, 8f, isInt = true) {
        onChange(config.copy(rings = it.toInt()))
    }
    LabeledSlider("Elements/Ring", config.elementsPerRing.toFloat(), 4f, 32f, isInt = true) {
        onChange(config.copy(elementsPerRing = it.toInt()))
    }
    LabeledSlider("Detail", config.detailLevel.toFloat(), 1f, 4f, isInt = true) {
        onChange(config.copy(detailLevel = it.toInt()))
    }
    LabeledSlider("Inner Speed", config.innerRotationSpeed, 0f, 2.0f) {
        onChange(config.copy(innerRotationSpeed = it))
    }
    LabeledSlider("Outer Speed", config.outerRotationSpeed, 0f, 2.0f) {
        onChange(config.copy(outerRotationSpeed = it))
    }
    LabeledSlider("Pulse", config.pulseIntensity, 0f, 1.0f) {
        onChange(config.copy(pulseIntensity = it))
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Connections", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
        Switch(
            checked = config.showConnections,
            onCheckedChange = { onChange(config.copy(showConnections = it)) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        )
    }
}

@Composable
private fun FlowFieldSettings(config: FlowFieldConfig, onChange: (FlowFieldConfig) -> Unit) {
    SectionHeader("Flow Field")
    LabeledSlider("Particles", config.particleCount.toFloat(), 500f, 5000f, isInt = true) {
        onChange(config.copy(particleCount = it.toInt()))
    }
    LabeledSlider("Noise Scale", config.noiseScale, 0.001f, 0.01f) {
        onChange(config.copy(noiseScale = it))
    }
    LabeledSlider("Field Speed", config.fieldSpeed, 0.001f, 0.05f) {
        onChange(config.copy(fieldSpeed = it))
    }
    LabeledSlider("Drag", config.particleDrag, 0.9f, 0.999f) {
        onChange(config.copy(particleDrag = it))
    }
    LabeledSlider("Speed", config.particleSpeed, 0.5f, 5.0f) {
        onChange(config.copy(particleSpeed = it))
    }
    LabeledSlider("Trail Length", config.trailLength.toFloat(), 1f, 15f, isInt = true) {
        onChange(config.copy(trailLength = it.toInt()))
    }
    LabeledSlider("Beat Jump", config.beatTimeJump, 0.5f, 5.0f) {
        onChange(config.copy(beatTimeJump = it))
    }
    LabeledSlider("Turb. Min", config.turbulenceMin, 0.0005f, 0.005f) {
        onChange(config.copy(turbulenceMin = it))
    }
    LabeledSlider("Turb. Max", config.turbulenceMax, 0.002f, 0.02f) {
        onChange(config.copy(turbulenceMax = it))
    }
}

@Composable
private fun TerrainSettings(config: TerrainConfig, onChange: (TerrainConfig) -> Unit) {
    SectionHeader("Terrain")
    LabeledSlider("Columns", config.gridCols.toFloat(), 10f, 80f, isInt = true) {
        onChange(config.copy(gridCols = it.toInt()))
    }
    LabeledSlider("Rows", config.gridRows.toFloat(), 10f, 60f, isInt = true) {
        onChange(config.copy(gridRows = it.toInt()))
    }
    LabeledSlider("Height", config.heightScale, 50f, 300f) {
        onChange(config.copy(heightScale = it))
    }
    LabeledSlider("Noise Scale", config.noiseScale, 0.01f, 0.15f) {
        onChange(config.copy(noiseScale = it))
    }
    LabeledSlider("Scroll Speed", config.scrollSpeed, 0.005f, 0.08f) {
        onChange(config.copy(scrollSpeed = it))
    }
    LabeledSlider("Focal Length", config.focalLength, 100f, 800f) {
        onChange(config.copy(focalLength = it))
    }
    LabeledSlider("Camera Tilt", config.cameraTilt, 0.1f, 1.5f) {
        onChange(config.copy(cameraTilt = it))
    }
    LabeledSlider("Fog Start", config.fogStart, 0f, 0.8f) {
        onChange(config.copy(fogStart = it))
    }
    LabeledSlider("Fog End", config.fogEnd, 0.3f, 1.0f) {
        onChange(config.copy(fogEnd = it))
    }
}

@Composable
private fun CurvesSettings(config: CurvesConfig, onChange: (CurvesConfig) -> Unit) {
    SectionHeader("Curves")

    // Curve mode dropdown
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Curve Type", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
        var expanded by remember { mutableStateOf(false) }
        Box {
            FilledTonalButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 10.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(config.curveMode.label, fontSize = 11.sp)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                CurveMode.entries.forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.label, fontSize = 12.sp) },
                        onClick = {
                            onChange(config.copy(curveMode = mode))
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    LabeledSlider("Points", config.pointCount.toFloat(), 500f, 6000f, isInt = true) {
        onChange(config.copy(pointCount = it.toInt()))
    }
    LabeledSlider("Line Width", config.lineWidth, 0.5f, 4.0f) {
        onChange(config.copy(lineWidth = it))
    }
    LabeledSlider("Trail Fade", config.trailAlpha, 0.01f, 0.2f) {
        onChange(config.copy(trailAlpha = it))
    }
    LabeledSlider("Speed", config.speedMultiplier, 0.2f, 3.0f) {
        onChange(config.copy(speedMultiplier = it))
    }
    LabeledSlider("Color Cycle", config.colorCycleSpeed, 0.1f, 2.0f) {
        onChange(config.copy(colorCycleSpeed = it))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        color = Color.White.copy(alpha = 0.9f),
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    min: Float,
    max: Float,
    isInt: Boolean = false,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            Text(
                if (isInt) value.toInt().toString() else String.format("%.2f", value),
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = min..max,
            modifier = Modifier.fillMaxWidth().height(24.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                inactiveTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}
