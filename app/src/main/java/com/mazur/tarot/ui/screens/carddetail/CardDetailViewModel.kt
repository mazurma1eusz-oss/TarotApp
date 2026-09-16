package com.mazur.tarot.ui.screens.carddetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import com.mazur.tarot.data.model.TarotCard
import com.mazur.tarot.data.repository.CardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardDetailViewModel(
    private val cardRepository: CardRepository,
    private val settingsDataStore: SettingsDataStore,
    private val cardId: Int,
) : ViewModel() {

    private val _card = MutableStateFlow<TarotCard?>(null)
    val card: StateFlow<TarotCard?> = _card.asStateFlow()

    /** Ulubione działają w pełni offline (DataStore) - bez AI, bez PRO. */
    val isFavorite: StateFlow<Boolean> = settingsDataStore.settingsFlow
        .map { it.favoriteCardIds.contains(cardId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            _card.value = cardRepository.getCardById(cardId)
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch { settingsDataStore.toggleFavorite(cardId) }
    }
}
