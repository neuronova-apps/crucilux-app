package com.neuronovaapps.crucilux.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronovaapps.crucilux.data.GameConfigProvider
import com.neuronovaapps.crucilux.data.repository.CrosswordProgressRepository
import com.neuronovaapps.crucilux.data.repository.GlobalProgressStats
import com.neuronovaapps.crucilux.ui.theme.CruciluxThemeColors
import com.neuronovaapps.crucilux.ui.theme.LocalCruciluxHighContrast
import com.neuronovaapps.crucilux.progression.PlayerProgress
import com.neuronovaapps.crucilux.ui.components.PlayerLevelCard

import com.neuronovaapps.crucilux.achievements.AchievementRepository
import com.neuronovaapps.crucilux.achievements.AchievementState
import com.neuronovaapps.crucilux.achievements.AchievementSummary
import com.neuronovaapps.crucilux.achievements.CruciluxAchievements

@Composable
fun ProgressScreen(
    onOpenAchievements: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val progressRepository = remember { CrosswordProgressRepository.getInstance(context) }
    val achievementRepository = remember { AchievementRepository.getInstance(context) }

    LaunchedEffect(Unit) {
        achievementRepository.evaluateAndSync()
    }

    val globalStats by progressRepository.observeGlobalStats()
        .collectAsState(initial = GlobalProgressStats())
    val playerProgress by progressRepository.observePlayerProgress()
        .collectAsState(initial = PlayerProgress())
    val achievements by achievementRepository.observeAchievements()
        .collectAsState(initial = CruciluxAchievements.ALL.map { AchievementState(it) })
    val achievementSummary by achievementRepository.observeSummary()
        .collectAsState(initial = AchievementSummary())
    val categories = GameConfigProvider.categories

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        // Cabecera
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 14.dp),
        ) {
            Text(
                text       = "Progreso",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text  = "Tu recorrido en Crucilux (${globalStats.totalBoards} tableros)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PlayerLevelCard(
            progress = playerProgress,
            detailed = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(14.dp))

        // Fila de tarjetas de resumen
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProgressSummaryCard(
                modifier    = Modifier.weight(1f),
                icon        = Icons.Default.CheckCircleOutline,
                iconTint    = CruciluxThemeColors.success,
                containerBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                value       = "${globalStats.completedBoards}",
                label       = "Completados",
                contentDesc = "Crucigramas completados: ${globalStats.completedBoards} de ${globalStats.totalBoards}",
            )
            ProgressSummaryCard(
                modifier    = Modifier.weight(1f),
                icon        = Icons.Default.PieChart,
                iconTint    = CruciluxThemeColors.progress,
                containerBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                value       = "${globalStats.globalPercent} %",
                label       = "Progreso global",
                contentDesc = "Progreso global: ${globalStats.globalPercent} por ciento",
            )
        }

        Spacer(Modifier.height(14.dp))

        // Desglose de progreso por categoría
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape     = RoundedCornerShape(16.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border    = if (LocalCruciluxHighContrast.current) {
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            } else {
                CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    )
                )
            },
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text       = "Progreso por categoría",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(12.dp))

                categories.forEachIndexed { index, cat ->
                    val stats by progressRepository.observeCategoryStats(cat.displayName)
                        .collectAsState(initial = null)
                    val completed = stats?.completedBoards ?: 0
                    val totalBoards = stats?.totalBoards ?: 0
                    val percent = stats?.completedPercent ?: 0

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = cat.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "$completed / $totalBoards ($percent %)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (percent == 100) CruciluxThemeColors.success else MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (percent == 100) CruciluxThemeColors.success else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }

                    if (index < categories.lastIndex) {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Sección destacada de Medallas y Logros
        MedalsSection(
            medals = achievements,
            summary = achievementSummary,
            onOpenAchievements = onOpenAchievements,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(24.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Sección y tarjetas de Medallas / Logros
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MedalsSection(
    medals: List<AchievementState>,
    summary: AchievementSummary,
    onOpenAchievements: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .then(
                if (onOpenAchievements != null) {
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(role = Role.Button, onClick = onOpenAchievements)
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (LocalCruciluxHighContrast.current) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        } else {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Medallas y Logros",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "${summary.unlockedCount} de ${summary.totalCount}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (summary.unlockedCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            medals.chunked(2).forEach { rowMedals ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowMedals.forEach { medal ->
                        MedalCard(
                            achievement = medal,
                            onClick = onOpenAchievements,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowMedals.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MedalCard(
    achievement: AchievementState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isHighContrast = LocalCruciluxHighContrast.current
    val isUnlocked = achievement.isUnlocked

    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = onClick)
                } else Modifier
            )
            .semantics(mergeDescendants = true) {
                contentDescription = achievement.contentDescription
            },
        shape = RoundedCornerShape(14.dp),
        color = when {
            isHighContrast -> MaterialTheme.colorScheme.surface
            isUnlocked -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = if (isHighContrast) 1.5.dp else 1.dp,
            color = when {
                isHighContrast -> MaterialTheme.colorScheme.outline
                isUnlocked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isUnlocked) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        )
                        .border(
                            width = if (isHighContrast) 1.5.dp else 1.dp,
                            color = if (isUnlocked) MaterialTheme.colorScheme.primary
                            else if (isHighContrast) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = achievement.initialLetter,
                        color = if (isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = achievement.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = if (isUnlocked) Icons.Default.EmojiEvents else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isUnlocked) CruciluxThemeColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = if (isUnlocked) "Desbloqueado" else "${achievement.currentProgress}/${achievement.targetProgress}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isUnlocked) CruciluxThemeColors.success else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

@Composable
private fun ProgressSummaryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    containerBg: Color,
    value: String,
    label: String,
    contentDesc: String,
) {
    Card(
        modifier  = modifier.semantics(mergeDescendants = true) {
            contentDescription = contentDesc
        },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = if (LocalCruciluxHighContrast.current) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        } else {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            )
        },
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = iconTint,
                    modifier           = Modifier.size(18.dp),
                )
            }
            Text(
                text       = value,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text      = label,
                style     = MaterialTheme.typography.labelSmall,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
