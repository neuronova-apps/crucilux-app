package com.neuronova.crucilux.engine

import com.neuronova.crucilux.model.CrosswordCell
import com.neuronova.crucilux.model.CrosswordClue
import com.neuronova.crucilux.model.CrosswordGrid
import com.neuronova.crucilux.model.CruciluxBoard
import com.neuronova.crucilux.model.CruciluxDirection

/**
 * Motor de cuadrícula de Crucilux.
 *
 * Construye un [CrosswordGrid] a partir de un [CruciluxBoard] real del banco maestro.
 * Recibe directamente el tipo de tablero definido en CruciluxBankModels.kt —
 * no crea modelos paralelos del banco.
 *
 * Responsabilidades:
 * 1. Crear la matriz de dimensiones reales del tablero.
 * 2. Inicializar todas las celdas como inactivas.
 * 3. Recorrer cada entrada del banco.
 * 4. Determinar la dirección real (horizontal/vertical).
 * 5. Recorrer cada carácter de la respuesta.
 * 6. Calcular la coordenada exacta de cada letra.
 * 7. Activar la celda correspondiente.
 * 8. Asociar la entrada horizontal o vertical a la celda.
 * 9. Validar que la posición esté dentro de límites.
 * 10. Validar que la longitud declarada coincida con la longitud real de la respuesta.
 * 11. Validar intersecciones: si una celda ya tiene letra, confirmar que coincida.
 * 12. Si no coincide, lanzar [CruciluxGridEngineException] — no corregir silenciosamente.
 * 13. Asignar numeración usando el número real de la entrada (no recalcular).
 * 14. Generar las listas de pistas horizontales y verticales ordenadas por número.
 */
object CruciluxGridEngine {

    /**
     * Construye la cuadrícula completa a partir de un tablero real del banco.
     *
     * @param board Tablero real obtenido de [com.neuronova.crucilux.data.bank.CruciluxBankRepository].
     * @return [CrosswordGrid] con la estructura completa del crucigrama.
     * @throws CruciluxGridEngineException si el banco contiene datos incoherentes.
     */
    fun buildGrid(board: CruciluxBoard): CrosswordGrid {
        val rows = board.rows
        val cols = board.cols

        // Matriz mutable de celdas — se irá rellenando entrada por entrada.
        // Usamos un array 2D de datos mutables para poder modificar celda a celda.
        val cellData = Array(rows) { r ->
            Array(cols) { c ->
                MutableCellData(row = r, col = c)
            }
        }

        // Procesar cada entrada del banco
        for (entry in board.entries) {
            // Validar longitud declarada vs longitud real de la respuesta
            if (entry.length != entry.answer.length) {
                throw CruciluxGridEngineException(
                    "Tablero ${board.id}: entrada ${entry.bankId} declara length=${entry.length} " +
                        "pero answer.length=${entry.answer.length}"
                )
            }

            val isHorizontal = entry.direction == CruciluxDirection.HORIZONTAL

            for (charIndex in entry.answer.indices) {
                val r = if (isHorizontal) entry.row else entry.row + charIndex
                val c = if (isHorizontal) entry.col + charIndex else entry.col

                // Validar límites
                if (r < 0 || r >= rows || c < 0 || c >= cols) {
                    throw CruciluxGridEngineException(
                        "Tablero ${board.id}: entrada ${entry.bankId} sale de límites " +
                            "en índice $charIndex → ($r, $c), tablero ${rows}x${cols}"
                    )
                }

                val letter = entry.answer[charIndex]
                val cell = cellData[r][c]

                // Validar intersección: si la celda ya tiene letra, debe coincidir
                if (cell.isActive && cell.solutionLetter != null && cell.solutionLetter != letter) {
                    throw CruciluxGridEngineException(
                        "Tablero ${board.id}: conflicto en intersección ($r, $c) — " +
                            "letra existente='${cell.solutionLetter}', nueva='$letter' " +
                            "entrada ${entry.bankId}"
                    )
                }

                // Activar celda y asignar letra interna (solo para validación, nunca para UI)
                cell.isActive = true
                cell.solutionLetter = letter

                // Asociar entrada según dirección
                if (isHorizontal) {
                    cell.horizontalEntryBankId = entry.bankId
                } else {
                    cell.verticalEntryBankId = entry.bankId
                }

                // Si es el inicio de la palabra, marcar y asignar número de pista
                if (charIndex == 0) {
                    cell.isWordStart = true
                    // Si ya había otro número asignado (otra entrada comienza aquí),
                    // conservar el menor (o el existente). En el banco bien formado,
                    // una misma celda de inicio puede tener H y V con el mismo número.
                    // Usamos el número real de la entrada, no lo recalculamos.
                    if (cell.clueNumber == null) {
                        cell.clueNumber = entry.number
                    }
                    // Si hay conflicto de número (dos entradas distintas inician aquí con
                    // distinto número), conservamos el menor para consistencia visual.
                    else if (entry.number < cell.clueNumber!!) {
                        cell.clueNumber = entry.number
                    }
                }
            }
        }

        // Construir la matriz inmutable de CrosswordCell
        val immutableCells: List<List<CrosswordCell>> = List(rows) { r ->
            List(cols) { c ->
                val d = cellData[r][c]
                CrosswordCell(
                    row = r,
                    col = c,
                    isActive = d.isActive,
                    clueNumber = d.clueNumber,
                    horizontalEntryBankId = d.horizontalEntryBankId,
                    verticalEntryBankId = d.verticalEntryBankId,
                    isWordStart = d.isWordStart,
                    solutionLetter = d.solutionLetter,
                )
            }
        }

        // Construir listas de pistas (sin respuesta)
        val horizontalClues: List<CrosswordClue> = board.entries
            .filter { it.direction == CruciluxDirection.HORIZONTAL }
            .map { entry ->
                CrosswordClue(
                    bankId = entry.bankId,
                    number = entry.number,
                    clue = entry.clue,
                    direction = CruciluxDirection.HORIZONTAL,
                    length = entry.length,
                    startRow = entry.row,
                    startCol = entry.col,
                )
            }
            .sortedBy { it.number }

        val verticalClues: List<CrosswordClue> = board.entries
            .filter { it.direction == CruciluxDirection.VERTICAL }
            .map { entry ->
                CrosswordClue(
                    bankId = entry.bankId,
                    number = entry.number,
                    clue = entry.clue,
                    direction = CruciluxDirection.VERTICAL,
                    length = entry.length,
                    startRow = entry.row,
                    startCol = entry.col,
                )
            }
            .sortedBy { it.number }

        return CrosswordGrid(
            rows = rows,
            cols = cols,
            cells = immutableCells,
            horizontalClues = horizontalClues,
            verticalClues = verticalClues,
        )
    }

    /**
     * Clase de datos mutable interna usada durante la construcción de la cuadrícula.
     * Solo existe dentro del motor y nunca se expone fuera de [buildGrid].
     */
    private data class MutableCellData(
        val row: Int,
        val col: Int,
        var isActive: Boolean = false,
        var clueNumber: Int? = null,
        var horizontalEntryBankId: String? = null,
        var verticalEntryBankId: String? = null,
        var isWordStart: Boolean = false,
        var solutionLetter: Char? = null,
    )
}

/**
 * Excepción lanzada cuando el motor detecta una incoherencia en los datos del banco.
 * No se muestra al usuario — solo aparece en logs de desarrollo y pruebas unitarias.
 */
class CruciluxGridEngineException(message: String) : Exception(message)
