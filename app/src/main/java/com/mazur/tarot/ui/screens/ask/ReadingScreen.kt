package com.mazur.tarot.ui.screens.ask

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazur.tarot.R
import com.mazur.tarot.data.model.ChatTurn
import com.mazur.tarot.ui.components.CardImage
import com.mazur.tarot.ui.components.FlipCard
import com.mazur.tarot.ui.components.MysticGradientButton
import com.mazur.tarot.ui.components.MysticTextField
import com.mazur.tarot.ui.theme.MysticGold
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticOutline
import com.mazur.tarot.ui.theme.MysticPanel
import com.mazur.tarot.ui.theme.MysticPurple
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.ui.theme.MysticTextSecondary
import com.mazur.tarot.util.AiResponseFormatter
import com.mazur.tarot.util.HapticUtil

/**
 * Osobny widok odczytu: zakryte karty do odsłonięcia stukiem oraz ciągła konwersacja
 * (czat) z AI, która rośnie w dół wraz z kolejnymi dopytaniami - żadna wcześniejsza
 * chmurka nigdy nie jest nadpisywana ani usuwana. Główna odpowiedź pojawia się poniżej
 * kart dopiero, gdy oba warunki są spełnione: użytkownik odwrócił wszystkie karty ORAZ
 * odczyt zdążył się przygotować (allCardsFlipped && isAiFinished).
 */
@Composable
fun ReadingScreen(
    state: AskUiState.Reading,
    onFlipCard: (Int) -> Unit,
    onRetry: () -> Unit,
    onFinish: () -> Unit,
    onFollowUpUnlockRequested: () -> Unit,
    onSubmitFollowUp: (String) -> Unit,
    soundVibrationEnabled: Boolean,
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val showResponseSection = state.allCardsFlipped && state.isAiFinished

    // Konwersacja rośnie w dół - przy każdej nowej chmurce (dopytanie/odpowiedź) przewijamy
    // czat do samego dołu, żeby użytkownik od razu widział najnowszą wymianę zdań.
    LaunchedEffect(state.followUps.size, state.followUpState) {
        val lastIndex = listState.layoutInfo.totalItemsCount - 1
        if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "question_header") {
            state.question?.let {
                Text(
                    text = "„$it”",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        item(key = "cards_row") {
            // Karty mają zawsze ten sam rozmiar co w rozkładzie 3-kartowym, niezależnie od
            // liczby wylosowanych kart - przy 1 karcie Row miałby tylko jedno dziecko z
            // weight(1f), które rozciągnęłoby się na całą szerokość ekranu. Zamiast tego
            // liczymy stałą szerokość karty na bazie podziału na 3, a przy 5 kartach (PRO)
            // dzielimy na dwa rzędy (3 + 2) zamiast poziomego przewijania.
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cardSpacing = 10.dp
                val cardWidth = (maxWidth - cardSpacing * 2) / 3
                val rows = state.cards.withIndex().chunked(3)
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(cardSpacing, Alignment.CenterHorizontally),
                        ) {
                            row.forEach { (index, cardState) ->
                                Column(modifier = Modifier.width(cardWidth)) {
                                    cardState.drawn.positionLabel?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }
                                    FlipCard(
                                        isRevealed = cardState.isFlipped,
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            if (!cardState.isFlipped) {
                                                if (soundVibrationEnabled) HapticUtil.vibrate(context)
                                            }
                                            onFlipCard(index)
                                        },
                                        back = { CardImage(imageResName = "card_back") },
                                        front = {
                                            CardImage(
                                                imageResName = cardState.drawn.card.imageResName,
                                                isReversed = cardState.drawn.isReversed,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            if (!showResponseSection) {
                Text(
                    text = if (!state.allCardsFlipped) {
                        stringResource(R.string.reading_flip_hint)
                    } else {
                        stringResource(R.string.reading_waiting_hint)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MysticTextSecondary,
                )
            }
        }

        item(key = "main_answer") {
            AnimatedVisibility(
                visible = showResponseSection,
                enter = fadeIn(animationSpec = tween(500)) + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ReadingResponseSection(aiState = state.aiState, onRetry = onRetry)
                }
            }
        }

        if (showResponseSection && state.aiState is AiReadingState.Success) {
            itemsIndexed(state.followUps, key = { index, _ -> "followup_$index" }) { _, turn ->
                Spacer(modifier = Modifier.height(14.dp))
                ChatBubblePair(turn = turn)
            }

            item(key = "followup_gate") {
                Spacer(modifier = Modifier.height(16.dp))
                FollowUpGate(
                    followUpState = state.followUpState,
                    onUnlockRequested = onFollowUpUnlockRequested,
                    onSubmit = onSubmitFollowUp,
                )
            }
        }

        item(key = "finish_button") {
            Spacer(modifier = Modifier.height(28.dp))
            MysticGradientButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.ask_finish_save),
                    style = MaterialTheme.typography.titleMedium,
                    color = MysticTextPrimary,
                )
            }
        }
    }
}

/** Jedna wymiana zdań w konwersacji: pytanie użytkownika (chmurka po prawej) i odpowiedź AI (po lewej). */
@Composable
private fun ChatBubblePair(turn: ChatTurn) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(MysticPurple.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .border(1.dp, MysticPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            Text(
                text = turn.question,
                style = MaterialTheme.typography.bodyMedium,
                color = MysticTextPrimary,
            )
        }
    }
    Spacer(modifier = Modifier.height(10.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(MysticPanel, RoundedCornerShape(16.dp))
                .border(1.dp, MysticOutline, RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            Text(
                text = turn.answer.replace("*", ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MysticTextPrimary,
            )
        }
    }
}

@Composable
private fun ReadingResponseSection(aiState: AiReadingState, onRetry: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MysticPanel, RoundedCornerShape(16.dp))
            .border(1.dp, MysticOutline, RoundedCornerShape(16.dp))
            .padding(18.dp),
    ) {
        when (aiState) {
            is AiReadingState.Loading -> {
                val infiniteTransition = rememberInfiniteTransition(label = "readingPulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 900, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "readingPulseAlpha",
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(20.dp)
                            .width(20.dp)
                            .graphicsLayer { this.alpha = alpha },
                        color = MaterialTheme.colorScheme.secondary,
                        strokeWidth = 2.dp,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.reading_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MysticTextSecondary,
                        modifier = Modifier.graphicsLayer { this.alpha = alpha },
                    )
                }
            }

            is AiReadingState.Success -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.height(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.reading_response_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MysticHeadingGold,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                AiResponseFormatter.parse(aiState.text, context).forEachIndexed { index, section ->
                    if (index > 0) Spacer(modifier = Modifier.height(14.dp))
                    section.heading?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleSmall,
                            color = MysticHeadingGold,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = section.body,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MysticTextPrimary,
                    )
                }
            }

            is AiReadingState.Error -> {
                Text(
                    text = aiState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MysticTextSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onRetry) {
                    Text(stringResource(R.string.reading_retry))
                }
            }
        }
    }
}

