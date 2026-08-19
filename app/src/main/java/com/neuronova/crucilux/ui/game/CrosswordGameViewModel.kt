package com.neuronova.crucilux.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.data.GameSessionState
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.engine.CruciluxGridEngine
import com.neuronova.crucilux.model.CruciluxBoard
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.model.CrosswordGrid
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
 * [CLASSIC] — acepta cualquier letra sin verificación inmediata.
 * [ASSISTED] — verifica cada letra y elimina las incorrectas con feedback.
 */
enum class CheckMode {
    CLASSIC,
    ASSISTED,
}

// ─────────────────────────────────────────────────────────────────────────────
// Resultado de comprobar palabra activa
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resultado de la acción "Comprobar palabra" disponible en modo Clásica.
 * Nunca revela automáticamente las letras correctas.
 */
sealed class CheckWordResult {
    /** La palabra activa tiene celdas vacías. */
    object Incomplete : CheckWordResult()

    /** Todas las letras del usuario coinciden con la solución. */
    object Correct : CheckWordResult()

    /** Algunas letras no coinciden con la solución. */
    object HasErrors : CheckWordResult()
}

// ─────────────────────────────────────────────────────────────────────────────
// Estado completo de la pantalla de juego — Etapa 2
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
 * @property incorrectCells Celdas con letra incorrecta en modo Asistida.
 * @property checkWordResult Resultado temporal de la última acción "Comprobar".
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
    val checkWordResult: CheckWordResult? = null,
    val isSessionSaved: Boolean = false,
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel de la pantalla de juego interactivo de Crucilux — Etapa 2.
 *
 * Gestiona:
 * - Carga del tablero desde [CruciluxBankRepository].
 * - Construcción de la cuadrícula mediante [CruciluxGridEngine].
 * - Selección de celda y alternancia de dirección H/V.
 * - Entrada de letras con avance automático.
 * - Modos de comprobación: Clásica y Asistida.
 * - Acción "Comprobar palabra" para el modo Clásica.
 * - Autoguardado mediante [GameSessionManager].
 * - Restauración de sesión guardada.
 *
 * Nunca expone ni registra en logs la [solutionLetter] de ninguna celda.
 */
