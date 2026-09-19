package com.mazur.tarot.data.repository

import com.mazur.tarot.data.local.db.ReadingDao
import com.mazur.tarot.data.local.db.ReadingEntity
import com.mazur.tarot.data.model.ChatTurn
import com.mazur.tarot.data.model.DrawnCard
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/** Wpis dziennika wzbogacony o pełne obiekty [DrawnCard] (nazwa, obraz, interpretacje)
 * oraz pełną historię dopytań (czatu) powiązaną z tym odczytem. */
data class ReadingDetails(
    val id: Long,
    val timestampMillis: Long,
    val spreadType: String,
    val question: String?,
    val drawnCards: List<DrawnCard>,
    val note: String,
    val aiResponse: String?,
    val followUps: List<ChatTurn> = emptyList(),
)

private const val FREE_HISTORY_DAYS = 3L

class JournalRepository(
    private val readingDao: ReadingDao,
    private val cardRepository: CardRepository,
) {

    suspend fun saveReading(
        spreadType: String,
        question: String?,
        drawnCards: List<DrawnCard>,
        aiResponse: String? = null,
        followUps: List<ChatTurn> = emptyList(),
    ): Long {
        val entity = ReadingEntity(
            timestampMillis = System.currentTimeMillis(),
            spreadType = spreadType,
            question = question?.takeIf { it.isNotBlank() },
            cardIdsCsv = ReadingEntity.encodeIds(drawnCards.map { it.card.id }),
            reversedFlagsCsv = ReadingEntity.encodeReversed(drawnCards.map { it.isReversed }),
            positionLabelsCsv = ReadingEntity.encodePositions(drawnCards.map { it.positionLabel ?: "" }),
            note = "",
            aiResponseText = aiResponse?.takeIf { it.isNotBlank() },
            followUpsCsv = ReadingEntity.encodeFollowUps(followUps),
        )
        return readingDao.insert(entity)
    }

    suspend fun updateNote(readingId: Long, note: String) {
        readingDao.updateNote(readingId, note)
    }

    /** Dopisuje najnowszą odpowiedź AI i pełną historię dopytań do już zapisanego odczytu -
     * używane przy auto-zapisie w [com.mazur.tarot.ui.screens.ask.AskCardsViewModel], żeby
     * wpis w Dzienniku rósł razem z konwersacją bez ręcznego "Zakończ i zapisz". */
    suspend fun updateAiResponseAndFollowUps(readingId: Long, aiResponse: String?, followUps: List<ChatTurn>) {
        readingDao.updateAiResponseAndFollowUps(
            readingId,
            aiResponse?.takeIf { it.isNotBlank() },
            ReadingEntity.encodeFollowUps(followUps),
        )
    }

    /** Usuwa całą historię odczytów (Karta Dnia + Zapytaj Kart). Nie dotyczy statusu PRO. */
    suspend fun deleteAllReadings() {
        readingDao.deleteAll()
    }

    /** Zwraca dzisiejsze losowanie Karty Dnia, jeśli już istnieje (limit: 1 losowanie / dobę). */
    suspend fun findTodaysCardOfDay(): ReadingEntity? {
        val startOfDay = startOfTodayMillis()
        return readingDao.findLatestOfType(com.mazur.tarot.data.local.db.SpreadType.CARD_OF_DAY, startOfDay)
    }

    /**
     * Dopisuje kartę dodatkową (Wsparcie / Na Co Uważać) do dzisiejszego wpisu Karty Dnia
     * w Dzienniku, żeby były widoczne w historii razem z główną kartą, a nie tylko "na dziś".
     */
    suspend fun appendCardToTodaysCardOfDay(drawnCard: DrawnCard, positionLabel: String) {
        val entity = findTodaysCardOfDay() ?: return
        val existingIds = entity.cardIds()
        val existingReversed = entity.reversedFlags()
        val existingPositions = entity.positionLabels()
        val paddedPositions = existingIds.indices.map { index -> existingPositions?.getOrNull(index) ?: "" }

        readingDao.update(
            entity.copy(
                cardIdsCsv = ReadingEntity.encodeIds(existingIds + drawnCard.card.id),
                reversedFlagsCsv = ReadingEntity.encodeReversed(existingReversed + drawnCard.isReversed),
                positionLabelsCsv = ReadingEntity.encodePositions(paddedPositions + positionLabel),
            ),
        )
    }

    /**
     * Strumień wpisów dziennika. W wersji darmowej (`isPro == false`) zwracane są
     * wyłącznie losowania z ostatnich [FREE_HISTORY_DAYS] dni.
     */
    fun observeReadings(isPro: Boolean): Flow<List<ReadingDetails>> {
        val source = if (isPro) {
            readingDao.observeAll()
        } else {
            val since = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(FREE_HISTORY_DAYS)
            readingDao.observeSince(since)
        }
        return source.map { entities -> entities.map { it.toDetails() } }
    }

    private suspend fun ReadingEntity.toDetails(): ReadingDetails {
        val allCards = cardRepository.getAllCards().associateBy { it.id }
        val ids = cardIds()
        val reversed = reversedFlags()
        val positions = positionLabels()
        val drawn = ids.indices.mapNotNull { index ->
            val card = allCards[ids[index]] ?: return@mapNotNull null
            DrawnCard(
                card = card,
                isReversed = reversed.getOrElse(index) { false },
                positionLabel = positions?.getOrNull(index)?.takeIf { it.isNotBlank() },
            )
        }
        return ReadingDetails(
            id = id,
            timestampMillis = timestampMillis,
            spreadType = spreadType,
            question = question,
            drawnCards = drawn,
            note = note,
            aiResponse = aiResponseText,
            followUps = followUps(),
        )
    }

    private fun startOfTodayMillis(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
