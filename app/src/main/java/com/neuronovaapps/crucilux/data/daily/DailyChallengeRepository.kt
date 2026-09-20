package com.neuronovaapps.crucilux.data.daily

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.neuronovaapps.crucilux.achievements.AchievementRepository
import com.neuronovaapps.crucilux.data.GameSessionManager
import com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
import com.neuronovaapps.crucilux.data.db.CrosswordBoardStatus
import com.neuronovaapps.crucilux.data.db.CrosswordProgressDao
import com.neuronovaapps.crucilux.data.db.CrosswordProgressEntity
import com.neuronovaapps.crucilux.data.db.CruciluxDatabase
import com.neuronovaapps.crucilux.data.db.DailyChallengeDao
import com.neuronovaapps.crucilux.data.db.DailyChallengeEntity
import com.neuronovaapps.crucilux.data.db.DailyChallengeStatus
import com.neuronovaapps.crucilux.model.CrosswordGrid
import com.neuronovaapps.crucilux.model.CruciluxBoard
import com.neuronovaapps.crucilux.model.CruciluxDirection
import com.neuronovaapps.crucilux.ui.game.CheckMode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Estado completo del Desafío Diario para consumo de la UI y ViewModel.
 */
data class DailyChallengeInfo(
    val dateKey: String,
    val boardId: String,
    val board: CruciluxBoard? = null,
    val status: DailyChallengeStatus = DailyChallengeStatus.NOT_STARTED,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val progressPercent: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val userLetters: Map<Pair<Int, Int>, Char> = emptyMap(),
    val selectedRow: Int = 0,
    val selectedCol: Int = 0,
    val selectedDirection: CruciluxDirection = CruciluxDirection.HORIZONTAL,
    val checkMode: CheckMode = CheckMode.CLASSIC,
    val hintsUsed: Int = 0,
    val hintRevealedCells: Set<Pair<Int, Int>> = emptySet(),
    val isRewardClaimed: Boolean = false,
    val bestTimeSeconds: Long? = null,
    val elapsedTimeSeconds: Long = 0L,
) {
    val isCompleted: Boolean get() = status == DailyChallengeStatus.COMPLETED
    val isInProgress: Boolean get() = status == DailyChallengeStatus.IN_PROGRESS
    val isNotStarted: Boolean get() = status == DailyChallengeStatus.NOT_STARTED
    val category: String get() = board?.category.orEmpty()
    val dimensionLabel: String get() = board?.dimensionLabel ?: "7x7"
}

/**
 * Repositorio centralizado para la lógica y persistencia del Desafío Diario.
 */
