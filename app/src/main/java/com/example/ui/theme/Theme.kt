package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudioColorScheme = darkColorScheme(
    primary = StudioPrimary,
    onPrimary = Color(0xFF041E34),
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFBAE6FD),
    secondary = StudioSecondary,
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = StudioTertiary,
    background = StudioDarkBackground,
    onBackground = StudioTextPrimary,
    surface = StudioDarkSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioDarkCard,
    onSurfaceVariant = StudioTextSecondary,
    error = StudioRecordRed,
    onError = Color.White
)

@Composable
fun WebAudioTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = StudioDarkBackground.toArgb()
                window.navigationBarColor = StudioDarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = StudioColorScheme,
        typography = Typography,
        content = content
    )
}
