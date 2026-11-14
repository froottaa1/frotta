package com.frotta

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val YellowColors = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFD600),
    primaryContainer = Color(0xFFFFF3B0),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF111111),
    onSecondary = Color(0xFFFFD600),
    background = Color(0xFFFFD600),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFFF4B8),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFFFE866),
    onSurfaceVariant = Color(0xFF111111),
    outline = Color(0xFF111111),
    outlineVariant = Color(0xFF5A4A00),
    error = Color(0xFFB3261E)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
private fun rememberAppFont(): FontFamily {
    val context = LocalContext.current
    return remember(context) {
        try {
            val r = context.resources
            val pkg = context.packageName
            val reg = r.getIdentifier("tajawal_regular", "font", pkg)
            val bold = r.getIdentifier("tajawal_bold", "font", pkg)
            if (reg != 0 && bold != 0) {
                FontFamily(
                    Font(reg, FontWeight.Normal),
                    Font(bold, FontWeight.Bold)
                )
            } else {
                FontFamily.Default
            }
        } catch (e: Exception) {
            FontFamily.Default
        }
    }
}

private fun typographyWith(f: FontFamily): Typography {
    val b = Typography()
    return Typography(
        displayLarge = b.displayLarge.copy(fontFamily = f),
        displayMedium = b.displayMedium.copy(fontFamily = f),
        displaySmall = b.displaySmall.copy(fontFamily = f),
        headlineLarge = b.headlineLarge.copy(fontFamily = f),
        headlineMedium = b.headlineMedium.copy(fontFamily = f),
        headlineSmall = b.headlineSmall.copy(fontFamily = f),
        titleLarge = b.titleLarge.copy(fontFamily = f),
        titleMedium = b.titleMedium.copy(fontFamily = f),
        titleSmall = b.titleSmall.copy(fontFamily = f),
        bodyLarge = b.bodyLarge.copy(fontFamily = f),
        bodyMedium = b.bodyMedium.copy(fontFamily = f),
        bodySmall = b.bodySmall.copy(fontFamily = f),
        labelLarge = b.labelLarge.copy(fontFamily = f),
        labelMedium = b.labelMedium.copy(fontFamily = f),
        labelSmall = b.labelSmall.copy(fontFamily = f)
    )
}

@Composable
fun FrottaTheme(content: @Composable () -> Unit) {
    val font = rememberAppFont()
    val typography = remember(font) { typographyWith(font) }
    MaterialTheme(
        colorScheme = YellowColors,
        typography = typography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFFDE2E),
                        Color(0xFFFFD600),
                        Color(0xFFF7C600)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        content()
    }
}
