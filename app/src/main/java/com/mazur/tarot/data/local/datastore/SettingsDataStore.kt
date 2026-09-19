package com.mazur.tarot.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private val Context.dataStore by preferencesDataStore(name = "tarot_settings")

/** Czas oczekiwania na darmowe odblokowanie kart dodatkowych Karty Dnia (bez reklamy). */
const val BONUS_WAIT_DURATION_MILLIS = 60 * 60 * 1000L

/** Liczba ukończonych odczytów, po której proszymy raz o ocenę w Google Play. */
private const val REVIEW_TRIGGER_COUNT = 3

/** Zabezpieczenie fair-use: subskrybenci PRO mają praktycznie nielimitowany dostęp, ale nie
 * dosłownie - powyżej tylu pytań/dopytań na dobę pokazujemy komunikat o odpoczynku do jutra. */
const val DAILY_PRO_QUESTION_LIMIT = 50

/** Migawka ustawień aplikacji odczytywana przez UI. */
data class AppSettings(
    val isProUnlocked: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 8,
    val reminderMinute: Int = 0,
    val lastDailyCardEpochDay: Long = -1L,
    val lastDailyCardId: Int = -1,
    val lastDailyCardReversed: Boolean = false,
    val supportCardId: Int = -1,
    val supportCardReversed: Boolean = false,
    val warningCardId: Int = -1,
    val warningCardReversed: Boolean = false,
    val lastFreeAskEpochDay: Long = -1L,
    val soundVibrationEnabled: Boolean = true,
    val questionCredits: Int = 0,
    val bonusUnlockedEpochDay: Long = -1L,
    val bonusWaitStartedEpochDay: Long = -1L,
    val bonusWaitStartedAtMillis: Long = -1L,
    val favoriteCardIds: Set<Int> = emptySet(),
    val personalizedAdsConsent: Boolean = true,
    val reversedCardsEnabled: Boolean = true,
    val musicMuted: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val userName: String = "",
    val userGender: String = "",
    val userBirthDay: Int = 0,
    val userBirthMonth: Int = 0,
) {
    /** Karty pomocnicze (Wsparcie / Na co uważać) są ważne tylko w dniu, w którym padły. */
    private val bonusCardsAreForToday: Boolean
        get() = lastDailyCardEpochDay == LocalDate.now().toEpochDay()

    val todaysSupportCardId: Int? get() = supportCardId.takeIf { it != -1 && bonusCardsAreForToday }
    val todaysWarningCardId: Int? get() = warningCardId.takeIf { it != -1 && bonusCardsAreForToday }

    private val today: Long get() = LocalDate.now().toEpochDay()

    /** Czy losowanie kart dodatkowych (Wsparcie / Na Co Uważać) jest dziś odblokowane - przez
     * obejrzenie reklamy lub odczekanie 60 minut. */
    val bonusCardsUnlocked: Boolean get() = bonusUnlockedEpochDay == today

    /** Czy trwa aktualnie 60-minutowe oczekiwanie na odblokowanie kart dodatkowych. */
    val bonusWaitInProgress: Boolean
        get() = !bonusCardsUnlocked && bonusWaitStartedEpochDay == today && bonusWaitStartedAtMillis != -1L

    /** Pozostały czas oczekiwania w milisekundach (0, gdy czas już minął). */
    fun bonusWaitRemainingMillis(nowMillis: Long): Long {
        if (!bonusWaitInProgress) return 0L
        val elapsed = nowMillis - bonusWaitStartedAtMillis
        return (BONUS_WAIT_DURATION_MILLIS - elapsed).coerceAtLeast(0L)
    }

    /** Czy dzisiejszy darmowy odczyt (1 KARTA lub 1 rozkład 3 kart w "Zapytaj Kart" - wspólna
     * pula, nie osobno dla każdego) nie został jeszcze wykorzystany. Resetuje się co dobę wraz
     * ze zmianą daty urządzenia, tak samo jak Karta Dnia. */
    val canAskFreeToday: Boolean
        get() = lastFreeAskEpochDay != today

    /** Czy użytkownik może zadać kartom pytanie (rozkład 1 lub 3 karty) bez wersji Premium -
     * albo wciąż ma dzisiejszy darmowy odczyt, albo ma kredyt pytania (z reklamy lub
     * z zakupionego pakietu). */
    val canAskFreeQuestion: Boolean
        get() = isProUnlocked || canAskFreeToday || questionCredits > 0
}

/**
 * Persystencja stanu licencji PRO oraz ustawień użytkownika: godzina przypomnienia,
 * dane o ostatnio wylosowanej Karcie Dnia i kartach dodatkowych (by wymusić jedno
 * losowanie na dobę), oraz jednorazowy darmowy dostęp do "Zapytaj Kart" na całą
 * instalację aplikacji.
 */
