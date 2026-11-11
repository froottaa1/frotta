package com.frotta

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val AppFont = FontFamily(
    Font(R.font.tajawal_regular, FontWeight.Normal),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)

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

private val base = Typography()

private val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = AppFont),
    displayMedium = base.displayMedium.copy(fontFamily = AppFont),
    displaySmall = base.displaySmall.copy(fontFamily = AppFont),
    headlineLarge = base.headlineLarge.copy(fontFamily = AppFont),
    headlineMedium = base.headlineMedium.copy(fontFamily = AppFont),
    headlineSmall = base.headlineSmall.copy(fontFamily = AppFont),
    titleLarge = base.titleLarge.copy(fontFamily = AppFont),
    titleMedium = base.titleMedium.copy(fontFamily = AppFont),
    titleSmall = base.titleSmall.copy(fontFamily = AppFont),
    bodyLarge = base.bodyLarge.copy(fontFamily = AppFont),
    bodyMedium = base.bodyMedium.copy(fontFamily = AppFont),
    bodySmall = base.bodySmall.copy(fontFamily = AppFont),
    labelLarge = base.labelLarge.copy(fontFamily = AppFont),
    labelMedium = base.labelMedium.copy(fontFamily = AppFont),
    labelSmall = base.labelSmall.copy(fontFamily = AppFont)
)

@Composable
fun FrottaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YellowColors,
        typography = AppTypography,
        content = content
    )
}
