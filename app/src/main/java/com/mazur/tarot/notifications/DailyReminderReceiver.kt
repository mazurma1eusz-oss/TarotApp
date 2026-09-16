package com.mazur.tarot.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mazur.tarot.MainActivity
import com.mazur.tarot.R
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Wyzwalany codziennie przez AlarmManager - pokazuje powiadomienie i planuje kolejny alarm. */
class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        NotificationScheduler.ensureChannel(context)
        showNotification(context)

        val dataStore = SettingsDataStore(context)
        runBlocking {
            val settings = dataStore.settingsFlow.first()
            if (settings.reminderEnabled) {
                NotificationScheduler.schedule(context, settings.reminderHour, settings.reminderMinute)
            }
        }
    }

    private fun showNotification(context: Context) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.channelId())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Brak zgody POST_NOTIFICATIONS - użytkownik jej nie udzielił, pomijamy powiadomienie.
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 2001
    }
}
