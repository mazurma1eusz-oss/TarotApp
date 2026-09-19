package com.mazur.tarot.data.repository

import android.content.Context
import com.mazur.tarot.data.model.CardType
import com.mazur.tarot.data.model.TarotCard
import com.mazur.tarot.util.resolvedAppLocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

/**
 * Wczytuje i buforuje w pamięci 78 kart tarota z jednego z trzech plików assets/cards_*.json
 * (pl/en/es), dobranego wg języka systemu wykrytego przy pierwszym dostępie - patrz
 * [assetFileNameForCurrentLocale]. Każdy język niesie własny tekst (nazwa, opisy), ale
 * [image_res_name] ZAWSZE bierzemy z cards_pl.json niezależnie od wybranego języka, bo pliki
 * en/es niosą inny (niepasujący do realnych zasobów) schemat nazewnictwa grafik - jedyne
 * prawdziwe źródło nazw plików w res/drawable to wersja polska.
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
        val imageResNameById = parseCardArray(readAsset(PL_ASSET_FILE_NAME)).associate { obj ->
            obj.getInt("id") to obj.getString("image_res_name")
        }
        val localeFileName = assetFileNameForCurrentLocale()
        val array = if (localeFileName == PL_ASSET_FILE_NAME) {
            parseCardArray(readAsset(PL_ASSET_FILE_NAME))
        } else {
            parseCardArray(readAsset(localeFileName))
        }
        return array.map { obj ->
            val id = obj.getInt("id")
            TarotCard(
                id = id,
                name = obj.getString("name"),
                type = CardType.fromJson(obj.getString("type")),
                imageResName = imageResNameById[id] ?: obj.getString("image_res_name"),
                descriptionGeneral = obj.getString("description_general"),
                descriptionLove = obj.getString("description_love"),
                descriptionWork = obj.getString("description_work"),
                descriptionReversedGeneral = obj.getString("description_reversed_general"),
                descriptionReversedLove = obj.getString("description_reversed_love"),
                descriptionReversedWork = obj.getString("description_reversed_work"),
            )
        }
    }

    private fun readAsset(fileName: String): String =
        context.assets.open(fileName).use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
        }

    private fun parseCardArray(json: String): List<org.json.JSONObject> {
        val array = JSONArray(json)
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    /** Polski i hiszpański mają własny plik treści; każdy inny język systemu (w tym angielski)
     * spada na cards_en.json, zgodnie z zasadą "nieobsługiwany język = angielski". */
    private fun assetFileNameForCurrentLocale(): String =
        when (resolvedAppLocale(context).language) {
            "pl" -> PL_ASSET_FILE_NAME
            "es" -> ES_ASSET_FILE_NAME
            else -> EN_ASSET_FILE_NAME
        }

    companion object {
        private const val PL_ASSET_FILE_NAME = "cards_pl.json"
        private const val EN_ASSET_FILE_NAME = "cards_en.json"
        private const val ES_ASSET_FILE_NAME = "cards_es.json"
    }
}
