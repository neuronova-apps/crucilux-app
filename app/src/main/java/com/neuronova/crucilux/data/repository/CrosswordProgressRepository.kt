package com.neuronova.crucilux.data.repository

import android.content.Context
import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.data.db.CrosswordBoardStatus
import com.neuronova.crucilux.data.db.CrosswordProgressDao
import com.neuronova.crucilux.data.db.CrosswordProgressEntity
import com.neuronova.crucilux.data.db.CruciluxDatabase
import com.neuronova.crucilux.model.CrosswordGrid
import com.neuronova.crucilux.model.CruciluxBoard
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.ui.game.CheckMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Modelo de progreso tipado para un tablero.
 */
data class CrosswordBoardProgress(
    val boardId: String,
    val category: String,
    val status: CrosswordBoardStatus = CrosswordBoardStatus.NOT_STARTED,
    val progressPercent: Int = 0,
    val userLetters: Map<Pair<Int, Int>, Char> = emptyMap(),
    val selectedRow: Int = 0,
    val selectedCol: Int = 0,
    val selectedDirection: CruciluxDirection = CruciluxDirection.HORIZONTAL,
    val checkMode: CheckMode = CheckMode.CLASSIC,
    val updatedAt: Long = 0L,
) {
    val isCompleted: Boolean get() = status == CrosswordBoardStatus.COMPLETED
    val isInProgress: Boolean get() = status == CrosswordBoardStatus.IN_PROGRESS
    val isNotStarted: Boolean get() = status == CrosswordBoardStatus.NOT_STARTED

    /**
     * Símbolo visual de representación:
     * - "○" para NOT_STARTED
     * - "XX %" para IN_PROGRESS
     * - "✓" para COMPLETED
     */
    val displaySymbol: String
        get() = when (status) {
            CrosswordBoardStatus.COMPLETED -> "✓"
            CrosswordBoardStatus.IN_PROGRESS -> "$progressPercent %"
            CrosswordBoardStatus.NOT_STARTED -> "○"
        }
}

/**
 * Estadísticas de progreso para una categoría temática (30 tableros).
 */
data class CategoryProgressStats(
    val category: String,
    val totalBoards: Int = 30,
    val completedBoards: Int = 0,
    val inProgressBoards: Int = 0,
) {
    val completedPercent: Int
        get() = if (totalBoards > 0) ((completedBoards * 100) / totalBoards).coerceIn(0, 100) else 0

    val isAllCompleted: Boolean
        get() = totalBoards > 0 && completedBoards >= totalBoards
}

/**
 * Estadísticas de progreso global de Crucilux (300 tableros).
 */
data class GlobalProgressStats(
    val totalBoards: Int = 300,
    val completedBoards: Int = 0,
    val inProgressBoards: Int = 0,
    val notStartedBoards: Int = 300,
) {
    val globalPercent: Int
        get() = if (totalBoards > 0) ((completedBoards * 100) / totalBoards).coerceIn(0, 100) else 0
}

/**
 * Repositorio centralizado de progreso multi-tablero respaldado por Room.
 */
