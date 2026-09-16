package com.mazur.tarot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mazur.tarot.R
import com.mazur.tarot.ui.theme.MysticBackground

/**
 * Stałe, mistyczne tło całej aplikacji: dostarczona grafika (gwiazdy, fazy księżyca,
 * mandala) w pełnym rozmiarze ekranu, przyciemniona subtelną warstwą, by tekst i karty
 * na wierzchu pozostawały w pełni czytelne.
 */
@Composable
fun MysticBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.main_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MysticBackground.copy(alpha = 0.4f)),
        )
        content()
    }
}
