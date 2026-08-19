package com.neuronova.crucilux.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neuronova.crucilux.model.CrosswordCell
import com.neuronova.crucilux.model.CrosswordGrid
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.ui.theme.SuccessGreen

/**
 * Componente interactivo de cuadrícula para Crucilux.
 *
 * Características:
 * - Centrado y responsive para cualquier dimensión dinámica rows × cols.
 * - Celdas activas seleccionables por toque.
 * - Resaltado visual para celda activa, palabra activa y celdas validadas (verde).
 * - Muestra las letras introducidas por el usuario (sin revelar soluciones del banco).
 * - Bloqueo visual e indicación verde accesible para palabras correctas.
 * - Número de pista en la esquina superior izquierda.
 * - Accesibilidad TalkBack completa con descripción de estado validado.
 */
@Composable
fun CrosswordBoardView(
    grid: CrosswordGrid,
    selectedRow: Int = -1,
    selectedCol: Int = -1,
    activeDirection: CruciluxDirection = CruciluxDirection.HORIZONTAL,
    activeCellsInWord: Set<Pair<Int, Int>> = emptySet(),
    userLetters: Map<Pair<Int, Int>, Char> = emptyMap(),
    validatedCells: Set<Pair<Int, Int>> = emptySet(),
    incorrectCells: Set<Pair<Int, Int>> = emptySet(),
    onCellTapped: (row: Int, col: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val availableWidth = maxWidth
        val cellSize: Dp = (availableWidth / grid.cols).coerceAtMost(48.dp)
        val boardWidth: Dp = cellSize * grid.cols
        val boardHeight: Dp = cellSize * grid.rows

        val clueNumberSp = when {
            cellSize < 24.dp -> 6.sp
            cellSize < 32.dp -> 7.sp
            cellSize < 42.dp -> 8.sp
            else -> 9.sp
        }

        val letterSp = when {
            cellSize < 24.dp -> 11.sp
            cellSize < 32.dp -> 14.sp
            cellSize < 42.dp -> 18.sp
            else -> 22.sp
        }

        Column(
            modifier = Modifier
                .width(boardWidth)
                .height(boardHeight)
                .clipToBounds(),
        ) {
            for (row in 0 until grid.rows) {
                Row(
                    modifier = Modifier
                        .width(boardWidth)
                        .height(cellSize),
                ) {
                    for (col in 0 until grid.cols) {
                        val cell = grid.cells[row][col]
                        val isSelected = (row == selectedRow && col == selectedCol)
                        val isInActiveWord = activeCellsInWord.contains(Pair(row, col))
                        val isValidated = validatedCells.contains(Pair(row, col))
                        val isIncorrect = incorrectCells.contains(Pair(row, col))
                        val letter = userLetters[Pair(row, col)]

                        CrosswordCellView(
                            cell = cell,
                            cellSize = cellSize,
                            clueNumberSp = clueNumberSp,
                            letterSp = letterSp,
                            isSelected = isSelected,
                            isInActiveWord = isInActiveWord,
                            isValidated = isValidated,
                            isIncorrect = isIncorrect,
                            userLetter = letter,
                            activeDirection = activeDirection,
                            onCellTapped = { onCellTapped(row, col) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Celda individual interactiva.
 */
@Composable
private fun CrosswordCellView(
    cell: CrosswordCell,
    cellSize: Dp,
    clueNumberSp: androidx.compose.ui.unit.TextUnit,
    letterSp: androidx.compose.ui.unit.TextUnit,
    isSelected: Boolean,
    isInActiveWord: Boolean,
    isValidated: Boolean,
    isIncorrect: Boolean,
    userLetter: Char?,
    activeDirection: CruciluxDirection,
    onCellTapped: () -> Unit,
) {
    if (!cell.isActive) {
        // Celda inactiva (bloque negro)
        Box(
            modifier = Modifier
                .size(cellSize)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f))
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                )
                .semantics { contentDescription = "Celda inactiva" },
        )
        return
    }

    // Colores y bordes según prioridad de estado:
    // 1. isIncorrect -> Error
    // 2. isValidated -> Verde accesible
    // 3. isSelected -> Primario destacado
    // 4. isInActiveWord -> Resaltado suave
    // 5. Normal -> Superficie estándar
    val backgroundColor = when {
        isIncorrect -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
        isValidated && isSelected -> SuccessGreen.copy(alpha = 0.35f)
        isValidated -> SuccessGreen.copy(alpha = 0.22f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
        isInActiveWord -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    val borderColor = when {
        isIncorrect -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        isValidated -> SuccessGreen.copy(alpha = 0.75f)
        isInActiveWord -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    }

    val borderWidth = when {
        isSelected -> 2.dp
        isIncorrect -> 1.5.dp
        isValidated -> 1.2.dp
        isInActiveWord -> 1.dp
        else -> 0.5.dp
    }

    val letterColor = when {
        isIncorrect -> MaterialTheme.colorScheme.error
        isValidated -> MaterialTheme.colorScheme.onSurface
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val semanticDesc = buildString {
        append("Celda fila ${cell.row + 1}, columna ${cell.col + 1}")
        if (cell.clueNumber != null) append(", número ${cell.clueNumber}")
        if (isSelected) {
            val dirName = if (activeDirection == CruciluxDirection.HORIZONTAL) "horizontal" else "vertical"
            append(", seleccionada en dirección $dirName")
        } else if (isInActiveWord) {
            append(", en palabra activa")
        }
        if (isValidated) {
            append(", palabra validada")
        }
        if (userLetter != null) {
            append(", letra $userLetter")
        } else {
            append(", vacía")
        }
        if (isIncorrect) append(", incorrecta")
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .background(backgroundColor)
            .border(width = borderWidth, color = borderColor)
            .clickable(
                role = Role.Button,
                onClick = onCellTapped,
            )
            .semantics {
                selected = isSelected
                contentDescription = semanticDesc
            },
        contentAlignment = Alignment.Center,
    ) {
        // Número de pista
        if (cell.clueNumber != null) {
            Text(
                text = cell.clueNumber.toString(),
                style = TextStyle(
                    fontSize = clueNumberSp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 1.5.dp, y = 0.5.dp),
            )
        }

        // Letra introducida por el usuario
        if (userLetter != null) {
            Text(
                text = userLetter.toString(),
                style = TextStyle(
                    fontSize = letterSp,
                    fontWeight = FontWeight.Bold,
                    color = letterColor,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
