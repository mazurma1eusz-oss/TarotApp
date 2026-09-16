package com.mazur.tarot.data.repository

import android.content.Context
import com.mazur.tarot.data.model.CardType
import com.mazur.tarot.data.model.TarotCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Wczytuje i buforuje w pamięci 78 kart tarota z pliku assets/cards.json.
 * Plik czytany jest raz, przy pierwszym dostępie (lazy, thread-safe dzięki Mutex-owi
 * ukrytemu w [kotlinx.coroutines.sync] nie jest tu potrzebny bo whole load jest idempotentny).
 */
class CardRepository(private val context: Context) {

    @Volatile
    private var cachedCards: List<TarotCard>? = null

    suspend fun getAllCards(): List<TarotCard> {
        cachedCards?.let { return it }
        return withContext(Dispatchers.IO) {
            cachedCards ?: loadFromAssets().also { cachedCards = it }
        }
    }

    suspend fun getCardById(id: Int): TarotCard? = getAllCards().firstOrNull { it.id == id }

    private fun loadFromAssets(): List<TarotCard> {
        val json = context.assets.open(ASSET_FILE_NAME).use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        }
        val array = JSONArray(json)
        val result = ArrayList<TarotCard>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                TarotCard(
                    id = obj.getInt("id"),
                    name = obj.getString("name"),
                    type = CardType.fromJson(obj.getString("type")),
                    imageResName = obj.getString("image_res_name"),
                    descriptionGeneral = obj.getString("description_general"),
                    descriptionLove = obj.getString("description_love"),
                    descriptionWork = obj.getString("description_work"),
                    descriptionReversedGeneral = obj.getString("description_reversed_general"),
                    descriptionReversedLove = obj.getString("description_reversed_love"),
                    descriptionReversedWork = obj.getString("description_reversed_work"),
                )
            )
        }
        return result
    }

    companion object {
        private const val ASSET_FILE_NAME = "cards.json"
    }
}
