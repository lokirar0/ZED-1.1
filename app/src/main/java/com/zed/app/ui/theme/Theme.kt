package com.zed.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ============================================================
// Корневая тема ZED.
// darkTheme управляется из настроек приложения (SYSTEM / LIGHT / DARK).
// ============================================================

@Composable
fun ZedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val zedColors = if (darkTheme) ZedDarkColors else ZedLightColors

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            background = ZedBlack,
            surface = ZedSurface,
            onBackground = ZedTextPrimary,
            onSurface = ZedTextPrimary,
            primary = ZedAccent,
            outline = ZedBorderVisible
        )
    } else {
        lightColorScheme(
            background = ZedLightBackground,
            surface = ZedLightSurface,
            onBackground = ZedLightTextPrimary,
            onSurface = ZedLightTextPrimary,
            primary = ZedLightAccent,
            outline = ZedLightBorderVisible
        )
    }

    // Цвет системных статус-баров под текущую тему
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalZedColors provides zedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ZedTypography,
            content = content
        )
    }
}
