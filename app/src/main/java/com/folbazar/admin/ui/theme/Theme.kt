package com.folbazar.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Red = Color(0xFFD32F2F)

private val FolBazarColorScheme = lightColorScheme(
    primary = Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEBEE),
    onPrimaryContainer = Color(0xFF8B0000),
    background = Color(0xFFF8F8F8),
    surface = Color.White
)

@Composable
fun FolBazarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FolBazarColorScheme,
        content = content
    )
}