class SettingsDataStore(private val context: Context) {

    private object Keys {
        val PRO_UNLOCKED = booleanPreferencesKey("pro_unlocked")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val LAST_CARD_EPOCH_DAY = longPreferencesKey("last_card_epoch_day")
        val LAST_CARD_ID = intPreferencesKey("last_card_id")
        val LAST_CARD_REVERSED = booleanPreferencesKey("last_card_reversed")
        val SUPPORT_CARD_ID = intPreferencesKey("support_card_id")
        val SUPPORT_CARD_REVERSED = booleanPreferencesKey("support_card_reversed")
        val WARNING_CARD_ID = intPreferencesKey("warning_card_id")
        val WARNING_CARD_REVERSED = booleanPreferencesKey("warning_card_reversed")
        val LAST_PURCHASE_TOKEN = stringPreferencesKey("last_purchase_token")
        val LAST_FREE_ASK_EPOCH_DAY = longPreferencesKey("last_free_ask_epoch_day")
        val SOUND_VIBRATION_ENABLED = booleanPreferencesKey("sound_vibration_enabled")
        val QUESTION_CREDITS = intPreferencesKey("question_credits")
        val BONUS_UNLOCKED_EPOCH_DAY = longPreferencesKey("bonus_unlocked_epoch_day")
        val BONUS_WAIT_STARTED_EPOCH_DAY = longPreferencesKey("bonus_wait_started_epoch_day")
        val BONUS_WAIT_STARTED_AT_MILLIS = longPreferencesKey("bonus_wait_started_at_millis")
        val COMPLETED_READINGS_COUNT = intPreferencesKey("completed_readings_count")
        val HAS_REQUESTED_REVIEW = booleanPreferencesKey("has_requested_review")
        val FAVORITE_CARD_IDS = stringSetPreferencesKey("favorite_card_ids")
        val PERSONALIZED_ADS_CONSENT = booleanPreferencesKey("personalized_ads_consent")
        val DAILY_PRO_QUESTIONS_COUNT = intPreferencesKey("daily_pro_questions_count")
        val DAILY_PRO_QUESTIONS_EPOCH_DAY = longPreferencesKey("daily_pro_questions_epoch_day")
        val REVERSED_CARDS_ENABLED = booleanPreferencesKey("reversed_cards_enabled")
        val MUSIC_MUTED = booleanPreferencesKey("music_muted")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_GENDER = stringPreferencesKey("user_gender")
        val USER_BIRTH_DAY = intPreferencesKey("user_birth_day")
        val USER_BIRTH_MONTH = intPreferencesKey("user_birth_month")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            isProUnlocked = prefs[Keys.PRO_UNLOCKED] ?: false,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 8,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            lastDailyCardEpochDay = prefs[Keys.LAST_CARD_EPOCH_DAY] ?: -1L,
            lastDailyCardId = prefs[Keys.LAST_CARD_ID] ?: -1,
            lastDailyCardReversed = prefs[Keys.LAST_CARD_REVERSED] ?: false,
            supportCardId = prefs[Keys.SUPPORT_CARD_ID] ?: -1,
            supportCardReversed = prefs[Keys.SUPPORT_CARD_REVERSED] ?: false,
            warningCardId = prefs[Keys.WARNING_CARD_ID] ?: -1,
            warningCardReversed = prefs[Keys.WARNING_CARD_REVERSED] ?: false,
            lastFreeAskEpochDay = prefs[Keys.LAST_FREE_ASK_EPOCH_DAY] ?: -1L,
            soundVibrationEnabled = prefs[Keys.SOUND_VIBRATION_ENABLED] ?: true,
            questionCredits = prefs[Keys.QUESTION_CREDITS] ?: 0,
            bonusUnlockedEpochDay = prefs[Keys.BONUS_UNLOCKED_EPOCH_DAY] ?: -1L,
            bonusWaitStartedEpochDay = prefs[Keys.BONUS_WAIT_STARTED_EPOCH_DAY] ?: -1L,
            bonusWaitStartedAtMillis = prefs[Keys.BONUS_WAIT_STARTED_AT_MILLIS] ?: -1L,
            favoriteCardIds = (prefs[Keys.FAVORITE_CARD_IDS] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet(),
            personalizedAdsConsent = prefs[Keys.PERSONALIZED_ADS_CONSENT] ?: true,
            reversedCardsEnabled = prefs[Keys.REVERSED_CARDS_ENABLED] ?: true,
            musicMuted = prefs[Keys.MUSIC_MUTED] ?: false,
            hasCompletedOnboarding = prefs[Keys.HAS_COMPLETED_ONBOARDING] ?: false,
            userName = prefs[Keys.USER_NAME] ?: "",
            userGender = prefs[Keys.USER_GENDER] ?: "",
            userBirthDay = prefs[Keys.USER_BIRTH_DAY] ?: 0,
            userBirthMonth = prefs[Keys.USER_BIRTH_MONTH] ?: 0,
        )
    }

