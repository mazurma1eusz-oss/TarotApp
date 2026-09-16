package com.mazur.tarot.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mazur.tarot.R
import com.mazur.tarot.ui.theme.MysticBackground
import com.mazur.tarot.ui.theme.MysticGold
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticOutline
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.ui.theme.MysticTextSecondary

/**
 * Mistyczny ekran zakupu - model hybrydowy: subskrypcja (miesięczna - rekomendowana, roczna,
 * tygodniowa - najmniej wyeksponowana), konsumowalne pakiety pytań (bez subskrypcji) oraz -
 * opcjonalnie w miejscach, gdzie ma to sens (np. dopytanie w Zapytaj Kart) - obejrzenie reklamy
 * albo zużycie już posiadanego kredytu pytania. [onWatchAd] i [onUseCredit] są opcjonalne i
 * pokazywane tylko, gdy podane. Kolejność CTA (góra->dół): miesiąc -> rok -> tydzień -> pakiety
 * -> kredyt -> reklama - celowo nie zmieniana.
 */
@Composable
fun PaywallDialog(
    weeklyPriceLabel: String?,
    monthlyPriceLabel: String?,
    yearlyPriceLabel: String?,
    packStartPriceLabel: String?,
    packStandardPriceLabel: String?,
    questionCredits: Int,
    onSubscribeWeekly: () -> Unit,
    onSubscribeMonthly: () -> Unit,
    onSubscribeYearly: () -> Unit,
    onBuyPackStart: () -> Unit,
    onBuyPackStandard: () -> Unit,
    onRestore: () -> Unit,
    onOpenTerms: () -> Unit,
    onDismiss: () -> Unit,
    onWatchAd: (() -> Unit)? = null,
    onUseCredit: (() -> Unit)? = null,
    /** Gdy true: ostatnia próba obejrzenia reklamy zakończyła się brakiem fillu - pokazujemy
     * jasną informację i podświetlamy Pakiet Start jako alternatywę, zamiast ciszy. */
    adUnavailable: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MysticBackground, RoundedCornerShape(24.dp))
                .border(1.dp, MysticGold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.paywall_title),
                style = MaterialTheme.typography.headlineSmall,
                color = MysticHeadingGold,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(20.dp))

            PaywallBenefitRow(stringResource(R.string.paywall_benefit_unlimited))
            Spacer(modifier = Modifier.height(10.dp))
            PaywallBenefitRow(stringResource(R.string.paywall_benefit_five_card))
            Spacer(modifier = Modifier.height(10.dp))
            PaywallBenefitRow(stringResource(R.string.paywall_benefit_no_limits))

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.paywall_recommended_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MysticGold,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Główna, wyróżniona opcja: fioletowy gradient + złote obramowanie.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, MysticGold, RoundedCornerShape(28.dp)),
            ) {
                MysticGradientButton(onClick = onSubscribeMonthly, modifier = Modifier.fillMaxWidth()) {
                    val label = stringResource(R.string.paywall_subscribe_monthly) +
                        (monthlyPriceLabel?.let { " ($it)" } ?: "")
                    Text(text = label, style = MaterialTheme.typography.titleMedium, color = MysticTextPrimary)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(onClick = onSubscribeYearly, modifier = Modifier.fillMaxWidth()) {
                val label = stringResource(R.string.paywall_subscribe_yearly) +
                    (yearlyPriceLabel?.let { " ($it)" } ?: "")
                Text(text = label)
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Tydzień: najmniej wyeksponowana opcja subskrypcji, mniejszy tekst, nie konkuruje
            // wizualnie z miesiącem (rekomendowanym) ani rokiem.
            OutlinedButton(onClick = onSubscribeWeekly, modifier = Modifier.fillMaxWidth()) {
                val label = stringResource(R.string.paywall_subscribe_weekly) +
                    (weeklyPriceLabel?.let { " ($it)" } ?: "")
                Text(text = label, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.paywall_cancel_anytime),
                style = MaterialTheme.typography.labelSmall,
                color = MysticTextSecondary,
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.paywall_or_buy_pack),
                style = MaterialTheme.typography.labelMedium,
                color = MysticTextSecondary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = onBuyPackStart,
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(
                        width = if (adUnavailable) 2.dp else 1.dp,
                        color = if (adUnavailable) MysticGold else MysticOutline,
                    ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.paywall_pack_start), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        packStartPriceLabel?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MysticTextSecondary)
                        }
                    }
                }
                OutlinedButton(onClick = onBuyPackStandard, modifier = Modifier.weight(1f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.paywall_pack_standard), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
                        packStandardPriceLabel?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MysticTextSecondary)
                        }
                    }
                }
            }

            if (onUseCredit != null && questionCredits > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(onClick = onUseCredit, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.paywall_use_credit, questionCredits))
                }
            }

            onWatchAd?.let {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = it, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.paywall_watch_ad), color = MysticTextSecondary)
                }
            }
            if (adUnavailable) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.paywall_ad_unavailable),
                    style = MaterialTheme.typography.labelSmall,
                    color = MysticGold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MysticOutline, thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                TextButton(onClick = onRestore) {
                    Text(
                        stringResource(R.string.paywall_restore),
                        color = MysticTextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                TextButton(onClick = onOpenTerms) {
                    Text(
                        stringResource(R.string.paywall_terms),
                        color = MysticTextSecondary,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MysticTextSecondary)
            }
        }
    }
}

@Composable
private fun PaywallBenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.Check, contentDescription = null, tint = MysticGold, modifier = Modifier.height(18.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MysticTextPrimary,
            modifier = Modifier.padding(start = 10.dp),
        )
    }
}
