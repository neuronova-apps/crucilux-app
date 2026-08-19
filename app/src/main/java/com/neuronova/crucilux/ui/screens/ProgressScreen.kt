package com.neuronova.crucilux.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.LocalFireDepartment
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
import com.neuronova.crucilux.data.GameConfigProvider
import com.neuronova.crucilux.data.repository.CrosswordProgressRepository
import com.neuronova.crucilux.data.repository.GlobalProgressStats
import com.neuronova.crucilux.ui.theme.ProgressBlue
import com.neuronova.crucilux.ui.theme.StreakOrange
import com.neuronova.crucilux.ui.theme.SuccessGreen
import com.neuronova.crucilux.progression.PlayerProgress
import com.neuronova.crucilux.ui.components.PlayerLevelCard

private data class MedalItem(
    val id: String,
    val name: String,
    val condition: String,
    val initialLetter: String,
    val isUnlocked: Boolean = false,
)

private val medalsList = listOf(
    MedalItem(
        id = "first_crossword",
        name = "Primer Crucigrama",
        condition = "Completa tu primer crucigrama",
        initialLetter = "P",
        isUnlocked = false,
    ),
    MedalItem(
        id = "word_master",
        name = "Vocabulario de Oro",
        condition = "Encuentra 50 palabras correctas",
        initialLetter = "V",
        isUnlocked = false,
    ),
    MedalItem(
        id = "fast_mind",
        name = "Mente Ágil",
        condition = "Completa una partida en tiempo récord",
        initialLetter = "M",
        isUnlocked = false,
    ),
    MedalItem(
        id = "streak_7",
        name = "Constancia",
        condition = "Mantén una racha de 7 días seguidos",
        initialLetter = "C",
        isUnlocked = false,
    ),
    MedalItem(
        id = "grand_grid",
        name = "Gran Tablero",
        condition = "Resuelve un crucigrama de 15×15",
        initialLetter = "G",
        isUnlocked = false,
    ),
    MedalItem(
        id = "master_solver",
        name = "Maestro de Letras",
        condition = "Completa 30 crucigramas",
        initialLetter = "L",
        isUnlocked = false,
    ),
)

@Composable
fun ProgressScreen() {
    val context = LocalContext.current
    val progressRepository = remember { CrosswordProgressRepository.getInstance(context) }
    val globalStats by progressRepository.observeGlobalStats()
        .collectAsState(initial = GlobalProgressStats())
    val playerProgress by progressRepository.observePlayerProgress()
        .collectAsState(initial = PlayerProgress())
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
                text  = "Tu recorrido en Crucilux (300 tableros)",
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
                iconTint    = SuccessGreen,
                containerBg = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                value       = "${globalStats.completedBoards}",
                label       = "Completados",
                contentDesc = "Crucigramas completados: ${globalStats.completedBoards} de 300",
            )
            ProgressSummaryCard(
                modifier    = Modifier.weight(1f),
                icon        = Icons.Default.PieChart,
                iconTint    = ProgressBlue,
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
            border    = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
            ),
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
                                text = "$completed / 30 ($percent %)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (percent == 100) SuccessGreen else MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (percent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (percent == 100) SuccessGreen else MaterialTheme.colorScheme.primary,
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
            medals   = medalsList,
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
    medals: List<MedalItem>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border    = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ),
    ) {
        Column(
            modifier            = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector        = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                        modifier           = Modifier.size(20.dp),
                    )
                    Text(
                        text       = "Medallas y Logros",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text       = "0 de ${medals.size}",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            medals.chunked(2).forEach { rowMedals ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    rowMedals.forEach { medal ->
                        MedalCard(medal = medal, modifier = Modifier.weight(1f))
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
    medal: MedalItem,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .semantics {
                contentDescription = "Medalla ${medal.name}, condición: ${medal.condition}, estado: Bloqueado"
            },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier            = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape,
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = medal.initialLetter,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Black,
                        fontSize   = 13.sp,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = medal.name,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.onSurface,
                        maxLines   = 1,
                    )
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Lock,
                            contentDescription = null,
                            tint               = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier           = Modifier.size(11.dp),
                        )
                        Text(
                            text       = if (medal.isUnlocked) "Desbloqueado" else "Bloqueado",
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (medal.isUnlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Text(
                text       = medal.condition,
                style      = MaterialTheme.typography.bodySmall,
                color      = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize   = 11.sp,
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
        border    = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ),
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
