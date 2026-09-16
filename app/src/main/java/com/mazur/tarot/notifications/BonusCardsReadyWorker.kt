package com.mazur.tarot.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mazur.tarot.R
import com.mazur.tarot.data.local.datastore.SettingsDataStore

private const val BONUS_NOTIFICATION_ID = 2001
const val BONUS_CARDS_WAIT_WORK_NAME = "bonus_cards_wait"

/**
 * Uruchamiane przez WorkManager po 60 minutach darmowego oczekiwania na karty dodatkowe
 * Karty Dnia - odblokowuje je w DataStore (niezależnie od tego, czy aplikacja jest otwarta)
 * i wysyła powiadomienie push informujące, że są gotowe do odkrycia.
 */
class BonusCardsReadyWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        SettingsDataStore(applicationContext).markBonusCardsUnlocked()
        showNotification()
        return Result.success()
    }

    private fun showNotification() {
        val context = applicationContext
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val notification = NotificationCompat.Builder(context, NotificationScheduler.channelId())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_bonus_ready_title))
            .setContentText(context.getString(R.string.notification_bonus_ready_text))
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(BONUS_NOTIFICATION_ID, notification)
    }
}