class CrosswordGameViewModel(
    private val sessionManager: GameSessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow(CrosswordGameState())
    val state: StateFlow<CrosswordGameState> = _state.asStateFlow()

    companion object {
        /**
         * Factory que inyecta [GameSessionManager] en el ViewModel.
         * Usar en Compose: `viewModel(factory = CrosswordGameViewModel.factory(sessionManager))`
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
     * Seguro llamarlo múltiples veces — solo carga una vez por boardId.
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
                val savedSession = withContext(Dispatchers.IO) {
                    try {
                        sessionManager.sessionFlow.first()
                    } catch (e: Exception) {
                        null
                    }
                }

                if (savedSession != null && savedSession.boardId == boardId && savedSession.hasActiveSession) {
                    // Restaurar desde sesión guardada
                    val userLetters = savedSession.userLetters
                    val savedRow = savedSession.selectedRow
                    val savedCol = savedSession.selectedCol
                    val savedDir = if (savedSession.activeDirection == "V") {
                        CruciluxDirection.VERTICAL
                    } else {
                        CruciluxDirection.HORIZONTAL
                    }
                    val savedMode = if (savedSession.checkMode == "ASSISTED") CheckMode.ASSISTED else CheckMode.CLASSIC

                    // Calcular la palabra activa a partir de la celda guardada
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
                        isSessionSaved = true,
                    )
                } else {
                    _state.value = CrosswordGameState(
                        isLoading = false,
                        board = board,
                        grid = grid,
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
    // Selección de celda
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Gestiona el toque sobre una celda del tablero.
     *
     * Comportamiento:
     * - Celdas inactivas: ignorado.
     * - Mismo toque que la celda seleccionada: alterna dirección si pertenece a ambas.
     * - Toque sobre otra celda: selecciona la celda con la dirección apropiada.
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
                // Alternar dirección al tocar la misma celda con cruce
                if (st.activeDirection == CruciluxDirection.HORIZONTAL) {
                    CruciluxDirection.VERTICAL
                } else {
                    CruciluxDirection.HORIZONTAL
                }
            }
            hasH && hasV -> {
                // Nueva celda con cruce: preferir horizontal, o la dirección activa si aplica
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
            checkWordResult = null,
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Entrada de letras
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Procesa una letra introducida mediante el teclado virtual.
     *
     * En modo [CheckMode.ASSISTED]: verifica la letra contra la solución interna.
     * Si es incorrecta, la elimina después de 300 ms y emite feedback accesible.
     * En modo [CheckMode.CLASSIC]: acepta la letra sin verificación inmediata.
     *
     * Nunca expone la [solutionLetter] al exterior.
     */
    fun onLetterEntered(letter: Char) {
        val st = _state.value
        val grid = st.grid ?: return
        if (st.selectedRow < 0 || st.selectedCol < 0) return
        val cell = grid.cellAt(st.selectedRow, st.selectedCol) ?: return
        if (!cell.isActive) return

        val pos = Pair(st.selectedRow, st.selectedCol)
        val upperLetter = letter.uppercaseChar()

        val newUserLetters = st.userLetters.toMutableMap()
        newUserLetters[pos] = upperLetter

        if (st.checkMode == CheckMode.ASSISTED) {
            val isCorrect = cell.solutionLetter?.uppercaseChar() == upperLetter
            if (!isCorrect) {
                // En modo asistida: mostrar brevemente y luego eliminar
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

        // Avance automático
        val next = nextCellInWord(grid, st.selectedRow, st.selectedCol, st.activeDirection, st.activeEntryBankId)
        if (next != null) {
            val (nr, nc) = next
            val (entryId, cells) = computeActiveWord(grid, nr, nc, st.activeDirection)
            _state.value = _state.value.copy(
                selectedRow = nr,
                selectedCol = nc,
                activeEntryBankId = entryId,
                activeCellsInWord = cells,
            )
        }

        triggerAutosave()
    }

    /**
     * Borra la letra de la celda seleccionada, o retrocede a la celda anterior
     * si la celda actual está vacía.
     */
    fun onDeleteLetter() {
        val st = _state.value
        val grid = st.grid ?: return
        if (st.selectedRow < 0 || st.selectedCol < 0) return

        val pos = Pair(st.selectedRow, st.selectedCol)
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
                val newLetters = st.userLetters - prevPos
                val newIncorrect = st.incorrectCells - prevPos
                val (entryId, cells) = computeActiveWord(grid, pr, pc, st.activeDirection)
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
    // Modo de comprobación
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Cambia el modo de comprobación y limpia los estados de error previos.
     */
    fun onSetCheckMode(mode: CheckMode) {
        _state.value = _state.value.copy(
            checkMode = mode,
            incorrectCells = emptySet(),
            checkWordResult = null,
        )
        triggerAutosave()
    }

    /**
     * Comprueba la palabra activa (disponible en modo Clásica).
     *
     * No revela automáticamente la letra correcta.
     * El resultado se elimina automáticamente después de 3 segundos.
     */
    fun onCheckWord() {
        val st = _state.value
        val grid = st.grid ?: return
        val activeCells = st.activeCellsInWord
        if (activeCells.isEmpty()) return

        // Verificar si está completa
        val incomplete = activeCells.any { (r, c) -> !st.userLetters.containsKey(Pair(r, c)) }
        if (incomplete) {
            _state.value = st.copy(checkWordResult = CheckWordResult.Incomplete)
            scheduleResultClear(2000L)
            return
        }

        // Verificar si todas son correctas (internamente, sin exponer solución)
        val allCorrect = activeCells.all { (r, c) ->
            val cell = grid.cellAt(r, c) ?: return@all false
            val userLetter = st.userLetters[Pair(r, c)]
            userLetter != null && cell.solutionLetter?.uppercaseChar() == userLetter.uppercaseChar()
        }

        val result = if (allCorrect) CheckWordResult.Correct else CheckWordResult.HasErrors
        _state.value = st.copy(checkWordResult = result)
        scheduleResultClear(3000L)
    }

    /** Elimina el resultado de comprobar de forma manual. */
    fun clearCheckResult() {
        _state.value = _state.value.copy(checkWordResult = null)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Autoguardado público
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Guarda la sesión actual inmediatamente.
     * Llamar cuando el usuario sale de la pantalla o el sistema necesita persistir.
     */
    fun saveSessionNow() {
        triggerAutosave()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Devuelve el bankId y el conjunto de coordenadas de la palabra activa
     * para la dirección dada, a partir de una celda (row, col).
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
     * Devuelve la siguiente celda en la palabra activa, o null si se llegó al final.
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
     * Devuelve la celda anterior en la palabra activa, o null si se está al inicio.
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

    /** Elimina automáticamente el checkWordResult tras [delayMs] milisegundos. */
    private fun scheduleResultClear(delayMs: Long) {
        viewModelScope.launch {
            delay(delayMs)
            _state.value = _state.value.copy(checkWordResult = null)
        }
    }

    /** Guarda la sesión actual en [GameSessionManager] de forma asíncrona. */
    private fun triggerAutosave() {
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
                    isFinished = false,
                    lastUpdatedMs = System.currentTimeMillis(),
                )
                sessionManager.saveSession(sessionState)
            } catch (e: Exception) {
                // Autoguardado silencioso — no interrumpir la partida
            }
        }
    }
}
