package com.mazur.tarot.ui.screens.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mazur.tarot.R
import com.mazur.tarot.ui.theme.MysticGold
import com.mazur.tarot.ui.theme.MysticPanelSecondary
import com.mazur.tarot.ui.theme.MysticPurple
import kotlinx.coroutines.delay

private const val SPLASH_DURATION_MILLIS = 3500L

/**
 * Ekran startowy: tło z assetu [R.drawable.splash_bg], nazwa aplikacji i minimalistyczny
 * pasek ładowania. Po krótkiej inicjalizacji danych (~2 s) przechodzi dalej i usuwa się
 * ze stosu cofania, by nie dało się do niego wrócić przyciskiem "wstecz".
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MILLIS)
        onFinished()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.splash_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MysticGold,
            )
            Spacer(modifier = Modifier.height(28.dp))
            LinearProgressIndicator(
                color = MysticPurple,
                trackColor = MysticPanelSecondary,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
            )
        }
    }
}
