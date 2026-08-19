package com.neuronova.crucilux.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.model.CrosswordClue
import com.neuronova.crucilux.model.CrosswordGrid
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.ui.theme.SuccessGreen

/**
 * Pantalla interactiva de juego de Crucilux — Etapa 2.
 *
 * Características:
 * - Selección de celdas y alternancia H/V.
 * - Resaltado de palabra activa y celda con foco.
 * - Teclado virtual adaptativo en español.
 * - Entrada y borrado de letras con avance automático.
 * - Modos de comprobación: Clásica y Asistida.
 * - Comprobar palabra activa en modo Clásica.
 * - Autoguardado transparente en DataStore.
 * - Barra activa de pista superior.
 * - Navegación e integración accesible para TalkBack.
 */
@Composable
fun CrosswordGameScreen(
    boardId: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sessionManager = remember { GameSessionManager.getInstance(context) }
    val viewModel: CrosswordGameViewModel = viewModel(
        factory = CrosswordGameViewModel.factory(sessionManager)
    )

    LaunchedEffect(boardId) {
        viewModel.loadBoard(boardId)
    }

    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Cabecera ─────────────────────────────────────────────────────────
        GameHeader(
            category = state.board?.category ?: "",
            size = state.board?.size ?: "",
            boardId = state.board?.id ?: "",
            checkMode = state.checkMode,
            onSetCheckMode = { viewModel.onSetCheckMode(it) },
            onVolver = {
                viewModel.saveSessionNow()
                onVolver()
            },
        )

        // ── Feedback temporal de comprobación ────────────────────────────────
        AnimatedVisibility(
            visible = state.checkWordResult != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            CheckResultBanner(result = state.checkWordResult)
        }

        // ── Contenido central ────────────────────────────────────────────────
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            state.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.errorMessage!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            state.grid != null && state.board != null -> {
                val grid = state.grid!!

                // Pista activa destacada
                ActiveClueCard(
                    grid = grid,
                    activeEntryBankId = state.activeEntryBankId,
                    activeDirection = state.activeDirection,
                )

                // Área desplazable con tablero y pistas
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp),
                ) {
                    Spacer(Modifier.height(4.dp))

                    // Tablero de juego
                    CrosswordBoardView(
                        grid = grid,
                        selectedRow = state.selectedRow,
                        selectedCol = state.selectedCol,
                        activeDirection = state.activeDirection,
                        activeCellsInWord = state.activeCellsInWord,
                        userLetters = state.userLetters,
                        incorrectCells = state.incorrectCells,
                        onCellTapped = { r, c -> viewModel.onCellTapped(r, c) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(8.dp))

                    // Botón comprobar en modo Clásica
                    if (state.checkMode == CheckMode.CLASSIC && state.activeCellsInWord.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.onCheckWord() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .semantics { contentDescription = "Comprobar palabra activa" },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Comprobar palabra",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Pistas expandidas
                    ClueSection(
                        title = "Horizontales",
                        clues = grid.horizontalClues,
                        activeEntryBankId = state.activeEntryBankId,
                        onClueTapped = { clue ->
                            viewModel.onCellTapped(clue.startRow, clue.startCol)
                        },
                    )

                    Spacer(Modifier.height(8.dp))

                    ClueSection(
                        title = "Verticales",
                        clues = grid.verticalClues,
                        activeEntryBankId = state.activeEntryBankId,
                        onClueTapped = { clue ->
                            viewModel.onCellTapped(clue.startRow, clue.startCol)
                        },
                    )

                    Spacer(Modifier.height(16.dp))
                }

                // Teclado virtual fijo en la parte inferior
                CruciluxKeyboardView(
                    onLetter = { viewModel.onLetterEntered(it) },
                    onDelete = { viewModel.onDeleteLetter() },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes auxiliares
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Cabecera con selector compacto de modo de comprobación.
 */
@Composable
private fun GameHeader(
    category: String,
    size: String,
    boardId: String,
    checkMode: CheckMode,
    onSetCheckMode: (CheckMode) -> Unit,
    onVolver: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onVolver,
            modifier = Modifier.semantics { contentDescription = "Volver" },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.ifBlank { "Crucigrama" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (size.isNotBlank()) {
                    Text(
                        text = size,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (boardId.isNotBlank()) {
                    Text(
                        text = "· $boardId",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }

        // Selector compacto Clásica / Asistida
        CompactCheckModeToggle(
            checkMode = checkMode,
            onSetCheckMode = onSetCheckMode,
        )
    }
}

/**
 * Selector compacto de modo de comprobación (discreto, no invasivo).
 */
@Composable
private fun CompactCheckModeToggle(
    checkMode: CheckMode,
    onSetCheckMode: (CheckMode) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    ) {
        Row(
            modifier = Modifier.padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            CheckModeChip(
                label = "Clásica",
                isSelected = checkMode == CheckMode.CLASSIC,
                onClick = { onSetCheckMode(CheckMode.CLASSIC) },
            )
            CheckModeChip(
                label = "Asistida",
                isSelected = checkMode == CheckMode.ASSISTED,
                onClick = { onSetCheckMode(CheckMode.ASSISTED) },
            )
        }
    }
}

@Composable
private fun CheckModeChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics {
                contentDescription = "Modo $label, ${if (isSelected) "activo" else "inactivo"}"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Tarjeta superior que muestra la pista de la palabra activa en tiempo real.
 */
@Composable
private fun ActiveClueCard(
    grid: CrosswordGrid,
    activeEntryBankId: String?,
    activeDirection: CruciluxDirection,
) {
    val activeClue = if (activeEntryBankId != null) {
        if (activeDirection == CruciluxDirection.HORIZONTAL) {
            grid.horizontalClues.firstOrNull { it.bankId == activeEntryBankId }
        } else {
            grid.verticalClues.firstOrNull { it.bankId == activeEntryBankId }
        }
    } else null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (activeClue != null) {
                val dirIcon = if (activeClue.direction == CruciluxDirection.HORIZONTAL) {
                    Icons.AutoMirrored.Filled.ArrowForward
                } else {
                    Icons.Default.SwapVert
                }
                val dirText = if (activeClue.direction == CruciluxDirection.HORIZONTAL) "H" else "V"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "${activeClue.number}$dirText",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Text(
                    text = activeClue.clue,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Toca una celda para ver su pista",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Banner de feedback para la acción Comprobar palabra.
 */
@Composable
private fun CheckResultBanner(result: CheckWordResult?) {
    if (result == null) return

    val (bgColor, textColor, icon, message) = when (result) {
        CheckWordResult.Incomplete -> Quad(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            Icons.Default.ErrorOutline,
            "Palabra incompleta. Completa todas las letras primero.",
        )
        CheckWordResult.Correct -> Quad(
            SuccessGreen.copy(alpha = 0.2f),
            SuccessGreen,
            Icons.Default.Check,
            "¡Palabra correcta!",
        )
        CheckWordResult.HasErrors -> Quad(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.ErrorOutline,
            "Hay letras incorrectas en la palabra.",
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = message,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
            )
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Sección desplegada de pistas con soporte de selección por toque.
 */
@Composable
private fun ClueSection(
    title: String,
    clues: List<CrosswordClue>,
    activeEntryBankId: String?,
    onClueTapped: (CrosswordClue) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (clues.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                clues.forEachIndexed { index, clue ->
                    val isActive = clue.bankId == activeEntryBankId
                    InteractiveClueItem(
                        clue = clue,
                        isActive = isActive,
                        onClick = { onClueTapped(clue) },
                    )

                    if (index < clues.lastIndex) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveClueItem(
    clue: CrosswordClue,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dirLabel = if (clue.direction == CruciluxDirection.HORIZONTAL) "horizontal" else "vertical"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                else androidx.compose.ui.graphics.Color.Transparent
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp)
            .semantics {
                contentDescription = "Pista $dirLabel número ${clue.number}: ${clue.clue}${if (isActive) ", seleccionada" else ""}"
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "${clue.number}.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 1.dp),
        )

        Text(
            text = clue.clue,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}
