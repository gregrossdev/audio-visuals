package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javax.sound.sampled.Mixer
import kotlin.math.cos
import kotlin.math.sin

enum class SourceMode { MIC, FILE }

@Composable
fun ControlBar(
    sourceMode: SourceMode,
    onSourceModeChanged: (SourceMode) -> Unit,
    devices: List<Mixer.Info>,
    selectedDevice: Mixer.Info?,
    onDeviceSelected: (Mixer.Info?) -> Unit,
    fileName: String?,
    onOpenFile: () -> Unit,
    gain: Float,
    onGainChange: (Float) -> Unit,
    layerCount: Int,
    layerSummary: String,
    logScale: Boolean,
    onLogScaleChanged: (Boolean) -> Unit,
    themePreset: ThemePreset,
    onThemeChanged: (ThemePreset) -> Unit,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onScreenshot: () -> Unit = {},
    isRecording: Boolean = false,
    onRecordToggle: () -> Unit = {},
    recordingEnabled: Boolean = true,
    settingsOpen: Boolean = false,
    onSettingsToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // === Source group: SourceModeToggle + DeviceSelector/FileSelector ===
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SourceModeToggle(
                sourceMode = sourceMode,
                onSourceModeChanged = onSourceModeChanged
            )

            when (sourceMode) {
                SourceMode.MIC -> {
                    DeviceSelector(
                        devices = devices,
                        selectedDevice = selectedDevice,
                        onDeviceSelected = onDeviceSelected,
                        modifier = Modifier.weight(1f)
                    )
                }
                SourceMode.FILE -> {
                    FileSelector(
                        fileName = fileName,
                        onOpenFile = onOpenFile,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        ControlBarDivider()

        // === Audio group: GainSlider + LogScale toggle ===
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GainSlider(
                gain = gain,
                onGainChange = onGainChange,
                modifier = Modifier.weight(1f)
            )

            FilledTonalButton(
                onClick = { onLogScaleChanged(!logScale) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (logScale) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (logScale) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Log", fontSize = 11.sp)
            }
        }

        ControlBarDivider()

        // === Display group: ThemeSelector + LayerSummary + Screenshot + Settings + Pause ===
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeSelector(
                themePreset = themePreset,
                onThemeChanged = onThemeChanged
            )

            FilledTonalButton(
                onClick = onSettingsToggle,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(layerSummary, fontSize = 11.sp)
            }

            // Screenshot button
            IconButton(onClick = onScreenshot) {
                CameraIcon(color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Record button
            IconButton(
                onClick = onRecordToggle,
                enabled = recordingEnabled
            ) {
                RecordIcon(
                    isRecording = isRecording,
                    color = if (isRecording) Color.Red
                    else if (recordingEnabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }

            // Settings gear toggle
            IconButton(onClick = onSettingsToggle) {
                GearIcon(
                    color = if (settingsOpen) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Pause/Resume toggle
            IconButton(onClick = onPauseToggle) {
                if (isPaused) {
                    PlayIcon(color = MaterialTheme.colorScheme.primary)
                } else {
                    PauseIcon(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

// ── Canvas-drawn icons ──────────────────────────────────────────────────────

@Composable
private fun RecordIcon(isRecording: Boolean, color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2
        val cy = h / 2

        if (isRecording) {
            // Stop: filled rounded square
            val squareSize = w * 0.45f
            drawRoundRect(
                color = color,
                topLeft = Offset(cx - squareSize / 2, cy - squareSize / 2),
                size = Size(squareSize, squareSize),
                cornerRadius = CornerRadius(w * 0.06f)
            )
        } else {
            // Record: filled circle
            drawCircle(
                color = color,
                radius = w * 0.3f,
                center = Offset(cx, cy)
            )
        }
    }
}

@Composable
private fun CameraIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)

        // Camera body: rounded rectangle
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.28f),
            size = Size(w * 0.84f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.08f),
            style = stroke
        )

        // Viewfinder bump on top
        val bumpPath = Path().apply {
            moveTo(w * 0.30f, h * 0.28f)
            lineTo(w * 0.36f, h * 0.14f)
            lineTo(w * 0.64f, h * 0.14f)
            lineTo(w * 0.70f, h * 0.28f)
        }
        drawPath(bumpPath, color = color, style = stroke)

        // Lens circle
        drawCircle(
            color = color,
            radius = w * 0.16f,
            center = Offset(w * 0.50f, h * 0.58f),
            style = stroke
        )
    }
}

@Composable
private fun GearIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val cx = w / 2f
        val cy = w / 2f
        val stroke = Stroke(width = w * 0.08f, cap = StrokeCap.Round)

        // Center circle
        drawCircle(
            color = color,
            radius = w * 0.16f,
            center = Offset(cx, cy),
            style = stroke
        )

        // Radiating tick marks (8 ticks)
        val innerR = w * 0.28f
        val outerR = w * 0.44f
        for (i in 0 until 8) {
            val angle = Math.toRadians(i * 45.0)
            val cosA = cos(angle).toFloat()
            val sinA = sin(angle).toFloat()
            drawLine(
                color = color,
                start = Offset(cx + innerR * cosA, cy + innerR * sinA),
                end = Offset(cx + outerR * cosA, cy + outerR * sinA),
                strokeWidth = w * 0.1f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun PlayIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.80f, h * 0.50f)
            lineTo(w * 0.25f, h * 0.85f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
private fun PauseIcon(color: Color) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val w = size.width
        val h = size.height
        val barWidth = w * 0.18f

        // Left bar
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.22f, h * 0.18f),
            size = Size(barWidth, h * 0.64f),
            cornerRadius = CornerRadius(barWidth * 0.3f)
        )
        // Right bar
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.60f, h * 0.18f),
            size = Size(barWidth, h * 0.64f),
            cornerRadius = CornerRadius(barWidth * 0.3f)
        )
    }
}

// ── Vertical divider between groups ─────────────────────────────────────────

@Composable
private fun ControlBarDivider() {
    Canvas(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
    ) {
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height),
            strokeWidth = size.width
        )
    }
}

// ── Existing sub-components (unchanged) ─────────────────────────────────────

@Composable
private fun SourceModeToggle(
    sourceMode: SourceMode,
    onSourceModeChanged: (SourceMode) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
        SourceMode.entries.forEach { mode ->
            val selected = mode == sourceMode
            FilledTonalButton(
                onClick = { onSourceModeChanged(mode) },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = if (selected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    text = if (mode == SourceMode.MIC) "Mic" else "File",
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun FileSelector(
    fileName: String?,
    onOpenFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = fileName ?: "No file selected",
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        OutlinedButton(
            onClick = onOpenFile,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text("Open", fontSize = 11.sp)
        }
    }
}

@Composable
private fun DeviceSelector(
    devices: List<Mixer.Info>,
    selectedDevice: Mixer.Info?,
    onDeviceSelected: (Mixer.Info?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(
                text = selectedDevice?.name ?: "Default Device",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 12.sp
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Default Device", fontSize = 12.sp) },
                onClick = {
                    onDeviceSelected(null)
                    expanded = false
                }
            )
            devices.forEach { device ->
                DropdownMenuItem(
                    text = { Text(device.name, fontSize = 12.sp) },
                    onClick = {
                        onDeviceSelected(device)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ThemeSelector(
    themePreset: ThemePreset,
    onThemeChanged: (ThemePreset) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FilledTonalButton(
            onClick = { expanded = true },
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(themePreset.label, fontSize = 11.sp)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ThemePreset.entries.forEach { preset ->
                DropdownMenuItem(
                    text = {
                        Text(
                            preset.label,
                            fontSize = 12.sp,
                            color = if (preset == themePreset) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        onThemeChanged(preset)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun GainSlider(
    gain: Float,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Gain",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            value = gain,
            onValueChange = onGainChange,
            valueRange = 0f..5f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = String.format("%.1fx", gain),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(36.dp)
        )
    }
}
