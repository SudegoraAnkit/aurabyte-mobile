package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.infrastructure.adapters.ui.ThemeMode

// Cyberpunk Dark Scheme
private val CyberColorScheme = darkColorScheme(
    primary = CyberPrimary,
    secondary = CyberSecondary,
    tertiary = CyberTertiary,
    background = CyberBg,
    surface = CyberCard,
    onPrimary = CyberBg,
    onSecondary = CyberText,
    onBackground = CyberText,
    onSurface = CyberText
)

// Sunset Warm Dusk Scheme
private val SunsetColorScheme = darkColorScheme(
    primary = SunsetPrimary,
    secondary = SunsetSecondary,
    tertiary = SunsetTertiary,
    background = SunsetBg,
    surface = SunsetCard,
    onPrimary = SunsetBg,
    onSecondary = SunsetText,
    onBackground = SunsetText,
    onSurface = SunsetText
)

// Monochrome Dusk Scheme
private val MonochromeColorScheme = darkColorScheme(
    primary = Color.White,
    secondary = Color(0xFF888888),
    tertiary = Color(0xFF444444),
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun HabitEngineTheme(
    themeMode: ThemeMode = ThemeMode.CYBERPUNK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.CYBERPUNK -> CyberColorScheme
        ThemeMode.SUNSET -> SunsetColorScheme
        ThemeMode.MONOCHROME -> MonochromeColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
