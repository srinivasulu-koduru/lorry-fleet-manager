package com.lorryowner.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BhpColorScheme = darkColorScheme(
    primary = BhpDarkForestGreen,
    onPrimary = BhpCardWhite,
    primaryContainer = BhpDarkForestGreen,
    onPrimaryContainer = BhpCardWhite,
    secondary = BhpBrightGreen,
    onSecondary = BhpDeepTeal,
    background = BhpDeepTeal,
    onBackground = BhpCardWhite,
    surface = BhpCardWhite,
    onSurface = BhpTextBlack,
    surfaceVariant = BhpInputBg,
    onSurfaceVariant = BhpTextMuted,
    outline = BhpInputBorder,
    error = BhpErrorRed,
    onError = BhpCardWhite
)

@Composable
fun LorryOwnerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = BhpColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BhpDeepTeal.toArgb()
            window.navigationBarColor = BhpCardWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
