package com.yudemeng.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B6FB5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E7FA),
    onPrimaryContainer = Color(0xFF06283F),
    secondary = Color(0xFF2E7D6F),
    onSecondary = Color.White,
    background = Color(0xFFFDFBFF),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFFDFBFF),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE7EEF5),
    onSurfaceVariant = Color(0xFF42474E),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CCBF3),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF0B4A73),
    onPrimaryContainer = Color(0xFFD3E7FA),
    secondary = Color(0xFF8FD3C4),
    onSecondary = Color(0xFF00382F),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF3F4750),
    onSurfaceVariant = Color(0xFFC0C7CF),
    error = Color(0xFFFFB4AB),
)

@Composable
fun FishDreamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
