package com.mazur.tarot.util

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Cienki wrapper na natywny Google Play In-App Review API. Wywoływane raz, po 3. ukończonym
 * odczycie (patrz [com.mazur.tarot.data.local.datastore.SettingsDataStore.recordCompletedReadingAndCheckReview]).
 * Google sam decyduje, czy prompt faktycznie się pokaże (limituje częstotliwość na poziomie
 * systemu), więc wywołanie jest zawsze bezpieczne i nie wymaga dodatkowej obsługi błędów w UI.
 */
object InAppReviewHelper {

    fun requestReview(activity: Activity) {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                manager.launchReviewFlow(activity, task.result)
            }
        }
    }
}
