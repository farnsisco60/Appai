package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = AccentCyan,
    onPrimary = Color(0xFF041E19),
    primaryContainer = Color(0xFF143E36),
    onPrimaryContainer = Color(0xFFA2FBEF),
    secondary = AccentPurple,
    onSecondary = Color(0xFF1A0A45),
    secondaryContainer = Color(0xFF2C1F61),
    onSecondaryContainer = Color(0xFFE2DCFF),
    background = StudioDarkBg,
    onBackground = TextPrimary,
    surface = StudioSurface,
    onSurface = TextPrimary,
    surfaceVariant = StudioSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = StudioCardBorder,
  )

@Composable
fun GhostStudioTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = DarkColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  GhostStudioTheme(content = content)
}

