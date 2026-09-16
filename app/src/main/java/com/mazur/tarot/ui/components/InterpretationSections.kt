package com.mazur.tarot.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mazur.tarot.R
import com.mazur.tarot.data.model.TarotCard
import com.mazur.tarot.ui.theme.MysticOutline
import com.mazur.tarot.ui.theme.MysticPanel
import com.mazur.tarot.ui.theme.MysticTextPrimary

/** Interpretacja karty podzielona na sekcje: Ogólne, Miłość, Praca - jak strony eleganckiej księgi. */
@Composable
fun InterpretationSections(
    card: TarotCard,
    isReversed: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        InterpretationSection(
            icon = Icons.Outlined.AutoAwesome,
            title = stringResource(R.string.section_general),
            text = if (isReversed) card.descriptionReversedGeneral else card.descriptionGeneral,
        )
        Spacer(modifier = Modifier.height(12.dp))
        InterpretationSection(
            icon = Icons.Outlined.FavoriteBorder,
            title = stringResource(R.string.section_love),
            text = if (isReversed) card.descriptionReversedLove else card.descriptionLove,
        )
        Spacer(modifier = Modifier.height(12.dp))
        InterpretationSection(
            icon = Icons.Outlined.WorkOutline,
            title = stringResource(R.string.section_work),
            text = if (isReversed) card.descriptionReversedWork else card.descriptionWork,
        )
    }
}

@Composable
private fun InterpretationSection(icon: ImageVector, title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MysticPanel, RoundedCornerShape(16.dp))
            .border(1.dp, MysticOutline, RoundedCornerShape(16.dp))
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.height(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MysticTextPrimary,
        )
    }
}
