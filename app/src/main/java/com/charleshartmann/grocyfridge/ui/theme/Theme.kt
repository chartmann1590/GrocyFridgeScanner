package com.charleshartmann.grocyfridge.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Teal40,
    onPrimary = Neutral99,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Green40,
    onSecondary = Neutral99,
    secondaryContainer = Green90,
    onSecondaryContainer = Green10,
    tertiary = Emerald40,
    onTertiary = Neutral99,
    background = SurfaceBright,
    onBackground = Neutral10,
    surface = SurfaceBright,
    onSurface = Neutral10,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = Neutral20,
    outline = Neutral90,
    outlineVariant = Neutral95,
    surfaceDim = SurfaceDim,
    surfaceBright = SurfaceBright,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal10,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Green80,
    onSecondary = Green20,
    secondaryContainer = Green30,
    onSecondaryContainer = Green90,
    tertiary = Emerald80,
    onTertiary = Green10,
    background = Sage10,
    onBackground = Neutral90,
    surface = Sage10,
    onSurface = Neutral90,
    surfaceVariant = Sage20,
    onSurfaceVariant = Neutral90,
    outline = Neutral20,
    outlineVariant = Neutral20,
    surfaceDim = Sage10,
    surfaceBright = Neutral20,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun GrocyFridgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GrocyTypography,
        content = content
    )
}
