package com.lorryowner.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BhpColorScheme = lightColorScheme(
    primary = BhpDarkForestGreen,
    onPrimary = Color.White,
    primaryContainer = BhpRunningGreenBg,
    onPrimaryContainer = BhpDarkForestGreen,
    secondary = BhpReadyBlue,
    onSecondary = Color.White,
    secondaryContainer = BhpReadyBlueBg,
    onSecondaryContainer = BhpReadyBlueText,
    tertiary = BhpBrightGreen,
    onTertiary = BhpDeepTeal,
    background = BhpScreenBg,
    onBackground = BhpTextPrimary,
    surface = Color.White,
    onSurface = BhpTextPrimary,
    surfaceVariant = Color.White,
    onSurfaceVariant = BhpTextSecondary,
    surfaceContainer = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    surfaceBright = Color.White,
    surfaceDim = Color(0xFFF8FAFC),
    outline = BhpInputBorder,
    outlineVariant = BhpCardBorder,
    error = BhpErrorRed,
    onError = Color.White
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
            window.statusBarColor = BhpNavyDark.toArgb()
            window.navigationBarColor = Color.White.toArgb()
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
