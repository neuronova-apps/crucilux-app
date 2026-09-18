package com.neuronovaapps.crucilux.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronovaapps.crucilux.achievements.AchievementState
import com.neuronovaapps.crucilux.ui.theme.CruciluxThemeColors
import com.neuronovaapps.crucilux.ui.theme.LocalCruciluxHighContrast

/**
 * Notificación visual discreta in-app para celebrar el desbloqueo de un logro.
 */
@Composable
fun AchievementUnlockBanner(
    achievement: AchievementState,
    modifier: Modifier = Modifier,
) {
    val isHighContrast = LocalCruciluxHighContrast.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "¡Logro desbloqueado! ${achievement.name}"
            },
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
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
    }
}
