package com.mazur.tarot.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mazur.tarot.ui.theme.MysticPurple

private val FlipCardShape = RoundedCornerShape(12.dp)

/**
 * Karta z animacją odwrócenia 3D (rewers -> awers) wykonaną za pomocą [graphicsLayer.rotationY].
 * W połowie animacji (90 stopni) podmieniana jest zawartość, imitując fizyczne obracanie karty.
 * Zachowuje rzeczywiste proporcje grafik ([CardAspectRatio]) i niesie ze sobą miękki,
 * fioletowy cień, by wyglądać jak fizyczny przedmiot leżący na stole.
 *
 * [modifier] powinien ograniczać dokładnie jeden wymiar (np. `height(300.dp)` albo
 * `fillMaxWidth(0.65f)`) - drugi zostanie dobrany automatycznie na podstawie [CardAspectRatio].
 */
@Composable
fun FlipCard(
    isRevealed: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    back: @Composable () -> Unit,
    front: @Composable () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isRevealed) 180f else 0f,
        animationSpec = tween(durationMillis = 700),
        label = "cardFlipRotation",
    )
    val density = LocalDensity.current
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .aspectRatio(CardAspectRatio)
            .shadow(
                elevation = 20.dp,
                shape = FlipCardShape,
                ambientColor = MysticPurple,
                spotColor = MysticPurple,
            )
            .clip(FlipCardShape)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 16f * density.density
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        if (rotation <= 90f) {
            back()
        } else {
            Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                front()
            }
        }
    }
}
