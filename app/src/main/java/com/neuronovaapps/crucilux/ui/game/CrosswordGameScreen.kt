package com.neuronovaapps.crucilux.ui.game

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neuronovaapps.crucilux.data.GameSessionManager
import com.neuronovaapps.crucilux.data.repository.CrosswordProgressRepository
import com.neuronovaapps.crucilux.data.db.CrosswordBoardStatus
import com.neuronovaapps.crucilux.model.CrosswordClue
import com.neuronovaapps.crucilux.model.CruciluxDirection
import com.neuronovaapps.crucilux.ui.theme.CruciluxThemeColors
import com.neuronovaapps.crucilux.ui.components.ModeSelectionDialog
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.neuronovaapps.crucilux.progression.HintRules

/**
 * Pantalla interactiva de juego de Crucilux con persistencia Room multi-tablero.
 *
 * Estructura en 4 zonas principales:
 * 1. CABECERA COMPACTA: Botón volver, Categoría y Modo de comprobación.
 * 2. ZONA SUPERIOR: Tablero interactivo responsive.
 * 3. ZONA CENTRAL: Tarjeta de Pista Activa con flechas de navegación ‹ y ›.
 * 4. ZONA INFERIOR: Teclado virtual adaptativo con Ñ y Borrar.
 */
@Composable
fun CrosswordGameScreen(
    boardId: String,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToNextBoard: (nextBoardId: String) -> Unit = {},
    dailyDateKey: String? = null,
) {
    val context = LocalContext.current
    val progressRepository = remember { CrosswordProgressRepository.getInstance(context) }
    val sessionManager = remember { GameSessionManager.getInstance(context) }
    val dailyChallengeRepository = remember { com.neuronovaapps.crucilux.data.daily.DailyChallengeRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    var showAdditionalHintDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var pendingNextBoardId by remember { mutableStateOf<String?>(null) }
    var isNavigatingBack by rememberSaveable { mutableStateOf(false) }

    val viewModel: CrosswordGameViewModel = viewModel(
        factory = CrosswordGameViewModel.factory(progressRepository, sessionManager, dailyChallengeRepository)
    )

    val handleVolver: () -> Unit = {
        when {
            pendingNextBoardId != null -> {
                pendingNextBoardId = null
            }
            showResetDialog -> {
                showResetDialog = false
            }
            showAdditionalHintDialog -> {
                showAdditionalHintDialog = false
            }
            !isNavigatingBack -> {
                isNavigatingBack = true
                viewModel.stopTimer()
                viewModel.saveSessionNow()
                onVolver()
            }
        }
    }

    BackHandler(enabled = true, onBack = handleVolver)

    LaunchedEffect(boardId, dailyDateKey) {
        viewModel.loadBoard(boardId, dailyDateKey)
    }

    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── A. Cabecera muy compacta ─────────────────────────────────────────
        GameHeader(
            category = state.board?.category ?: "",
            checkMode = state.checkMode,
            hintsUsed = state.hintsUsed,
            xpPossible = state.xpPossible,
            hintEnabled = !state.isCompleted && viewModel.canUseHint(),
            elapsedTimeSeconds = state.elapsedTimeSeconds,
            isDaily = state.dailyDateKey != null,
            onHint = {
                if (HintRules.requiresAdditionalConfirmation(state.hintsUsed)) showAdditionalHintDialog = true
                else viewModel.useHint()
            },
            onVolver = handleVolver,
        )

        // ── Contenido principal de partida ───────────────────────────────────
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

                // Pista activa
                val activeClue = if (state.activeEntryBankId != null) {
                    if (state.activeDirection == CruciluxDirection.HORIZONTAL) {
                        grid.horizontalClues.firstOrNull { it.bankId == state.activeEntryBankId }
                    } else {
                        grid.verticalClues.firstOrNull { it.bankId == state.activeEntryBankId }
                    }
                } else null

                val isClueValidated = state.activeEntryBankId != null &&
                    state.activeEntryBankId in state.validatedEntryBankIds

                // ── B. ZONA SUPERIOR: Tablero ────────────────────────────────
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CrosswordBoardView(
                        grid = grid,
                        selectedRow = state.selectedRow,
                        selectedCol = state.selectedCol,
                        activeDirection = state.activeDirection,
                        activeCellsInWord = state.activeCellsInWord,
                        userLetters = state.userLetters,
                        validatedCells = state.validatedCells,
                        hintRevealedCells = state.hintRevealedCells,
                        incorrectCells = state.incorrectCells,
                        onCellTapped = { r, c -> viewModel.onCellTapped(r, c) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                // ── C. ZONA CENTRAL: Barra de Pista Activa con Flechas ───────
                ActiveClueNavigationCard(
                    activeClue = activeClue,
                    isValidated = isClueValidated,
                    onPrevious = { viewModel.onPreviousClue() },
                    onNext = { viewModel.onNextClue() },
                )

                // ── D. ZONA INFERIOR: Teclado Virtual ────────────────────────
                CruciluxKeyboardView(
                    onLetter = { viewModel.onLetterEntered(it) },
                    onDelete = { viewModel.onDeleteLetter() },
                )
            }
        }
    }

    // ── Diálogo de felicitación al completar el tablero ──────────────────────
    if (state.isCompleted) {
        CompletionDialog(
            isDaily = dailyDateKey != null,
            totalEntries = state.board?.entries?.size ?: 0,
            nextBoardId = state.nextBoardId,
            completionResult = state.completionResult,
            bestXpEarned = state.bestXpEarned,
            isReviewMode = state.isReviewMode,
            saveErrorMessage = state.saveErrorMessage,
            onNextBoard = { nextId ->
                coroutineScope.launch {
                    val progress = progressRepository.getProgress(nextId)
                    withContext(Dispatchers.Main.immediate) {
                        if (progress.status == CrosswordBoardStatus.NOT_STARTED) {
                            pendingNextBoardId = nextId
                        } else {
                            onNavigateToNextBoard(nextId)
                        }
                    }
                }
            },
            onViewBoards = handleVolver,
            onVolver = handleVolver,
            onRequestReset = { showResetDialog = true },
            onRetrySave = { viewModel.saveSessionNow() },
        )
    }

    if (showAdditionalHintDialog) {
        AlertDialog(
            onDismissRequest = { showAdditionalHintDialog = false },
            title = { Text("Pista adicional") },
            text = { Text("Ya utilizaste las 3 pistas iniciales. Esta ayuda tendrá una penalización mayor de XP.") },
            confirmButton = {
                Button(onClick = {
                    showAdditionalHintDialog = false
                    viewModel.useHint()
                }) { Text("Usar pista") }
            },
            dismissButton = {
                TextButton(onClick = { showAdditionalHintDialog = false }) { Text("Cancelar") }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reiniciar tablero") },
            text = { Text("Se borrarán las letras y pistas de esta partida. Podrás elegir modalidad de nuevo.") },
            confirmButton = {
                Button(onClick = {
                    showResetDialog = false
                    viewModel.resetBoardAfterConfirmation(onReset = onVolver)
                }) { Text("Reiniciar") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Cancelar") }
            },
        )
    }

    pendingNextBoardId?.let { nextId ->
        ModeSelectionDialog(
            onSelect = { mode ->
                coroutineScope.launch {
                    val nextBoard = com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
                        .getInstance().getBoardById(nextId)
                    if (nextBoard != null) {
                        progressRepository.startBoard(nextId, nextBoard.category, mode)
                        withContext(Dispatchers.Main.immediate) {
                            pendingNextBoardId = null
                            onNavigateToNextBoard(nextId)
                        }
                    }
                }
            },
            onDismiss = { pendingNextBoardId = null },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes auxiliares
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GameHeader(
    category: String,
    checkMode: CheckMode,
    hintsUsed: Int,
    xpPossible: Int,
    hintEnabled: Boolean,
    elapsedTimeSeconds: Long = 0L,
    isDaily: Boolean = false,
    onHint: () -> Unit,
    onVolver: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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

            Text(
                text = category.ifBlank { "Crucigrama" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )

            if (isDaily || elapsedTimeSeconds > 0L) {
                val minutes = elapsedTimeSeconds / 60
                val seconds = elapsedTimeSeconds % 60
                val mm = minutes.toString().padStart(2, '0')
                val ss = seconds.toString().padStart(2, '0')
                val timeFormatted = "$mm:$ss"
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text(
                        text = "⏱ $timeFormatted",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .semantics { contentDescription = "Tiempo transcurrido: $minutes minutos y $seconds segundos" },
                    )
                }
            }

            Text(
                text = if (checkMode == CheckMode.CLASSIC) "Clásica" else "Asistida",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .semantics { contentDescription = "Modo de partida bloqueado: ${if (checkMode == CheckMode.CLASSIC) "Clásica" else "Asistida"}" },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val initialDots = (0 until 3).joinToString(" ") { index ->
                if (index < hintsUsed.coerceAtMost(3)) "○" else "●"
            }
            Text(
                text = "Pistas: $initialDots",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics {
                    contentDescription = "Pistas usadas: $hintsUsed. Pistas iniciales restantes: ${(3 - hintsUsed).coerceAtLeast(0)}"
                },
            )
            Text(
                text = "XP posibles: $xpPossible",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            OutlinedButton(
                onClick = onHint,
                enabled = hintEnabled,
                modifier = Modifier.defaultMinSize(minHeight = 36.dp),
                border = if (com.neuronovaapps.crucilux.ui.theme.LocalCruciluxHighContrast.current) {
                    BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                } else {
                    ButtonDefaults.outlinedButtonBorder(enabled = hintEnabled)
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Pista", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun ActiveClueNavigationCard(
    activeClue: CrosswordClue?,
    isValidated: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isHighContrast = com.neuronovaapps.crucilux.ui.theme.LocalCruciluxHighContrast.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isValidated) CruciluxThemeColors.success.copy(alpha = if (isHighContrast) 0.25f else 0.12f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isHighContrast) {
            BorderStroke(1.5.dp, if (isValidated) CruciluxThemeColors.success else MaterialTheme.colorScheme.outline)
        } else null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Pista anterior" },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (activeClue != null) {
                        val dirName = if (activeClue.direction == CruciluxDirection.HORIZONTAL) "Horizontal" else "Vertical"
                        Text(
                            text = "${activeClue.number} $dirName",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (isHighContrast) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isValidated) CruciluxThemeColors.success else MaterialTheme.colorScheme.onSurface,
                        )

                        if (isValidated) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .then(
                                        if (isHighContrast) {
                                            Modifier.border(width = 1.dp, color = CruciluxThemeColors.success, shape = RoundedCornerShape(4.dp))
                                        } else Modifier
                                    )
                                    .background(CruciluxThemeColors.success.copy(alpha = if (isHighContrast) 0.35f else 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "✓ Resuelta",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = CruciluxThemeColors.success,
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Selecciona una casilla",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { contentDescription = "Pista siguiente" },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (activeClue != null) {
                Text(
                    text = activeClue.clue,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isHighContrast) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                )

                Text(
                    text = activeClue.formatLengthInfo(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isHighContrast) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isValidated) CruciluxThemeColors.success else MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 1.dp, bottom = 2.dp),
                )
            }
        }
    }
}

/**
 * Diálogo de felicitación cuando se completa el crucigrama al 100%.
 */
@Composable
private fun CompletionDialog(
    isDaily: Boolean = false,
    totalEntries: Int,
    nextBoardId: String?,
    completionResult: com.neuronovaapps.crucilux.data.repository.BoardCompletionResult?,
    bestXpEarned: Int,
    isReviewMode: Boolean,
    saveErrorMessage: String?,
    onNextBoard: (nextBoardId: String) -> Unit,
    onViewBoards: () -> Unit,
    onVolver: () -> Unit,
    onRequestReset: () -> Unit,
    onRetrySave: () -> Unit,
) {
    val isCategoryFinished = nextBoardId == null
    val actionsEnabled = isReviewMode || completionResult != null

    AlertDialog(
        onDismissRequest = { /* Modal permanente */ },
        icon = {
            Icon(
                imageVector = if (isDaily || isCategoryFinished) Icons.Default.EmojiEvents else Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CruciluxThemeColors.success,
                modifier = Modifier.size(48.dp),
            )
        },
        title = {
            Text(
                text = when {
                    isDaily -> "¡Desafío diario completado!"
                    isCategoryFinished -> "¡Categoría completada!"
                    else -> "¡Crucigrama completado!"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = when {
                        isDaily -> "¡Felicitaciones! Has resuelto exitosamente el desafío diario de hoy."
                        isCategoryFinished -> "¡Felicitaciones extraordinarias! Has resuelto todos los 30 tableros de esta categoría."
                        else -> "¡Felicitaciones! Has resuelto todas las pistas de este tablero."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "$totalEntries de $totalEntries palabras resueltas (100%)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CruciluxThemeColors.success,
                    textAlign = TextAlign.Center,
                )
                if (completionResult != null) {
                    Text(
                        text = "XP obtenido: +${completionResult.xpAwarded}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = if (completionResult.isNewBest) {
                            "Nueva mejor puntuación: ${completionResult.bestXpEarned} XP"
                        } else {
                            "Mejor XP del tablero: ${completionResult.bestXpEarned}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    if (saveErrorMessage != null) {
                        Text(
                            text = saveErrorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    } else if (!isReviewMode) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .semantics { contentDescription = "Guardando finalización y XP" },
                        )
                        Text(
                            text = "Guardando progreso…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    } else {
                        Text(
                            text = "Mejor XP del tablero: $bestXpEarned",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (isDaily && actionsEnabled) {
                    Button(
                        onClick = onVolver,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Volver al Desafío Diario")
                    }
                } else if (actionsEnabled && nextBoardId != null) {
                    Button(
                        onClick = { onNextBoard(nextBoardId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text("Siguiente tablero")
                    }
                }

                if (!isDaily && actionsEnabled) {
                    Button(
                        onClick = onViewBoards,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (nextBoardId != null) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.primary,
                            contentColor = if (nextBoardId != null) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text("Ver tableros")
                    }

                    TextButton(onClick = onRequestReset) {
                        Text("Reiniciar tablero")
                    }
                } else if (saveErrorMessage != null) {
                    Button(
                        onClick = onRetrySave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Reintentar guardado")
                    }
                }
            }
        },
        dismissButton = {
            if (actionsEnabled) {
                TextButton(
                    onClick = onVolver,
                ) {
                    Text("Volver")
                }
            }
        },
    )
}
