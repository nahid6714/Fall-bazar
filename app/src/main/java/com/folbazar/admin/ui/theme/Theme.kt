package com.folbazar.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Red = Color(0xFFDF2D4D)

private val LightColors = lightColorScheme(
    primary = Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = Color(0xFF8B0000),
    background = Color(0xFFF8F8F8),
    surface = Color.White,
    surfaceContainerLow = Color(0xFFF3F1F1)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6B85),
    onPrimary = Color(0xFF3A0012),
    primaryContainer = Color(0xFF5C1224),
    onPrimaryContainer = Color(0xFFFFD9DF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFEAEAEA),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFEAEAEA),
    surfaceContainerLow = Color(0xFF242426),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFC7C7C9),
    outline = Color(0xFF444448)
)

/**
 * @param darkTheme defaults to following the system setting; pass an explicit value
 * when the admin has chosen Light/Dark manually (see ThemePrefs).
 */
@Composable
fun FolBazarTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
