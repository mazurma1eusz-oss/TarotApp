package com.mazur.tarot.ui.screens.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.mazur.tarot.ui.components.MysticGradientButton
import com.mazur.tarot.ui.components.MysticTextField
import com.mazur.tarot.ui.tarotApp
import com.mazur.tarot.ui.theme.MysticGold
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticPurple
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.util.InAppReviewHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val app = tarotApp()
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: SettingsViewModel = viewModel(
        factory = remember {
            viewModelFactory {
                initializer { SettingsViewModel(app, app.settingsDataStore, app.billingManager, app.journalRepository) }
            }
        },
    )
    val settings by viewModel.settings.collectAsState()
    var showTimePicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showConsentDialog by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.setReminderEnabled(true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MysticHeadingGold,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(text = "Twój profil", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Dobrowolne - pomaga kartom zwracać się do Ciebie właściwą formą.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
        ProfileSection(
            initialName = settings.userName,
            initialGender = settings.userGender,
            initialBirthDay = settings.userBirthDay,
            initialBirthMonth = settings.userBirthMonth,
            onSave = { name, gender, day, month -> viewModel.saveProfile(name, gender, day, month) },
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(R.string.settings_account_section), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (settings.isProUnlocked) {
                        stringResource(R.string.settings_pro_active)
                    } else {
                        stringResource(R.string.settings_account_free_credits, settings.questionCredits)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.settings_device_local_limits),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!settings.isProUnlocked) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_pro_inactive),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.paywall_recommended_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MysticGold,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    // Główna, wyróżniona opcja: subskrypcja miesięczna (fioletowy gradient +
                    // złote obramowanie - ten sam akcent co reprezentacyjne CTA w całej appce).
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, MysticGold, RoundedCornerShape(28.dp)),
                    ) {
                        MysticGradientButton(
                            onClick = { activity?.let { viewModel.buyMonthlySubscription(it) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.paywall_subscribe_monthly) + " (${viewModel.monthlyPriceLabel})",
                                color = MysticTextPrimary,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { activity?.let { viewModel.buyYearlySubscription(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.paywall_subscribe_yearly) + " (${viewModel.yearlyPriceLabel})",
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { activity?.let { viewModel.buyWeeklySubscription(it) } },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.paywall_subscribe_weekly) + " (${viewModel.weeklyPriceLabel})",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.paywall_cancel_anytime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.settings_or_buy_pack),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { activity?.let { viewModel.buyPackStart(it) } },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.paywall_pack_start), style = MaterialTheme.typography.labelSmall)
                                Text(viewModel.packStartPriceLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { activity?.let { viewModel.buyPackStandard(it) } },
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(stringResource(R.string.paywall_pack_standard), style = MaterialTheme.typography.labelSmall)
                                Text(viewModel.packStandardPriceLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { viewModel.restorePurchases() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_restore_purchases))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(R.string.settings_experience_section), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.quick_settings_sound_vibration),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = settings.soundVibrationEnabled,
                onCheckedChange = { viewModel.setSoundVibrationEnabled(it) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_reversed_cards),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = settings.reversedCardsEnabled,
                onCheckedChange = { viewModel.setReversedCardsEnabled(it) },
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                val url = context.getString(R.string.privacy_policy_url)
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.quick_settings_privacy_policy))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(R.string.settings_notifications_section), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_daily_reminder),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Switch(
                checked = settings.reminderEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.setReminderEnabled(enabled)
                    }
                },
            )
        }

        if (settings.reminderEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_reminder_time),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showTimePicker = true }) {
                    Text(String.format("%02d:%02d", settings.reminderHour, settings.reminderMinute))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(R.string.settings_support_section), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = {
                val email = context.getString(R.string.support_email)
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                intent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
                context.startActivity(Intent.createChooser(intent, null))
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_contact_us))
        }
        TextButton(
            onClick = { activity?.let { InAppReviewHelper.requestReview(it) } },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_rate_app))
        }
        TextButton(
            onClick = { showConsentDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_manage_consent))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))

        Text(text = stringResource(R.string.settings_data_section), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_delete_history))
        }

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
        Text(text = stringResource(R.string.settings_about_section), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.app_name) + " · wersja " + com.mazur.tarot.BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.settings_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.settings_delete_history_title)) },
            text = { Text(stringResource(R.string.settings_delete_history_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteAllHistory()
                    showDeleteConfirm = false
                }) {
                    Text(stringResource(R.string.settings_delete_history))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text(stringResource(R.string.settings_consent_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.settings_consent_message), style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.settings_consent_toggle), modifier = Modifier.weight(1f))
                        Switch(
                            checked = settings.personalizedAdsConsent,
                            onCheckedChange = { viewModel.setPersonalizedAdsConsent(it) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConsentDialog = false }) { Text(stringResource(R.string.close)) }
            },
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = settings.reminderHour,
            initialMinute = settings.reminderMinute,
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setReminderTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Anuluj") }
            },
            text = { TimePicker(state = timePickerState) },
        )
    }
}

private val PROFILE_GENDER_OPTIONS = listOf("female" to "Kobieta", "male" to "Mężczyzna", "" to "Wolę nie podawać")

/** Ten sam, dobrowolny profil co na ekranie powitalnym (imię/płeć/dzień i miesiąc urodzenia),
 * dostępny tu do edycji po pominięciu lub zmiany zdania - patrz [OnboardingScreen]. */
@Composable
private fun ProfileSection(
    initialName: String,
    initialGender: String,
    initialBirthDay: Int,
    initialBirthMonth: Int,
    onSave: (name: String, gender: String, day: Int, month: Int) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var gender by remember { mutableStateOf(initialGender) }
    var dayText by remember { mutableStateOf(if (initialBirthDay > 0) initialBirthDay.toString() else "") }
    var monthText by remember { mutableStateOf(if (initialBirthMonth > 0) initialBirthMonth.toString() else "") }

    // Odśwież pola, jeśli profil zmienił się z zewnątrz (np. po zapisaniu) bez resetowania
    // tego, co użytkownik właśnie wpisuje w trakcie edycji.
    LaunchedEffect(initialName, initialGender, initialBirthDay, initialBirthMonth) {
        name = initialName
        gender = initialGender
        dayText = if (initialBirthDay > 0) initialBirthDay.toString() else ""
        monthText = if (initialBirthMonth > 0) initialBirthMonth.toString() else ""
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(modifier = Modifier.padding(16.dp)) {
            MysticTextField(
                value = name,
                onValueChange = { name = it },
                label = "Imię",
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PROFILE_GENDER_OPTIONS.forEach { (value, label) ->
                    FilterChip(
                        selected = gender == value,
                        onClick = { gender = value },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MysticPurple.copy(alpha = 0.35f),
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MysticTextField(
                    value = dayText,
                    onValueChange = { if (it.length <= 2) dayText = it.filter(Char::isDigit) },
                    label = "Dzień urodzenia",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                MysticTextField(
                    value = monthText,
                    onValueChange = { if (it.length <= 2) monthText = it.filter(Char::isDigit) },
                    label = "Miesiąc",
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val day = dayText.toIntOrNull()?.coerceIn(1, 31) ?: 0
                    val month = monthText.toIntOrNull()?.coerceIn(1, 12) ?: 0
                    onSave(name, gender, day, month)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Zapisz profil")
            }
        }
    }
}
