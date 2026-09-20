package com.neuronovaapps.crucilux.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronovaapps.crucilux.data.GameConfigProvider
import com.neuronovaapps.crucilux.data.daily.DailyChallengeInfo
import com.neuronovaapps.crucilux.data.daily.DailyChallengeRepository
import com.neuronovaapps.crucilux.data.db.DailyChallengeStatus
import com.neuronovaapps.crucilux.ui.theme.LocalCruciluxHighContrast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla dedicada del Desafío Diario en Crucilux.
 *
 * Muestra el crucigrama seleccionado determinísticamente para el día de hoy,
 * su progreso aislado, racha actual y mejor racha acumulada.
 */
@Composable
fun DailyChallengeScreen(
    onVolver: () -> Unit,
    onPlayBoard: (boardId: String, dailyDateKey: String) -> Unit,
    modifier: Modifier = Modifier,
    repository: DailyChallengeRepository? = null,
) {
    val context = LocalContext.current
    val effectiveRepo = repository ?: remember { DailyChallengeRepository.getInstance(context) }
    val challengeInfo by effectiveRepo.observeTodayChallenge().collectAsState(initial = null)

    val isHighContrast = LocalCruciluxHighContrast.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Cabecera accesible con botón de retorno
        DailyChallengeHeader(
            onVolver = onVolver,
            isHighContrast = isHighContrast,
        )

        val challenge = challengeInfo
        if (challenge == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.semantics {
                        contentDescription = "Cargando desafío diario"
                    },
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Insignia de fecha local
                DateBadge(dateKey = challenge.dateKey, isHighContrast = isHighContrast)

                // Tarjeta de información del crucigrama de hoy
                TodayBoardCard(
                    challenge = challenge,
                    isHighContrast = isHighContrast,
                )

                // Tarjeta de rachas (actual y mejor racha)
                StreakCard(
                    currentStreak = challenge.currentStreak,
                    bestStreak = challenge.bestStreak,
                    isHighContrast = isHighContrast,
                )

                Spacer(modifier = Modifier.weight(1f, fill = false))

                // Acciones principales
                DailyChallengeActions(
                    challenge = challenge,
                    onPlayBoard = onPlayBoard,
                    isHighContrast = isHighContrast,
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes visuales
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailyChallengeHeader(
    onVolver: () -> Unit,
    isHighContrast: Boolean,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = if (isHighContrast) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onVolver,
                modifier = Modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = "Volver a la pantalla principal" },
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Desafío diario",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun DateBadge(
    dateKey: String,
    isHighContrast: Boolean,
) {
    val formattedDate = remember(dateKey) { formatSpanishDate(dateKey) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f))
            .then(
                if (isHighContrast) Modifier.background(MaterialTheme.colorScheme.surface) else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .semantics { contentDescription = "Fecha del desafío: $formattedDate" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CalendarToday,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun TodayBoardCard(
    challenge: DailyChallengeInfo,
    isHighContrast: Boolean,
) {
    val categoryIcon = GameConfigProvider.getIconForCategory(challenge.category)

    val (statusLabel, statusColor, statusBgColor) = when (challenge.status) {
        DailyChallengeStatus.COMPLETED -> Triple(
            "Completado",
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        )
        DailyChallengeStatus.IN_PROGRESS -> Triple(
            "En progreso (${challenge.progressPercent}%)",
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        )
        DailyChallengeStatus.NOT_STARTED -> Triple(
            "Disponible hoy",
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
        )
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isHighContrast) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        } else {
            CardDefaults.outlinedCardBorder()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Crucigrama de hoy, Categoría ${challenge.category}, tamaño ${challenge.dimensionLabel}, estado $statusLabel"
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Crucigrama de hoy",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                )

                // Badge de estado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusBgColor)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Column {
                    Text(
                        text = challenge.category.ifBlank { "Crucigrama" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cuadrícula: ${challenge.dimensionLabel}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (challenge.isInProgress || challenge.isCompleted) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Progreso del crucigrama",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "${challenge.progressPercent}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    LinearProgressIndicator(
                        progress = { challenge.progressPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = if (challenge.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )

                    if (challenge.bestTimeSeconds != null || challenge.elapsedTimeSeconds > 0L) {
                        val best = challenge.bestTimeSeconds
                        val elapsed = challenge.elapsedTimeSeconds
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (best != null) {
                                val m = (best / 60).toString().padStart(2, '0')
                                val s = (best % 60).toString().padStart(2, '0')
                                val timeStr = "$m:$s"
                                Text(
                                    text = "Mejor tiempo: $timeStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else if (elapsed > 0L) {
                                val m = (elapsed / 60).toString().padStart(2, '0')
                                val s = (elapsed % 60).toString().padStart(2, '0')
                                val timeStr = "$m:$s"
                                Text(
                                    text = "Tiempo acumulado: $timeStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakCard(
    currentStreak: Int,
    bestStreak: Int,
    isHighContrast: Boolean,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = if (isHighContrast) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
        } else {
            CardDefaults.outlinedCardBorder()
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Racha actual: $currentStreak días. Mejor racha: $bestStreak días."
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Racha de desafíos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StreakStatItem(
                    count = currentStreak,
                    label = "Racha actual",
                    unit = if (currentStreak == 1) "día" else "días",
                    highlight = currentStreak > 0,
                )

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )

                StreakStatItem(
                    count = bestStreak,
                    label = "Mejor racha",
                    unit = if (bestStreak == 1) "día" else "días",
                    highlight = bestStreak > 0,
                )
            }

            Text(
                text = "Resuelve un crucigrama cada día para mantener y hacer crecer tu racha.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StreakStatItem(
    count: Int,
    label: String,
    unit: String,
    highlight: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "$label ($unit)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DailyChallengeActions(
    challenge: DailyChallengeInfo,
    onPlayBoard: (boardId: String, dailyDateKey: String) -> Unit,
    isHighContrast: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when {
            challenge.isNotStarted -> {
                Button(
                    onClick = { onPlayBoard(challenge.boardId, challenge.dateKey) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .semantics { contentDescription = "Comenzar desafío de hoy" },
                    shape = RoundedCornerShape(14.dp),
                    border = if (isHighContrast) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Comenzar desafío",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            challenge.isInProgress -> {
                Button(
                    onClick = { onPlayBoard(challenge.boardId, challenge.dateKey) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .semantics {
                            contentDescription = "Continuar desafío de hoy, ${challenge.progressPercent} por ciento completado"
                        },
                    shape = RoundedCornerShape(14.dp),
                    border = if (isHighContrast) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continuar desafío",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            challenge.isCompleted -> {
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .semantics { contentDescription = "Desafío completado con éxito" },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Desafío completado",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                }

                OutlinedButton(
                    onClick = { onPlayBoard(challenge.boardId, challenge.dateKey) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .semantics { contentDescription = "Ver crucigrama resuelto de hoy" },
                    shape = RoundedCornerShape(14.dp),
                    border = if (isHighContrast) {
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                    } else {
                        ButtonDefaults.outlinedButtonBorder(enabled = true)
                    },
                ) {
                    Text(
                        text = "Ver crucigrama resuelto",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private fun formatSpanishDate(dateKey: String): String {
    return try {
        val date = LocalDate.parse(dateKey)
        val esLocale = Locale.forLanguageTag("es-ES")
        val formatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", esLocale)
        val formatted = date.format(formatter)
        formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(esLocale) else it.toString() }
    } catch (_: Exception) {
        dateKey
    }
}
