package com.neuronova.crucilux.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.data.GameSessionState
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.data.db.CrosswordBoardStatus
import com.neuronova.crucilux.data.repository.CrosswordProgressRepository
import com.neuronova.crucilux.data.repository.BoardCompletionResult
import com.neuronova.crucilux.engine.CruciluxGridEngine
import com.neuronova.crucilux.model.CrosswordClue
import com.neuronova.crucilux.model.CrosswordGrid
import com.neuronova.crucilux.model.CruciluxBoard
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.progression.XpCalculator
import com.neuronova.crucilux.progression.HintRules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    val hintsUsed: Int = 0,
    val hintRevealedCells: Set<Pair<Int, Int>> = emptySet(),
    val xpPossible: Int = 0,
    val bestXpEarned: Int = 0,
    val incorrectCells: Set<Pair<Int, Int>> = emptySet(),
    val validatedEntryBankIds: Set<String> = emptySet(),
    val validatedCells: Set<Pair<Int, Int>> = emptySet(),
    val isCompleted: Boolean = false,
    val isReviewMode: Boolean = false,
    val completionResult: BoardCompletionResult? = null,
    val isSessionSaved: Boolean = false,
    val progressPercent: Int = 0,
    val nextBoardId: String? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel de la pantalla de juego interactivo de Crucilux con Room Database.
 */
