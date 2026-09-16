package com.mazur.tarot.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import com.mazur.tarot.ui.theme.MysticOutline
import com.mazur.tarot.ui.theme.MysticPanel
import com.mazur.tarot.ui.theme.MysticPurple
import com.mazur.tarot.ui.theme.MysticTextPrimary
import com.mazur.tarot.ui.theme.MysticTextSecondary
import androidx.compose.ui.unit.dp

/** Pole tekstowe w stylu strony księgi: ciemny panel, delikatna ramka (domyślnie fioletowa,
 * można nadpisać [accentColor]), subtelna poświata przy fokusie. */
@Composable
fun MysticTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
    accentColor: Color = MysticPurple,
) {
    val shape = RoundedCornerShape(14.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        interactionSource = interactionSource,
        shape = shape,
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MysticPanel,
            unfocusedContainerColor = MysticPanel,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = MysticOutline,
            focusedLabelColor = accentColor,
            unfocusedLabelColor = MysticTextSecondary,
            cursorColor = accentColor,
            focusedTextColor = MysticTextPrimary,
            unfocusedTextColor = MysticTextPrimary,
        ),
        modifier = modifier.shadow(
            elevation = if (isFocused) 10.dp else 0.dp,
            shape = shape,
            ambientColor = accentColor,
            spotColor = accentColor,
        ),
    )
}
