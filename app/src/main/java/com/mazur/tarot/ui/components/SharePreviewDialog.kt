package com.mazur.tarot.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mazur.tarot.R
import com.mazur.tarot.ui.theme.MysticBackground
import com.mazur.tarot.ui.theme.MysticHeadingGold
import com.mazur.tarot.ui.theme.MysticOutline

/**
 * Podgląd wygenerowanej grafiki udostępniania (dokładnie ta bitmapa, która trafi do
 * systemowego selektora) przed jego otwarciem - użytkownik widzi efekt, zanim go potwierdzi.
 */
@Composable
fun SharePreviewDialog(bitmap: Bitmap, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MysticBackground, RoundedCornerShape(20.dp))
                .border(1.dp, MysticOutline, RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.share_preview_title),
                style = MaterialTheme.typography.titleMedium,
                color = MysticHeadingGold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(9f / 16f)
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.cancel))
                }
                OutlinedButton(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.share))
                }
            }
        }
    }
}
