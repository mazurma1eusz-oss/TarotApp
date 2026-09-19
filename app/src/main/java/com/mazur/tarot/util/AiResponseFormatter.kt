package com.mazur.tarot.util

import android.content.Context
import com.mazur.tarot.R

/** Jedna sekcja odpowiedzi AI: opcjonalny nagłówek (np. "Wgląd Kart") i treść pod nim. */
data class AiResponseSection(val heading: String?, val body: String)

/**
 * Rozbija surowy tekst odpowiedzi modelu na sekcje z nagłówkami, usuwając przy okazji
 * znaki Markdown ('**', '*'), których model mimo instrukcji czasem wciąż używa. Rozpoznaje
 * ustalone, NIEtłumaczone tagi strukturalne ('[WGLĄD]' / '[PRZESŁANIE]', zawsze emitowane
 * przez model w tej formie niezależnie od języka odpowiedzi - patrz system prompt w
 * [com.mazur.tarot.ai.AiReadingRepository]) oraz starszy format nagłówków tekstowych
 * ("Wgląd Kart" / "Przesłanie na Ścieżkę" / "Krótki Wniosek" / "Wskazówka Działania") dla
 * wstecznej zgodności z odczytami zapisanymi wcześniej w Dzienniku, i zawsze wyświetla je
 * pod nagłówkami przetłumaczonymi na aktualny język aplikacji (patrz [canonicalHeading]).
 */
object AiResponseFormatter {

    private val headingRegex = Regex(
        "(?i)\\[?(WGLĄD|PRZESŁANIE|Wgląd Kart|Przesłanie na Ścieżkę|Krótki Wniosek|Wskazówka Działania)\\]?\\s*:?",
    )

    fun parse(raw: String, context: Context): List<AiResponseSection> {
        val cleaned = raw.replace("*", "").replace("#", "").trim()
        if (cleaned.isEmpty()) return emptyList()

        val matches = headingRegex.findAll(cleaned).toList()
        if (matches.isEmpty()) return listOf(AiResponseSection(null, cleaned))

        val sections = mutableListOf<AiResponseSection>()
        val preamble = cleaned.substring(0, matches.first().range.first).trim()
        if (preamble.isNotEmpty()) sections.add(AiResponseSection(null, preamble))

        matches.forEachIndexed { index, match ->
            val heading = canonicalHeading(match.groupValues[1], context)
            val bodyStart = match.range.last + 1
            val bodyEnd = matches.getOrNull(index + 1)?.range?.first ?: cleaned.length
            val body = cleaned.substring(bodyStart, bodyEnd).trim()
            if (body.isNotEmpty()) sections.add(AiResponseSection(heading, body))
        }
        return sections
    }

    private fun canonicalHeading(raw: String, context: Context): String {
        val normalized = raw.trim().uppercase()
        return when {
            normalized.startsWith("WGLĄD") || normalized.startsWith("KRÓTKI WNIOSEK") ->
                context.getString(R.string.ai_heading_insight)
            normalized.startsWith("PRZESŁANIE") || normalized.startsWith("WSKAZÓWKA") ->
                context.getString(R.string.ai_heading_message)
            else -> raw.trim().replaceFirstChar { it.uppercase() }
        }
    }
}
