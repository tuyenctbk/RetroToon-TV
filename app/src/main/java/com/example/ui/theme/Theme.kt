package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinemaColorScheme = darkColorScheme(
    primary = RetroGold,
    onPrimary = CinemaBlack,
    primaryContainer = CinemaSurfaceVariant,
    onPrimaryContainer = RetroGoldLight,
    secondary = RetroRuby,
    onSecondary = Color.White,
    secondaryContainer = RetroRubyDark,
    onSecondaryContainer = RetroCream,
    tertiary = RetroTeal,
    onTertiary = Color.White,
    background = CinemaBlack,
    onBackground = CinemaTextPrimary,
    surface = CinemaDark,
    onSurface = CinemaTextPrimary,
    surfaceVariant = CinemaSurface,
    onSurfaceVariant = CinemaTextSecondary,
    outline = CinemaBorder
)

@Composable
fun RetroToonTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CinemaColorScheme,
        typography = Typography,
        content = content
    )
}
