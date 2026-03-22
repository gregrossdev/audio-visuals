package ui

import kotlinx.serialization.Serializable

@Serializable
enum class VisualizationMode { BARS, WAVEFORM, CIRCULAR, PARTICLES, KALEIDOSCOPE, TRAILS, MANDALA }
