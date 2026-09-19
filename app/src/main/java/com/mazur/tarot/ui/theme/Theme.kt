package com.mazur.tarot.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
            // window.statusBarColor/navigationBarColor są wycofane w Androidzie 15 (wyświetlanie
            // bez ramki jest tam wymuszone dla targetSdk 35+ - te settery są ignorowane, więc
            // Play Console flaguje je jako "wycofane API"). Paski systemowe są już przezroczyste
            // dzięki enableEdgeToEdge() w MainActivity - tu tylko sterujemy KOLOREM ich ikon, żeby
            // były czytelne na naszym zawsze-ciemnym tle.
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = false
            insetsController.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TarotTypography,
        content = content,
    )
}
