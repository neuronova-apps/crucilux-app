package com.neuronovaapps.crucilux.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronovaapps.crucilux.achievements.AchievementState
import com.neuronovaapps.crucilux.ui.theme.CruciluxThemeColors
import com.neuronovaapps.crucilux.ui.theme.LocalCruciluxHighContrast
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Notificación visual discreta in-app para celebrar el desbloqueo de un logro.
 * Incorpora botón accesible de cierre manual, gesto de deslizamiento horizontal (swipe to dismiss),
 * y navegación accesible hacia la pantalla de logros.
 */
@Composable
fun AchievementUnlockBanner(
    achievement: AchievementState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val isHighContrast = LocalCruciluxHighContrast.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val offsetX = remember(achievement.id) { Animatable(0f) }
    val dismissThresholdPx = with(density) { 72.dp.toPx() }

    val draggableState = rememberDraggableState { delta ->
        coroutineScope.launch {
            offsetX.snapTo(offsetX.value + delta)
        }
    }

    val dragAlpha = (1f - (abs(offsetX.value) / (dismissThresholdPx * 2.5f))).coerceIn(0.2f, 1f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .alpha(dragAlpha)
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    if (abs(offsetX.value) > dismissThresholdPx || abs(velocity) > 1000f) {
                        val target = if (offsetX.value >= 0) 1000f else -1000f
                        coroutineScope.launch {
                            offsetX.animateTo(target, animationSpec = tween(durationMillis = 150))
                            onDismiss()
                        }
                    } else {
                        coroutineScope.launch {
                            offsetX.animateTo(0f, animationSpec = tween(durationMillis = 200))
                        }
                    }
                },
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighContrast) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = if (isHighContrast) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
        } else {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = "Ver logros",
                                onClick = onClick,
                            )
                        } else Modifier
                    )
                    .semantics(mergeDescendants = true) {
                        contentDescription = "¡Logro desbloqueado! ${achievement.name}. ${achievement.description}"
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "¡Logro desbloqueado!",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighContrast) MaterialTheme.colorScheme.onSurface else CruciluxThemeColors.success,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        text = achievement.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Cerrar notificación de logro"
                    },
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = if (isHighContrast) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}
