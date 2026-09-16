package com.mazur.tarot.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mazur.tarot.R
import com.mazur.tarot.data.model.DrawnCard
import com.mazur.tarot.ui.theme.MysticHeadingGold

/**
 * Miniatura wylosowanej karty obok jej nazwy i interpretacji - używana w wynikach
 * rozkładu ("Zapytaj Kart") oraz przy kartach dodatkowych Karty Dnia.
 */
@Composable
fun DrawnCardRow(drawn: DrawnCard, modifier: Modifier = Modifier, label: String? = null) {
    Column(modifier = modifier.fillMaxWidth()) {
        label?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            CardArt(
                imageResName = drawn.card.imageResName,
                isReversed = drawn.isReversed,
                modifier = Modifier.weight(0.32f),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(0.68f)) {
                val title = drawn.card.name + if (drawn.isReversed) " " + stringResource(R.string.reversed_suffix) else ""
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MysticHeadingGold,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = drawn.activeDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
