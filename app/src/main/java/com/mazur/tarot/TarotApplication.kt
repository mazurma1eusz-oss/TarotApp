package com.mazur.tarot

import android.app.Application
import com.mazur.tarot.ads.RewardedAdManager
import com.mazur.tarot.ai.AiReadingRepository
import com.mazur.tarot.billing.BillingManager
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import com.mazur.tarot.data.local.db.AppDatabase
import com.mazur.tarot.data.repository.CardRepository
import com.mazur.tarot.data.repository.JournalRepository
import com.mazur.tarot.notifications.NotificationScheduler

/**
 * Kontener prostych, ręcznie tworzonych singletonów (bez frameworka DI) - wystarczający
 * dla zakresu tej aplikacji. Wszystkie repozytoria/managery żyją tak długo jak proces appki.
 */
class TarotApplication : Application() {

    val cardRepository: CardRepository by lazy { CardRepository(this) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(this) }
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val journalRepository: JournalRepository by lazy { JournalRepository(database.readingDao(), cardRepository) }
    val billingManager: BillingManager by lazy { BillingManager(this, settingsDataStore) }
    val aiReadingRepository: AiReadingRepository by lazy { AiReadingRepository() }
    val rewardedAdManager: RewardedAdManager by lazy { RewardedAdManager(this) }

    override fun onCreate() {
        super.onCreate()
        NotificationScheduler.ensureChannel(this)
        billingManager.startConnection()
        rewardedAdManager.initialize()
    }
}
