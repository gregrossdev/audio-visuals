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

@Composable
fun ControlBar(
    devices: List<Mixer.Info>,
    selectedDevice: Mixer.Info?,
    onDeviceSelected: (Mixer.Info?) -> Unit,
    gain: Float,
    onGainChange: (Float) -> Unit,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Device dropdown
        DeviceSelector(
            devices = devices,
            selectedDevice = selectedDevice,
            onDeviceSelected = onDeviceSelected,
            modifier = Modifier.weight(1f)
        )

        // Gain slider
        GainSlider(
            gain = gain,
            onGainChange = onGainChange,
            modifier = Modifier.weight(1f)
        )

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
