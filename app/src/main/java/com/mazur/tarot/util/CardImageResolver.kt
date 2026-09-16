package com.mazur.tarot.util

import android.content.Context
import androidx.annotation.DrawableRes
import com.mazur.tarot.R

/**
 * Dynamicznie mapuje `image_res_name` z cards.json na zasób w res/drawable.
 * Jeśli grafika danej karty nie została jeszcze dodana do projektu, używany jest
 * elegancki placeholder ([R.drawable.card_placeholder]), dzięki czemu aplikacja
 * kompiluje się i działa nawet bez kompletu 78 unikalnych obrazów.
 */
object CardImageResolver {

    private val cache = HashMap<String, Int>()

    @DrawableRes
    fun resolve(context: Context, imageResName: String): Int {
        cache[imageResName]?.let { return it }
        val resId = context.resources.getIdentifier(imageResName, "drawable", context.packageName)
        val resolved = if (resId != 0) resId else R.drawable.card_placeholder
        cache[imageResName] = resolved
        return resolved
    }
}
