package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MotoDarkColorScheme = darkColorScheme(
    primary = MotoCyan,
    onPrimary = MotoBackground,
    primaryContainer = MotoCardBgElevated,
    onPrimaryContainer = MotoCyan,
    secondary = MotoViolet,
    onSecondary = MotoBackground,
    secondaryContainer = MotoCardBg,
    onSecondaryContainer = MotoViolet,
    tertiary = MotoPink,
    onTertiary = MotoBackground,
    background = MotoBackground,
    onBackground = MotoTextPrimary,
    surface = MotoSurface,
    onSurface = MotoTextPrimary,
    surfaceVariant = MotoCardBg,
    onSurfaceVariant = MotoTextSecondary,
    outline = MotoBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = MotoBackground.toArgb()
                window.navigationBarColor = MotoBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = MotoDarkColorScheme,
        typography = Typography,
        content = content
    )
}
