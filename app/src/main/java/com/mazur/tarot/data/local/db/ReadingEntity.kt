package com.mazur.tarot.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mazur.tarot.data.model.ChatTurn

/** Typ rozkładu użytego przy losowaniu - przechowywany jako string dla czytelności w DB. */
object SpreadType {
    const val CARD_OF_DAY = "CARD_OF_DAY"
    const val ONE_CARD = "ONE_CARD"
    const val THREE_CARD = "THREE_CARD"
    const val FIVE_CARD = "FIVE_CARD"
}

// Separatory nieużywane w naturalnym tekście - bezpieczne kodowanie listy dopytań (pytanie +
// odpowiedź) jako pojedynczy string, bez dodatkowych TypeConverterów Room ani zależności JSON.
private const val FOLLOW_UP_FIELD_SEPARATOR = ""
private const val FOLLOW_UP_RECORD_SEPARATOR = ""

/**
 * Pojedynczy wpis w historii losowań (Dziennik Tarota).
 * [cardIds], [reversedFlags], [positionLabels] i [followUps] są zserializowane jako proste
 * stringi rozdzielane separatorami - nie wymaga to dodatkowych TypeConverterów Room.
 */
@Entity(tableName = "readings")
data class ReadingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val spreadType: String,
    val question: String?,
    val cardIdsCsv: String,
    val reversedFlagsCsv: String,
    val positionLabelsCsv: String?,
    val note: String = "",
    val aiResponseText: String? = null,
    val followUpsCsv: String? = null,
) {
    fun cardIds(): List<Int> = cardIdsCsv.split(",").filter { it.isNotBlank() }.map { it.toInt() }

    fun reversedFlags(): List<Boolean> =
        reversedFlagsCsv.split(",").filter { it.isNotBlank() }.map { it == "1" }

    fun positionLabels(): List<String>? =
        positionLabelsCsv?.split("|")?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }

    /** Dekoduje pełną historię dopytań (chmurki czatu) zapisaną razem z tym odczytem. */
    fun followUps(): List<ChatTurn> =
        followUpsCsv?.split(FOLLOW_UP_RECORD_SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?.mapNotNull { record ->
                val parts = record.split(FOLLOW_UP_FIELD_SEPARATOR)
                if (parts.size == 2) ChatTurn(parts[0], parts[1]) else null
            }
            ?: emptyList()

    companion object {
        fun encodeIds(ids: List<Int>): String = ids.joinToString(",")
        fun encodeReversed(flags: List<Boolean>): String = flags.joinToString(",") { if (it) "1" else "0" }
        fun encodePositions(labels: List<String>?): String? = labels?.joinToString("|")
        fun encodeFollowUps(followUps: List<ChatTurn>): String? =
            followUps.takeIf { it.isNotEmpty() }
                ?.joinToString(FOLLOW_UP_RECORD_SEPARATOR) { "${it.question}$FOLLOW_UP_FIELD_SEPARATOR${it.answer}" }
    }
}
