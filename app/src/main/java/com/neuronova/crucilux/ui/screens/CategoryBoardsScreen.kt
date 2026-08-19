package com.neuronova.crucilux.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.data.db.CrosswordBoardStatus
import com.neuronova.crucilux.data.repository.CrosswordBoardProgress
import com.neuronova.crucilux.data.repository.CrosswordProgressRepository
import com.neuronova.crucilux.model.CruciluxBoard
import com.neuronova.crucilux.ui.theme.ProgressBlue
import com.neuronova.crucilux.ui.theme.SuccessGreen
import com.neuronova.crucilux.ui.components.ModeSelectionDialog
import kotlinx.coroutines.launch
import com.neuronova.crucilux.progression.GameStartRules

/**
 * Pantalla que muestra la colección de los 30 tableros de una categoría temática.
 */
@Composable
fun CategoryBoardsScreen(
    category: String,
    onSelectBoard: (boardId: String) -> Unit,
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bankRepository = remember { CruciluxBankRepository.getInstance() }
    val progressRepository = remember { CrosswordProgressRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    var pendingBoard by remember { mutableStateOf<CruciluxBoard?>(null) }

    // Tableros de la categoría en orden determinista
    val allCategoryBoards = remember(category) {
        bankRepository.getBoardsByCategory(category)
    }

    // Progreso observado de todos los tableros de esta categoría
    val progressMap by progressRepository.observeProgressForCategory(category)
        .collectAsState(initial = emptyMap())

    val isBibleCategory = category.equals("Biblia", ignoreCase = true)
    var selectedBibleFilter by remember { mutableStateOf("Todos") }

    // Filtrado para Biblia si aplica
    val filteredBoards = remember(allCategoryBoards, selectedBibleFilter, isBibleCategory) {
        if (!isBibleCategory || selectedBibleFilter == "Todos") {
            allCategoryBoards
        } else {
            allCategoryBoards.filter { board ->
                when (selectedBibleFilter) {
                    "AT" -> board.subcategory.equals("AT", ignoreCase = true) || board.subcategory.contains("Antiguo", ignoreCase = true)
                    "NT" -> board.subcategory.equals("NT", ignoreCase = true) || board.subcategory.contains("Nuevo", ignoreCase = true)
                    "Ambos" -> board.subcategory.equals("Ambos", ignoreCase = true)
                    else -> true
                }
            }
        }
    }

    // Estadísticas de categoría
    val completedCount = allCategoryBoards.count {
        progressMap[it.id]?.status == CrosswordBoardStatus.COMPLETED
    }
    val inProgressCount = allCategoryBoards.count {
        progressMap[it.id]?.status == CrosswordBoardStatus.IN_PROGRESS
    }
    val percent = if (allCategoryBoards.isNotEmpty()) {
        ((completedCount * 100) / allCategoryBoards.size).coerceIn(0, 100)
    } else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // ── Cabecera ─────────────────────────────────────────────────────────
        CategoryHeader(
            category = category,
            completedCount = completedCount,
            totalCount = allCategoryBoards.size,
            percent = percent,
            inProgressCount = inProgressCount,
            onVolver = onVolver,
        )

        // ── Filtros para Biblia (AT 9 / NT 9 / Ambos 12) ─────────────────────
        if (isBibleCategory) {
            BibleFilterBar(
                selectedFilter = selectedBibleFilter,
                onFilterSelected = { selectedBibleFilter = it },
                allBoards = allCategoryBoards,
            )
        }

        // ── Grid de 30 Tableros ──────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(filteredBoards) { _, board ->
                val stableIndex = allCategoryBoards.indexOfFirst { it.id == board.id } + 1
                val progress = progressMap[board.id] ?: CrosswordBoardProgress(
                    boardId = board.id,
                    category = board.category,
                    status = CrosswordBoardStatus.NOT_STARTED,
                    progressPercent = 0,
                )

                BoardCardItem(
                    index = stableIndex,
                    board = board,
                    progress = progress,
                    onClick = {
                        coroutineScope.launch {
                            val latest = progressRepository.getProgress(board.id)
                            if (GameStartRules.shouldRequestMode(latest.status)) {
                                pendingBoard = board
                            } else {
                                onSelectBoard(board.id)
                            }
                        }
                    },
                )
            }
        }
    }

    pendingBoard?.let { board ->
        ModeSelectionDialog(
            onSelect = { mode ->
                coroutineScope.launch {
                    progressRepository.startBoard(board.id, board.category, mode)
                    pendingBoard = null
                    onSelectBoard(board.id)
                }
            },
            onDismiss = { pendingBoard = null },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componentes auxiliares
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CategoryHeader(
    category: String,
    completedCount: Int,
    totalCount: Int,
    percent: Int,
    inProgressCount: Int,
    onVolver: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                IconButton(
                    onClick = onVolver,
                    modifier = Modifier.semantics { contentDescription = "Volver a categorías" },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "$completedCount / $totalCount completados ($percent %)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (percent == 100) SuccessGreen else MaterialTheme.colorScheme.primary,
                    )
                }

                if (inProgressCount > 0 && percent < 100) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(ProgressBlue.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = "$inProgressCount en progreso",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = ProgressBlue,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Barra de progreso horizontal
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (percent == 100) SuccessGreen else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
private fun BibleFilterBar(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    allBoards: List<CruciluxBoard>,
) {
    val atCount = allBoards.count { it.subcategory.equals("AT", ignoreCase = true) || it.subcategory.contains("Antiguo", ignoreCase = true) }
    val ntCount = allBoards.count { it.subcategory.equals("NT", ignoreCase = true) || it.subcategory.contains("Nuevo", ignoreCase = true) }
    val bothCount = allBoards.count { it.subcategory.equals("Ambos", ignoreCase = true) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedFilter == "Todos",
            onClick = { onFilterSelected("Todos") },
            label = { Text("Todos (30)") },
        )
        FilterChip(
            selected = selectedFilter == "AT",
            onClick = { onFilterSelected("AT") },
            label = { Text("A.T. ($atCount)") },
        )
        FilterChip(
            selected = selectedFilter == "NT",
            onClick = { onFilterSelected("NT") },
            label = { Text("N.T. ($ntCount)") },
        )
        FilterChip(
            selected = selectedFilter == "Ambos",
            onClick = { onFilterSelected("Ambos") },
            label = { Text("Ambos ($bothCount)") },
        )
    }
}

@Composable
private fun BoardCardItem(
    index: Int,
    board: CruciluxBoard,
    progress: CrosswordBoardProgress,
    onClick: () -> Unit,
) {
    val formattedIndex = String.format("%02d", index)
    val status = progress.status

    val (cardBg, borderColor) = when (status) {
        CrosswordBoardStatus.COMPLETED -> Pair(
            SuccessGreen.copy(alpha = 0.12f),
            SuccessGreen.copy(alpha = 0.6f),
        )
        CrosswordBoardStatus.IN_PROGRESS -> Pair(
            ProgressBlue.copy(alpha = 0.08f),
            ProgressBlue.copy(alpha = 0.5f),
        )
        CrosswordBoardStatus.NOT_STARTED -> Pair(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        )
    }

    val contentDesc = when (status) {
        CrosswordBoardStatus.COMPLETED -> "Tablero $formattedIndex, completado al 100 por ciento"
        CrosswordBoardStatus.IN_PROGRESS -> "Tablero $formattedIndex, en progreso ${progress.progressPercent} por ciento"
        CrosswordBoardStatus.NOT_STARTED -> "Tablero $formattedIndex, no iniciado"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .semantics { contentDescription = contentDesc },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Número secuencial del tablero (01..30)
            Text(
                text = formattedIndex,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = when (status) {
                    CrosswordBoardStatus.COMPLETED -> SuccessGreen
                    CrosswordBoardStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                    CrosswordBoardStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurface
                },
            )

            // Indicador de estado (○ / XX % / ✓)
            when (status) {
                CrosswordBoardStatus.COMPLETED -> {
                    Text(
                        text = "✓",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = SuccessGreen,
                    )
                }
                CrosswordBoardStatus.IN_PROGRESS -> {
                    Text(
                        text = "${progress.progressPercent} %",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProgressBlue,
                    )
                }
                CrosswordBoardStatus.NOT_STARTED -> {
                    Text(
                        text = "○",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                }
            }

            // Metadatos discretos del tablero
            Text(
                text = "${board.rows}×${board.cols}",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            )

            if (progress.bestXpEarned > 0) {
                Text(
                    text = "Mejor ${progress.bestXpEarned} XP",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
