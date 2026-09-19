package com.mazur.tarot.ui.screens.ask

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazur.tarot.R
import com.mazur.tarot.ai.AiReadingRepository
import java.io.IOException
import com.mazur.tarot.data.local.db.SpreadType
import com.mazur.tarot.data.local.datastore.AppSettings
import com.mazur.tarot.data.local.datastore.DAILY_PRO_QUESTION_LIMIT
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import com.mazur.tarot.data.model.ChatTurn
import com.mazur.tarot.data.model.DrawnCard
import com.mazur.tarot.data.repository.CardRepository
import com.mazur.tarot.data.repository.JournalRepository
import com.mazur.tarot.util.resolvedAppLocale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class SpreadOption(val cardCount: Int, val requiresPro: Boolean, val dbType: String) {
    ONE(1, false, SpreadType.ONE_CARD),
    THREE(3, false, SpreadType.THREE_CARD),
    FIVE(5, true, SpreadType.FIVE_CARD),
}

/**
 * Opcjonalna intencja wybrana przed losowaniem ("Z czym dziś przychodzisz?") - przekazywana
 * do AI wyłącznie jako dodatkowy kontekst interpretacji. NIGDY nie wpływa na to, które karty
 * zostaną wylosowane - losowanie w [AskCardsViewModel.requestReading] jest od niej całkowicie
 * niezależne.
 */
enum class ReadingIntent(@StringRes val labelResId: Int, val symbol: String) {
    LOVE(R.string.intent_love, "♡"),
    WORK(R.string.intent_work, "♧"),
    DECISION(R.string.intent_decision, "✦"),
    RELATIONSHIP(R.string.intent_relationship, "☽"),
    GENERAL(R.string.intent_general, "✧"),
}

/** Pojedyncza karta w trakcie odczytu - niesie ze sobą to, czy użytkownik ją już odsłonił. */
data class ReadingCardState(val drawn: DrawnCard, val isFlipped: Boolean = false)

/** Jedna wylosowana karta w formie gotowej do złączenia w zapytanie o spersonalizowany odczyt. */
data class AiCardEntry(val position: String?, val name: String, val reversed: Boolean)

/** Pytanie użytkownika, wylosowane karty i opcjonalna intencja - materiał wejściowy do promptu dla
 * modelu. [userName]/[userGender] pochodzą z dobrowolnego profilu z ekranu powitalnego (patrz
 * [com.mazur.tarot.data.local.datastore.AppSettings]) - puste, gdy użytkownik ich nie podał. */
data class AiReadingRequest(
    val question: String?,
    val cards: List<AiCardEntry>,
    val intent: String? = null,
    val userName: String = "",
    val userGender: String = "",
    val languageCode: String = "pl",
)

sealed interface AiReadingState {
    data object Loading : AiReadingState
    data class Success(val text: String) : AiReadingState
    data class Error(val message: String) : AiReadingState
}

/**
 * Stan bramy dopytania o gotowy odczyt ("Dopytaj o ten odczyt..."). Pierwsze dopytanie w
 * ramach KONKRETNEGO odczytu jest zawsze darmowe - dopiero kolejne (Reading.followUpsUsed >= 1)
 * wymagają PRO, kredytu z pakietu lub obejrzenia reklamy. Limit dotyczy tylko tego jednego
 * odczytu; nowy odczyt zawsze zaczyna od zera. Każde UDANE dopytanie trafia jako nowa "chmurka"
 * do [AskUiState.Reading.followUps] - ten stan opisuje wyłącznie bramę/pole wejścia dla
 * NASTĘPNEGO, jeszcze nie wysłanego dopytania.
 */
sealed interface FollowUpState {
    /** Wymaga PRO, kredytu lub obejrzenia reklamy, zanim pokaże się pole tekstowe. */
    data object Locked : FollowUpState

    /** Brama przeszła (albo to pierwsze, darmowe dopytanie) - pole tekstowe jest widoczne. */
    data object Unlocked : FollowUpState
    data object Loading : FollowUpState
    data class Error(val question: String, val message: String) : FollowUpState
}

sealed interface AskUiState {
    /** Ekran wprowadzania pytania i wyboru rozkładu. */
    data object Setup : AskUiState

