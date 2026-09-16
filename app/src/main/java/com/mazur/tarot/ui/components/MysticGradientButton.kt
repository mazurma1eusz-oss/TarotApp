package com.mazur.tarot.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.mazur.tarot.ui.theme.MysticLightPurple
import com.mazur.tarot.ui.theme.MysticPanelSecondary
import com.mazur.tarot.ui.theme.MysticPurple

/**
 * Główny przycisk akcji aplikacji ("Losuj karty", "Wylosuj kartę"): fioletowy gradient,
 * miękka poświata i bardzo subtelny, wolno przesuwający się połysk - bez neonowego efektu.
 */
@Composable
fun MysticGradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = RoundedCornerShape(28.dp)

    BoxWithConstraints(
        modifier = modifier
            .shadow(
                elevation = if (enabled) 14.dp else 0.dp,
                shape = shape,
                ambientColor = MysticPurple,
                spotColor = MysticPurple,
            )
            .clip(shape)
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(MysticPurple, MysticLightPurple))
                } else {
                    Brush.horizontalGradient(listOf(MysticPanelSecondary, MysticPanelSecondary))
                },
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (enabled) {
            val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
            val infiniteTransition = rememberInfiniteTransition(label = "buttonShimmer")
            val progress by infiniteTransition.animateFloat(
                initialValue = -0.6f,
                targetValue = 1.6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "shimmerProgress",
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.14f), Color.Transparent),
                            start = Offset(progress * widthPx - widthPx * 0.25f, 0f),
                            end = Offset(progress * widthPx + widthPx * 0.25f, 0f),
                        ),
                    ),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = content)
    }
}
