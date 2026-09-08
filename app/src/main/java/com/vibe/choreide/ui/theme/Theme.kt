package com.vibe.choreide.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ChoreColorScheme = darkColorScheme(
    primary = IdePrimary,
    background = IdeBackground,
    surface = IdeSurface,
    surfaceVariant = IdeSurfaceVariant,
    outline = IdeBorder,
    onBackground = IdeOnBackground,
    onSurface = IdeOnSurface,
    onSurfaceVariant = IdeOnSurfaceVariant,
    secondary = IdeSecondary,
    error = IdeError
)

@Composable
fun ChoreIDETheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ChoreColorScheme,
        content = content
    )
}
