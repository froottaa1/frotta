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
import androidx.compose.material3.darkColorScheme
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

object Pal {
    val dark = true

    val accent = Color(0xFFFFE0E6)
    val ink = Color(0xFF2A0A14)

    val page = Color(0xFF0E0E0E)
    val onPage = Color(0xFFF2F2F2)
    val brand = Color(0xFFE6718C)

    val card = Color(0xFF5E1229)
    val onCardSub = Color(0xFFE9C4CE)

    val soft = Color(0xFF2A141B)
    val onSoft = Color(0xFFF2F2F2)

    val chip = accent
    val onChip = ink
    val onChipSub = Color(0xCC2A0A14)

    val barColor = accent
    val barTrack = Color(0xFF444444)

    val radar = accent

    val modeOnBg = ink
    val modeOnFg = accent
    val modeOffBg = Color(0x22000000)
    val modeOffFg = ink

    fun circleBg(on: Boolean): Color = if (on) ink else accent
    fun onCircle(on: Boolean): Color = if (on) accent else ink
}

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFC2476A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF5E1229),
    onPrimaryContainer = Color(0xFFFFE0E6),
    secondary = Color(0xFFC2476A),
    onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1F1F1F),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF2B2B2B),
    onSurfaceVariant = Color(0xFFC8C8C8),
    outline = Color(0xFF8A6A74),
    outlineVariant = Color(0xFF3C3C3C),
    error = Color(0xFFFF6B60)
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
        colorScheme = DarkScheme,
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
                    listOf(Color(0xFF181818), Color(0xFF0E0E0E), Color(0xFF080808))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        content()
    }
}
