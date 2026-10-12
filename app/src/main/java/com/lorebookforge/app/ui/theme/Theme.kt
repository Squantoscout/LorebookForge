package com.lorebookforge.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BrassLight = Color(0xFFB07D2E)
private val BrassDark = Color(0xFFD9A84F)
private val InkLight = Color(0xFFF2F0E8)
private val InkDark = Color(0xFF15181B)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceDark = Color(0xFF1D2227)
private val OnInkLight = Color(0xFF241F19)
private val OnInkDark = Color(0xFFECE8DF)

private val LightColors = lightColorScheme(
    primary = BrassLight,
    onPrimary = Color.White,
    background = InkLight,
    onBackground = OnInkLight,
    surface = SurfaceLight,
    onSurface = OnInkLight,
    surfaceVariant = Color(0xFFEDEAE0),
    onSurfaceVariant = Color(0xFF6F6859)
)

private val DarkColors = darkColorScheme(
    primary = BrassDark,
    onPrimary = Color(0xFF1C140A),
    background = InkDark,
    onBackground = OnInkDark,
    surface = SurfaceDark,
    onSurface = OnInkDark,
    surfaceVariant = Color(0xFF242A31),
    onSurfaceVariant = Color(0xFF9BA1A8)
)

@Composable
fun LorebookForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
