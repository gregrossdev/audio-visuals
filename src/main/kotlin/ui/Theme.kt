package ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7B61FF),
    onPrimary = Color.White,
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE0E0E0),
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0)
)

@Composable
fun AudioVisualsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
