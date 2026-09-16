package com.mazur.tarot.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Po restarcie urządzenia AlarmManager traci zaplanowane alarmy - odtwarzamy je tutaj. */
class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val dataStore = SettingsDataStore(context)
        runBlocking {
            val settings = dataStore.settingsFlow.first()
            if (settings.reminderEnabled) {
                NotificationScheduler.ensureChannel(context)
                NotificationScheduler.schedule(context, settings.reminderHour, settings.reminderMinute)
            }
        }
    }
}
