package com.mazur.tarot.ui.screens.ask

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mazur.tarot.R
import com.mazur.tarot.TarotApplication
import com.mazur.tarot.ui.components.CardAspectRatio
import com.mazur.tarot.ui.components.CardImage
import com.mazur.tarot.ui.components.MysticGradientButton
import com.mazur.tarot.ui.components.MysticTextField
import com.mazur.tarot.ui.components.PaywallDialog
import com.mazur.tarot.ui.tarotApp
import com.mazur.tarot.ui.theme.MysticGold
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticOutline
import com.mazur.tarot.ui.theme.MysticPanel
import com.mazur.tarot.ui.theme.MysticPurple
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.ui.theme.MysticTextSecondary
import com.mazur.tarot.util.InAppReviewHelper

@Composable
fun AskCardsScreen(onProRequired: () -> Unit) {
    val app = tarotApp()
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: AskCardsViewModel = viewModel(
        factory = remember {
            viewModelFactory {
                initializer {
                    AskCardsViewModel(app.cardRepository, app.journalRepository, app.settingsDataStore, app.aiReadingRepository)
                }
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    var question by rememberSaveable { mutableStateOf("") }
    var selectedSpread by rememberSaveable { mutableStateOf(SpreadOption.ONE) }
    var selectedIntent by rememberSaveable { mutableStateOf<ReadingIntent?>(null) }
    var showProDialog by remember { mutableStateOf(false) }
    var showFollowUpPaywall by remember { mutableStateOf(false) }
    var showDailyLimitDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.proRequiredEvent.collect { showProDialog = true }
    }
    LaunchedEffect(Unit) {
        viewModel.followUpPaywallEvent.collect { showFollowUpPaywall = true }
    }
    LaunchedEffect(Unit) {
        viewModel.dailyLimitEvent.collect { showDailyLimitDialog = true }
    }
    LaunchedEffect(Unit) {
        viewModel.reviewRequestEvent.collect { activity?.let { InAppReviewHelper.requestReview(it) } }
    }

    if (showDailyLimitDialog) {
        AlertDialog(
            onDismissRequest = { showDailyLimitDialog = false },
            title = { Text(stringResource(R.string.daily_limit_title)) },
            text = { Text(stringResource(R.string.daily_limit_message)) },
            confirmButton = {
                TextButton(onClick = { showDailyLimitDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    if (showProDialog) {
        HybridPaywallDialog(
            app = app,
            activity = activity,
            context = context,
            questionCredits = settings.questionCredits,
            onDismiss = { showProDialog = false },
            onAdRewardEarned = { viewModel.grantQuestionCredits(1) },
        )
    }

    if (showFollowUpPaywall) {
        val readingState = uiState as? AskUiState.Reading
        HybridPaywallDialog(
            app = app,
            activity = activity,
            context = context,
            questionCredits = settings.questionCredits,
            onDismiss = { showFollowUpPaywall = false },
            onAdRewardEarned = { viewModel.unlockFollowUp() },
            onUseCredit = if (readingState != null) {
                { viewModel.useCreditForFollowUp() }
            } else null,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.ask_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MysticHeadingGold,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (val state = uiState) {
                is AskUiState.Setup -> {
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        MysticTextField(
                            value = question,
                            onValueChange = { question = it },
                            label = stringResource(R.string.ask_question_hint),
                            minLines = 4,
                            accentColor = MysticGold,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                        )
                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Z czym dziś przychodzisz?",
                            style = MaterialTheme.typography.labelLarge,
                            color = MysticTextSecondary,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        IntentChipsRow(selected = selectedIntent, onSelect = { selectedIntent = it })
                        Spacer(modifier = Modifier.height(20.dp))

                        SpreadTile(
                            label = stringResource(R.string.ask_spread_1),
                            helper = stringResource(R.string.ask_spread_1_helper),
                            selected = selectedSpread == SpreadOption.ONE,
                            locked = false,
                            onClick = { selectedSpread = SpreadOption.ONE },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SpreadTile(
                            label = stringResource(R.string.ask_spread_3),
                            helper = stringResource(R.string.ask_spread_3_helper),
                            selected = selectedSpread == SpreadOption.THREE,
                            locked = false,
                            onClick = { selectedSpread = SpreadOption.THREE },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SpreadTile(
                            label = stringResource(R.string.ask_spread_5),
                            helper = stringResource(R.string.ask_spread_5_helper),
                            selected = selectedSpread == SpreadOption.FIVE,
                            locked = !settings.isProUnlocked,
                            onClick = { selectedSpread = SpreadOption.FIVE },
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                        MysticGradientButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.requestReading(question, selectedSpread, selectedIntent)
                            },
                            enabled = question.trim().length >= 5,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.ask_draw_button),
                                style = MaterialTheme.typography.titleMedium,
                                color = MysticTextPrimary,
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                is AskUiState.Reading -> {
                    ReadingScreen(
                        state = state,
                        onFlipCard = { index -> viewModel.flipCard(index) },
                        onRetry = { viewModel.retryReading() },
                        onFinish = {
                            viewModel.finishAndSaveReading()
                            question = ""
                            selectedIntent = null
                        },
                        onFollowUpUnlockRequested = { viewModel.requestFollowUpUnlock() },
                        onSubmitFollowUp = { followUpQuestion -> viewModel.submitFollowUp(followUpQuestion) },
                    )
                }
            }
        }
    }
}

/**
 * Wspólne okno hybrydowej monetyzacji (subskrypcja tygodniowa/miesięczna, pakiety pytań,
 * reklama, opcjonalnie zużycie już posiadanego kredytu) - używane zarówno przy blokadzie
 * pierwszego pytania, jak i przy blokadzie kolejnego dopytania w trwającej konwersacji.
 */
@Composable
private fun HybridPaywallDialog(
    app: TarotApplication,
    activity: Activity?,
    context: Context,
    questionCredits: Int,
    onDismiss: () -> Unit,
    onAdRewardEarned: () -> Unit,
    onUseCredit: (() -> Unit)? = null,
) {
    // Świeży stan przy każdym otwarciu dialogu (composable jest tworzony/niszczony razem z
    // showProDialog/showFollowUpPaywall) - nie trzeba go ręcznie resetować przy zamknięciu.
    var adUnavailable by remember { mutableStateOf(false) }

    PaywallDialog(
        weeklyPriceLabel = app.billingManager.weeklyPriceLabel,
        monthlyPriceLabel = app.billingManager.monthlyPriceLabel,
        yearlyPriceLabel = app.billingManager.yearlyPriceLabel,
        packStartPriceLabel = app.billingManager.packStartPriceLabel,
        packStandardPriceLabel = app.billingManager.packStandardPriceLabel,
        questionCredits = questionCredits,
        onSubscribeWeekly = {
            onDismiss()
            activity?.let { app.billingManager.launchWeeklySubscriptionPurchaseFlow(it) }
        },
        onSubscribeMonthly = {
            onDismiss()
            activity?.let { app.billingManager.launchSubscriptionPurchaseFlow(it) }
        },
        onSubscribeYearly = {
            onDismiss()
            activity?.let { app.billingManager.launchYearlySubscriptionPurchaseFlow(it) }
        },
        onBuyPackStart = { activity?.let { app.billingManager.launchPackStartPurchaseFlow(it) } },
        onBuyPackStandard = { activity?.let { app.billingManager.launchPackStandardPurchaseFlow(it) } },
        onRestore = { app.billingManager.restorePurchases() },
        onOpenTerms = {
            val url = context.getString(R.string.privacy_policy_url)
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        },
        onDismiss = onDismiss,
        onWatchAd = {
            adUnavailable = false
            activity?.let {
                app.rewardedAdManager.showAd(
                    activity = it,
                    onRewardEarned = {
                        onAdRewardEarned()
                        onDismiss()
                    },
                    onUnavailable = { adUnavailable = true },
                )
            }
        },
        onUseCredit = onUseCredit?.let { useCredit ->
            {
                useCredit()
                onDismiss()
            }
        },
        adUnavailable = adUnavailable,
    )
}

/**
 * Opcjonalne chipy intencji ("Z czym dziś przychodzisz?"). Wybór jest w pełni dobrowolny -
 * nie ma przycisku "Pomiń", bo domyślnie żadna intencja nie jest wybrana (kliknięcie już
 * zaznaczonego chipa odznacza go). Wpływa wyłącznie na kontekst przekazywany do AI.
 */
@Composable
private fun IntentChipsRow(selected: ReadingIntent?, onSelect: (ReadingIntent?) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ReadingIntent.entries.forEach { intent ->
            val isSelected = selected == intent
            Row(
                modifier = Modifier
                    .background(
                        color = if (isSelected) MysticPurple.copy(alpha = 0.28f) else MysticPanel,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) MysticGold else MysticOutline,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { onSelect(if (isSelected) null else intent) }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = intent.symbol, color = if (isSelected) MysticHeadingGold else MysticTextSecondary)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = intent.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MysticTextPrimary else MysticTextSecondary,
                )
            }
        }
    }
}

/** Duży, estetyczny kafelek wyboru rozkładu z grafiką rewersu karty zamiast pigułki tekstowej. */
@Composable
private fun SpreadTile(
    label: String,
    helper: String,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (selected) MysticPurple.copy(alpha = 0.22f) else MysticPanel,
                shape = RoundedCornerShape(16.dp),
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MysticGold else MysticOutline,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .aspectRatio(CardAspectRatio),
        ) {
            CardImage(imageResName = "card_back")
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) MysticHeadingGold else MysticTextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = MysticTextSecondary,
            )
        }
        if (locked) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MysticTextSecondary, modifier = Modifier.height(20.dp))
        }
    }
}
