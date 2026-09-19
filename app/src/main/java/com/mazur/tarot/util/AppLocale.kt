package com.mazur.tarot.util

import android.content.Context
import java.util.Locale

/**
 * Rozwiązuje język treści aplikacji (karty, daty, prompt AI) wg tej samej reguły co
 * zasoby [com.mazur.tarot.R.string]: polski i hiszpański mają własną wersję, każdy inny
 * język systemu spada na angielski.
 */
fun resolvedAppLocale(context: Context): Locale {
    val language = context.resources.configuration.locales[0].language
    return when (language) {
        "pl" -> Locale("pl", "PL")
        "es" -> Locale("es", "ES")
        else -> Locale.ENGLISH
    }
}
