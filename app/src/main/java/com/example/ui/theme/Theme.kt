package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val DarkColorScheme = darkColorScheme(
    primary = CfOrange,
    onPrimary = Color.Black,
    primaryContainer = CfOrangeDark,
    onPrimaryContainer = Color.White,
    secondary = AmberAccent,
    onSecondary = Color.Black,
    surface = DarkSurface,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = DarkCard,
    onSurfaceVariant = Color(0xFF94A3B8),
    background = DarkBackground,
    onBackground = Color(0xFFF8FAFC),
    outline = DarkCardBorder,
    outlineVariant = Color(0xFF1E2536),
    error = StatusRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = CfOrange,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEAD6),
    onPrimaryContainer = CfOrangeDark,
    secondary = AmberAccent,
    onSecondary = Color.Black,
    surface = LightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LightCard,
    onSurfaceVariant = Color(0xFF475569),
    background = LightBackground,
    onBackground = Color(0xFF020617),
    outline = LightCardBorder,
    outlineVariant = Color(0xFFF1F5F9),
    error = StatusRed,
    onError = Color.White
)

@Composable
fun CleanIpScannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isFa: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val typography = remember(isFa) {
        if (isFa) createAppTypography(VazirmatnFontFamily) else createAppTypography(FontFamily.Default)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}

