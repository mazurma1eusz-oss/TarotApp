package com.mazur.tarot.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import com.mazur.tarot.data.repository.JournalRepository
import com.mazur.tarot.data.repository.ReadingDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JournalUiState(
    val readings: List<ReadingDetails> = emptyList(),
    val isProUnlocked: Boolean = false,
    val isLoading: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
class JournalViewModel(
    private val journalRepository: JournalRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val isProFlow = settingsDataStore.settingsFlow.map { it.isProUnlocked }.distinctUntilChanged()

    val uiState: StateFlow<JournalUiState> = isProFlow
        .flatMapLatest { isPro ->
            journalRepository.observeReadings(isPro).map { readings ->
                JournalUiState(readings = readings, isProUnlocked = isPro, isLoading = false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalUiState())

    fun updateNote(readingId: Long, note: String) {
        viewModelScope.launch {
            journalRepository.updateNote(readingId, note)
        }
    }
}
