package com.mazur.tarot.ui.screens.cardofday

import android.app.Activity
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.mazur.tarot.R
import com.mazur.tarot.data.local.datastore.AppSettings
import com.mazur.tarot.data.local.datastore.BONUS_WAIT_DURATION_MILLIS
import com.mazur.tarot.notifications.BONUS_CARDS_WAIT_WORK_NAME
import com.mazur.tarot.notifications.BonusCardsReadyWorker
import com.mazur.tarot.ui.components.CardAspectRatio
import com.mazur.tarot.ui.components.CardImage
import com.mazur.tarot.ui.components.DrawnCardRow
import com.mazur.tarot.ui.components.FlipCard
import com.mazur.tarot.ui.components.InterpretationSections
import com.mazur.tarot.ui.components.MysticGradientButton
import com.mazur.tarot.ui.components.SharePreviewDialog
import com.mazur.tarot.ui.tarotApp
import com.mazur.tarot.ui.theme.MysticGold
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticOutline
import com.mazur.tarot.ui.theme.MysticPanel
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.ui.theme.MysticTextSecondary
import com.mazur.tarot.util.CardShareUtil
import com.mazur.tarot.util.InAppReviewHelper
import java.util.concurrent.TimeUnit

@Composable
fun CardOfDayScreen() {
    val app = tarotApp()
    val viewModel: CardOfDayViewModel = viewModel(
        factory = remember {
            viewModelFactory {
                initializer {
                    CardOfDayViewModel(app.cardRepository, app.journalRepository, app.settingsDataStore)
                }
            }
        },
    )
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val activity = context as? Activity
    var sharePreviewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        viewModel.reviewRequestEvent.collect { activity?.let { InAppReviewHelper.requestReview(it) } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.card_of_day_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MysticHeadingGold,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp),
        )

        when (val state = uiState) {
            is CardOfDayUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is CardOfDayUiState.ReadyToDraw -> {
                ReadyToDrawHero(onDraw = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.drawCard()
                })
            }

            is CardOfDayUiState.Drawn -> {
                FlipCard(
                    isRevealed = state.revealed,
                    modifier = Modifier.height(300.dp),
                    onClick = {
                        if (!state.revealed) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.reveal()
                        }
                    },
                    back = {
                        CardImage(imageResName = "card_back")
                    },
                    front = {
                        CardImage(
                            imageResName = state.drawnCard.card.imageResName,
                            isReversed = state.drawnCard.isReversed,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (!state.revealed) {
                    Text(
                        text = stringResource(R.string.card_of_day_tap_to_reveal),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(durationMillis = 450)),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val cardTitle = state.drawnCard.card.name +
                                if (state.drawnCard.isReversed) " " + stringResource(R.string.reversed_suffix) else ""
                            Text(
                                text = cardTitle,
                                style = MaterialTheme.typography.titleLarge,
                                color = MysticHeadingGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp),
                            )

                            InterpretationSections(
                                card = state.drawnCard.card,
                                isReversed = state.drawnCard.isReversed,
                            )

                            OutlinedButton(
                                onClick = {
                                    sharePreviewBitmap = CardShareUtil.renderCardBitmap(
                                        context,
                                        state.drawnCard.card,
                                        state.drawnCard.isReversed,
                                    )
                                },
                                modifier = Modifier.padding(top = 16.dp),
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null)
                                Text(
                                    text = " " + stringResource(R.string.share),
                                    modifier = Modifier.padding(start = 4.dp),
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            if (!settings.isProUnlocked && !settings.bonusCardsUnlocked) {
                                BonusUnlockGate(
                                    settings = settings,
                                    onWatchAd = {
                                        activity?.let {
                                            app.rewardedAdManager.showAd(
                                                activity = it,
                                                onRewardEarned = { viewModel.unlockBonusCardsViaAd() },
                                            )
                                        }
                                    },
                                    onStartWait = {
                                        viewModel.startBonusWait()
                                        WorkManager.getInstance(context).enqueueUniqueWork(
                                            BONUS_CARDS_WAIT_WORK_NAME,
                                            ExistingWorkPolicy.REPLACE,
                                            OneTimeWorkRequestBuilder<BonusCardsReadyWorker>()
                                                .setInitialDelay(BONUS_WAIT_DURATION_MILLIS, TimeUnit.MILLISECONDS)
                                                .build(),
                                        )
                                    },
                                    onWaitFinished = { viewModel.completeBonusWait() },
                                )
                            } else {
                                if (state.supportCard == null) {
                                    OutlinedButton(
                                        onClick = { viewModel.drawSupportCard() },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.card_of_day_draw_support))
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MysticPanel, RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                    ) {
                                        DrawnCardRow(
                                            drawn = state.supportCard,
                                            label = stringResource(R.string.card_of_day_support_label),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                if (state.warningCard == null) {
                                    OutlinedButton(
                                        onClick = { viewModel.drawWarningCard() },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(stringResource(R.string.card_of_day_draw_warning))
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MysticPanel, RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                    ) {
                                        DrawnCardRow(
                                            drawn = state.warningCard,
                                            label = stringResource(R.string.card_of_day_warning_label),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }

    sharePreviewBitmap?.let { bitmap ->
        SharePreviewDialog(
            bitmap = bitmap,
            onConfirm = {
                CardShareUtil.shareBitmap(context, bitmap, "Udostępnij Kartę Dnia", "card_of_day_share.png")
                sharePreviewBitmap = null
            },
            onDismiss = { sharePreviewBitmap = null },
        )
    }
}

/**
 * Ekran "gotowy do losowania": zamiast płaskiego przycisku na środku, elegancka zakryta
 * karta tarota z powolną, złotą poświatą (pulsowanie + delikatne skalowanie) sugerującą
 * gotowość do odkrycia. Dotknięcie karty LUB przycisku poniżej losuje kartę - odwrócenie
 * (flip) następuje automatycznie chwilę później w [CardOfDayViewModel.drawCard].
 */
@Composable
private fun ReadyToDrawHero(onDraw: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Weź głęboki oddech i skup się na dzisiejszym dniu…",
            style = MaterialTheme.typography.bodyMedium,
            color = MysticTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(28.dp))

        val infiniteTransition = rememberInfiniteTransition(label = "cardOfDayGlow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.25f,
            targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "cardOfDayGlowAlpha",
        )
        val glowScale by infiniteTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "cardOfDayGlowScale",
        )

        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .graphicsLayer {
                        scaleX = glowScale
                        scaleY = glowScale
                        alpha = glowAlpha
                    }
                    .background(
                        Brush.radialGradient(listOf(MysticGold, MysticGold.copy(alpha = 0f))),
                        CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .height(240.dp)
                    .aspectRatio(CardAspectRatio)
                    .shadow(
                        elevation = 22.dp,
                        shape = RoundedCornerShape(14.dp),
                        ambientColor = MysticGold,
                        spotColor = MysticGold,
                    )
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDraw,
                    ),
            ) {
                CardImage(imageResName = "card_back")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.5.dp, MysticGold, RoundedCornerShape(28.dp)),
        ) {
            MysticGradientButton(onClick = onDraw, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "✦ ODKRYJ KARTĘ DNIA ✦",
                    style = MaterialTheme.typography.titleMedium,
                    color = MysticTextPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * Brama odblokowania kart dodatkowych (Wsparcie / Na Co Uważać): albo natychmiast przez
 * obejrzenie reklamy nagradzanej, albo za darmo po odczekaniu 60 minut (z tykającym
 * odliczaniem w UI oraz powiadomieniem push planowanym przez WorkManager w tle).
 */
@Composable
private fun BonusUnlockGate(
    settings: AppSettings,
    onWatchAd: () -> Unit,
    onStartWait: () -> Unit,
    onWaitFinished: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MysticPanel, RoundedCornerShape(12.dp))
            .border(1.dp, MysticOutline, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (settings.bonusWaitInProgress) {
            var remainingMillis by remember { mutableLongStateOf(settings.bonusWaitRemainingMillis(System.currentTimeMillis())) }
            LaunchedEffect(settings.bonusWaitStartedAtMillis) {
                while (true) {
                    remainingMillis = settings.bonusWaitRemainingMillis(System.currentTimeMillis())
                    if (remainingMillis <= 0L) {
                        onWaitFinished()
                        break
                    }
                    kotlinx.coroutines.delay(1000L)
                }
            }
            val minutes = (remainingMillis / 60000L).toInt()
            val seconds = ((remainingMillis / 1000L) % 60L).toInt()
            Text(
                text = "Karty dodatkowe odblokują się za %02d:%02d".format(minutes, seconds),
                style = MaterialTheme.typography.bodyMedium,
                color = MysticTextSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(onClick = onWatchAd, modifier = Modifier.fillMaxWidth()) {
                Text("Obejrzyj reklamę (odblokuj teraz)")
            }
        } else {
            Text(
                text = "Odblokuj Kartę Wsparcia i Na Co Uważać",
                style = MaterialTheme.typography.titleSmall,
                color = MysticHeadingGold,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onWatchAd, modifier = Modifier.fillMaxWidth()) {
                Text("Obejrzyj reklamę (odblokowanie natychmiastowe)")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onStartWait, modifier = Modifier.fillMaxWidth()) {
                Text("Poczekaj 60 minut (za darmo)")
            }
        }
    }
}
