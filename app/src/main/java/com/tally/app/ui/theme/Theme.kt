package com.tally.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Palette drawn from the Tally logo: vivid blue primary, fresh green accent.
private val Blue = Color(0xFF1D63E8)
private val BlueDeep = Color(0xFF0C44E8)
private val Green = Color(0xFF16A34A)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBE6FF),
    onPrimaryContainer = Color(0xFF0A2E7A),
    secondary = Green,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFF3DC),
    onSecondaryContainer = Color(0xFF064023),
    tertiary = Color(0xFF2E7BE8),
    background = Color(0xFFF4F7FD),
    onBackground = Color(0xFF121622),
    surface = Color.White,
    onSurface = Color(0xFF121622),
    surfaceVariant = Color(0xFFE9EFF8),
    onSurfaceVariant = Color(0xFF55606F),
    outline = Color(0xFFC6D1E2),
    outlineVariant = Color(0xFFDCE4F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFCE4E4),
    onErrorContainer = Color(0xFF7F1D1D)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF7CA9FF),
    onPrimary = Color(0xFF07205C),
    primaryContainer = Color(0xFF16398C),
    onPrimaryContainer = Color(0xFFD9E4FF),
    secondary = Color(0xFF4ADE80),
    onSecondary = Color(0xFF053019),
    secondaryContainer = Color(0xFF115030),
    onSecondaryContainer = Color(0xFFBFF3D2),
    tertiary = Color(0xFF8CC0FF),
    background = Color(0xFF0B0F17),
    onBackground = Color(0xFFE7ECF4),
    surface = Color(0xFF131A26),
    onSurface = Color(0xFFE7ECF4),
    surfaceVariant = Color(0xFF1E2838),
    onSurfaceVariant = Color(0xFF9FB0C2),
    outline = Color(0xFF35445A),
    outlineVariant = Color(0xFF263347),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF5B1A1A),
    onErrorContainer = Color(0xFFFFD9D9)
)

@Composable
fun PillSyncTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        window.statusBarColor = colors.background.toArgb()
        window.navigationBarColor = colors.background.toArgb()
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = !darkTheme
        controller.isAppearanceLightNavigationBars = !darkTheme
    }
    MaterialTheme(colorScheme = colors, content = content)
}
