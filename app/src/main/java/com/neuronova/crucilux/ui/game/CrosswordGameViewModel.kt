package com.neuronova.crucilux.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.data.GameSessionState
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.engine.CruciluxGridEngine
import com.neuronova.crucilux.model.CrosswordClue
import com.neuronova.crucilux.model.CrosswordGrid
import com.neuronova.crucilux.model.CruciluxBoard
import com.neuronova.crucilux.model.CruciluxDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ─────────────────────────────────────────────────────────────────────────────
// Modo de comprobación
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Modo de comprobación de respuestas en la partida.
 *
 * [CLASSIC] — acepta letras y valida palabras automáticamente al completarlas.
 * [ASSISTED] — verifica cada letra individualmente y elimina las incorrectas con feedback.
 */
enum class CheckMode {
    CLASSIC,
    ASSISTED,
}

// ─────────────────────────────────────────────────────────────────────────────
// Estado completo de la pantalla de juego
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Estado completo e inmutable de la pantalla de juego interactivo.
 *
 * @property isLoading `true` mientras se carga y construye el tablero.
 * @property board Tablero real del banco, o null si aún no se cargó.
 * @property grid Cuadrícula construida por [CruciluxGridEngine].
 * @property errorMessage Mensaje de error interno sin datos del banco.
 * @property selectedRow Fila de la celda actualmente seleccionada (-1 = ninguna).
 * @property selectedCol Columna de la celda seleccionada (-1 = ninguna).
 * @property activeDirection Dirección activa de la palabra actual.
 * @property activeEntryBankId bankId de la palabra actualmente activa.
 * @property activeCellsInWord Conjunto de coordenadas de todas las celdas de la palabra activa.
 * @property userLetters Letras introducidas por el usuario. Nunca contiene soluciones del banco.
 * @property checkMode Modo de comprobación actual.
 * @property incorrectCells Celdas con letra incorrecta temporal en modo Asistida.
 * @property validatedEntryBankIds Conjunto de bankIds de palabras validadas correctamente.
 * @property validatedCells Conjunto de coordenadas (row, col) pertenecientes a palabras validadas (bloqueadas).
 * @property isCompleted `true` cuando todas las palabras del tablero han sido validadas.
 * @property isSessionSaved `true` si hay una sesión guardada localmente.
 */
