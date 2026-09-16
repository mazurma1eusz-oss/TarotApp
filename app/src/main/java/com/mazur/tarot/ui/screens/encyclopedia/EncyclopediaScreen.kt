package com.mazur.tarot.ui.screens.encyclopedia

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mazur.tarot.R
import com.mazur.tarot.data.model.CardType
import com.mazur.tarot.data.model.TarotCard
import com.mazur.tarot.ui.components.CardArt
import com.mazur.tarot.ui.tarotApp
import com.mazur.tarot.ui.theme.MysticHeadingGold

@Composable
fun EncyclopediaScreen(onCardClick: (Int) -> Unit) {
    val app = tarotApp()
    val viewModel: EncyclopediaViewModel = viewModel(
        factory = remember {
            viewModelFactory {
                initializer { EncyclopediaViewModel(app.cardRepository, app.settingsDataStore) }
            }
        },
    )
    val cards by viewModel.filteredCards.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val filter by viewModel.typeFilter.collectAsState()
    val favoritesOnly by viewModel.favoritesOnly.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.encyclopedia_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MysticHeadingGold,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onSearchQueryChange,
            label = { Text(stringResource(R.string.search_hint)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            FilterChip(
                selected = filter == null && !favoritesOnly,
                onClick = { viewModel.onFilterChange(null); if (favoritesOnly) viewModel.toggleFavoritesOnly() },
                label = { Text(stringResource(R.string.filter_all)) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = filter == CardType.MAJOR,
                onClick = { viewModel.onFilterChange(CardType.MAJOR) },
                label = { Text(stringResource(R.string.filter_major)) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = filter == CardType.MINOR,
                onClick = { viewModel.onFilterChange(CardType.MINOR) },
                label = { Text(stringResource(R.string.filter_minor)) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilterChip(
                selected = favoritesOnly,
                onClick = { viewModel.toggleFavoritesOnly() },
                leadingIcon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
                label = { Text(stringResource(R.string.filter_favorites)) },
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(cards, key = { it.id }) { card ->
                CardListRow(card = card, onClick = { onCardClick(card.id) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CardListRow(card: TarotCard, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        ListItem(
            headlineContent = {
                Text(
                    card.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MysticHeadingGold,
                    fontWeight = FontWeight.Bold,
                )
            },
            supportingContent = {
                Text(
                    text = if (card.type == CardType.MAJOR) stringResource(R.string.filter_major) else stringResource(R.string.filter_minor),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            leadingContent = {
                CardArt(
                    imageResName = card.imageResName,
                    glow = false,
                    modifier = Modifier.width(46.dp),
                )
            },
        )
    }
}
