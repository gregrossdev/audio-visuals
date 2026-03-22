package ui

import audio.ReactivityConfig
import kotlinx.serialization.Serializable

@Serializable
data class Preset(
    val name: String,
    val layers: List<VisualizerLayer>,
    val themePreset: ThemePreset,
    val reactivity: ReactivityConfig = ReactivityConfig()
)
