package com.mazur.tarot.ui.screens.journal

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mazur.tarot.R
import com.mazur.tarot.data.local.db.SpreadType
import com.mazur.tarot.data.model.ChatTurn
import com.mazur.tarot.data.repository.ReadingDetails
import com.mazur.tarot.ui.components.CardArt
import com.mazur.tarot.ui.components.DrawnCardRow
import com.mazur.tarot.ui.components.InterpretationSections
import com.mazur.tarot.ui.components.SharePreviewDialog
import com.mazur.tarot.ui.theme.MysticBackground
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticOutline
import com.mazur.tarot.ui.theme.MysticPanel
import com.mazur.tarot.ui.theme.MysticPurple
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.ui.theme.MysticTextSecondary
import com.mazur.tarot.util.AiResponseFormatter
import com.mazur.tarot.util.CardShareUtil
import com.mazur.tarot.util.resolvedAppLocale
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Pełny podgląd zapisanego odczytu z Dziennika: data, pytanie, miniaturki wylosowanych
 * kart z nazwami i pozycjami (bez encyklopedycznych opisów - te są dostępne osobno w
 * Encyklopedii) oraz zapisana odpowiedź wygenerowana przez model AI.
 */
@Composable
fun ReadingDetailDialog(reading: ReadingDetails, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("d MMMM yyyy, HH:mm", resolvedAppLocale(context)) }
    var includeQuestionOnShare by remember { mutableStateOf(false) }
    var sharePreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sharePreviewText by remember { mutableStateOf("") }
    val shareChooserTitle = stringResource(R.string.share_chooser_title)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(MysticBackground, RoundedCornerShape(20.dp))
                .border(1.dp, MysticOutline, RoundedCornerShape(20.dp))
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = readingSpreadLabel(reading.spreadType),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MysticHeadingGold,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = dateFormat.format(Date(reading.timestampMillis)),
                        style = MaterialTheme.typography.labelLarge,
                        color = MysticTextSecondary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = MysticTextSecondary)
                }
            }

            reading.question?.let {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "„$it”",
                    style = MaterialTheme.typography.titleMedium,
                    color = MysticTextPrimary,
                )
            }

            if (reading.spreadType == SpreadType.CARD_OF_DAY && reading.drawnCards.isNotEmpty()) {
                val mainCard = reading.drawnCards.first()
                val bonusCards = reading.drawnCards.drop(1)

                Spacer(modifier = Modifier.height(20.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    CardArt(
                        imageResName = mainCard.card.imageResName,
                        isReversed = mainCard.isReversed,
                        modifier = Modifier.fillMaxWidth(0.55f),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = mainCard.card.name + if (mainCard.isReversed) " " + stringResource(R.string.reversed_suffix) else "",
                        style = MaterialTheme.typography.titleMedium,
                        color = MysticHeadingGold,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                InterpretationSections(card = mainCard.card, isReversed = mainCard.isReversed)

                bonusCards.forEach { bonus ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MysticPanel, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        DrawnCardRow(drawn = bonus, label = bonus.positionLabel, modifier = Modifier.fillMaxWidth())
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    reading.drawnCards.forEach { drawn ->
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            drawn.positionLabel?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            CardArt(
                                imageResName = drawn.card.imageResName,
                                isReversed = drawn.isReversed,
                                glow = false,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = drawn.card.name + if (drawn.isReversed) " " + stringResource(R.string.reversed_suffix) else "",
                                style = MaterialTheme.typography.labelMedium,
                                color = MysticTextSecondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                reading.aiResponse?.let { response ->
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MysticPanel, RoundedCornerShape(16.dp))
                            .border(1.dp, MysticOutline, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.reading_response_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MysticHeadingGold,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        AiResponseFormatter.parse(response, context).forEachIndexed { index, section ->
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
                }

                if (reading.followUps.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    reading.followUps.forEach { turn ->
                        SavedChatBubblePair(turn = turn)
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            if (reading.question != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = includeQuestionOnShare,
                        onCheckedChange = { includeQuestionOnShare = it },
                    )
                    Text(
                        text = stringResource(R.string.share_show_question),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MysticTextSecondary,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
            OutlinedButton(
                onClick = {
                    sharePreviewBitmap = CardShareUtil.renderReadingBitmap(context, reading, includeQuestionOnShare)
                    val cardNames = reading.drawnCards.map { it.card.name + if (it.isReversed) " (odwr.)" else "" }
                    sharePreviewText = CardShareUtil.buildShareCaption(context, cardNames)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Text(text = " " + stringResource(R.string.share), modifier = Modifier.padding(start = 4.dp))
            }
        }
    }

    sharePreviewBitmap?.let { bitmap ->
        SharePreviewDialog(
            bitmap = bitmap,
            onConfirm = {
                CardShareUtil.shareBitmap(context, bitmap, shareChooserTitle, "reading_share.png", sharePreviewText)
                sharePreviewBitmap = null
            },
            onDismiss = { sharePreviewBitmap = null },
        )
    }
}

/** Zapisana wymiana zdań (pytanie/odpowiedź) z historii dopytań tego odczytu. */
@Composable
private fun SavedChatBubblePair(turn: ChatTurn) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(MysticPurple.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                .border(1.dp, MysticPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            Text(text = turn.question, style = MaterialTheme.typography.bodyMedium, color = MysticTextPrimary)
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
            Text(text = turn.answer.replace("*", ""), style = MaterialTheme.typography.bodyMedium, color = MysticTextPrimary)
        }
    }
}

@Composable
private fun readingSpreadLabel(spreadType: String): String = when (spreadType) {
    "CARD_OF_DAY" -> stringResource(R.string.spread_label_card_of_day)
    "ONE_CARD" -> stringResource(R.string.spread_label_one)
    "THREE_CARD" -> stringResource(R.string.spread_label_three)
    "FIVE_CARD" -> stringResource(R.string.spread_label_five)
    else -> spreadType
}
