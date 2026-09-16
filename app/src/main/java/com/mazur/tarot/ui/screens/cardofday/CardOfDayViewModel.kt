package com.mazur.tarot.ui.screens.cardofday

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazur.tarot.data.local.db.SpreadType
import com.mazur.tarot.data.local.datastore.AppSettings
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import com.mazur.tarot.data.model.DrawnCard
import com.mazur.tarot.data.repository.CardRepository
import com.mazur.tarot.data.repository.JournalRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

sealed interface CardOfDayUiState {
    data object Loading : CardOfDayUiState
    data object ReadyToDraw : CardOfDayUiState
    data class Drawn(
        val drawnCard: DrawnCard,
        val revealed: Boolean,
        val supportCard: DrawnCard? = null,
        val warningCard: DrawnCard? = null,
    ) : CardOfDayUiState
}

class CardOfDayViewModel(
    private val cardRepository: CardRepository,
    private val journalRepository: JournalRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CardOfDayUiState>(CardOfDayUiState.Loading)
    val uiState: StateFlow<CardOfDayUiState> = _uiState.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _reviewRequestEvent = MutableSharedFlow<Unit>()
    val reviewRequestEvent: SharedFlow<Unit> = _reviewRequestEvent

    init {
        checkTodaysCard()
    }

    /** Odblokowuje karty dodatkowe natychmiast, po obejrzeniu reklamy nagradzanej. */
    fun unlockBonusCardsViaAd() {
        viewModelScope.launch { settingsDataStore.markBonusCardsUnlocked() }
    }

    /** Rozpoczyna darmowe 60-minutowe oczekiwanie na odblokowanie kart dodatkowych. */
    fun startBonusWait() {
        viewModelScope.launch { settingsDataStore.startBonusWait() }
    }

    /** Wywoływane, gdy lokalny odliczający zegar w UI dojdzie do zera - odblokowuje karty
     * natychmiast zamiast czekać na WorkManager (który i tak wykona ten sam zapis). */
    fun completeBonusWait() {
        viewModelScope.launch { settingsDataStore.markBonusCardsUnlocked() }
    }

    private fun checkTodaysCard() {
        viewModelScope.launch {
            val settings = settingsDataStore.settingsFlow.first()
            val today = LocalDate.now().toEpochDay()
            if (settings.lastDailyCardEpochDay == today && settings.lastDailyCardId != -1) {
                val card = cardRepository.getCardById(settings.lastDailyCardId)
                if (card != null) {
                    val supportCard = settings.todaysSupportCardId
                        ?.let { cardRepository.getCardById(it) }
                        ?.let { DrawnCard(it, settings.supportCardReversed) }
                    val warningCard = settings.todaysWarningCardId
                        ?.let { cardRepository.getCardById(it) }
                        ?.let { DrawnCard(it, settings.warningCardReversed) }
                    _uiState.value = CardOfDayUiState.Drawn(
                        drawnCard = DrawnCard(card, settings.lastDailyCardReversed),
                        revealed = true,
                        supportCard = supportCard,
                        warningCard = warningCard,
                    )
                    return@launch
                }
            }
            _uiState.value = CardOfDayUiState.ReadyToDraw
        }
    }

    fun drawCard() {
        if (_uiState.value !is CardOfDayUiState.ReadyToDraw) return
        viewModelScope.launch {
            val cards = cardRepository.getAllCards()
            if (cards.isEmpty()) return@launch
            val card = cards.random()
            val reversed = settings.value.reversedCardsEnabled && Random.nextBoolean()
            val drawn = DrawnCard(card, reversed)

            _uiState.value = CardOfDayUiState.Drawn(drawn, revealed = false)

            val today = LocalDate.now().toEpochDay()
            settingsDataStore.saveLastDailyCard(today, card.id, reversed)
            journalRepository.saveReading(SpreadType.CARD_OF_DAY, null, listOf(drawn))
            if (settingsDataStore.recordCompletedReadingAndCheckReview()) {
                _reviewRequestEvent.emit(Unit)
            }

            // Krótka pauza, zanim karta sama się odwróci - imituje fizyczne dobranie karty ze
            // stosu, zamiast pokazywać awers natychmiast. Użytkownik może też sam dotknąć
            // kartę wcześniej - reveal() jest bezpieczne do wywołania w obu miejscach.
            delay(500)
            reveal()
        }
    }

    fun reveal() {
        val current = _uiState.value
        if (current is CardOfDayUiState.Drawn && !current.revealed) {
            _uiState.value = current.copy(revealed = true)
        }
    }

    /** Losuje "Kartę Wsparcia" - dodatkową kartę na dziś, pomijając już wylosowane karty. */
    fun drawSupportCard() {
        val current = _uiState.value
        if (current !is CardOfDayUiState.Drawn || !current.revealed || current.supportCard != null) return
        viewModelScope.launch {
            val excluded = setOfNotNull(current.drawnCard.card.id)
            val card = cardRepository.getAllCards().filterNot { it.id in excluded }.randomOrNull() ?: return@launch
            val reversed = settings.value.reversedCardsEnabled && Random.nextBoolean()
            val drawn = DrawnCard(card, reversed)
            _uiState.value = current.copy(supportCard = drawn)
            settingsDataStore.saveSupportCard(card.id, reversed)
            journalRepository.appendCardToTodaysCardOfDay(drawn, "Karta Wsparcia")
        }
    }

    /** Losuje kartę "Na Co Uważać" - dodatkową kartę na dziś, pomijając już wylosowane karty. */
    fun drawWarningCard() {
        val current = _uiState.value
        if (current !is CardOfDayUiState.Drawn || !current.revealed || current.warningCard != null) return
        viewModelScope.launch {
            val excluded = setOfNotNull(current.drawnCard.card.id, current.supportCard?.card?.id)
            val card = cardRepository.getAllCards().filterNot { it.id in excluded }.randomOrNull() ?: return@launch
            val reversed = settings.value.reversedCardsEnabled && Random.nextBoolean()
            val drawn = DrawnCard(card, reversed)
            _uiState.value = current.copy(warningCard = drawn)
            settingsDataStore.saveWarningCard(card.id, reversed)
            journalRepository.appendCardToTodaysCardOfDay(drawn, "Na Co Uważać")
        }
    }
}
