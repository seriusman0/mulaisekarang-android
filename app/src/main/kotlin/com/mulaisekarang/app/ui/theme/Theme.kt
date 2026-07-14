package com.mulaisekarang.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E9F7),
    onPrimaryContainer = PrimaryDark,
    secondary = PrimaryDark,
    onSecondary = Color.White,
    tertiary = Accent,
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = BorderLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = BorderMedium,
    outlineVariant = BorderLight,
    error = Error,
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7FB8E0),
    onPrimary = Color(0xFF00344F),
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color(0xFFD6E9F7),
    secondary = Color(0xFF9CC9E8),
    onSecondary = Color(0xFF00344F),
    tertiary = Accent,
    onTertiary = Color(0xFF3D2100),
    background = GlassSurfaceDark,
    onBackground = Color(0xFFE7E9EE),
    surface = Color(0xFF161C33),
    onSurface = Color(0xFFE7E9EE),
    surfaceVariant = Color(0xFF232B47),
    onSurfaceVariant = Color(0xFFAAB0C4),
    outline = Color(0xFF3A4262),
    outlineVariant = Color(0xFF232B47),
    error = Color(0xFFE57373),
    onError = Color(0xFF3B0A0A),
)

@Composable
fun MulaiSekarangTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = MulaiSekarangShapes,
        content = content,
    )
}
