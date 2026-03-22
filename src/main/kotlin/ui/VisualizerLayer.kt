package ui

import androidx.compose.ui.graphics.BlendMode
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class LayerBlendMode(val label: String) {
    NORMAL("Normal"),
    SCREEN("Screen"),
    LIGHTEN("Lighten"),
    MULTIPLY("Multiply"),
    OVERLAY("Overlay"),
    DIFFERENCE("Difference"),
    COLOR_DODGE("Dodge"),
    PLUS("Plus");

    fun toComposeBlendMode(): BlendMode = when (this) {
        NORMAL -> BlendMode.SrcOver
        SCREEN -> BlendMode.Screen
        LIGHTEN -> BlendMode.Lighten
        MULTIPLY -> BlendMode.Multiply
        OVERLAY -> BlendMode.Overlay
        DIFFERENCE -> BlendMode.Difference
        COLOR_DODGE -> BlendMode.ColorDodge
        PLUS -> BlendMode.Plus
    }
}

@Serializable
data class VisualizerLayer(
    val id: String,
    val vizMode: VisualizationMode,
    val config: VisualizerConfig,
    val opacity: Float = 1.0f,
    val blendMode: LayerBlendMode = LayerBlendMode.SCREEN,
    val enabled: Boolean = true
)

fun defaultLayer(): VisualizerLayer = VisualizerLayer(
    id = generateLayerId(),
    vizMode = VisualizationMode.BARS,
    config = BarsConfig(),
    opacity = 1.0f,
    blendMode = LayerBlendMode.NORMAL
)

fun generateLayerId(): String = UUID.randomUUID().toString().take(8)
