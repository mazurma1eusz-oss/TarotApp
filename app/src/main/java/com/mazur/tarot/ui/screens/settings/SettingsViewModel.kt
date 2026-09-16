package com.mazur.tarot.ui.screens.settings

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazur.tarot.billing.BillingManager
import com.mazur.tarot.data.local.datastore.AppSettings
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import com.mazur.tarot.data.repository.JournalRepository
import com.mazur.tarot.notifications.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore,
    private val billingManager: BillingManager,
    private val journalRepository: JournalRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    val monthlyPriceLabel: String get() = billingManager.monthlyPriceLabel
    val weeklyPriceLabel: String get() = billingManager.weeklyPriceLabel
    val yearlyPriceLabel: String get() = billingManager.yearlyPriceLabel
    val packStartPriceLabel: String get() = billingManager.packStartPriceLabel
    val packStandardPriceLabel: String get() = billingManager.packStandardPriceLabel

    fun buyMonthlySubscription(activity: Activity) {
        billingManager.launchSubscriptionPurchaseFlow(activity)
    }

    fun buyWeeklySubscription(activity: Activity) {
        billingManager.launchWeeklySubscriptionPurchaseFlow(activity)
    }

    fun buyYearlySubscription(activity: Activity) {
        billingManager.launchYearlySubscriptionPurchaseFlow(activity)
    }

    fun buyPackStart(activity: Activity) {
        billingManager.launchPackStartPurchaseFlow(activity)
    }

    fun buyPackStandard(activity: Activity) {
        billingManager.launchPackStandardPurchaseFlow(activity)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    fun setSoundVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setSoundVibrationEnabled(enabled) }
    }

    fun setPersonalizedAdsConsent(consent: Boolean) {
        viewModelScope.launch { settingsDataStore.setPersonalizedAdsConsent(consent) }
    }

    fun setReversedCardsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setReversedCardsEnabled(enabled) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = settings.value
            settingsDataStore.setReminder(enabled, current.reminderHour, current.reminderMinute)
            if (enabled) {
                NotificationScheduler.ensureChannel(context)
                NotificationScheduler.schedule(context, current.reminderHour, current.reminderMinute)
            } else {
                NotificationScheduler.cancel(context)
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val enabled = settings.value.reminderEnabled
            settingsDataStore.setReminder(enabled, hour, minute)
            if (enabled) {
                NotificationScheduler.schedule(context, hour, minute)
            }
        }
    }

    /** Usuwa lokalną historię (odczyty, notatki, ulubione). NIE dotyka statusu PRO. */
    fun deleteAllHistory() {
        viewModelScope.launch {
            journalRepository.deleteAllReadings()
            settingsDataStore.clearLocalUserData()
        }
    }
}
