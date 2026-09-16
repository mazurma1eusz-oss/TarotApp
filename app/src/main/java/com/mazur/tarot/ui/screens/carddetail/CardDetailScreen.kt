package com.mazur.tarot.ui.screens.carddetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mazur.tarot.ui.components.CardArt
import com.mazur.tarot.ui.components.InterpretationSections
import com.mazur.tarot.ui.tarotApp
import com.mazur.tarot.ui.theme.MysticGold
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(cardId: Int, onBack: () -> Unit) {
    val app = tarotApp()
    val viewModel: CardDetailViewModel = viewModel(
        factory = remember(cardId) {
            viewModelFactory {
                initializer { CardDetailViewModel(app.cardRepository, app.settingsDataStore, cardId) }
            }
        },
    )
    val card by viewModel.card.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = card?.name.orEmpty(),
                    color = MysticHeadingGold,
                    fontWeight = FontWeight.Bold,
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            },
            actions = {
                IconButton(onClick = { viewModel.toggleFavorite() }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isFavorite) MysticGold else MysticTextSecondary,
                    )
                }
            },
        )

        val currentCard = card
        if (currentCard == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CardArt(
                    imageResName = currentCard.imageResName,
                    modifier = Modifier.fillMaxWidth(0.55f),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = currentCard.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MysticHeadingGold,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(16.dp))
                InterpretationSections(card = currentCard, isReversed = false)
            }
        }
    }
}
