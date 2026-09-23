package com.kruju.habits.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A dark, high-contrast look: near-black background, charcoal surfaces and one hot-pink accent.
object Palette {
    val Background = Color(0xFF111111)
    val Surface = Color(0xFF1B1B1B)
    val Card = Color(0xFF252525)
    val CardHigh = Color(0xFF2F2F2F)
    val Divider = Color(0xFF2A2A2A)
    val Accent = Color(0xFFE8175D)
    val AccentDim = Color(0xFF74102F)
    val Text = Color(0xFFF2F2F2)
    val TextSoft = Color(0xFF9E9E9E)
    val TextFaint = Color(0xFF5E5E5E)
    val Success = Color(0xFF3DBE6B)
    val Fail = Color(0xFFE5484D)
}

val CategoryColors = listOf(
    0xFFE8175D, 0xFFF4511E, 0xFFFB8C00, 0xFFFFB300, 0xFF7CB342, 0xFF43A047,
    0xFF26A69A, 0xFF00ACC1, 0xFF1E88E5, 0xFF5C6BC0, 0xFFAB47BC, 0xFF8D6E63,
)

@Composable
fun HabitTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Palette.Accent,
        onPrimary = Color.White,
        primaryContainer = Palette.AccentDim,
        onPrimaryContainer = Color.White,
        secondary = Palette.Accent,
        onSecondary = Color.White,
        secondaryContainer = Palette.AccentDim,
        onSecondaryContainer = Color.White,
        background = Palette.Background,
        onBackground = Palette.Text,
        surface = Palette.Background,
        onSurface = Palette.Text,
        surfaceVariant = Palette.Card,
        onSurfaceVariant = Palette.TextSoft,
        surfaceContainerLowest = Palette.Background,
        surfaceContainerLow = Palette.Surface,
        surfaceContainer = Palette.Surface,
        surfaceContainerHigh = Palette.Card,
        surfaceContainerHighest = Palette.CardHigh,
        outline = Palette.TextFaint,
        outlineVariant = Palette.Divider,
        error = Palette.Fail,
    )
    MaterialTheme(colorScheme = colors, content = content)
}
