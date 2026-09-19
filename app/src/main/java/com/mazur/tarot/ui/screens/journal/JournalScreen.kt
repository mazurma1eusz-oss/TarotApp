package com.mazur.tarot.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mazur.tarot.R
import com.mazur.tarot.data.model.CardType
import com.mazur.tarot.data.repository.ReadingDetails
import com.mazur.tarot.ui.components.CardArt
import com.mazur.tarot.ui.tarotApp
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticPanel
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.ui.theme.MysticTextSecondary
import com.mazur.tarot.util.resolvedAppLocale
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun JournalScreen(onUnlockPro: () -> Unit) {
    val app = tarotApp()
    val viewModel: JournalViewModel = viewModel(
        factory = remember {
            viewModelFactory {
                initializer { JournalViewModel(app.journalRepository, app.settingsDataStore) }
            }
        },
    )
    val state by viewModel.uiState.collectAsState()
    var selectedReading by remember { mutableStateOf<ReadingDetails?>(null) }

    selectedReading?.let { reading ->
        ReadingDetailDialog(reading = reading, onDismiss = { selectedReading = null })
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.journal_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MysticHeadingGold,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (!state.isProUnlocked) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.journal_locked_message), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = onUnlockPro) { Text(stringResource(R.string.unlock_pro)) }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            state.isLoading -> {
                CircularProgressIndicator()
            }
            state.readings.isEmpty() -> {
                Text(
                    text = stringResource(R.string.journal_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        JournalStatsCard(readings = state.readings)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    items(state.readings, key = { it.id }) { reading ->
                        JournalEntryCard(
                            reading = reading,
                            onNoteSave = { note -> viewModel.updateNote(reading.id, note) },
                            onClick = { selectedReading = reading },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalEntryCard(reading: ReadingDetails, onNoteSave: (String) -> Unit, onClick: () -> Unit) {
    var isEditing by remember { mutableStateOf(false) }
    var noteDraft by rememberSaveable(reading.id) { mutableStateOf(reading.note) }
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", resolvedAppLocale(context)) }
    val reversedSuffix = stringResource(R.string.reversed_suffix)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.clickable(enabled = !isEditing, onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = dateFormat.format(Date(reading.timestampMillis)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = spreadLabel(reading.spreadType),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            reading.question?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "„$it”", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                reading.drawnCards.forEach { drawn ->
                    CardArt(
                        imageResName = drawn.card.imageResName,
                        isReversed = drawn.isReversed,
                        glow = false,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = reading.drawnCards.joinToString(", ") { it.card.name + if (it.isReversed) " $reversedSuffix" else "" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(10.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = noteDraft,
                    onValueChange = { noteDraft = it },
                    label = { Text(stringResource(R.string.journal_note_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = {
                        onNoteSave(noteDraft)
                        isEditing = false
                    }) {
                        Text(stringResource(R.string.journal_save_note))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (reading.note.isNotBlank()) {
                        Text(text = reading.note, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    TextButton(onClick = { isEditing = true }, modifier = Modifier.align(Alignment.End)) {
                        Text(if (reading.note.isBlank()) stringResource(R.string.journal_note_hint) else stringResource(R.string.journal_edit_note))
                    }
                }
            }
        }
    }
}

/**
 * Spokojna, nieprzeładowana statystyka "Moja Historia Tarota" wyliczana bezpośrednio
 * z już wczytanej listy odczytów - bez dodatkowych zapytań do bazy ani nowego schematu.
 */
@Composable
private fun JournalStatsCard(readings: List<ReadingDetails>) {
    val allDrawn = readings.flatMap { it.drawnCards }
    if (allDrawn.isEmpty()) return

    val mostDrawnName = allDrawn
        .groupingBy { it.card.name }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    val reversedCount = allDrawn.count { it.isReversed }
    val uprightCount = allDrawn.size - reversedCount
    val majorCount = allDrawn.count { it.card.type == CardType.MAJOR }
    val minorCount = allDrawn.size - majorCount
    val mostDrawnElement = allDrawn
        .mapNotNull { elementLabelFor(it.card.imageResName) }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MysticPanel, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.journal_stats_title),
            style = MaterialTheme.typography.titleSmall,
            color = MysticHeadingGold,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        StatsRow(label = stringResource(R.string.journal_stats_reading_count), value = readings.size.toString())
        mostDrawnName?.let {
            StatsRow(label = stringResource(R.string.journal_stats_most_drawn_card), value = it)
        }
        StatsRow(label = stringResource(R.string.journal_stats_upright_reversed), value = "$uprightCount / $reversedCount")
        StatsRow(label = stringResource(R.string.journal_stats_major_minor), value = "$majorCount / $minorCount")
        mostDrawnElement?.let {
            StatsRow(label = stringResource(R.string.journal_stats_most_drawn_element), value = it)
        }
    }
}

@Composable
private fun elementLabelFor(imageResName: String): String? = when (imageResName.substringBefore("_")) {
    "wands" -> stringResource(R.string.element_fire)
    "cups" -> stringResource(R.string.element_water)
    "swords" -> stringResource(R.string.element_air)
    "pentacles" -> stringResource(R.string.element_earth)
    else -> null
}

@Composable
private fun StatsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MysticTextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, color = MysticTextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun spreadLabel(spreadType: String): String = when (spreadType) {
    "CARD_OF_DAY" -> stringResource(R.string.spread_label_card_of_day)
    "ONE_CARD" -> stringResource(R.string.spread_label_one)
    "THREE_CARD" -> stringResource(R.string.spread_label_three)
    "FIVE_CARD" -> stringResource(R.string.spread_label_five)
    else -> spreadType
}
