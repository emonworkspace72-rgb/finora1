package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = FinoraNeonLime,
    onPrimary = Color(0xFF0F1A06),
    primaryContainer = FinoraCardGlow,
    onPrimaryContainer = FinoraLimeBright,
    secondary = FinoraLimeBright,
    onSecondary = Color(0xFF0C100D),
    background = FinoraDarkBg,
    onBackground = FinoraDarkTextPrimary,
    surface = FinoraSurface,
    onSurface = FinoraDarkTextPrimary,
    surfaceVariant = FinoraSurfaceHigh,
    onSurfaceVariant = FinoraDarkTextSecondary,
    outline = FinoraDarkBorder,
    error = FinoraExpenseRed,
    onError = Color(0xFF380710)
)

private val LightColorScheme = lightColorScheme(
    primary = FinoraLightPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCFCE7),
    onPrimaryContainer = Color(0xFF14532D),
    secondary = FinoraLightAccent,
    onSecondary = Color.White,
    background = FinoraLightBg,
    onBackground = FinoraLightTextPrimary,
    surface = FinoraLightSurface,
    onSurface = FinoraLightTextPrimary,
    surfaceVariant = FinoraLightSurfaceHigh,
    onSurfaceVariant = FinoraLightTextSecondary,
    outline = FinoraLightBorder,
    error = FinoraLightExpense,
    onError = Color.White
)

@Composable
fun FinoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = Color.Transparent.toArgb()
                window.navigationBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