class DailyChallengeRepository(
    private val dailyChallengeDao: DailyChallengeDao,
    private val progressDao: CrosswordProgressDao,
    private val bankRepository: CruciluxBankRepository = CruciluxBankRepository.getInstance(),
    private val selector: DailyChallengeSelector = DailyChallengeSelector(bankRepository),
    private val dateProvider: DateProvider = DefaultDateProvider(),
    private val database: CruciluxDatabase? = null,
    private val achievementRepository: AchievementRepository? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    // ──────────────────────────────────────────────────────────────────────────
    // Observabilidad
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Observa en tiempo real el desafío del día correspondiente a la fecha local actual.
     */
    fun observeTodayChallenge(): Flow<DailyChallengeInfo> {
        val todayKey = dateProvider.todayKey()
        val todayDate = dateProvider.today()

        return combine(
            dailyChallengeDao.observeChallenge(todayKey),
            dailyChallengeDao.observeAllCompleted(),
            bankRepository.loadStatus,
        ) { entity, completedList, _ ->
            val completedDates = completedList.mapNotNull {
                try { LocalDate.parse(it.dateKey) } catch (_: Exception) { null }
            }.toSet()

            val currentStreak = calculateCurrentStreak(completedDates, todayDate)
            val bestStreak = calculateBestStreak(completedDates)

            val board = selector.selectBoardForDate(todayDate)
            val effectiveBoardId = entity?.boardId ?: board?.id ?: ""

            if (entity != null) {
                entityToInfo(
                    entity = entity,
                    board = board ?: bankRepository.getBoardById(effectiveBoardId),
                    currentStreak = currentStreak,
                    bestStreak = bestStreak,
                )
            } else {
                DailyChallengeInfo(
                    dateKey = todayKey,
                    boardId = effectiveBoardId,
                    board = board,
                    status = DailyChallengeStatus.NOT_STARTED,
                    progressPercent = 0,
                    currentStreak = currentStreak,
                    bestStreak = bestStreak,
                )
            }
        }.distinctUntilChanged()
    }

    /**
     * Observa la racha actual de días consecutivos completados.
     */
    fun observeCurrentStreak(): Flow<Int> {
        return dailyChallengeDao.observeAllCompleted().map { completedList ->
            val completedDates = completedList.mapNotNull {
                try { LocalDate.parse(it.dateKey) } catch (_: Exception) { null }
            }.toSet()
            calculateCurrentStreak(completedDates, dateProvider.today())
        }.distinctUntilChanged()
    }

    /**
     * Observa la mejor racha histórica alcanzada.
     */
    fun observeBestStreak(): Flow<Int> {
        return dailyChallengeDao.observeAllCompleted().map { completedList ->
            val completedDates = completedList.mapNotNull {
                try { LocalDate.parse(it.dateKey) } catch (_: Exception) { null }
            }.toSet()
            calculateBestStreak(completedDates)
        }.distinctUntilChanged()
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Consultas y Acciones Síncronas / Suspendidas
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Obtiene el estado actual del desafío diario de hoy.
     */
    suspend fun getTodayChallenge(): DailyChallengeInfo = withContext(ioDispatcher) {
        val todayKey = dateProvider.todayKey()
        val todayDate = dateProvider.today()
        val entity = dailyChallengeDao.getChallenge(todayKey)
        val completedList = dailyChallengeDao.getAllCompleted()
        val completedDates = completedList.mapNotNull {
            try { LocalDate.parse(it.dateKey) } catch (_: Exception) { null }
        }.toSet()

        val currentStreak = calculateCurrentStreak(completedDates, todayDate)
        val bestStreak = calculateBestStreak(completedDates)

        val board = selector.selectBoardForDate(todayDate)
        val effectiveBoardId = entity?.boardId ?: board?.id ?: ""

        if (entity != null) {
            entityToInfo(
                entity = entity,
                board = board ?: bankRepository.getBoardById(effectiveBoardId),
                currentStreak = currentStreak,
                bestStreak = bestStreak,
            )
        } else {
            DailyChallengeInfo(
                dateKey = todayKey,
                boardId = effectiveBoardId,
                board = board,
                status = DailyChallengeStatus.NOT_STARTED,
                progressPercent = 0,
                currentStreak = currentStreak,
                bestStreak = bestStreak,
            )
        }
    }

    /**
     * Obtiene el estado de un desafío diario para una fecha específica.
     */
    suspend fun getChallenge(dateKey: String): DailyChallengeInfo? = withContext(ioDispatcher) {
        val entity = dailyChallengeDao.getChallenge(dateKey) ?: return@withContext null
        val completedList = dailyChallengeDao.getAllCompleted()
        val completedDates = completedList.mapNotNull {
            try { LocalDate.parse(it.dateKey) } catch (_: Exception) { null }
        }.toSet()
        val date = try { LocalDate.parse(dateKey) } catch (_: Exception) { dateProvider.today() }
        val currentStreak = calculateCurrentStreak(completedDates, date)
        val bestStreak = calculateBestStreak(completedDates)
        val board = bankRepository.getBoardById(entity.boardId) ?: selector.selectBoardForDate(date)
        entityToInfo(entity, board, currentStreak, bestStreak)
    }

    /**
     * Alias de conveniencia para getChallenge.
     */
    suspend fun getDailySession(dateKey: String): DailyChallengeInfo? = getChallenge(dateKey)

    /**
     * Alias de conveniencia para guardar sesión interactiva.
     */
    suspend fun saveDailySession(
        dateKey: String,
        boardId: String,
        userLetters: Map<Pair<Int, Int>, Char>,
        grid: CrosswordGrid? = null,
        selectedRow: Int = 0,
        selectedCol: Int = 0,
        selectedDirection: CruciluxDirection = CruciluxDirection.HORIZONTAL,
        checkMode: CheckMode = CheckMode.CLASSIC,
        hintsUsed: Int = 0,
        hintRevealedCells: Set<Pair<Int, Int>> = emptySet(),
        isCompleted: Boolean = false,
        elapsedTimeSeconds: Long = 0L,
    ): DailyChallengeInfo = saveDailyProgress(
        dateKey = dateKey,
        userLetters = userLetters,
        grid = grid,
        selectedRow = selectedRow,
        selectedCol = selectedCol,
        selectedDirection = selectedDirection,
        checkMode = checkMode,
        hintsUsed = hintsUsed,
        hintRevealedCells = hintRevealedCells,
        isCompletedOverride = isCompleted,
        elapsedTimeSeconds = elapsedTimeSeconds,
    )

    /**
     * Inicia el desafío diario de hoy si aún no ha sido iniciado.
     * Es idempotente y no altera un desafío ya completado.
     */
    suspend fun startTodayChallenge(): DailyChallengeInfo = withContext(ioDispatcher) {
        val todayKey = dateProvider.todayKey()
        val todayDate = dateProvider.today()
        val existing = dailyChallengeDao.getChallenge(todayKey)

        if (existing == null) {
            val board = selector.selectBoardForDate(todayDate)
            val boardId = board?.id ?: ""
            val newEntity = DailyChallengeEntity(
                dateKey = todayKey,
                boardId = boardId,
                status = DailyChallengeStatus.IN_PROGRESS.name,
                startedAt = System.currentTimeMillis(),
                attemptCount = 1,
            )
            upsertEntity(newEntity)
        } else if (existing.challengeStatus == DailyChallengeStatus.NOT_STARTED) {
            val updated = existing.copy(
                status = DailyChallengeStatus.IN_PROGRESS.name,
                startedAt = existing.startedAt ?: System.currentTimeMillis(),
                attemptCount = existing.attemptCount + 1,
            )
            upsertEntity(updated)
        }

        getTodayChallenge()
    }

    /**
     * Guarda el progreso de la sesión interactiva del desafío diario.
     */
    suspend fun saveDailyProgress(
        dateKey: String,
        userLetters: Map<Pair<Int, Int>, Char>,
        grid: CrosswordGrid?,
        selectedRow: Int = 0,
        selectedCol: Int = 0,
        selectedDirection: CruciluxDirection = CruciluxDirection.HORIZONTAL,
        checkMode: CheckMode = CheckMode.CLASSIC,
        hintsUsed: Int = 0,
        hintRevealedCells: Set<Pair<Int, Int>> = emptySet(),
        isCompletedOverride: Boolean = false,
        elapsedTimeSeconds: Long = 0L,
    ): DailyChallengeInfo = withContext(ioDispatcher) {
        val existing = dailyChallengeDao.getChallenge(dateKey)
        val board = bankRepository.getBoardById(existing?.boardId.orEmpty())
            ?: selector.selectBoardForDate(dateKey)
        val boardId = board?.id ?: existing?.boardId ?: ""

        val isAlreadyCompleted = existing?.isCompleted == true
        val (calcStatus, calcPercent) = if (grid != null) {
            calculateProgress(grid, userLetters, isCompletedOverride || isAlreadyCompleted)
        } else {
            Pair(
                if (isCompletedOverride || isAlreadyCompleted) DailyChallengeStatus.COMPLETED else DailyChallengeStatus.IN_PROGRESS,
                if (isCompletedOverride || isAlreadyCompleted) 100 else 0,
            )
        }

        val finalStatus = if (isAlreadyCompleted || isCompletedOverride || calcStatus == DailyChallengeStatus.COMPLETED) {
            DailyChallengeStatus.COMPLETED
        } else {
            DailyChallengeStatus.IN_PROGRESS
        }

        val finalPercent = if (finalStatus == DailyChallengeStatus.COMPLETED) 100 else calcPercent

        val completedAt = if (finalStatus == DailyChallengeStatus.COMPLETED) {
            existing?.completedAt ?: System.currentTimeMillis()
        } else {
            null
        }

        val currentElapsed = if (elapsedTimeSeconds > 0L) {
            elapsedTimeSeconds
        } else {
            existing?.elapsedTimeSeconds ?: 0L
        }

        val finalBestTime = if (finalStatus == DailyChallengeStatus.COMPLETED) {
            val candidate = if (currentElapsed > 0L) currentElapsed else existing?.bestTimeSeconds
            when {
                existing?.bestTimeSeconds != null && candidate != null -> minOf(existing.bestTimeSeconds, candidate)
                candidate != null -> candidate
                else -> null
            }
        } else {
            existing?.bestTimeSeconds
        }

        val updatedEntity = DailyChallengeEntity(
            dateKey = dateKey,
            boardId = boardId,
            status = finalStatus.name,
            startedAt = existing?.startedAt ?: System.currentTimeMillis(),
            completedAt = completedAt,
            bestTimeSeconds = finalBestTime,
            elapsedTimeSeconds = currentElapsed,
            attemptCount = existing?.attemptCount?.coerceAtLeast(1) ?: 1,
            isRewardClaimed = existing?.isRewardClaimed ?: false,
            userLetters = GameSessionManager.serializeLetters(userLetters),
            progressPercent = finalPercent,
            hintsUsed = hintsUsed.coerceAtLeast(existing?.hintsUsed ?: 0),
            hintRevealedCells = CrosswordProgressEntity.serializePositions(
                hintRevealedCells.ifEmpty { existing?.parseHintRevealedCells().orEmpty() }
            ),
            checkMode = if (checkMode == CheckMode.ASSISTED) "ASSISTED" else "CLASSIC",
            selectedRow = selectedRow,
            selectedCol = selectedCol,
            selectedDirection = if (selectedDirection == CruciluxDirection.VERTICAL) "V" else "H",
        )

        upsertEntity(updatedEntity)

        // Sincronizar con el progreso histórico normal del tablero sin duplicar ni degradar
        if (finalStatus == DailyChallengeStatus.COMPLETED && board != null) {
            syncNormalBoardCompletion(boardId, board.category, userLetters)
        }

        getTodayChallenge()
    }

    /**
     * Marca el desafío del día de hoy como completado de manera explícita e idempotente.
     */
    suspend fun completeTodayChallenge(): DailyChallengeInfo = withContext(ioDispatcher) {
        val todayKey = dateProvider.todayKey()
        val existing = dailyChallengeDao.getChallenge(todayKey)
        val board = selector.selectBoardForDate(dateProvider.today())
        val boardId = board?.id ?: existing?.boardId ?: ""

        if (existing == null) {
            val newEntity = DailyChallengeEntity(
                dateKey = todayKey,
                boardId = boardId,
                status = DailyChallengeStatus.COMPLETED.name,
                startedAt = System.currentTimeMillis(),
                completedAt = System.currentTimeMillis(),
                progressPercent = 100,
                attemptCount = 1,
            )
            upsertEntity(newEntity)
        } else if (existing.challengeStatus != DailyChallengeStatus.COMPLETED) {
            val currentElapsed = existing.elapsedTimeSeconds
            val best = if (currentElapsed > 0L) {
                if (existing.bestTimeSeconds != null) minOf(existing.bestTimeSeconds, currentElapsed) else currentElapsed
            } else existing.bestTimeSeconds
            val updated = existing.copy(
                status = DailyChallengeStatus.COMPLETED.name,
                completedAt = System.currentTimeMillis(),
                progressPercent = 100,
                bestTimeSeconds = best,
            )
            upsertEntity(updated)
        }

        if (board != null) {
            syncNormalBoardCompletion(boardId, board.category, emptyMap())
        }

        getTodayChallenge()
    }

    /**
     * Registra la finalización de un desafío diario para una fecha y tablero específicos.
     */
    suspend fun onBoardCompleted(dateKey: String, boardId: String) = withContext(ioDispatcher) {
        val existing = dailyChallengeDao.getChallenge(dateKey)
        if (existing == null) {
            val newEntity = DailyChallengeEntity(
                dateKey = dateKey,
                boardId = boardId,
                status = DailyChallengeStatus.COMPLETED.name,
                startedAt = System.currentTimeMillis(),
                completedAt = System.currentTimeMillis(),
                progressPercent = 100,
                attemptCount = 1,
            )
            upsertEntity(newEntity)
        } else if (existing.challengeStatus != DailyChallengeStatus.COMPLETED) {
            val updated = existing.copy(
                status = DailyChallengeStatus.COMPLETED.name,
                completedAt = System.currentTimeMillis(),
                progressPercent = 100,
            )
            upsertEntity(updated)
        }
    }

    /**
     * Callback invocado cuando un tablero normal es completado en [CrosswordProgressRepository].
     * Si coincide con el tablero del desafío diario de hoy, marca el desafío diario como COMPLETED.
     */
    suspend fun onBoardCompleted(boardId: String) = withContext(ioDispatcher) {
        val todayKey = dateProvider.todayKey()
        val todayBoard = selector.selectBoardForDate(dateProvider.today())
        if (todayBoard != null && todayBoard.id == boardId) {
            onBoardCompleted(todayKey, boardId)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers de Cálculo de Rachas e Integración
    // ──────────────────────────────────────────────────────────────────────────

    private suspend fun syncNormalBoardCompletion(
        boardId: String,
        category: String,
        userLetters: Map<Pair<Int, Int>, Char>,
    ) {
        try {
            val normal = progressDao.getProgress(boardId)
            if (normal == null || normal.status != CrosswordBoardStatus.COMPLETED.name) {
                val entity = CrosswordProgressEntity(
                    boardId = boardId,
                    category = category,
                    status = CrosswordBoardStatus.COMPLETED.name,
                    progressPercent = 100,
                    userLetters = if (userLetters.isNotEmpty()) GameSessionManager.serializeLetters(userLetters) else normal?.userLetters.orEmpty(),
                    updatedAt = System.currentTimeMillis(),
                )
                progressDao.insertOrUpdate(entity)
            }
            achievementRepository?.evaluateAndSync()
        } catch (e: Exception) {
            Log.w(TAG, "Error sincronizando progreso normal para $boardId", e)
        }
    }

    private suspend fun upsertEntity(entity: DailyChallengeEntity) {
        inTransaction {
            val rowId = dailyChallengeDao.insertIgnore(entity)
            if (rowId == -1L) {
                dailyChallengeDao.update(entity)
            }
        }
    }

    private suspend fun <T> inTransaction(block: suspend () -> T): T {
        return if (database != null) database.withTransaction { block() } else block()
    }

    private fun entityToInfo(
        entity: DailyChallengeEntity,
        board: CruciluxBoard?,
        currentStreak: Int,
        bestStreak: Int,
    ): DailyChallengeInfo {
        return DailyChallengeInfo(
            dateKey = entity.dateKey,
            boardId = entity.boardId,
            board = board,
            status = entity.challengeStatus,
            startedAt = entity.startedAt,
            completedAt = entity.completedAt,
            progressPercent = entity.progressPercent,
            currentStreak = currentStreak,
            bestStreak = bestStreak,
            userLetters = entity.parseUserLetters(),
            selectedRow = entity.selectedRow,
            selectedCol = entity.selectedCol,
            selectedDirection = entity.direction,
            checkMode = entity.parsedCheckMode,
            hintsUsed = entity.hintsUsed,
            hintRevealedCells = entity.parseHintRevealedCells(),
            isRewardClaimed = entity.isRewardClaimed,
            bestTimeSeconds = entity.bestTimeSeconds,
            elapsedTimeSeconds = entity.elapsedTimeSeconds,
        )
    }

    companion object {
        private const val TAG = "DailyChallengeRepo"

        @Volatile
        private var instance: DailyChallengeRepository? = null

        fun getInstance(context: Context): DailyChallengeRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val db = CruciluxDatabase.getInstance(context)
                    val bankRepo = CruciluxBankRepository.getInstance()
                    DailyChallengeRepository(
                        dailyChallengeDao = db.dailyChallengeDao(),
                        progressDao = db.progressDao(),
                        bankRepository = bankRepo,
                        selector = DailyChallengeSelector(bankRepo),
                        dateProvider = DefaultDateProvider(),
                        database = db,
                        achievementRepository = AchievementRepository.getInstance(context),
                    ).also { instance = it }
                }
            }
        }

        fun calculateProgress(
            grid: CrosswordGrid,
            userLetters: Map<Pair<Int, Int>, Char>,
            isCompleted: Boolean,
        ): Pair<DailyChallengeStatus, Int> {
            if (isCompleted) {
                return Pair(DailyChallengeStatus.COMPLETED, 100)
            }
            if (userLetters.isEmpty()) {
                return Pair(DailyChallengeStatus.NOT_STARTED, 0)
            }

            val playableCells = grid.cells.flatten().filter { it.isActive }
            val totalPlayable = playableCells.size
            if (totalPlayable == 0) return Pair(DailyChallengeStatus.NOT_STARTED, 0)

            val correctPlayable = playableCells.count { cell ->
                val pos = Pair(cell.row, cell.col)
                val userChar = userLetters[pos]?.uppercaseChar()
                val solChar = cell.solutionLetter?.uppercaseChar()
                userChar != null && userChar == solChar
            }

            if (correctPlayable == totalPlayable && playableCells.isNotEmpty()) {
                return Pair(DailyChallengeStatus.COMPLETED, 100)
            }

            val percent = ((correctPlayable * 100) / totalPlayable).coerceIn(0, 99)
            return Pair(DailyChallengeStatus.IN_PROGRESS, percent)
        }

        fun calculateCurrentStreak(completedDates: Set<LocalDate>, today: LocalDate): Int {
            if (completedDates.isEmpty()) return 0
            val startDay = when {
                today in completedDates -> today
                today.minusDays(1) in completedDates -> today.minusDays(1)
                else -> return 0
            }
            var streak = 0
            var check = startDay
            while (check in completedDates) {
                streak++
                check = check.minusDays(1)
            }
            return streak
        }

        fun calculateBestStreak(completedDates: Set<LocalDate>): Int {
            if (completedDates.isEmpty()) return 0
            val sorted = completedDates.sorted()
            var maxStreak = 0
            var currentRun = 0
            var prev: LocalDate? = null

            for (date in sorted) {
                if (prev == null || date == prev.plusDays(1)) {
                    currentRun++
                } else if (date != prev) {
                    currentRun = 1
                }
                if (currentRun > maxStreak) {
                    maxStreak = currentRun
                }
                prev = date
            }
            return maxStreak
        }
    }
}
