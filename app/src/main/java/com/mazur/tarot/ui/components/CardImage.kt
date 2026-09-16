package com.mazur.tarot.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mazur.tarot.R
import com.mazur.tarot.ui.theme.MysticPurple
import com.mazur.tarot.util.CardImageResolver

/** Rzeczywisty stosunek boków grafik kart (300 x 527 px) - używany wszędzie, by nigdy ich nie rozciągać. */
const val CardAspectRatio = 300f / 527f

private val CardCornerShape = RoundedCornerShape(10.dp)

/** Wyświetla grafikę karty rozwiązywaną dynamicznie z `image_res_name` (z placeholderem). */
@Composable
fun CardImage(
    imageResName: String,
    modifier: Modifier = Modifier,
    isReversed: Boolean = false,
) {
    val context = LocalContext.current
    val resId = remember(imageResName) { CardImageResolver.resolve(context, imageResName) }
    Image(
        painter = painterResource(id = resId),
        contentDescription = stringResource(R.string.content_desc_card_image),
        contentScale = ContentScale.Fit,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = if (isReversed) 180f else 0f },
    )
}

/**
 * Karta "jako fizyczny przedmiot": poprawne proporcje, delikatne zaokrąglenie i miękka
 * fioletowa poświata. Używana wszędzie tam, gdzie karta jest prezentowana bez animacji
 * odwracania (encyklopedia, wyniki rozkładu, dziennik, szczegóły karty).
 */
@Composable
fun CardArt(
    imageResName: String,
    modifier: Modifier = Modifier,
    isReversed: Boolean = false,
    glow: Boolean = true,
) {
    Box(
        modifier = modifier
            .aspectRatio(CardAspectRatio)
            .then(
                if (glow) {
                    Modifier.shadow(
                        elevation = if (isReversed) 10.dp else 14.dp,
                        shape = CardCornerShape,
                        ambientColor = MysticPurple,
                        spotColor = MysticPurple,
                    )
                } else {
                    Modifier
                },
            )
            .clip(CardCornerShape),
    ) {
        CardImage(imageResName = imageResName, isReversed = isReversed, modifier = Modifier.fillMaxSize())
    }
}