    /** Osobny widok odczytu: zakryte karty do odsłonięcia + ciągła konwersacja (czat) z AI. */
    data class Reading(
        val question: String?,
        val spreadDbType: String,
        val cards: List<ReadingCardState>,
        val aiState: AiReadingState,
        val intent: String? = null,
        val followUps: List<ChatTurn> = emptyList(),
        val followUpState: FollowUpState = FollowUpState.Locked,
        val followUpsUsed: Int = 0,
        /** Id wpisu w Dzienniku, jeśli ten odczyt zdążył się już auto-zapisać (patrz
         * [AskCardsViewModel.generateReading]). Null dopóki AI nie odpowie po raz pierwszy. */
        val savedReadingId: Long? = null,
    ) : AskUiState {
        val allCardsFlipped: Boolean get() = cards.all { it.isFlipped }
        val isAiFinished: Boolean get() = aiState !is AiReadingState.Loading

        /** Pierwsze dopytanie w tym odczycie jest zawsze darmowe, bez PRO/reklamy/kredytu. */
        val nextFollowUpIsFree: Boolean get() = followUpsUsed == 0
    }
}

class AskCardsViewModel(
    private val context: Context,
    private val cardRepository: CardRepository,
    private val journalRepository: JournalRepository,
    private val settingsDataStore: SettingsDataStore,
    private val aiReadingRepository: AiReadingRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsDataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _uiState = MutableStateFlow<AskUiState>(AskUiState.Setup)
    val uiState: StateFlow<AskUiState> = _uiState.asStateFlow()

    private val _proRequiredEvent = MutableSharedFlow<Unit>()
    val proRequiredEvent: SharedFlow<Unit> = _proRequiredEvent

    private val _followUpPaywallEvent = MutableSharedFlow<Unit>()
    val followUpPaywallEvent: SharedFlow<Unit> = _followUpPaywallEvent

    /** Emitowane, gdy subskrybent PRO wyczerpał dzienny limit fair-use (patrz [DAILY_PRO_QUESTION_LIMIT]). */
    private val _dailyLimitEvent = MutableSharedFlow<Unit>()
    val dailyLimitEvent: SharedFlow<Unit> = _dailyLimitEvent

    private val _reviewRequestEvent = MutableSharedFlow<Unit>()
    val reviewRequestEvent: SharedFlow<Unit> = _reviewRequestEvent

    /** Przyznaje kredyty pytań AI - za obejrzenie reklamy (1) albo zakup pakietu (10/50). */
    fun grantQuestionCredits(amount: Int) {
        viewModelScope.launch { settingsDataStore.grantQuestionCredits(amount) }
    }

    /**
     * Uruchamiane po kliknięciu "Zadaj pytanie". Rozkład 5 kart zawsze wymaga wersji PRO;
     * rozkłady 1 i 3 kart są darmowe dokładnie raz na całą instalację aplikacji - kolejne
     * próby (bez PRO/kredytu) pokazują zachętę do zakupu.
     */
    fun requestReading(question: String, spread: SpreadOption, intent: ReadingIntent?) {
        val current = settings.value
        if (spread.requiresPro && !current.isProUnlocked) {
            viewModelScope.launch { _proRequiredEvent.emit(Unit) }
            return
        }
        if (!current.canAskFreeQuestion) {
            viewModelScope.launch { _proRequiredEvent.emit(Unit) }
            return
        }

        viewModelScope.launch {
            if (current.isProUnlocked) {
                // Subskrybenci PRO nie zużywają kredytów, ale podlegają dziennemu limitowi
                // fair-use (DAILY_PRO_QUESTION_LIMIT) - powyżej niego prosimy o powrót jutro.
                if (!settingsDataStore.recordProQuestionUsage()) {
                    _dailyLimitEvent.emit(Unit)
                    return@launch
                }
            } else {
                settingsDataStore.consumeAskAllowance(current.canAskFreeToday)
            }

            // Losowanie jest całkowicie niezależne od pytania, intencji, PRO i reklam - odczyt
            // interpretuje wynik, ale nigdy nie wybiera ani nie wpływa na wylosowane karty.
            val allCards = cardRepository.getAllCards()
            val drawnRaw = allCards.shuffled().take(spread.cardCount)
            val positions = positionLabelsFor(spread)
            val drawn = drawnRaw.mapIndexed { index, card ->
                DrawnCard(
                    card = card,
                    isReversed = current.reversedCardsEnabled && Random.nextBoolean(),
                    positionLabel = positions?.getOrNull(index),
                )
            }
            val trimmedQuestion = question.trim().takeIf { it.isNotBlank() }
            val intentLabel = intent?.let { context.getString(it.labelResId) }

            _uiState.value = AskUiState.Reading(
                question = trimmedQuestion,
                spreadDbType = spread.dbType,
                cards = drawn.map { ReadingCardState(it) },
                aiState = AiReadingState.Loading,
                intent = intentLabel,
            )
            generateReading(trimmedQuestion, drawn, intentLabel)
        }
    }

    /** Odsłania jedną kartę w trwającym odczycie (animacja obrotu obsługiwana w UI). */
    fun flipCard(index: Int) {
        val state = _uiState.value
        if (state !is AskUiState.Reading) return
        val updated = state.cards.mapIndexed { i, c -> if (i == index) c.copy(isFlipped = true) else c }
        _uiState.value = state.copy(cards = updated)
    }

    /** Ponawia zapytanie do modelu po błędzie, bez losowania kart na nowo. */
    fun retryReading() {
        val state = _uiState.value
        if (state !is AskUiState.Reading) return
        _uiState.value = state.copy(aiState = AiReadingState.Loading)
        generateReading(state.question, state.cards.map { it.drawn }, state.intent)
    }

    /**
     * Wraca do ekranu wprowadzania nowego pytania. Sam zapis w Dzienniku dzieje się już
     * automatycznie (patrz [generateReading] i [submitFollowUp]) - ten przycisk to teraz
     * tylko "gotowe, zakończ", z zapisem awaryjnym na wypadek, gdyby z jakiegoś powodu
     * auto-zapis się nie zdążył (np. odczyt zakończony błędem AI).
     */
    fun finishAndSaveReading() {
        val state = _uiState.value
        if (state !is AskUiState.Reading) return
        viewModelScope.launch {
            if (state.savedReadingId == null) {
                val aiResponseText = (state.aiState as? AiReadingState.Success)?.text
                journalRepository.saveReading(
                    state.spreadDbType,
                    state.question,
                    state.cards.map { it.drawn },
                    aiResponseText,
                    state.followUps,
                )
            }
            if (settingsDataStore.recordCompletedReadingAndCheckReview()) {
                _reviewRequestEvent.emit(Unit)
            }
            _uiState.value = AskUiState.Setup
        }
    }

    /**
     * Punkt wejścia dla UI, gdy użytkownik chce zadać kolejne dopytanie: sam decyduje, którą
     * "przepustką" je odblokować, więc ekran nie musi znać zasad monetyzacji.
     * - Subskrybenci PRO: zawsze bez ograniczeń poza dziennym limitem fair-use - po jego
     *   przekroczeniu emitowany jest [dailyLimitEvent].
     * - Pierwsze dopytanie w danym odczycie: zawsze darmowe.
     * - Kolejne (bez PRO): emitowany jest [followUpPaywallEvent] - ekran pokazuje wtedy
     *   [com.mazur.tarot.ui.components.PaywallDialog] z opcją reklamy/kredytu/zakupu.
     */
    fun requestFollowUpUnlock() {
        val state = _uiState.value
        if (state !is AskUiState.Reading) return
        val current = settings.value
        viewModelScope.launch {
            when {
                current.isProUnlocked -> {
                    if (settingsDataStore.recordProQuestionUsage()) unlockFollowUp() else _dailyLimitEvent.emit(Unit)
                }
                state.nextFollowUpIsFree -> unlockFollowUp()
                else -> _followUpPaywallEvent.emit(Unit)
            }
        }
    }

    /** Odsłania pole dopytania - wywoływane po przejściu przez bramę w [requestFollowUpUnlock]
     * albo bezpośrednio po obejrzeniu reklamy/zużyciu kredytu w Paywallu. */
    fun unlockFollowUp() {
        val state = _uiState.value
        if (state !is AskUiState.Reading) return
        if (state.followUpState is FollowUpState.Loading || state.followUpState is FollowUpState.Unlocked) return
        _uiState.value = state.copy(followUpState = FollowUpState.Unlocked)
    }

    /** Zużywa jeden kredyt pytania z puli (reklama/pakiet) i - jeśli się udało - odblokowuje dopytanie. */
    fun useCreditForFollowUp() {
        viewModelScope.launch {
            if (settingsDataStore.consumeQuestionCredit()) {
                unlockFollowUp()
            }
        }
    }

    /**
     * Wysyła jedno dopytanie kontekstowe do tego samego odczytu (te same karty, cała
     * dotychczasowa konwersacja jako kontekst). Udane dopytanie dopisuje nową "chmurkę" czatu
     * do [AskUiState.Reading.followUps] - konwersacja rośnie w dół, nic nie jest nadpisywane.
     */
    fun submitFollowUp(question: String) {
        val state = _uiState.value
        if (state !is AskUiState.Reading) return
        val previousAnswer = state.followUps.lastOrNull()?.answer
            ?: (state.aiState as? AiReadingState.Success)?.text
            ?: return
        val trimmed = question.trim()
        if (trimmed.isEmpty()) return

        _uiState.value = state.copy(followUpState = FollowUpState.Loading)
        viewModelScope.launch {
            val request = AiReadingRequest(
                question = state.question,
                cards = state.cards.map { AiCardEntry(it.drawn.positionLabel, it.drawn.card.name, it.drawn.isReversed) },
                intent = state.intent,
                userName = settings.value.userName,
                userGender = settings.value.userGender,
                languageCode = resolvedAppLocale(context).language,
            )
            val result = aiReadingRepository.generateFollowUp(request, previousAnswer, trimmed)
            val current = _uiState.value
            if (current is AskUiState.Reading) {
                val updated = result.fold(
                    onSuccess = { answer ->
                        current.copy(
                            followUps = current.followUps + ChatTurn(trimmed, answer),
                            followUpState = FollowUpState.Locked,
                            followUpsUsed = current.followUpsUsed + 1,
                        )
                    },
                    onFailure = { throwable ->
                        // Błąd nie zużywa darmowej/kredytowej puli - użytkownik może spróbować ponownie.
                        current.copy(followUpState = FollowUpState.Error(trimmed, errorMessageFor(throwable)))
                    },
                )
                _uiState.value = updated
                if (result.isSuccess && updated.savedReadingId != null) {
                    journalRepository.updateAiResponseAndFollowUps(
                        updated.savedReadingId,
                        (updated.aiState as? AiReadingState.Success)?.text,
                        updated.followUps,
                    )
                }
            }
        }
    }

    /** Wysyła pytanie i wylosowane karty do modelu Gemini, publikuje wynik w stanie odczytu
     * i - przy sukcesie - od razu zapisuje odczyt w Dzienniku (bez czekania na "Zakończ"). */
    private fun generateReading(question: String?, drawnCards: List<DrawnCard>, intent: String?) {
        viewModelScope.launch {
            val request = AiReadingRequest(
                question = question,
                cards = drawnCards.map { AiCardEntry(it.positionLabel, it.card.name, it.isReversed) },
                intent = intent,
                userName = settings.value.userName,
                userGender = settings.value.userGender,
                languageCode = resolvedAppLocale(context).language,
            )
            val result = aiReadingRepository.generateReading(request)

            val current = _uiState.value
            if (current is AskUiState.Reading) {
                val aiState = result.fold(
                    onSuccess = { AiReadingState.Success(it) },
                    onFailure = { throwable -> AiReadingState.Error(errorMessageFor(throwable)) },
                )
                _uiState.value = current.copy(aiState = aiState)

                if (aiState is AiReadingState.Success) {
                    val savedId = journalRepository.saveReading(
                        current.spreadDbType,
                        current.question,
                        current.cards.map { it.drawn },
                        aiState.text,
                        current.followUps,
                    )
                    val afterSave = _uiState.value
                    if (afterSave is AskUiState.Reading) {
                        _uiState.value = afterSave.copy(savedReadingId = savedId)
                    }
                }
            }
        }
    }

    private fun positionLabelsFor(spread: SpreadOption): List<String>? = when (spread) {
        SpreadOption.ONE -> null
        SpreadOption.THREE -> listOf(
            context.getString(R.string.position_past),
            context.getString(R.string.position_present),
            context.getString(R.string.position_future),
        )
        SpreadOption.FIVE -> listOf(
            context.getString(R.string.position_situation),
            context.getString(R.string.position_challenge),
            context.getString(R.string.position_past),
            context.getString(R.string.position_future),
            context.getString(R.string.position_result),
        )
    }

    private fun errorMessageFor(throwable: Throwable): String = if (throwable is IOException) {
        context.getString(R.string.error_no_internet)
    } else {
        context.getString(R.string.error_ai_generic)
    }
}
