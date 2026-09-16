package com.folbazar.admin.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Red=Color(0xFFD32F2F)
private val Scheme=lightColorScheme(primary=Red,onPrimary=Color.White,primaryContainer=Color(0xFFFFEBEE),onPrimaryContainer=Color(0xFF8B0000),background=Color(0xFFF8F8F8),surface=Color.White)
@Composable fun FolBazarTheme(content:@Composable()->Unit){MaterialTheme(colorScheme=Scheme,content=content)}
