package com.mazur.tarot.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val MysticDarkColorScheme = darkColorScheme(
    primary = MysticPrimary,
    onPrimary = MysticBackground,
    primaryContainer = MysticPrimaryVariant,
    onPrimaryContainer = MysticOnBackground,
    secondary = MysticSecondary,
    onSecondary = MysticBackground,
    background = MysticBackground,
    onBackground = MysticOnBackground,
    surface = MysticSurface,
    onSurface = MysticOnBackground,
    surfaceVariant = MysticSurfaceVariant,
    onSurfaceVariant = MysticOnSurfaceMuted,
    error = MysticError,
    outline = MysticOutline,
)

/**
 * Aplikacja ma zawsze ciemny, mistyczny motyw - niezależnie od ustawień systemowych,
 * zgodnie z wymaganiem estetyki tarota.
 */
@Composable
fun TarotAppTheme(content: @Composable () -> Unit) {
    val colorScheme = MysticDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TarotTypography,
        content = content,
    )
}
