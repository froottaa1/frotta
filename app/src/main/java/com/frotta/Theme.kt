package com.frotta

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val YellowColors = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFD600),
    primaryContainer = Color(0xFFFFF3B0),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF111111),
    onSecondary = Color(0xFFFFD600),
    background = Color(0xFFFFD600),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFD600),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFFFE866),
    onSurfaceVariant = Color(0xFF111111),
    outline = Color(0xFF111111),
    error = Color(0xFFB3261E)
)

@Composable
fun FrottaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = YellowColors, content = content)
}
