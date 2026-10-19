package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeAccent(val displayName: String, val primary: Color, val secondary: Color) {
    NEON_PURPLE("Neon Purple", VantaPrimary, AccentNeonCyan),
    NEON_CYAN("Neon Cyan", AccentNeonCyan, VantaPrimary),
    ELECTRIC_LIME("Electric Lime", AccentElectricLime, VantaTertiary),
    SUNSET_ORANGE("Sunset Orange", AccentSunsetOrange, VantaTertiary)
}

@Composable
fun VantaWaveTheme(
    accent: ThemeAccent = ThemeAccent.NEON_PURPLE,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = accent.primary,
        onPrimary = Color.White,
        primaryContainer = VantaSurfaceElevated,
        onPrimaryContainer = accent.primary,
        secondary = accent.secondary,
        onSecondary = Color.Black,
        secondaryContainer = VantaSurfaceVariant,
        onSecondaryContainer = accent.secondary,
        tertiary = VantaTertiary,
        onTertiary = Color.White,
        background = VantaBackground,
        onBackground = VantaTextPrimary,
        surface = VantaSurface,
        onSurface = VantaTextPrimary,
        surfaceVariant = VantaSurfaceVariant,
        onSurfaceVariant = VantaTextSecondary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