/**
 * Brama/pole wejścia dla NASTĘPNEGO, jeszcze nie wysłanego dopytania. [FollowUpState.Locked]
 * pokazuje tylko przycisk - decyzję o tym, czy od razu odblokować (PRO/kredyt) czy najpierw
 * pokazać reklamę, podejmuje wywołujący ekran w [onUnlockRequested]. Udane dopytania NIE
 * przechodzą przez ten stan - trafiają od razu jako nowa chmurka czatu w [ReadingScreen].
 */
@Composable
private fun FollowUpGate(
    followUpState: FollowUpState,
    onUnlockRequested: () -> Unit,
    onSubmit: (String) -> Unit,
) {
    when (followUpState) {
        FollowUpState.Locked -> {
            OutlinedButton(onClick = onUnlockRequested, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.follow_up_locked_button))
            }
        }

        FollowUpState.Unlocked -> {
            var followUpText by rememberSaveable { mutableStateOf("") }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MysticPanel, RoundedCornerShape(16.dp))
                    .border(1.dp, MysticGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                MysticTextField(
                    value = followUpText,
                    onValueChange = { followUpText = it },
                    label = stringResource(R.string.follow_up_text_field_label),
                    accentColor = MysticGold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(12.dp))
                MysticGradientButton(
                    onClick = { onSubmit(followUpText) },
                    enabled = followUpText.trim().length >= 3,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.follow_up_send_button),
                        style = MaterialTheme.typography.titleMedium,
                        color = MysticTextPrimary,
                    )
                }
            }
        }

        FollowUpState.Loading -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MysticPanel, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp).width(18.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.follow_up_loading_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MysticTextSecondary,
                )
            }
        }

        is FollowUpState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MysticPanel, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text(
                    text = followUpState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MysticTextSecondary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = { onSubmit(followUpState.question) }) {
                    Text(stringResource(R.string.reading_retry))
                }
            }
        }
    }
}
