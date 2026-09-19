package com.folbazar.admin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Fol Bazar brand palette — derived from the new green/lime admin logo.
private val FolGreen = Color(0xFF00A651)
private val FolGreenDark = Color(0xFF008C45)
private val FolGreenSoft = Color(0xFFE9F9EF)
private val FolLime = Color(0xFFB7F000)

private val LightColors = lightColorScheme(
    primary = FolGreen,
    onPrimary = Color.White,
    primaryContainer = FolGreenSoft,
    onPrimaryContainer = Color(0xFF005A2A),
    secondary = FolGreenDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF5E6),
    onSecondaryContainer = Color(0xFF003D1D),
    tertiary = FolLime,
    onTertiary = Color(0xFF163000),
    background = Color(0xFFF7FBF8),
    surface = Color.White,
    surfaceContainerLow = Color(0xFFF0F8F2)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF39D77A),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF005A2A),
    onPrimaryContainer = Color(0xFFB8F5CA),
    secondary = Color(0xFF5BE68D),
    onSecondary = Color(0xFF00391A),
    secondaryContainer = Color(0xFF174F2B),
    onSecondaryContainer = Color(0xFFB8F5CA),
    tertiary = Color(0xFFB7F000),
    onTertiary = Color(0xFF163000),
    background = Color(0xFF071A10),
    onBackground = Color(0xFFE4F3E8),
    surface = Color(0xFF0D2417),
    onSurface = Color(0xFFE4F3E8),
    surfaceContainerLow = Color(0xFF12301E),
    surfaceVariant = Color(0xFF183C25),
    onSurfaceVariant = Color(0xFFB8CDBE),
    outline = Color(0xFF42634D)
)

@Composable
fun FolBazarTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