class CrosswordGameViewModel(
    private val progressRepository: CrosswordProgressRepository? = null,
    private val sessionManager: GameSessionManager? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(CrosswordGameState())
    val state: StateFlow<CrosswordGameState> = _state.asStateFlow()
    private var completionSubmitted = false
    private val saveMutex = Mutex()

    companion object {
        /**
         * Factory que inyecta [CrosswordProgressRepository] y opcionalmente [GameSessionManager].
         */
        fun factory(
            progressRepository: CrosswordProgressRepository,
            sessionManager: GameSessionManager? = null,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return CrosswordGameViewModel(progressRepository, sessionManager) as T
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Carga y restauración
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Carga el tablero por su ID y restaura la sesión guardada desde Room.
     */
    fun loadBoard(boardId: String) {
        val current = _state.value
        if (!current.isLoading && current.board?.id == boardId && current.grid != null) return

        viewModelScope.launch(Dispatchers.Default) {
            completionSubmitted = false
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

                // 1. Intentar restaurar desde Room Repository
                val savedProgress = if (progressRepository != null) {
                    try {
                        progressRepository.getProgress(boardId)
                    } catch (e: Exception) {
                        null
                    }
                } else null

                // 2. Si no hay Room, intentar DataStore legacy
                val legacySession = if (savedProgress == null && sessionManager != null) {
                    try {
                        sessionManager.sessionFlow.firstOrNull()
                    } catch (e: Exception) {
                        null
                    }
                } else null

                val hasSavedRoom = savedProgress != null && savedProgress.status != CrosswordBoardStatus.NOT_STARTED
                val hasSavedLegacy = legacySession != null && legacySession.boardId == boardId && legacySession.hasActiveSession

                if (hasSavedRoom) {
                    val userLetters = savedProgress!!.userLetters
                    val isAlreadyCompleted = savedProgress.status == CrosswordBoardStatus.COMPLETED

                    val (valBankIds, valCells) = if (isAlreadyCompleted) {
                        Pair(
                            board.entries.map { it.bankId }.toSet(),
                            grid.cells.flatten().filter { it.isActive }.map { Pair(it.row, it.col) }.toSet(),
                        )
                    } else {
                        computeValidatedState(grid, board, userLetters)
                    }

                    val isComp = isAlreadyCompleted || (valBankIds.size == board.entries.size && board.entries.isNotEmpty())
                    val savedRow = savedProgress.selectedRow
                    val savedCol = savedProgress.selectedCol
                    val savedDir = savedProgress.selectedDirection
                    val savedMode = savedProgress.checkMode

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
                        hintsUsed = savedProgress.hintsUsed,
                        hintRevealedCells = savedProgress.hintRevealedCells,
                        xpPossible = XpCalculator.finalXp(board.entries.size, savedMode, savedProgress.hintsUsed),
                        bestXpEarned = savedProgress.bestXpEarned,
                        validatedEntryBankIds = valBankIds,
                        validatedCells = valCells,
                        isCompleted = isComp,
                        isReviewMode = isAlreadyCompleted,
                        isSessionSaved = true,
                        progressPercent = savedProgress.progressPercent,
                    )

                    if (isComp) {
                        findNextBoard(board.category, board.id)
                    }
                } else if (hasSavedLegacy) {
                    val userLetters = legacySession!!.userLetters
                    val (valBankIds, valCells) = computeValidatedState(grid, board, userLetters)
                    val isComp = legacySession.isFinished || (valBankIds.size == board.entries.size && board.entries.isNotEmpty())
                    val savedRow = legacySession.selectedRow
                    val savedCol = legacySession.selectedCol
                    val savedDir = if (legacySession.activeDirection == "V") CruciluxDirection.VERTICAL else CruciluxDirection.HORIZONTAL
                    val savedMode = if (legacySession.checkMode == "ASSISTED") CheckMode.ASSISTED else CheckMode.CLASSIC

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
                        xpPossible = XpCalculator.finalXp(board.entries.size, savedMode, 0),
                        validatedEntryBankIds = valBankIds,
                        validatedCells = valCells,
                        isCompleted = isComp,
                        isSessionSaved = true,
                    )

                    if (isComp) {
                        findNextBoard(board.category, board.id)
                    }
                } else {
                    // Estado inicial limpio
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
                        progressPercent = 0,
                        xpPossible = XpCalculator.finalXp(board.entries.size, CheckMode.CLASSIC, 0),
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

        val (newEntryBankId, activeCells) = computeActiveWord(grid, row, col, newDirection)

        _state.value = st.copy(
            selectedRow = row,
            selectedCol = col,
            activeDirection = newDirection,
            activeEntryBankId = newEntryBankId,
            activeCellsInWord = activeCells,
            incorrectCells = emptySet(),
        )

        triggerAutosave()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Navegación entre pistas (Flechas ‹ y ›)
    // ──────────────────────────────────────────────────────────────────────────

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

    fun selectClue(clue: CrosswordClue) {
        val st = _state.value
        val grid = st.grid ?: return

        val (entryId, cells) = computeActiveWord(grid, clue.startRow, clue.startCol, clue.direction)
        val targetCell = findFirstUnvalidatedCellInWord(
            cells,
            st.validatedCells + st.hintRevealedCells,
        ) ?: Pair(clue.startRow, clue.startCol)

        _state.value = st.copy(
            selectedRow = targetCell.first,
            selectedCol = targetCell.second,
            activeDirection = clue.direction,
            activeEntryBankId = entryId,
            activeCellsInWord = cells,
            incorrectCells = emptySet(),
        )

        triggerAutosave()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Entrada y borrado de letras
    // ──────────────────────────────────────────────────────────────────────────

    fun onLetterEntered(rawChar: Char) {
        val st = _state.value
        val grid = st.grid ?: return
        val board = st.board ?: return
        if (st.selectedRow < 0 || st.selectedCol < 0) return

        val currentPos = Pair(st.selectedRow, st.selectedCol)
        if (HintRules.isProtected(
                isValidated = currentPos in st.validatedCells,
                isHintRevealed = currentPos in st.hintRevealedCells,
            )
        ) {
            advanceCursorToNextEditableCell()
            return
        }

        val cell = grid.cellAt(st.selectedRow, st.selectedCol) ?: return
        if (!cell.isActive) return

        val normalized = rawChar.uppercaseChar()

        if (st.checkMode == CheckMode.ASSISTED) {
            val expectedChar = cell.solutionLetter?.uppercaseChar()
            if (expectedChar != null && normalized != expectedChar) {
                _state.value = st.copy(
                    incorrectCells = setOf(currentPos),
                )
                viewModelScope.launch {
                    delay(700)
                    _state.value = _state.value.copy(
                        incorrectCells = _state.value.incorrectCells - currentPos,
                    )
                }
                return
            }
        }

        val newLetters = st.userLetters + (currentPos to normalized)
        val (newValidatedBankIds, newValidatedCells) = computeValidatedState(grid, board, newLetters)
        val isAllCompleted = newValidatedBankIds.size == board.entries.size && board.entries.isNotEmpty()

        val nextPos = findNextEditableCell(
            current = currentPos,
            cellsInWord = st.activeCellsInWord,
            validatedCells = newValidatedCells + st.hintRevealedCells,
        )

        val newRow = nextPos?.first ?: st.selectedRow
        val newCol = nextPos?.second ?: st.selectedCol

        _state.value = st.copy(
            userLetters = newLetters,
            selectedRow = newRow,
            selectedCol = newCol,
            validatedEntryBankIds = newValidatedBankIds,
            validatedCells = newValidatedCells,
            isCompleted = isAllCompleted,
            incorrectCells = emptySet(),
        )

        if (isAllCompleted) {
            findNextBoard(board.category, board.id)
        }

        triggerAutosave()
    }

    fun onDeleteLetter() {
        val st = _state.value
        if (st.selectedRow < 0 || st.selectedCol < 0) return

        val currentPos = Pair(st.selectedRow, st.selectedCol)

        val protectedCells = st.validatedCells + st.hintRevealedCells

        if (currentPos in protectedCells) {
            val prevPos = findPreviousEditableCell(
                current = currentPos,
                cellsInWord = st.activeCellsInWord,
                validatedCells = protectedCells,
            )
            if (prevPos != null) {
                _state.value = st.copy(
                    selectedRow = prevPos.first,
                    selectedCol = prevPos.second,
                    incorrectCells = emptySet(),
                )
                triggerAutosave()
            }
            return
        }

        val hasLetter = st.userLetters.containsKey(currentPos)

        if (hasLetter) {
            val newLetters = st.userLetters - currentPos
            val board = st.board
            val grid = st.grid
            val (newValidatedBankIds, newValidatedCells) = if (grid != null && board != null) {
                computeValidatedState(grid, board, newLetters)
            } else {
                Pair(st.validatedEntryBankIds, st.validatedCells)
            }

            _state.value = st.copy(
                userLetters = newLetters,
                validatedEntryBankIds = newValidatedBankIds,
                validatedCells = newValidatedCells,
                incorrectCells = emptySet(),
            )
            triggerAutosave()
        } else {
            val prevPos = findPreviousEditableCell(
                current = currentPos,
                cellsInWord = st.activeCellsInWord,
                validatedCells = protectedCells,
            )
            if (prevPos != null) {
                val newLetters = if (prevPos !in protectedCells) {
                    st.userLetters - prevPos
                } else {
                    st.userLetters
                }

                _state.value = st.copy(
                    selectedRow = prevPos.first,
                    selectedCol = prevPos.second,
                    userLetters = newLetters,
                    incorrectCells = emptySet(),
                )
                triggerAutosave()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Sistema de pistas y reinicio confirmado
    // ──────────────────────────────────────────────────────────────────────────

    fun canUseHint(): Boolean {
        val st = _state.value
        val grid = st.grid ?: return false
        val pos = Pair(st.selectedRow, st.selectedCol)
        val cell = grid.cellAt(pos.first, pos.second) ?: return false
        val expected = cell.solutionLetter?.uppercaseChar() ?: return false
        val current = st.userLetters[pos]?.uppercaseChar()
        return HintRules.canReveal(
            isPlayable = cell.isActive,
            currentLetter = current,
            expectedLetter = expected,
            isValidated = pos in st.validatedCells,
            isAlreadyRevealed = pos in st.hintRevealedCells,
        )
    }

    /** Aplica exactamente una pista válida. La UI confirma previamente desde la cuarta. */
    fun useHint(): Boolean {
        if (!canUseHint()) return false
        val st = _state.value
        val grid = st.grid ?: return false
        val board = st.board ?: return false
        val pos = Pair(st.selectedRow, st.selectedCol)
        val expected = grid.cellAt(pos.first, pos.second)?.solutionLetter?.uppercaseChar() ?: return false
        val newLetters = st.userLetters + (pos to expected)
        val newHinted = st.hintRevealedCells + pos
        val newHintsUsed = st.hintsUsed + 1
        val (validatedIds, validatedCells) = computeValidatedState(grid, board, newLetters)
        val completed = validatedIds.size == board.entries.size && board.entries.isNotEmpty()
        val nextPos = findNextEditableCell(
            current = pos,
            cellsInWord = st.activeCellsInWord,
            validatedCells = validatedCells + newHinted,
        )

        _state.value = st.copy(
            userLetters = newLetters,
            selectedRow = nextPos?.first ?: st.selectedRow,
            selectedCol = nextPos?.second ?: st.selectedCol,
            hintsUsed = newHintsUsed,
            hintRevealedCells = newHinted,
            xpPossible = XpCalculator.finalXp(board.entries.size, st.checkMode, newHintsUsed),
            validatedEntryBankIds = validatedIds,
            validatedCells = validatedCells,
            incorrectCells = emptySet(),
            isCompleted = completed,
            isReviewMode = false,
        )

        if (completed) findNextBoard(board.category, board.id)
        triggerAutosave()
        return true
    }

    fun resetBoardAfterConfirmation(onReset: () -> Unit = {}) {
        val board = _state.value.board ?: return

        viewModelScope.launch(Dispatchers.IO) {
            progressRepository?.resetBoardProgress(board.id, board.category)
            withContext(Dispatchers.Main) { onReset() }
        }
    }

    fun saveSessionNow() {
        triggerAutosave()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Siguiente Tablero
    // ──────────────────────────────────────────────────────────────────────────

    private fun findNextBoard(category: String, currentBoardId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val next = progressRepository?.getNextUncompletedBoard(category, currentBoardId)
            _state.value = _state.value.copy(nextBoardId = next?.id)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers de cálculo y validación
    // ──────────────────────────────────────────────────────────────────────────

    fun getAllCluesOrdered(grid: CrosswordGrid): List<CrosswordClue> {
        val all = grid.horizontalClues + grid.verticalClues
        return all.sortedWith(
            compareBy<CrosswordClue> { it.number }
                .thenBy { if (it.direction == CruciluxDirection.HORIZONTAL) 0 else 1 }
        )
    }

    private fun computeActiveWord(
        grid: CrosswordGrid,
        row: Int,
        col: Int,
        direction: CruciluxDirection,
    ): Pair<String?, Set<Pair<Int, Int>>> {
        val cell = grid.cellAt(row, col) ?: return Pair(null, emptySet())
        val bankId = if (direction == CruciluxDirection.HORIZONTAL) {
            cell.horizontalEntryBankId ?: cell.verticalEntryBankId
        } else {
            cell.verticalEntryBankId ?: cell.horizontalEntryBankId
        }

        if (bankId == null) return Pair(null, emptySet())

        val cells = grid.cells.flatten()
            .filter { it.horizontalEntryBankId == bankId || it.verticalEntryBankId == bankId }
            .map { Pair(it.row, it.col) }
            .toSet()

        return Pair(bankId, cells)
    }

    private fun computeValidatedState(
        grid: CrosswordGrid,
        board: CruciluxBoard,
        userLetters: Map<Pair<Int, Int>, Char>,
    ): Pair<Set<String>, Set<Pair<Int, Int>>> {
        val validatedBankIds = mutableSetOf<String>()
        val validatedCells = mutableSetOf<Pair<Int, Int>>()

        for (entry in board.entries) {
            val entryCells = mutableListOf<Pair<Int, Int>>()
            for (i in 0 until entry.length) {
                val r = if (entry.direction == CruciluxDirection.VERTICAL) entry.row + i else entry.row
                val c = if (entry.direction == CruciluxDirection.HORIZONTAL) entry.col + i else entry.col
                entryCells.add(Pair(r, c))
            }

            val isCompleteAndCorrect = entryCells.all { pos ->
                val enteredChar = userLetters[pos]?.uppercaseChar()
                val solChar = grid.cellAt(pos.first, pos.second)?.solutionLetter?.uppercaseChar()
                enteredChar != null && solChar != null && enteredChar == solChar
            }

            if (isCompleteAndCorrect) {
                validatedBankIds.add(entry.bankId)
                validatedCells.addAll(entryCells)
            }
        }

        return Pair(validatedBankIds, validatedCells)
    }

    private fun advanceCursorToNextEditableCell() {
        val st = _state.value
        val currentPos = Pair(st.selectedRow, st.selectedCol)
        val next = findNextEditableCell(
            currentPos,
            st.activeCellsInWord,
            st.validatedCells + st.hintRevealedCells,
        )
        if (next != null) {
            _state.value = st.copy(
                selectedRow = next.first,
                selectedCol = next.second,
            )
        }
    }

    private fun findNextEditableCell(
        current: Pair<Int, Int>,
        cellsInWord: Set<Pair<Int, Int>>,
        validatedCells: Set<Pair<Int, Int>>,
    ): Pair<Int, Int>? {
        val sorted = sortWordCells(cellsInWord, _state.value.activeDirection)
        val currentIndex = sorted.indexOf(current)
        if (currentIndex < 0) return null

        for (i in (currentIndex + 1) until sorted.size) {
            val candidate = sorted[i]
            if (candidate !in validatedCells) return candidate
        }
        return null
    }

    private fun findPreviousEditableCell(
        current: Pair<Int, Int>,
        cellsInWord: Set<Pair<Int, Int>>,
        validatedCells: Set<Pair<Int, Int>>,
    ): Pair<Int, Int>? {
        val sorted = sortWordCells(cellsInWord, _state.value.activeDirection)
        val currentIndex = sorted.indexOf(current)
        if (currentIndex <= 0) return null

        for (i in (currentIndex - 1) downTo 0) {
            val candidate = sorted[i]
            if (candidate !in validatedCells) return candidate
        }
        return null
    }

    private fun findFirstUnvalidatedCellInWord(
        cellsInWord: Set<Pair<Int, Int>>,
        validatedCells: Set<Pair<Int, Int>>,
    ): Pair<Int, Int>? {
        val sorted = sortWordCells(cellsInWord, _state.value.activeDirection)
        return sorted.firstOrNull { it !in validatedCells }
    }

    private fun sortWordCells(
        cells: Set<Pair<Int, Int>>,
        direction: CruciluxDirection,
    ): List<Pair<Int, Int>> {
        return if (direction == CruciluxDirection.HORIZONTAL) {
            cells.sortedBy { it.second }
        } else {
            cells.sortedBy { it.first }
        }
    }

    private fun triggerAutosave() {
        val st = _state.value
        val board = st.board ?: return
        val shouldAwardCompletion = st.isCompleted && !st.isReviewMode && !completionSubmitted
        if (shouldAwardCompletion) completionSubmitted = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                saveMutex.withLock {
                    val latest = _state.value
                    val latestBoard = latest.board ?: return@withLock
                    if (latestBoard.id != board.id) return@withLock

                    val completionResult = progressRepository?.saveProgress(
                        boardId = latestBoard.id,
                        category = latestBoard.category,
                        userLetters = latest.userLetters,
                        grid = latest.grid,
                        selectedRow = latest.selectedRow,
                        selectedCol = latest.selectedCol,
                        selectedDirection = latest.activeDirection,
                        checkMode = latest.checkMode,
                        hintsUsed = latest.hintsUsed,
                        hintRevealedCells = latest.hintRevealedCells,
                        isCompletedOverride = latest.isCompleted,
                        awardCompletion = shouldAwardCompletion,
                        xpFinal = latest.xpPossible,
                    )

                    if (completionResult != null) {
                        _state.value = _state.value.copy(
                            completionResult = completionResult,
                            bestXpEarned = completionResult.bestXpEarned,
                        )
                    }

                    sessionManager?.saveSession(
                        GameSessionState(
                            boardId = latestBoard.id,
                            category = latestBoard.category,
                            boardSize = latestBoard.size,
                            selectedRow = latest.selectedRow,
                            selectedCol = latest.selectedCol,
                            activeDirection = if (latest.activeDirection == CruciluxDirection.VERTICAL) "V" else "H",
                            checkMode = if (latest.checkMode == CheckMode.ASSISTED) "ASSISTED" else "CLASSIC",
                            userLetters = latest.userLetters,
                            isFinished = latest.isCompleted,
                            lastUpdatedMs = System.currentTimeMillis(),
                        )
                    )
                }
            } catch (e: Exception) {
                if (shouldAwardCompletion) completionSubmitted = false
                // Autoguardado silencioso
            }
        }
    }
}