    suspend fun setProUnlocked(unlocked: Boolean) {
        context.dataStore.edit { it[Keys.PRO_UNLOCKED] = unlocked }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.REMINDER_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun saveLastDailyCard(epochDay: Long, cardId: Int, reversed: Boolean) {
        context.dataStore.edit {
            it[Keys.LAST_CARD_EPOCH_DAY] = epochDay
            it[Keys.LAST_CARD_ID] = cardId
            it[Keys.LAST_CARD_REVERSED] = reversed
            // Nowy dzień - nowa Karta Dnia, więc karty dodatkowe też muszą zostać wylosowane na nowo.
            it[Keys.SUPPORT_CARD_ID] = -1
            it[Keys.WARNING_CARD_ID] = -1
        }
    }

    suspend fun saveSupportCard(cardId: Int, reversed: Boolean) {
        context.dataStore.edit {
            it[Keys.SUPPORT_CARD_ID] = cardId
            it[Keys.SUPPORT_CARD_REVERSED] = reversed
        }
    }

    suspend fun saveWarningCard(cardId: Int, reversed: Boolean) {
        context.dataStore.edit {
            it[Keys.WARNING_CARD_ID] = cardId
            it[Keys.WARNING_CARD_REVERSED] = reversed
        }
    }

    suspend fun saveLastPurchaseToken(token: String) {
        context.dataStore.edit { it[Keys.LAST_PURCHASE_TOKEN] = token }
    }

    /** Zużywa dzisiejszy darmowy odczyt (1 karta lub 1 rozkład 3 kart, wspólna pula) - wraca
     * jutro, tak samo jak Karta Dnia. Kolejna próba tego samego dnia wymaga kredytu/Premium. */
    suspend fun consumeFreeAskToday() {
        context.dataStore.edit { it[Keys.LAST_FREE_ASK_EPOCH_DAY] = LocalDate.now().toEpochDay() }
    }

    suspend fun setSoundVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_VIBRATION_ENABLED] = enabled }
    }

    /** Przyznaje kredyty pytań AI - w zamian za obejrzenie reklamy nagradzanej (amount=1)
     * albo za zakup pakietu pytań (amount=10/50). Wspólna pula dla nowych pytań i dopytań. */
    suspend fun grantQuestionCredits(amount: Int) {
        context.dataStore.edit { it[Keys.QUESTION_CREDITS] = (it[Keys.QUESTION_CREDITS] ?: 0) + amount }
    }

    /** Zużywa jeden kredyt pytania z puli, jeśli dostępny. Zwraca `true`, gdy się udało. */
    suspend fun consumeQuestionCredit(): Boolean {
        var consumed = false
        context.dataStore.edit {
            val current = it[Keys.QUESTION_CREDITS] ?: 0
            if (current > 0) {
                it[Keys.QUESTION_CREDITS] = current - 1
                consumed = true
            }
        }
        return consumed
    }

    /** Zużywa "przepustkę" na odczyt - najpierw dzisiejszy darmowy odczyt, potem pula kredytów. */
    suspend fun consumeAskAllowance(canAskFreeToday: Boolean) {
        if (canAskFreeToday) {
            consumeFreeAskToday()
        } else {
            consumeQuestionCredit()
        }
    }

    suspend fun setPersonalizedAdsConsent(consent: Boolean) {
        context.dataStore.edit { it[Keys.PERSONALIZED_ADS_CONSENT] = consent }
    }

    /** Włącza/wyłącza losowanie kart odwróconych (Karta Dnia i Zapytaj Kart). Domyślnie włączone. */
    suspend fun setReversedCardsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REVERSED_CARDS_ENABLED] = enabled }
    }

    /** Wycisza/odcisza muzykę w tle. Domyślnie odciszona (muzyka gra). */
    suspend fun setMusicMuted(muted: Boolean) {
        context.dataStore.edit { it[Keys.MUSIC_MUTED] = muted }
    }

    /**
     * Zabezpieczenie fair-use dla subskrybentów PRO: sprawdza i zapisuje w JEDNEJ transakcji,
     * czy dzisiejszy licznik pytań/dopytań nie przekroczył jeszcze [DAILY_PRO_QUESTION_LIMIT].
     * Licznik resetuje się automatycznie wraz ze zmianą dnia. Zwraca `false`, gdy limit został
     * już osiągnięty - w takim wypadku NIC nie jest inkrementowane ani zużywane.
     */
    suspend fun recordProQuestionUsage(): Boolean {
        var allowed = false
        context.dataStore.edit { prefs ->
            val today = LocalDate.now().toEpochDay()
            val storedDay = prefs[Keys.DAILY_PRO_QUESTIONS_EPOCH_DAY] ?: -1L
            val currentCount = if (storedDay == today) prefs[Keys.DAILY_PRO_QUESTIONS_COUNT] ?: 0 else 0
            if (currentCount < DAILY_PRO_QUESTION_LIMIT) {
                prefs[Keys.DAILY_PRO_QUESTIONS_EPOCH_DAY] = today
                prefs[Keys.DAILY_PRO_QUESTIONS_COUNT] = currentCount + 1
                allowed = true
            }
        }
        return allowed
    }

    /** Odblokowuje dziś losowanie kart dodatkowych Karty Dnia - przez reklamę lub upływ czasu. */
    suspend fun markBonusCardsUnlocked() {
        context.dataStore.edit { it[Keys.BONUS_UNLOCKED_EPOCH_DAY] = LocalDate.now().toEpochDay() }
    }

    /** Rozpoczyna 60-minutowe darmowe oczekiwanie na odblokowanie kart dodatkowych. */
    suspend fun startBonusWait() {
        context.dataStore.edit {
            it[Keys.BONUS_WAIT_STARTED_EPOCH_DAY] = LocalDate.now().toEpochDay()
            it[Keys.BONUS_WAIT_STARTED_AT_MILLIS] = System.currentTimeMillis()
        }
    }

    /**
     * Zlicza kolejny ukończony odczyt (Karta Dnia lub Zapytaj Kart) i zwraca `true` dokładnie
     * raz - w momencie, gdy licznik osiąga [REVIEW_TRIGGER_COUNT] po raz pierwszy - sygnał dla
     * UI, by pokazać natywny prompt oceny Google Play. Flaga "już poproszono" jest ustawiana
     * w tej samej transakcji, więc kolejne wywołania zawsze zwrócą `false`.
     */
    suspend fun recordCompletedReadingAndCheckReview(): Boolean {
        var shouldRequestReview = false
        context.dataStore.edit { prefs ->
            val newCount = (prefs[Keys.COMPLETED_READINGS_COUNT] ?: 0) + 1
            prefs[Keys.COMPLETED_READINGS_COUNT] = newCount
            val alreadyRequested = prefs[Keys.HAS_REQUESTED_REVIEW] ?: false
            if (newCount >= REVIEW_TRIGGER_COUNT && !alreadyRequested) {
                prefs[Keys.HAS_REQUESTED_REVIEW] = true
                shouldRequestReview = true
            }
        }
        return shouldRequestReview
    }

    /** Przełącza kartę jako ulubioną/nieulubioną - działa w pełni offline, bez AI i bez PRO. */
    suspend fun toggleFavorite(cardId: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.FAVORITE_CARD_IDS] ?: emptySet()
            val key = cardId.toString()
            prefs[Keys.FAVORITE_CARD_IDS] = if (current.contains(key)) current - key else current + key
        }
    }

    /** Zapisuje opcjonalny profil z ekranu powitalnego (wszystkie pola dobrowolne, puste/0
     * oznacza "nie podano") i oznacza onboarding jako ukończony - niezależnie od tego, czy
     * użytkownik cokolwiek wypełnił, czy od razu pominął ten ekran. */
    suspend fun saveOnboardingProfile(name: String, gender: String, birthDay: Int, birthMonth: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_NAME] = name
            prefs[Keys.USER_GENDER] = gender
            prefs[Keys.USER_BIRTH_DAY] = birthDay
            prefs[Keys.USER_BIRTH_MONTH] = birthMonth
            prefs[Keys.HAS_COMPLETED_ONBOARDING] = true
        }
    }

    /** Pomija ekran powitalny bez zapisywania żadnych danych profilu. */
    suspend fun skipOnboarding() {
        context.dataStore.edit { it[Keys.HAS_COMPLETED_ONBOARDING] = true }
    }

    /** Czyści lokalne dane użytkownika (ulubione, liczniki) - wywoływane z "Usuń historię"
     * w Ustawieniach. NIE dotyka statusu PRO ani zapisanego tokenu zakupu. */
    suspend fun clearLocalUserData() {
        context.dataStore.edit { prefs ->
            prefs[Keys.FAVORITE_CARD_IDS] = emptySet()
            prefs[Keys.COMPLETED_READINGS_COUNT] = 0
        }
    }
}
