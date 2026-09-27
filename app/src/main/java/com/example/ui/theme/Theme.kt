package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LunaColorScheme = darkColorScheme(
    primary = LunaCyan,
    onPrimary = LunaSpaceDark,
    primaryContainer = LunaSurfaceElevated,
    onPrimaryContainer = LunaCyan,
    secondary = LunaViolet,
    onSecondary = LunaSpaceDark,
    secondaryContainer = LunaSurfaceElevated,
    onSecondaryContainer = LunaViolet,
    tertiary = LunaMoonGold,
    background = LunaSpaceDark,
    onBackground = LunaTextPrimary,
    surface = LunaSurfaceDark,
    onSurface = LunaTextPrimary,
    surfaceVariant = LunaSurfaceElevated,
    onSurfaceVariant = LunaTextSecondary,
    outline = LunaBorderGlow
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LunaColorScheme,
        typography = Typography,
        content = content
    )
}
