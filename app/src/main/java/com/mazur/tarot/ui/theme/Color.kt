package com.mazur.tarot.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta mistyczna - "stara księga Tarota + nocne niebo". Fiolet = interakcja/energia,
// złoto = akcent Tarota/nagłówki, krem = tekst, ciemny fiolet = powierzchnie.
val MysticBackground = Color(0xFF090718)
val MysticBackgroundAlt = Color(0xFF120B26)
val MysticPanel = Color(0xFF18112F)
val MysticPanelSecondary = Color(0xFF21173D)

val MysticPurple = Color(0xFF8B5CF6)
val MysticLightPurple = Color(0xFFA78BFA)

val MysticGold = Color(0xFFD4AF37)
val MysticLightGold = Color(0xFFE6C85C)

// Jaśniejsze, bardziej kontrastowe złoto - dedykowane nagłówkom ekranów i nazwom kart,
// by zawsze wyraźnie odcinały się od ciemnego tła.
val MysticHeadingGold = Color(0xFFFFD700)

val MysticTextPrimary = Color(0xFFF2EEFF)
val MysticTextSecondary = Color(0xFFB8B0D0)

val MysticError = Color(0xFFB08CA0)

// Aliasy zachowane dla czytelności w miejscach, gdzie nazwa "Mystic*Surface*" już
// występowała w kodzie (Theme.kt buduje na nich ColorScheme Material3).
val MysticSurface = MysticPanel
val MysticSurfaceVariant = MysticPanelSecondary
val MysticPrimary = MysticPurple
val MysticPrimaryVariant = MysticLightPurple
val MysticSecondary = MysticGold
val MysticOnBackground = MysticTextPrimary
val MysticOnSurfaceMuted = MysticTextSecondary
val MysticOutline = Color(0xFFA78BFA).copy(alpha = 0.16f)
