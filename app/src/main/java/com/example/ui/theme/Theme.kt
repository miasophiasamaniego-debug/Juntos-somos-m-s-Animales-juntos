package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = VibrantPrimary,
    primaryContainer = VibrantPrimaryContainer,
    onPrimaryContainer = VibrantOnPrimaryContainer,
    secondary = VibrantEcoGreen,
    background = VibrantBackground,
    surface = VibrantSurface,
    onBackground = VibrantTextPrimary,
    onSurface = VibrantTextPrimary,
    errorContainer = VibrantHeartContainer,
    onErrorContainer = VibrantOnHeartContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = VibrantPrimaryContainer,
    primaryContainer = VibrantPrimary,
    secondary = VibrantEcoGreen,
    background = VibrantControllerBg,
    surface = VibrantControllerBg,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun AnimalEcoJumpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
