package ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import javax.sound.sampled.Mixer

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
        // Source mode toggle
        SourceModeToggle(
            sourceMode = sourceMode,
            onSourceModeChanged = onSourceModeChanged
        )

        // Source-specific controls
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

        // Gain slider
        GainSlider(
            gain = gain,
            onGainChange = onGainChange,
            modifier = Modifier.weight(1f)
        )

        // Layer indicator
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

        // Log scale toggle
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

        // Theme selector
        ThemeSelector(
            themePreset = themePreset,
            onThemeChanged = onThemeChanged
        )

        // Settings gear toggle
        IconButton(onClick = onSettingsToggle) {
            Text(
                text = "\u2699",
                fontSize = 18.sp,
                color = if (settingsOpen) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Pause/Resume toggle
        IconButton(onClick = onPauseToggle) {
            Text(
                text = if (isPaused) "\u25B6" else "\u23F8",
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

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
