package ui

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class VisualizerConfig {
    abstract val label: String
}

@Serializable
@SerialName("bars")
data class BarsConfig(
    override val label: String = "Bars"
) : VisualizerConfig()

@Serializable
@SerialName("waveform")
data class WaveformConfig(
    override val label: String = "Waveform"
) : VisualizerConfig()

@Serializable
@SerialName("circular")
data class CircularConfig(
    override val label: String = "Circular"
) : VisualizerConfig()

@Serializable
@SerialName("particles")
data class ParticleConfig(
    val maxParticles: Int = 800,
    val bassSpawnRate: Int = 6,
    val midSpawnRate: Int = 4,
    val highSpawnRate: Int = 4,
    val beatBurstCount: Int = 25,
    val drag: Float = 0.985f,
    val lifeDecay: Float = 0.015f,
    val bassSpeedMin: Float = 1.5f,
    val bassSpeedScale: Float = 2f,
    val bassSizeMin: Float = 5f,
    val bassSizeScale: Float = 6f,
    val midSpeedMin: Float = 2.5f,
    val midSpeedScale: Float = 3f,
    val midSizeMin: Float = 3f,
    val midSizeScale: Float = 3f,
    val highSpeedMin: Float = 4f,
    val highSpeedScale: Float = 4f,
    val highSizeMin: Float = 1.5f,
    val highSizeScale: Float = 2f,
    override val label: String = "Particles"
) : VisualizerConfig()

@Serializable
@SerialName("kaleidoscope")
data class KaleidoscopeConfig(
    val segments: Int = 8,
    val shapesPerWedge: Int = 12,
    val baseRotationSpeed: Float = 0.2f,
    val energyRotationScale: Float = 1.5f,
    val beatSpeedBoost: Float = 3f,
    val breathMin: Float = 0.9f,
    val breathScale: Float = 0.25f,
    val breathEasing: Float = 0.15f,
    override val label: String = "Kaleidoscope"
) : VisualizerConfig()

@Serializable
@SerialName("trails")
data class TrailsConfig(
    val historySize: Int = 40,
    val pointsPerFrame: Int = 30,
    val verticalSpread: Float = 0.7f,
    val topMargin: Float = 0.05f,
    val hueFadeSpeed: Float = 0.6f,
    val saturationFade: Float = 0.3f,
    val lightnessFade: Float = 0.5f,
    override val label: String = "Trails"
) : VisualizerConfig()

@Serializable
@SerialName("mandala")
data class MandalaConfig(
    val rings: Int = 4,
    val elementsPerRing: Int = 12,
    val shapeType: MandalaShape = MandalaShape.CIRCLE,
    val innerRotationSpeed: Float = 0.3f,
    val outerRotationSpeed: Float = 0.8f,
    val pulseIntensity: Float = 0.3f,
    val showConnections: Boolean = true,
    val connectionAlpha: Float = 0.4f,
    override val label: String = "Mandala"
) : VisualizerConfig()

@Serializable
enum class MandalaShape(val label: String) {
    CIRCLE("Circle"),
    DIAMOND("Diamond"),
    TRIANGLE("Triangle"),
    LINE("Line")
}

@Serializable
@SerialName("flow_field")
data class FlowFieldConfig(
    val particleCount: Int = 3000,
    val noiseScale: Float = 0.003f,
    val fieldSpeed: Float = 0.01f,
    val particleDrag: Float = 0.98f,
    val particleSpeed: Float = 2.0f,
    val trailLength: Int = 5,
    val beatTimeJump: Float = 2.0f,
    val turbulenceMin: Float = 0.001f,
    val turbulenceMax: Float = 0.008f,
    override val label: String = "Flow Field"
) : VisualizerConfig()