class CrosswordProgressRepository(
    private val dao: CrosswordProgressDao,
    private val bankRepository: CruciluxBankRepository = CruciluxBankRepository.getInstance(),
) {

    // ──────────────────────────────────────────────────────────────────────────
    // Consultas y Observabilidad
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene el progreso síncrono/suspendido de un tablero por su ID.
     */
    suspend fun getProgress(boardId: String): CrosswordBoardProgress {
        val entity = dao.getProgress(boardId)
        val board = bankRepository.getBoardById(boardId)
        val category = board?.category ?: entity?.category ?: ""
        return if (entity != null) {
            entityToModel(entity)
        } else {
            CrosswordBoardProgress(
                boardId = boardId,
                category = category,
                status = CrosswordBoardStatus.NOT_STARTED,
                progressPercent = 0,
            )
        }
    }

    /**
     * Observa en tiempo real el progreso de un tablero específico.
     */
    fun observeProgress(boardId: String): Flow<CrosswordBoardProgress> {
        val board = bankRepository.getBoardById(boardId)
        val category = board?.category ?: ""
        return dao.observeProgress(boardId).map { entity ->
            if (entity != null) {
                entityToModel(entity)
            } else {
                CrosswordBoardProgress(
                    boardId = boardId,
                    category = category,
                    status = CrosswordBoardStatus.NOT_STARTED,
                    progressPercent = 0,
                )
            }
        }
    }

    /**
     * Observa el progreso de todos los tableros de una categoría indexados por boardId.
     */
    fun observeProgressForCategory(category: String): Flow<Map<String, CrosswordBoardProgress>> {
        return dao.observeProgressByCategory(category).map { list ->
            list.associate { it.boardId to entityToModel(it) }
        }
    }

    /**
     * Observa las estadísticas de una categoría específica (X / 30 completados).
     */
    fun observeCategoryStats(category: String): Flow<CategoryProgressStats> {
        return dao.observeProgressByCategory(category).map { list ->
            val completed = list.count { it.status == CrosswordBoardStatus.COMPLETED.name }
            val inProgress = list.count { it.status == CrosswordBoardStatus.IN_PROGRESS.name }
            CategoryProgressStats(
                category = category,
                totalBoards = 30,
                completedBoards = completed,
                inProgressBoards = inProgress,
            )
        }
    }

    /**
     * Observa las estadísticas globales sobre los 300 tableros.
     */
    fun observeGlobalStats(): Flow<GlobalProgressStats> {
        return dao.observeAllProgress().map { list ->
            val completed = list.count { it.status == CrosswordBoardStatus.COMPLETED.name }
            val inProgress = list.count { it.status == CrosswordBoardStatus.IN_PROGRESS.name }
            val notStarted = (300 - completed - inProgress).coerceAtLeast(0)
            GlobalProgressStats(
                totalBoards = 300,
                completedBoards = completed,
                inProgressBoards = inProgress,
                notStartedBoards = notStarted,
            )
        }
    }

    /**
     * Observa la partida IN_PROGRESS más reciente para la tarjeta "Continuar partida".
     */
    fun observeMostRecentInProgress(): Flow<CrosswordProgressEntity?> {
        return dao.observeMostRecentInProgress()
    }

    /**
     * Obtiene la partida IN_PROGRESS más reciente.
     */
    suspend fun getMostRecentInProgress(): CrosswordProgressEntity? {
        return dao.getMostRecentInProgress()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Guardado y Actualización
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Guarda o actualiza el progreso de un tablero en Room calculando el porcentaje
     * exacto sobre las casillas jugables del grid.
     */
    suspend fun saveProgress(
        boardId: String,
        category: String,
        userLetters: Map<Pair<Int, Int>, Char>,
        grid: CrosswordGrid?,
        selectedRow: Int = 0,
        selectedCol: Int = 0,
        selectedDirection: CruciluxDirection = CruciluxDirection.HORIZONTAL,
        checkMode: CheckMode = CheckMode.CLASSIC,
        isCompletedOverride: Boolean = false,
    ) {
        val existing = dao.getProgress(boardId)
        val alreadyCompleted = existing?.status == CrosswordBoardStatus.COMPLETED.name

        val (status, percent) = when {
            isCompletedOverride || alreadyCompleted -> {
                Pair(CrosswordBoardStatus.COMPLETED, 100)
            }
            grid != null -> {
                calculateProgress(grid, userLetters, isCompleted = false)
            }
            userLetters.isNotEmpty() -> {
                Pair(CrosswordBoardStatus.IN_PROGRESS, existing?.progressPercent ?: 1)
            }
            else -> {
                Pair(CrosswordBoardStatus.NOT_STARTED, 0)
            }
        }

        // Si ya estaba completado, nunca degradar a IN_PROGRESS
        val finalStatus = if (alreadyCompleted) CrosswordBoardStatus.COMPLETED else status
        val finalPercent = if (alreadyCompleted) 100 else percent

        val entity = CrosswordProgressEntity(
            boardId = boardId,
            category = category,
            status = finalStatus.name,
            progressPercent = finalPercent,
            userLetters = GameSessionManager.serializeLetters(userLetters),
            selectedRow = selectedRow,
            selectedCol = selectedCol,
            selectedDirection = if (selectedDirection == CruciluxDirection.VERTICAL) "V" else "H",
            checkMode = if (checkMode == CheckMode.ASSISTED) "ASSISTED" else "CLASSIC",
            updatedAt = System.currentTimeMillis(),
        )

        dao.insertOrUpdate(entity)
    }

    /**
     * Reinicia el progreso de un tablero volviéndolo a NOT_STARTED.
     */
    suspend fun resetBoardProgress(boardId: String, category: String) {
        val entity = CrosswordProgressEntity(
            boardId = boardId,
            category = category,
            status = CrosswordBoardStatus.NOT_STARTED.name,
            progressPercent = 0,
            userLetters = "",
            updatedAt = System.currentTimeMillis(),
        )
        dao.insertOrUpdate(entity)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Siguiente Tablero
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Busca el siguiente tablero no completado dentro de la misma categoría.
     *
     * Reglas:
     * 1. Primer tablero posterior en la categoría que NO esté COMPLETED.
     * 2. Si todos los posteriores están completados: busca desde el inicio (0..currentIndex).
     * 3. Si los 30 están completados: retorna null (categoría completada).
     */
    suspend fun getNextUncompletedBoard(category: String, currentBoardId: String): CruciluxBoard? {
        val boards = bankRepository.getBoardsByCategory(category)
        if (boards.isEmpty()) return null

        val progressList = dao.getProgressByCategory(category)
        val completedIds = progressList
            .filter { it.status == CrosswordBoardStatus.COMPLETED.name }
            .map { it.boardId }
            .toSet()

        val currentIndex = boards.indexOfFirst { it.id == currentBoardId }
        val effectiveIndex = if (currentIndex >= 0) currentIndex else 0

        // 1. Buscar en los posteriores
        for (i in (effectiveIndex + 1) until boards.size) {
            val candidate = boards[i]
            if (candidate.id !in completedIds) {
                return candidate
            }
        }

        // 2. Búsqueda cíclica desde el inicio
        for (i in 0..effectiveIndex) {
            val candidate = boards[i]
            if (candidate.id != currentBoardId && candidate.id !in completedIds) {
                return candidate
            }
        }

        // 3. Todos los tableros están completados
        return null
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Migración Legacy desde DataStore
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Migra la sesión legacy almacenada en DataStore a Room si existe y no ha sido migrada.
     */
    suspend fun migrateLegacySessionIfNeeded(sessionManager: GameSessionManager) {
        withContext(Dispatchers.IO) {
            try {
                val sessionState = sessionManager.sessionFlow.firstOrNull() ?: return@withContext
                if (sessionState.hasActiveSession && sessionState.boardId.isNotBlank()) {
                    val existing = dao.getProgress(sessionState.boardId)
                    if (existing == null) {
                        val board = bankRepository.getBoardById(sessionState.boardId)
                        val category = board?.category ?: sessionState.category
                        val status = if (sessionState.isFinished) {
                            CrosswordBoardStatus.COMPLETED
                        } else {
                            CrosswordBoardStatus.IN_PROGRESS
                        }

                        val entity = CrosswordProgressEntity(
                            boardId = sessionState.boardId,
                            category = category,
                            status = status.name,
                            progressPercent = if (sessionState.isFinished) 100 else 10,
                            userLetters = GameSessionManager.serializeLetters(sessionState.userLetters),
                            selectedRow = sessionState.selectedRow,
                            selectedCol = sessionState.selectedCol,
                            selectedDirection = sessionState.activeDirection,
                            checkMode = sessionState.checkMode,
                            updatedAt = sessionState.lastUpdatedMs.takeIf { it > 0 } ?: System.currentTimeMillis(),
                        )
                        dao.insertOrUpdate(entity)
                    }
                }
            } catch (e: Exception) {
                // Migración tolerante a fallos
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers de conversión y cálculo
    // ──────────────────────────────────────────────────────────────────────────

    private fun entityToModel(entity: CrosswordProgressEntity): CrosswordBoardProgress {
        return CrosswordBoardProgress(
            boardId = entity.boardId,
            category = entity.category,
            status = entity.boardStatus,
            progressPercent = entity.progressPercent,
            userLetters = entity.parseUserLetters(),
            selectedRow = entity.selectedRow,
            selectedCol = entity.selectedCol,
            selectedDirection = entity.direction,
            checkMode = entity.parsedCheckMode,
            updatedAt = entity.updatedAt,
        )
    }

    companion object {
        @Volatile
        private var instance: CrosswordProgressRepository? = null

        fun getInstance(context: Context): CrosswordProgressRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val db = CruciluxDatabase.getInstance(context)
                    CrosswordProgressRepository(
                        dao = db.progressDao(),
                        bankRepository = CruciluxBankRepository.getInstance(),
                    ).also { instance = it }
                }
            }
        }

        /**
         * Calcula el progreso porcentual exacto sobre casillas jugables:
         * casillas jugables correctamente rellenadas / total de casillas jugables.
         */
        fun calculateProgress(
            grid: CrosswordGrid,
            userLetters: Map<Pair<Int, Int>, Char>,
            isCompleted: Boolean,
        ): Pair<CrosswordBoardStatus, Int> {
            if (isCompleted) {
                return Pair(CrosswordBoardStatus.COMPLETED, 100)
            }
            if (userLetters.isEmpty()) {
                return Pair(CrosswordBoardStatus.NOT_STARTED, 0)
            }

            val playableCells = grid.cells.flatten().filter { it.isActive }
            val totalPlayable = playableCells.size
            if (totalPlayable == 0) return Pair(CrosswordBoardStatus.NOT_STARTED, 0)

            val correctPlayable = playableCells.count { cell ->
                val pos = Pair(cell.row, cell.col)
                val userChar = userLetters[pos]?.uppercaseChar()
                val solChar = cell.solutionLetter?.uppercaseChar()
                userChar != null && userChar == solChar
            }

            if (correctPlayable == totalPlayable && playableCells.isNotEmpty()) {
                return Pair(CrosswordBoardStatus.COMPLETED, 100)
            }

            val percent = ((correctPlayable * 100) / totalPlayable).coerceIn(0, 99)
            return Pair(CrosswordBoardStatus.IN_PROGRESS, percent)
        }
    }
}
