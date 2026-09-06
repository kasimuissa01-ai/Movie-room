package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CinematicColorScheme = darkColorScheme(
    primary = CineRedPrimary,
    onPrimary = Color.White,
    primaryContainer = CineRedDark,
    onPrimaryContainer = Color.White,
    secondary = CineRedBright,
    onSecondary = Color.White,
    secondaryContainer = CineSurfaceElevated,
    onSecondaryContainer = CineTextPrimary,
    tertiary = CineGold,
    onTertiary = Color.Black,
    background = CineBlack,
    onBackground = CineTextPrimary,
    surface = CineSurface,
    onSurface = CineTextPrimary,
    surfaceVariant = CineSurfaceVariant,
    onSurfaceVariant = CineTextSecondary,
    outline = CineCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CinematicColorScheme,
        typography = Typography,
        content = content
    )
}
