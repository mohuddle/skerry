package io.github.mohuddle.skerry.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SkerryColors = darkColorScheme(
    background = Color(0xFF12110F),
    surface = Color(0xFF1C1A17),
    primary = Color(0xFFE7E1D6),
    onBackground = Color(0xFFF4F1EA),
    onSurface = Color(0xFFF4F1EA),
    onPrimary = Color(0xFF1A1814),
)

@Composable
fun SkerryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SkerryColors,
        content = content,
    )
}
