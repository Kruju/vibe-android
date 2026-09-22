package com.kruju.habits.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val HabitColors = listOf(
    Color(0xFFFF6B3D),
    Color(0xFF2E9BF0),
    Color(0xFF34B36B),
    Color(0xFF9C5CF5),
    Color(0xFFF5B521),
    Color(0xFFEF4F8B),
    Color(0xFF14B8A6),
    Color(0xFF6B7280),
)

fun habitColor(index: Int): Color = HabitColors[index.mod(HabitColors.size)]

@Composable
fun HabitTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = colors, content = content)
}
