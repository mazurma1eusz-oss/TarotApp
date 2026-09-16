package com.mazur.tarot.ui.screens.encyclopedia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mazur.tarot.data.local.datastore.SettingsDataStore
import com.mazur.tarot.data.model.CardType
import com.mazur.tarot.data.model.TarotCard
import com.mazur.tarot.data.repository.CardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EncyclopediaViewModel(
    private val cardRepository: CardRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val allCards = MutableStateFlow<List<TarotCard>>(emptyList())
    val searchQuery = MutableStateFlow("")
    val typeFilter = MutableStateFlow<CardType?>(null)
    val favoritesOnly = MutableStateFlow(false)

    val favoriteIds: StateFlow<Set<Int>> = settingsDataStore.settingsFlow
        .map { it.favoriteCardIds }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    val filteredCards: StateFlow<List<TarotCard>> = combine(
        allCards, searchQuery, typeFilter, favoritesOnly, favoriteIds,
    ) { cards, query, type, favOnly, favIds ->
        cards.filter { card ->
            val matchesType = type == null || card.type == type
            val matchesQuery = query.isBlank() || card.name.contains(query, ignoreCase = true)
            val matchesFavorite = !favOnly || favIds.contains(card.id)
            matchesType && matchesQuery && matchesFavorite
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            allCards.value = cardRepository.getAllCards().sortedBy { it.id }
        }
    }

    fun onSearchQueryChange(value: String) {
        searchQuery.value = value
    }

    fun onFilterChange(value: CardType?) {
        typeFilter.value = value
    }

    fun toggleFavoritesOnly() {
        favoritesOnly.value = !favoritesOnly.value
    }
}
