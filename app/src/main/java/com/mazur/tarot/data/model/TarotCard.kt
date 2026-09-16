package com.mazur.tarot.data.model

/**
 * Reprezentuje pojedynczą kartę tarota wczytaną z assets/cards.json.
 */
data class TarotCard(
    val id: Int,
    val name: String,
    val type: CardType,
    val imageResName: String,
    val descriptionGeneral: String,
    val descriptionLove: String,
    val descriptionWork: String,
    val descriptionReversedGeneral: String,
    val descriptionReversedLove: String,
    val descriptionReversedWork: String,
)

/**
 * Wylosowana karta w konkretnym rozkładzie - niesie ze sobą informację
 * o odwróceniu (50% szans) oraz opcjonalną pozycję w rozkładzie
 * (np. "Przeszłość" / "Teraźniejszość" / "Przyszłość").
 */
data class DrawnCard(
    val card: TarotCard,
    val isReversed: Boolean,
    val positionLabel: String? = null,
) {
    val activeDescription: String
        get() = if (isReversed) card.descriptionReversedGeneral else card.descriptionGeneral

    val activeLoveDescription: String
        get() = if (isReversed) card.descriptionReversedLove else card.descriptionLove

    val activeWorkDescription: String
        get() = if (isReversed) card.descriptionReversedWork else card.descriptionWork
}
