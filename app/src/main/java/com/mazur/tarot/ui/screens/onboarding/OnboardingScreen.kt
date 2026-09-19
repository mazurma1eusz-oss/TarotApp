package com.mazur.tarot.ui.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazur.tarot.ui.components.MysticGradientButton
import com.mazur.tarot.ui.components.MysticTextField
import com.mazur.tarot.ui.theme.MysticGold
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticPurple
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.ui.tarotApp
import com.mazur.tarot.ui.theme.MysticTextSecondary
import kotlinx.coroutines.launch

private val GENDER_OPTIONS = listOf(
    "female" to "Kobieta",
    "male" to "Mężczyzna",
    "" to "Wolę nie podawać",
)

/**
 * Jednorazowy ekran powitalny (po starcie aplikacji, zanim onboarding zostanie ukończony -
 * patrz [com.mazur.tarot.data.local.datastore.AppSettings.hasCompletedOnboarding]). Wszystkie
 * pola są w pełni dobrowolne - służą wyłącznie do tego, by Gemini zwracał się do użytkownika
 * poprawną formą gramatyczną i mógł nawiązać do daty urodzenia, jeśli użytkownik ją poda.
 */
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val app = tarotApp()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthDayText by remember { mutableStateOf("") }
    var birthMonthText by remember { mutableStateOf("") }

    fun saveAndContinue() {
        val day = birthDayText.toIntOrNull()?.coerceIn(1, 31) ?: 0
        val month = birthMonthText.toIntOrNull()?.coerceIn(1, 12) ?: 0
        scope.launch {
            app.settingsDataStore.saveOnboardingProfile(name.trim(), gender, day, month)
            onFinished()
        }
    }

    fun skip() {
        scope.launch {
            app.settingsDataStore.skipOnboarding()
            onFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .navigationBarsPadding(),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Zanim zaczniemy",
            style = MaterialTheme.typography.headlineMedium,
            color = MysticHeadingGold,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Te dane są całkowicie dobrowolne - pomagają kartom zwracać się do Ciebie " +
                "właściwym imieniem i formą. Możesz je pominąć i wrócić do tego później w Ustawieniach.",
            style = MaterialTheme.typography.bodyMedium,
            color = MysticTextSecondary,
        )
        Spacer(modifier = Modifier.height(28.dp))

        MysticTextField(
            value = name,
            onValueChange = { name = it },
            label = "Twoje imię (opcjonalnie)",
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Płeć (opcjonalnie)",
            style = MaterialTheme.typography.titleSmall,
            color = MysticTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GENDER_OPTIONS.forEach { (value, label) ->
                FilterChip(
                    selected = gender == value,
                    onClick = { gender = value },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MysticPurple.copy(alpha = 0.35f),
                        selectedLabelColor = MysticTextPrimary,
                        labelColor = MysticTextSecondary,
                    ),
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Data urodzenia - dzień i miesiąc (opcjonalnie)",
            style = MaterialTheme.typography.titleSmall,
            color = MysticTextPrimary,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MysticTextField(
                value = birthDayText,
                onValueChange = { if (it.length <= 2) birthDayText = it.filter(Char::isDigit) },
                label = "Dzień",
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            MysticTextField(
                value = birthMonthText,
                onValueChange = { if (it.length <= 2) birthMonthText = it.filter(Char::isDigit) },
                label = "Miesiąc",
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
        MysticGradientButton(onClick = ::saveAndContinue, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Zapisz i kontynuuj",
                style = MaterialTheme.typography.titleMedium,
                color = MysticTextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = ::skip, modifier = Modifier.fillMaxWidth()) {
            Text("Pomiń", color = MysticGold)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}