data class CrosswordGameState(
    val isLoading: Boolean = true,
    val board: CruciluxBoard? = null,
    val grid: CrosswordGrid? = null,
    val errorMessage: String? = null,
    val selectedRow: Int = -1,
    val selectedCol: Int = -1,
    val activeDirection: CruciluxDirection = CruciluxDirection.HORIZONTAL,
    val activeEntryBankId: String? = null,
    val activeCellsInWord: Set<Pair<Int, Int>> = emptySet(),
    val userLetters: Map<Pair<Int, Int>, Char> = emptyMap(),
    val checkMode: CheckMode = CheckMode.CLASSIC,
    val incorrectCells: Set<Pair<Int, Int>> = emptySet(),
    val validatedEntryBankIds: Set<String> = emptySet(),
    val validatedCells: Set<Pair<Int, Int>> = emptySet(),
    val isCompleted: Boolean = false,
    val isSessionSaved: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel de la pantalla de juego interactivo de Crucilux.
 *
 * Gestiona:
 * - Carga del tablero desde [CruciluxBankRepository].
 * - Construcción de la cuadrícula mediante [CruciluxGridEngine].
 * - Selección de celda y alternancia de dirección H/V.
 * - Entrada de letras con avance automático y bloqueo de celdas validadas.
 * - Validación automática continua de palabras completas.
 * - Navegación ordenada entre pistas (anterior / siguiente).
 * - Detección automática de tablero completado al 100%.
 * - Modos de comprobación: Clásica y Asistida.
 * - Autoguardado y restauración transparente mediante [GameSessionManager].
 *
 * Nunca expone ni registra en logs la solución del banco.
 */
class CrosswordGameViewModel(
    private val sessionManager: GameSessionManager? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(CrosswordGameState())
    val state: StateFlow<CrosswordGameState> = _state.asStateFlow()

    companion object {
        /**
         * Factory que inyecta [GameSessionManager] en el ViewModel.
         */
        fun factory(sessionManager: GameSessionManager): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CrosswordGameViewModel(sessionManager) as T
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Carga y restauración
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Carga el tablero por su ID y restaura la sesión guardada si coincide.
     */
    fun loadBoard(boardId: String) {
        val current = _state.value
        if (!current.isLoading && current.board?.id == boardId && current.grid != null) return

        viewModelScope.launch(Dispatchers.Default) {
            _state.value = CrosswordGameState(isLoading = true)
            try {
                val repository = CruciluxBankRepository.getInstance()
                val board = repository.getBoardById(boardId)

                if (board == null) {
                    _state.value = CrosswordGameState(
                        isLoading = false,
                        errorMessage = "No se encontró el tablero: $boardId",
                    )
                    return@launch
                }

                val grid = CruciluxGridEngine.buildGrid(board)

                // Intentar restaurar sesión guardada
                val savedSession = if (sessionManager != null) {
                    withContext(Dispatchers.IO) {
                        try {
                            sessionManager.sessionFlow.first()
                        } catch (e: Exception) {
                            null
                        }
                    }
                } else null

                if (savedSession != null && savedSession.boardId == boardId && savedSession.hasActiveSession) {
                    val userLetters = savedSession.userLetters
                    val savedRow = savedSession.selectedRow
                    val savedCol = savedSession.selectedCol
                    val savedDir = if (savedSession.activeDirection == "V") {
                        CruciluxDirection.VERTICAL
                    } else {
                        CruciluxDirection.HORIZONTAL
                    }
                    val savedMode = if (savedSession.checkMode == "ASSISTED") CheckMode.ASSISTED else CheckMode.CLASSIC

                    // Derivar estado de validación a partir de userLetters guardadas
                    val (valBankIds, valCells) = computeValidatedState(grid, board, userLetters)
                    val isComp = valBankIds.size == board.entries.size && board.entries.isNotEmpty()

                    // Calcular palabra activa
                    val (entryId, cells) = computeActiveWord(grid, savedRow, savedCol, savedDir)

                    _state.value = CrosswordGameState(
                        isLoading = false,
                        board = board,
                        grid = grid,
                        selectedRow = savedRow,
                        selectedCol = savedCol,
                        activeDirection = savedDir,
                        activeEntryBankId = entryId,
                        activeCellsInWord = cells,
                        userLetters = userLetters,
                        checkMode = savedMode,
                        validatedEntryBankIds = valBankIds,
                        validatedCells = valCells,
                        isCompleted = isComp,
                        isSessionSaved = true,
                    )
                } else {
                    // Estado inicial limpio: seleccionar la primera pista por defecto
                    val allClues = getAllCluesOrdered(grid)
                    val firstClue = allClues.firstOrNull()
                    val startRow = firstClue?.startRow ?: 0
                    val startCol = firstClue?.startCol ?: 0
                    val startDir = firstClue?.direction ?: CruciluxDirection.HORIZONTAL
                    val (entryId, cells) = computeActiveWord(grid, startRow, startCol, startDir)

                    _state.value = CrosswordGameState(
                        isLoading = false,
                        board = board,
                        grid = grid,
                        selectedRow = startRow,
                        selectedCol = startCol,
                        activeDirection = startDir,
                        activeEntryBankId = entryId,
                        activeCellsInWord = cells,
                    )
                }
            } catch (e: Exception) {
                _state.value = CrosswordGameState(
                    isLoading = false,
                    errorMessage = "Error al construir el tablero. Inténtalo de nuevo.",
                )
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Selección de celda y alternancia de dirección
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Gestiona el toque sobre una celda del tablero.
     */
    fun onCellTapped(row: Int, col: Int) {
        val st = _state.value
        val grid = st.grid ?: return
        val cell = grid.cellAt(row, col) ?: return
        if (!cell.isActive) return

        val hasH = cell.horizontalEntryBankId != null
        val hasV = cell.verticalEntryBankId != null
        val isSameCell = (row == st.selectedRow && col == st.selectedCol)

        val newDirection = when {
            isSameCell && hasH && hasV -> {
                if (st.activeDirection == CruciluxDirection.HORIZONTAL) {
                    CruciluxDirection.VERTICAL
                } else {
                    CruciluxDirection.HORIZONTAL
                }
            }
            hasH && hasV -> {
                if (st.activeDirection == CruciluxDirection.VERTICAL) {
                    CruciluxDirection.VERTICAL
                } else {
                    CruciluxDirection.HORIZONTAL
                }
            }
            hasV -> CruciluxDirection.VERTICAL
            else -> CruciluxDirection.HORIZONTAL
        }

        val (entryId, cells) = computeActiveWord(grid, row, col, newDirection)

        _state.value = st.copy(
            selectedRow = row,
            selectedCol = col,
            activeDirection = newDirection,
            activeEntryBankId = entryId,
            activeCellsInWord = cells,
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Navegación entre pistas (Flechas ‹ y ›)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Selecciona la pista siguiente en el orden canónico del crucigrama.
     */
    fun onNextClue() {
        val st = _state.value
        val grid = st.grid ?: return
        val allClues = getAllCluesOrdered(grid)
        if (allClues.isEmpty()) return

        val currentIndex = allClues.indexOfFirst {
            it.bankId == st.activeEntryBankId && it.direction == st.activeDirection
        }
        val nextIndex = if (currentIndex < 0 || currentIndex >= allClues.lastIndex) 0 else currentIndex + 1
        selectClue(allClues[nextIndex])
    }

    /**
     * Selecciona la pista anterior en el orden canónico del crucigrama.
     */
    fun onPreviousClue() {
        val st = _state.value
        val grid = st.grid ?: return
        val allClues = getAllCluesOrdered(grid)
        if (allClues.isEmpty()) return

        val currentIndex = allClues.indexOfFirst {
            it.bankId == st.activeEntryBankId && it.direction == st.activeDirection
        }
        val prevIndex = if (currentIndex <= 0) allClues.lastIndex else currentIndex - 1
        selectClue(allClues[prevIndex])
    }

    /**
     * Selecciona directamente una pista específica y sitúa el cursor en su primera casilla editable.
     */
    fun selectClue(clue: CrosswordClue) {
        val st = _state.value
        val grid = st.grid ?: return
        val (entryId, cells) = computeActiveWord(grid, clue.startRow, clue.startCol, clue.direction)

        // Ordenar las celdas de la palabra para ubicar la primera casilla editable
        val sortedCells = wordCellsSorted(grid, clue.bankId, clue.direction)
        val firstEditable = sortedCells.firstOrNull { it !in st.validatedCells } ?: sortedCells.firstOrNull()
        val targetRow = firstEditable?.first ?: clue.startRow
        val targetCol = firstEditable?.second ?: clue.startCol

        _state.value = st.copy(
            selectedRow = targetRow,
            selectedCol = targetCol,
            activeDirection = clue.direction,
            activeEntryBankId = entryId,
            activeCellsInWord = cells,
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Entrada de letras con validación automática y bloqueo
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Procesa una letra introducida mediante el teclado virtual.
     *
     * Reglas:
     * - Si la celda actual pertenece a una palabra ya validada (bloqueada): ignora la edición y avanza.
     * - Si no está bloqueada: asigna la letra.
     * - Verifica automáticamente si alguna palabra que cruza por la celda quedó completada y correcta.
     * - Si es correcta: marca como VALIDADA, bloquea sus celdas y muestra en verde.
     * - Si está completa pero incorrecta: se mantiene editable sin borrar ni penalizar.
     * - Verifica si todo el tablero quedó completado (100%).
     */
    fun onLetterEntered(letter: Char) {
        val st = _state.value
        val grid = st.grid ?: return
        val board = st.board ?: return
        if (st.selectedRow < 0 || st.selectedCol < 0) return
        val cell = grid.cellAt(st.selectedRow, st.selectedCol) ?: return
        if (!cell.isActive) return

        val pos = Pair(st.selectedRow, st.selectedCol)

        // Si la celda está bloqueada (validada), no permitir modificar su letra
        if (pos in st.validatedCells) {
            advanceToNextCell(grid, st.selectedRow, st.selectedCol, st.activeDirection, st.activeEntryBankId)
            return
        }

        val upperLetter = letter.uppercaseChar()
        val newUserLetters = st.userLetters.toMutableMap()
        newUserLetters[pos] = upperLetter

        if (st.checkMode == CheckMode.ASSISTED) {
            val isCorrect = cell.solutionLetter?.uppercaseChar() == upperLetter
            if (!isCorrect) {
                val newIncorrect = st.incorrectCells + pos
                _state.value = st.copy(
                    userLetters = newUserLetters,
                    incorrectCells = newIncorrect,
                )
                viewModelScope.launch {
                    delay(350L)
                    val current = _state.value
                    _state.value = current.copy(
                        userLetters = current.userLetters - pos,
                        incorrectCells = current.incorrectCells - pos,
                    )
                    triggerAutosave()
                }
                return
            } else {
                val newIncorrect = st.incorrectCells - pos
                _state.value = st.copy(
                    userLetters = newUserLetters,
                    incorrectCells = newIncorrect,
                )
            }
        } else {
            _state.value = st.copy(userLetters = newUserLetters)
        }

        // Validación automática tras introducir la letra
        val (valBankIds, valCells) = computeValidatedState(grid, board, newUserLetters)
        val isCompleted = valBankIds.size == board.entries.size && board.entries.isNotEmpty()

        _state.value = _state.value.copy(
            validatedEntryBankIds = valBankIds,
            validatedCells = valCells,
            isCompleted = isCompleted,
        )

        // Avance automático a la siguiente casilla de la palabra
        advanceToNextCell(grid, st.selectedRow, st.selectedCol, st.activeDirection, st.activeEntryBankId)

        triggerAutosave()
    }

    /**
     * Borra la letra de la celda seleccionada si no está bloqueada.
     * Si la celda actual está vacía, retrocede a la casilla editable anterior.
     */
    fun onDeleteLetter() {
        val st = _state.value
        val grid = st.grid ?: return
        if (st.selectedRow < 0 || st.selectedCol < 0) return

        val pos = Pair(st.selectedRow, st.selectedCol)

        // Si la casilla actual está bloqueada, no borrar su letra; simplemente retroceder
        if (pos in st.validatedCells) {
            val prev = prevCellInWord(grid, st.selectedRow, st.selectedCol, st.activeDirection, st.activeEntryBankId)
            if (prev != null) {
                val (pr, pc) = prev
                val (entryId, cells) = computeActiveWord(grid, pr, pc, st.activeDirection)
                _state.value = st.copy(
                    selectedRow = pr,
                    selectedCol = pc,
                    activeEntryBankId = entryId,
                    activeCellsInWord = cells,
                )
            }
            return
        }

        val hasLetter = st.userLetters.containsKey(pos)

        if (hasLetter) {
            val newLetters = st.userLetters - pos
            val newIncorrect = st.incorrectCells - pos
            _state.value = st.copy(userLetters = newLetters, incorrectCells = newIncorrect)
        } else {
            // Retroceder a celda anterior
            val prev = prevCellInWord(grid, st.selectedRow, st.selectedCol, st.activeDirection, st.activeEntryBankId)
            if (prev != null) {
                val (pr, pc) = prev
                val prevPos = Pair(pr, pc)
                val (entryId, cells) = computeActiveWord(grid, pr, pc, st.activeDirection)

                // Solo borrar la letra anterior si no está bloqueada
                val newLetters = if (prevPos !in st.validatedCells) {
                    st.userLetters - prevPos
                } else {
                    st.userLetters
                }
                val newIncorrect = st.incorrectCells - prevPos

                _state.value = st.copy(
                    selectedRow = pr,
                    selectedCol = pc,
                    activeEntryBankId = entryId,
                    activeCellsInWord = cells,
                    userLetters = newLetters,
                    incorrectCells = newIncorrect,
                )
            }
        }
        triggerAutosave()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Reiniciar partida (Volver a Jugar)
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Reinicia el tablero actual limpiando las letras y estados de validación.
     */
    fun onPlayAgain() {
        val st = _state.value
        val grid = st.grid ?: return
        val allClues = getAllCluesOrdered(grid)
        val firstClue = allClues.firstOrNull()
        val startRow = firstClue?.startRow ?: 0
        val startCol = firstClue?.startCol ?: 0
        val startDir = firstClue?.direction ?: CruciluxDirection.HORIZONTAL
        val (entryId, cells) = computeActiveWord(grid, startRow, startCol, startDir)

        _state.value = st.copy(
            selectedRow = startRow,
            selectedCol = startCol,
            activeDirection = startDir,
            activeEntryBankId = entryId,
            activeCellsInWord = cells,
            userLetters = emptyMap(),
            incorrectCells = emptySet(),
            validatedEntryBankIds = emptySet(),
            validatedCells = emptySet(),
            isCompleted = false,
        )
        triggerAutosave()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Modo de comprobación
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Cambia el modo de comprobación.
     */
    fun onSetCheckMode(mode: CheckMode) {
        _state.value = _state.value.copy(
            checkMode = mode,
            incorrectCells = emptySet(),
        )
        triggerAutosave()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Autoguardado público
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Guarda la sesión actual inmediatamente.
     */
    fun saveSessionNow() {
        triggerAutosave()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers privados y de cálculo
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Devuelve todas las pistas del crucigrama ordenadas canónicamente:
     * por número de pista ascendente, y ante igualdad, HORIZONTAL antes que VERTICAL.
     */
    fun getAllCluesOrdered(grid: CrosswordGrid): List<CrosswordClue> {
        val all = grid.horizontalClues + grid.verticalClues
        return all.sortedWith(
            compareBy<CrosswordClue> { it.number }
                .thenBy { if (it.direction == CruciluxDirection.HORIZONTAL) 0 else 1 }
        )
    }

    /**
     * Determina de forma determinista qué palabras y celdas están validadas.
     */
    private fun computeValidatedState(
        grid: CrosswordGrid,
        board: CruciluxBoard,
        userLetters: Map<Pair<Int, Int>, Char>,
    ): Pair<Set<String>, Set<Pair<Int, Int>>> {
        val valBankIds = mutableSetOf<String>()
        val valCells = mutableSetOf<Pair<Int, Int>>()

        for (entry in board.entries) {
            val isHorizontal = entry.direction == CruciluxDirection.HORIZONTAL
            var isAllCorrect = true
            val entryCells = mutableListOf<Pair<Int, Int>>()

            for (i in entry.answer.indices) {
                val r = if (isHorizontal) entry.row else entry.row + i
                val c = if (isHorizontal) entry.col + i else entry.col
                val pos = Pair(r, c)
                entryCells.add(pos)

                val userCh = userLetters[pos]?.uppercaseChar()
                val solCh = grid.cellAt(r, c)?.solutionLetter?.uppercaseChar()
                if (userCh == null || solCh == null || userCh != solCh) {
                    isAllCorrect = false
                }
            }

            if (isAllCorrect) {
                valBankIds.add(entry.bankId)
                valCells.addAll(entryCells)
            }
        }

        return Pair(valBankIds, valCells)
    }

    /**
     * Avanza el cursor a la siguiente casilla de la palabra.
     */
    private fun advanceToNextCell(
        grid: CrosswordGrid,
        row: Int,
        col: Int,
        direction: CruciluxDirection,
        entryBankId: String?,
    ) {
        val next = nextCellInWord(grid, row, col, direction, entryBankId)
        if (next != null) {
            val (nr, nc) = next
            val (entryId, cells) = computeActiveWord(grid, nr, nc, direction)
            _state.value = _state.value.copy(
                selectedRow = nr,
                selectedCol = nc,
                activeEntryBankId = entryId,
                activeCellsInWord = cells,
            )
        }
    }

    /**
     * Devuelve el bankId y las coordenadas de la palabra activa para (row, col).
     */
    private fun computeActiveWord(
        grid: CrosswordGrid,
        row: Int,
        col: Int,
        direction: CruciluxDirection,
    ): Pair<String?, Set<Pair<Int, Int>>> {
        val cell = grid.cellAt(row, col) ?: return Pair(null, emptySet())
        val entryBankId = if (direction == CruciluxDirection.HORIZONTAL) {
            cell.horizontalEntryBankId
        } else {
            cell.verticalEntryBankId
        }
        if (entryBankId == null) return Pair(null, emptySet())

        val cells = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until grid.rows) {
            for (c in 0 until grid.cols) {
                val candidate = grid.cellAt(r, c) ?: continue
                val belongs = if (direction == CruciluxDirection.HORIZONTAL) {
                    candidate.horizontalEntryBankId == entryBankId
                } else {
                    candidate.verticalEntryBankId == entryBankId
                }
                if (belongs) cells.add(Pair(r, c))
            }
        }
        return Pair(entryBankId, cells)
    }

    /**
     * Devuelve la siguiente celda en la palabra activa.
     */
    private fun nextCellInWord(
        grid: CrosswordGrid,
        row: Int,
        col: Int,
        direction: CruciluxDirection,
        entryBankId: String?,
    ): Pair<Int, Int>? {
        if (entryBankId == null) return null
        val sortedCells = wordCellsSorted(grid, entryBankId, direction)
        val currentIndex = sortedCells.indexOfFirst { it.first == row && it.second == col }
        if (currentIndex < 0 || currentIndex >= sortedCells.lastIndex) return null
        return sortedCells[currentIndex + 1]
    }

    /**
     * Devuelve la celda anterior en la palabra activa.
     */
    private fun prevCellInWord(
        grid: CrosswordGrid,
        row: Int,
        col: Int,
        direction: CruciluxDirection,
        entryBankId: String?,
    ): Pair<Int, Int>? {
        if (entryBankId == null) return null
        val sortedCells = wordCellsSorted(grid, entryBankId, direction)
        val currentIndex = sortedCells.indexOfFirst { it.first == row && it.second == col }
        if (currentIndex <= 0) return null
        return sortedCells[currentIndex - 1]
    }

    /**
     * Devuelve las celdas de una palabra ordenadas por fila (V) o columna (H).
     */
    private fun wordCellsSorted(
        grid: CrosswordGrid,
        entryBankId: String,
        direction: CruciluxDirection,
    ): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until grid.rows) {
            for (c in 0 until grid.cols) {
                val cell = grid.cellAt(r, c) ?: continue
                val belongs = if (direction == CruciluxDirection.HORIZONTAL) {
                    cell.horizontalEntryBankId == entryBankId
                } else {
                    cell.verticalEntryBankId == entryBankId
                }
                if (belongs) cells.add(Pair(r, c))
            }
        }
        return if (direction == CruciluxDirection.HORIZONTAL) {
            cells.sortedBy { it.second }
        } else {
            cells.sortedBy { it.first }
        }
    }

    /** Guarda la sesión actual en [GameSessionManager] de forma asíncrona. */
    private fun triggerAutosave() {
        val mgr = sessionManager ?: return
        val st = _state.value
        val board = st.board ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionState = GameSessionState(
                    boardId = board.id,
                    category = board.category,
                    boardSize = board.size,
                    selectedRow = st.selectedRow,
                    selectedCol = st.selectedCol,
                    activeDirection = if (st.activeDirection == CruciluxDirection.VERTICAL) "V" else "H",
                    checkMode = if (st.checkMode == CheckMode.ASSISTED) "ASSISTED" else "CLASSIC",
                    userLetters = st.userLetters,
                    isFinished = st.isCompleted,
                    lastUpdatedMs = System.currentTimeMillis(),
                )
                mgr.saveSession(sessionState)
            } catch (e: Exception) {
                // Autoguardado silencioso — no interrumpir la partida
            }
        }
    }
}
