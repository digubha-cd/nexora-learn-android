package com.example.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * NEXORA LEARN — THREE-THEME APPEARANCE SYSTEM
 * 1. Light (Focus Mode)
 * 2. Deep Navy
 * 3. Navy Night
 */

private val NexoraLightColorScheme = lightColorScheme(
    primary = Color(0xFF315FEA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = Color(0xFF5B21B6),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color.White,
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF202124),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF202124),
    surfaceVariant = Color(0xFFF5F7FB),
    onSurfaceVariant = Color(0xFF5F6368),
    outline = Color(0xFFE0E5ED),
    error = NexoraError,
    onError = Color.White
)

private val NexoraDeepNavyColorScheme = darkColorScheme(
    primary = Color(0xFF82B4FF),
    onPrimary = Color(0xFF101B33),
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFBFDBFE),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF101B33),
    secondaryContainer = Color(0xFF3B2E63),
    onSecondaryContainer = Color(0xFFE9D5FF),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF101B33),
    background = Color(0xFF101B33),
    onBackground = Color(0xFFF5F7FF),
    surface = Color(0xFF192844),
    onSurface = Color(0xFFF5F7FF),
    surfaceVariant = Color(0xFF223553),
    onSurfaceVariant = Color(0xFFD0DAEA),
    outline = Color(0xFF405474),
    error = NexoraError,
    onError = Color.White
)

private val NexoraNavyNightColorScheme = darkColorScheme(
    primary = Color(0xFF79A9FF),
    onPrimary = Color(0xFF050914),
    primaryContainer = Color(0xFF172554),
    onPrimaryContainer = Color(0xFF93C5FD),
    secondary = Color(0xFF8B5CF6),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = Color(0xFF06B6D4),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFF050914),
    onBackground = Color(0xFFF8FAFF),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFF8FAFF),
    surfaceVariant = Color(0xFF101A2C),
    onSurfaceVariant = Color(0xFFD0DAEA),
    outline = Color(0xFF29364D),
    error = NexoraError,
    onError = Color.White
)

@Composable
fun NexoraTheme(
    themeMode: String = "LIGHT",
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    
    val colorScheme = when (themeMode) {
        "DEEP_NAVY" -> NexoraDeepNavyColorScheme
        "NAVY_NIGHT" -> NexoraNavyNightColorScheme
        else -> NexoraLightColorScheme
    }

    val resolvedIsDark = themeMode == "DEEP_NAVY" || themeMode == "NAVY_NIGHT" || darkTheme

    val targetBgColor = colorScheme.background

    val animatedBgColor by animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = tween(durationMillis = 400),
        label = "ThemeTransition"
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = animatedBgColor.toArgb()
                window.navigationBarColor = animatedBgColor.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !resolvedIsDark
                controller.isAppearanceLightNavigationBars = !resolvedIsDark
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    NexoraTheme(darkTheme = darkTheme, content = content)
}
