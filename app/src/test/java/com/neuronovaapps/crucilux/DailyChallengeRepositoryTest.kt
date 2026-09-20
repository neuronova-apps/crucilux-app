package com.neuronovaapps.crucilux

import com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
import com.neuronovaapps.crucilux.data.daily.DailyChallengeRepository
import com.neuronovaapps.crucilux.data.daily.DailyChallengeSelector
import com.neuronovaapps.crucilux.data.daily.FakeDateProvider
import com.neuronovaapps.crucilux.data.db.CrosswordBoardStatus
import com.neuronovaapps.crucilux.data.db.CrosswordProgressDao
import com.neuronovaapps.crucilux.data.db.CrosswordProgressEntity
import com.neuronovaapps.crucilux.data.db.DailyChallengeDao
import com.neuronovaapps.crucilux.data.db.DailyChallengeEntity
import com.neuronovaapps.crucilux.data.db.DailyChallengeStatus
import com.neuronovaapps.crucilux.data.repository.CrosswordProgressRepository
import com.neuronovaapps.crucilux.engine.CruciluxGridEngine
import com.neuronovaapps.crucilux.model.CruciluxDirection
import com.neuronovaapps.crucilux.ui.game.CheckMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate

/**
 * Fake DAO para pruebas unitarias de DailyChallengeDao en JVM.
 */
class FakeDailyChallengeDao : DailyChallengeDao {
    private val data = mutableMapOf<String, DailyChallengeEntity>()
    private val flow = MutableStateFlow<List<DailyChallengeEntity>>(emptyList())

    private fun notifyFlow() {
        flow.value = data.values.toList()
    }

    override suspend fun getChallenge(dateKey: String): DailyChallengeEntity? = data[dateKey]

    override fun observeChallenge(dateKey: String): Flow<DailyChallengeEntity?> {
        return flow.map { list -> list.firstOrNull { it.dateKey == dateKey } }
    }

    override suspend fun insertIgnore(entity: DailyChallengeEntity): Long {
        if (data.containsKey(entity.dateKey)) {
            return -1L
        }
        data[entity.dateKey] = entity
        notifyFlow()
        return 1L
    }

    override suspend fun update(entity: DailyChallengeEntity): Int {
        return if (data.containsKey(entity.dateKey)) {
            data[entity.dateKey] = entity
            notifyFlow()
            1
        } else {
            0
        }
    }

    override suspend fun getAllCompleted(): List<DailyChallengeEntity> {
        return data.values.filter { it.status == DailyChallengeStatus.COMPLETED.name }
            .sortedBy { it.dateKey }
    }

    override fun observeAllCompleted(): Flow<List<DailyChallengeEntity>> {
        return flow.map { list ->
            list.filter { it.status == DailyChallengeStatus.COMPLETED.name }
                .sortedBy { it.dateKey }
        }
    }

    override suspend fun getAllChallenges(): List<DailyChallengeEntity> {
        return data.values.sortedByDescending { it.dateKey }
    }

    override fun observeAllChallenges(): Flow<List<DailyChallengeEntity>> {
        return flow.map { list -> list.sortedByDescending { it.dateKey } }
    }

    override suspend fun countCompleted(): Int {
        return data.values.count { it.status == DailyChallengeStatus.COMPLETED.name }
    }

    override fun observeCompletedCount(): Flow<Int> {
        return flow.map { list -> list.count { it.status == DailyChallengeStatus.COMPLETED.name } }
    }

    override suspend fun deleteChallenge(dateKey: String): Int {
        val removed = data.remove(dateKey) != null
        notifyFlow()
        return if (removed) 1 else 0
    }
}

/**
 * Suite exhaustiva de pruebas unitarias para DailyChallengeRepository.
 * Verifica persistencia de sesión aislada, cálculo de rachas, no-regresión de progreso y XP.
 */
class DailyChallengeRepositoryTest {

    private lateinit var bankRepository: CruciluxBankRepository
    private lateinit var fakeDailyDao: FakeDailyChallengeDao
    private lateinit var fakeProgressDao: FakeCrosswordProgressDao
    private lateinit var dateProvider: FakeDateProvider
    private lateinit var selector: DailyChallengeSelector
    private lateinit var dailyRepository: DailyChallengeRepository
    private lateinit var progressRepository: CrosswordProgressRepository

    @Before
    fun setUp() {
        bankRepository = CruciluxBankRepository.getInstance()
        val assetFile = File("src/main/assets/crucilux_bank_v1_37.json")
        val finalFile = if (assetFile.exists()) assetFile
        else File("app/src/main/assets/crucilux_bank_v1_37.json")
        assertTrue("El archivo crucilux_bank_v1_37.json debe existir", finalFile.exists())
        FileInputStream(finalFile).use { stream ->
            bankRepository.loadFromStream(stream)
        }

        fakeDailyDao = FakeDailyChallengeDao()
        fakeProgressDao = FakeCrosswordProgressDao()
        dateProvider = FakeDateProvider(LocalDate.of(2026, 9, 18))
        selector = DailyChallengeSelector(bankRepository)

        dailyRepository = DailyChallengeRepository(
            dailyChallengeDao = fakeDailyDao,
            progressDao = fakeProgressDao,
            bankRepository = bankRepository,
            selector = selector,
            dateProvider = dateProvider,
            database = null,
            achievementRepository = null,
            ioDispatcher = Dispatchers.Unconfined,
        )

        progressRepository = CrosswordProgressRepository(
            dao = fakeProgressDao,
            bankRepository = bankRepository,
            dailyChallengeRepository = dailyRepository,
        )
    }

    @Test
    fun `01 desafio de hoy inicia en NOT_STARTED con 0 por ciento de progreso`() = runBlocking {
        val challenge = dailyRepository.getTodayChallenge()
        assertEquals(dateProvider.todayKey(), challenge.dateKey)
        assertEquals(DailyChallengeStatus.NOT_STARTED, challenge.status)
        assertEquals(0, challenge.progressPercent)
        assertEquals(0, challenge.currentStreak)
        assertEquals(0, challenge.bestStreak)
        assertNotNull(challenge.board)
    }

    @Test
    fun `02 iniciar desafio actualiza estado a IN_PROGRESS y persiste attemptCount`() = runBlocking {
        val started = dailyRepository.startTodayChallenge()
        assertEquals(DailyChallengeStatus.IN_PROGRESS, started.status)
        assertNotNull(started.startedAt)

        val retrieved = dailyRepository.getTodayChallenge()
        assertEquals(DailyChallengeStatus.IN_PROGRESS, retrieved.status)
    }

    @Test
    fun `03 guardar sesion diaria persiste letras aisladas sin tocar progreso regular`() = runBlocking {
        val today = dailyRepository.getTodayChallenge()
        val board = today.board!!
        val grid = CruciluxGridEngine.buildGrid(board)

        // Escribir 1 letra en la sesión diaria
        val firstActive = grid.cells.flatten().first { it.isActive }
        val letters = mapOf(Pair(firstActive.row, firstActive.col) to 'A')

        dailyRepository.saveDailySession(
            dateKey = today.dateKey,
            boardId = board.id,
            userLetters = letters,
            grid = grid,
            selectedRow = 0,
            selectedCol = 0,
            selectedDirection = CruciluxDirection.HORIZONTAL,
            checkMode = CheckMode.CLASSIC,
            hintsUsed = 0,
            hintRevealedCells = emptySet(),
            isCompleted = false,
        )

        // Verificar sesión diaria actualizada
        val dailySession = dailyRepository.getDailySession(today.dateKey)
        assertNotNull(dailySession)
        assertEquals(1, dailySession!!.userLetters.size)
        assertEquals('A', dailySession.userLetters[Pair(firstActive.row, firstActive.col)])

        // Verificar que el DAO de progreso regular sigue vacío
        val regularProgress = fakeProgressDao.getProgress(board.id)
        assertTrue("El progreso regular debe permanecer intacto", regularProgress == null)
    }

    @Test
    fun `04 tablero completado previamente en modo regular no autocompleta el desafio diario de hoy`() = runBlocking {
        val today = dailyRepository.getTodayChallenge()
        val board = today.board!!

        // Simular que el usuario ya completó este tablero en juego libre regular
        fakeProgressDao.insertOrUpdate(
            CrosswordProgressEntity(
                boardId = board.id,
                category = board.category,
                userLetters = "{\"0,0\":\"Z\"}",
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
                bestXpEarned = 150,
            )
        )

        // El desafío diario de hoy debe seguir NOT_STARTED con sesión limpia
        val daily = dailyRepository.getTodayChallenge()
        assertEquals("El desafío de hoy no debe autocompletarse por partidas regulares", DailyChallengeStatus.NOT_STARTED, daily.status)
        assertEquals(0, daily.progressPercent)
        assertTrue("Las letras del desafío diario deben estar vacías", daily.userLetters.isEmpty())
    }

    @Test
    fun `05 completar desafio diario actualiza estado a COMPLETED y registra completedAt`() = runBlocking {
        val today = dailyRepository.getTodayChallenge()
        val board = today.board!!
        val grid = CruciluxGridEngine.buildGrid(board)

        // Rellenar con la solución completa
        val fullSolution = mutableMapOf<Pair<Int, Int>, Char>()
        grid.cells.flatten().filter { it.isActive }.forEach { cell ->
            fullSolution[Pair(cell.row, cell.col)] = cell.solutionLetter ?: 'A'
        }

        dailyRepository.saveDailySession(
            dateKey = today.dateKey,
            boardId = board.id,
            userLetters = fullSolution,
            grid = grid,
            selectedRow = 0,
            selectedCol = 0,
            selectedDirection = CruciluxDirection.HORIZONTAL,
            checkMode = CheckMode.CLASSIC,
            hintsUsed = 0,
            hintRevealedCells = emptySet(),
            isCompleted = true,
        )

        val updated = dailyRepository.getTodayChallenge()
        assertEquals(DailyChallengeStatus.COMPLETED, updated.status)
        assertEquals(100, updated.progressPercent)
        assertNotNull(updated.completedAt)
        assertEquals(1, updated.currentStreak)
        assertEquals(1, updated.bestStreak)
    }

    @Test
    fun `06 calculo de rachas consecutivas incrementa correctamente`() = runBlocking {
        // Completar día 1: 2026-09-16
        dateProvider.setDate(LocalDate.of(2026, 9, 16))
        dailyRepository.onBoardCompleted(dateProvider.todayKey(), "board_1")

        // Completar día 2: 2026-09-17
        dateProvider.setDate(LocalDate.of(2026, 9, 17))
        dailyRepository.onBoardCompleted(dateProvider.todayKey(), "board_2")

        // Completar día 3: 2026-09-18
        dateProvider.setDate(LocalDate.of(2026, 9, 18))
        dailyRepository.onBoardCompleted(dateProvider.todayKey(), "board_3")

        val today = dailyRepository.getTodayChallenge()
        assertEquals("La racha actual debe ser de 3 días", 3, today.currentStreak)
        assertEquals("La mejor racha debe ser de 3 días", 3, today.bestStreak)
    }

    @Test
    fun `07 racha pendiente se mantiene activa si se jugo ayer y hoy aun no se completa`() = runBlocking {
        // Completar día 1: 2026-09-17
        dateProvider.setDate(LocalDate.of(2026, 9, 17))
        dailyRepository.onBoardCompleted(dateProvider.todayKey(), "board_yesterday")

        // Llegar al día siguiente: 2026-09-18 (hoy aún no completado)
        dateProvider.setDate(LocalDate.of(2026, 9, 18))
        val today = dailyRepository.getTodayChallenge()

        assertEquals("La racha de ayer se mantiene activa como 1 pendiente", 1, today.currentStreak)
        assertEquals(1, today.bestStreak)
        assertEquals(DailyChallengeStatus.NOT_STARTED, today.status)
    }

    @Test
    fun `08 dia no jugado rompe la racha actual pero preserva la mejor racha`() = runBlocking {
        // Racha previa de 3 días: días 10, 11 y 12
        dateProvider.setDate(LocalDate.of(2026, 9, 10))
        dailyRepository.onBoardCompleted(dateProvider.todayKey(), "b10")

        dateProvider.setDate(LocalDate.of(2026, 9, 11))
        dailyRepository.onBoardCompleted(dateProvider.todayKey(), "b11")

        dateProvider.setDate(LocalDate.of(2026, 9, 12))
        dailyRepository.onBoardCompleted(dateProvider.todayKey(), "b12")

        // Usuario se salta el día 13 y 14. Vuelve el día 15
        dateProvider.setDate(LocalDate.of(2026, 9, 15))
        val todayBeforePlay = dailyRepository.getTodayChallenge()
        assertEquals("Racha rota debe ser 0", 0, todayBeforePlay.currentStreak)
        assertEquals("Mejor racha debe preservar 3", 3, todayBeforePlay.bestStreak)

        // Completa el día 15: nueva racha inicia en 1
        dailyRepository.onBoardCompleted(dateProvider.todayKey(), "b15")
        val todayAfterPlay = dailyRepository.getTodayChallenge()
        assertEquals("Nueva racha debe ser 1", 1, todayAfterPlay.currentStreak)
        assertEquals("Mejor racha sigue siendo 3", 3, todayAfterPlay.bestStreak)
    }

    @Test
    fun `09 sincronizacion no destructiva con progreso regular y no duplicacion de XP`() = runBlocking {
        val today = dailyRepository.getTodayChallenge()
        val board = today.board!!

        // Caso A: El tablero NO estaba completado en juego regular.
        // Al completar el desafío diario, debe registrarse en juego regular.
        progressRepository.markBoardCompleted(
            boardId = board.id,
            xpEarned = 120,
            completionTimeSeconds = 45,
            hintsUsed = 0,
            dailyDateKey = today.dateKey,
        )

        val regularAfterDaily = fakeProgressDao.getProgress(board.id)
        assertNotNull(regularAfterDaily)
        assertEquals(CrosswordBoardStatus.COMPLETED.name, regularAfterDaily!!.status)
        assertEquals(120, regularAfterDaily.bestXpEarned)

        // Caso B: Rejugar un desafío diario ya completado no duplica XP
        val prevXp = regularAfterDaily.bestXpEarned
        progressRepository.markBoardCompleted(
            boardId = board.id,
            xpEarned = 120,
            completionTimeSeconds = 40,
            hintsUsed = 0,
            dailyDateKey = today.dateKey,
        )

        val regularAfterReplay = fakeProgressDao.getProgress(board.id)
        assertEquals("El XP no debe duplicarse en rejugado", prevXp, regularAfterReplay!!.bestXpEarned)
    }

    @Test
    fun `10 cambio de fecha local conmuta al nuevo desafio diario correspondiente`() = runBlocking {
        dateProvider.setDate(LocalDate.of(2026, 9, 18))
        val day1Challenge = dailyRepository.getTodayChallenge()

        dateProvider.setDate(LocalDate.of(2026, 9, 19))
        val day2Challenge = dailyRepository.getTodayChallenge()

        assertEquals("2026-09-18", day1Challenge.dateKey)
        assertEquals("2026-09-19", day2Challenge.dateKey)
        assertFalse(
            "Los tableros de días consecutivos no deben ser iguales",
            day1Challenge.boardId == day2Challenge.boardId
        )
    }

    @Test
    fun `11 auditoria bestTimeSeconds - inicio de tiempo en 0 al iniciar nuevo desafio`() = runBlocking {
        val today = dailyRepository.getTodayChallenge()
        assertEquals(0L, today.elapsedTimeSeconds)
        assertEquals(null, today.bestTimeSeconds)

        dailyRepository.startTodayChallenge()
        val inProgress = dailyRepository.getTodayChallenge()
        assertEquals(DailyChallengeStatus.IN_PROGRESS, inProgress.status)
        assertEquals(0L, inProgress.elapsedTimeSeconds)
        assertEquals(null, inProgress.bestTimeSeconds)
    }

    @Test
    fun `12 auditoria bestTimeSeconds - persistencia de tiempo transcurrido en sesion activa`() = runBlocking {
        val today = dailyRepository.getTodayChallenge()
        val board = today.board!!
        val grid = CruciluxGridEngine.buildGrid(board)
        val cell = grid.cells.flatten().first { it.isActive }

        // Guardar progreso con 45 segundos acumulados
        dailyRepository.saveDailySession(
            dateKey = today.dateKey,
            boardId = board.id,
            userLetters = mapOf(Pair(cell.row, cell.col) to 'A'),
            grid = grid,
            elapsedTimeSeconds = 45L,
        )

        val retrieved = dailyRepository.getTodayChallenge()
        assertEquals(DailyChallengeStatus.IN_PROGRESS, retrieved.status)
        assertEquals(45L, retrieved.elapsedTimeSeconds)
        assertEquals(null, retrieved.bestTimeSeconds)
    }

    @Test
    fun `13 auditoria bestTimeSeconds - acumulacion al continuar partida sin reinicio a cero`() = runBlocking {
        val today = dailyRepository.getTodayChallenge()
        val board = today.board!!
        val grid = CruciluxGridEngine.buildGrid(board)
        val cell = grid.cells.flatten().first { it.isActive }

        // Sesión inicial: 45 segundos
        dailyRepository.saveDailySession(
            dateKey = today.dateKey,
            boardId = board.id,
            userLetters = mapOf(Pair(cell.row, cell.col) to 'A'),
            grid = grid,
            elapsedTimeSeconds = 45L,
        )

        // Continuar más tarde: el usuario juega 50 segundos más (acumulado = 95s)
        val resumed = dailyRepository.getTodayChallenge()
        val accumulatedTime = resumed.elapsedTimeSeconds + 50L
        dailyRepository.saveDailySession(
            dateKey = today.dateKey,
            boardId = board.id,
            userLetters = mapOf(Pair(cell.row, cell.col) to 'A'),
            grid = grid,
            elapsedTimeSeconds = accumulatedTime,
        )

        val finalState = dailyRepository.getTodayChallenge()
        assertEquals(95L, finalState.elapsedTimeSeconds)
    }

    @Test
    fun `14 auditoria bestTimeSeconds - detencion al completar y registro inicial del mejor tiempo`() = runBlocking {
        val today = dailyRepository.getTodayChallenge()
        val board = today.board!!
        val grid = CruciluxGridEngine.buildGrid(board)

        val fullSolution = mutableMapOf<Pair<Int, Int>, Char>()
        grid.cells.flatten().filter { it.isActive }.forEach { cell ->
            fullSolution[Pair(cell.row, cell.col)] = cell.solutionLetter ?: 'A'
        }

        // Completar con tiempo transcurrido final de 95 segundos
        dailyRepository.saveDailySession(
            dateKey = today.dateKey,
            boardId = board.id,
            userLetters = fullSolution,
            grid = grid,
            isCompleted = true,
            elapsedTimeSeconds = 95L,
        )

        val completed = dailyRepository.getTodayChallenge()
        assertEquals(DailyChallengeStatus.COMPLETED, completed.status)
        assertEquals(95L, completed.elapsedTimeSeconds)
        assertEquals(95L, completed.bestTimeSeconds)
    }

    @Test
    fun `15 auditoria bestTimeSeconds - conservacion del mejor tiempo en rejugado minOf`() = runBlocking {
        val today = dailyRepository.getTodayChallenge()
        val board = today.board!!
        val grid = CruciluxGridEngine.buildGrid(board)

        val fullSolution = mutableMapOf<Pair<Int, Int>, Char>()
        grid.cells.flatten().filter { it.isActive }.forEach { cell ->
            fullSolution[Pair(cell.row, cell.col)] = cell.solutionLetter ?: 'A'
        }

        // 1ª partida completada en 95 segundos
        dailyRepository.saveDailySession(
            dateKey = today.dateKey,
            boardId = board.id,
            userLetters = fullSolution,
            grid = grid,
            isCompleted = true,
            elapsedTimeSeconds = 95L,
        )
        assertEquals(95L, dailyRepository.getTodayChallenge().bestTimeSeconds)

        // 2ª partida rejugada y superada con mejor tiempo (65 segundos < 95 segundos)
        dailyRepository.saveDailySession(
            dateKey = today.dateKey,
            boardId = board.id,
            userLetters = fullSolution,
            grid = grid,
            isCompleted = true,
            elapsedTimeSeconds = 65L,
        )
        assertEquals("Mejor tiempo debe actualizarse a 65 segundos", 65L, dailyRepository.getTodayChallenge().bestTimeSeconds)

        // 3ª partida rejugada con peor tiempo (120 segundos > 65 segundos)
        dailyRepository.saveDailySession(
            dateKey = today.dateKey,
            boardId = board.id,
            userLetters = fullSolution,
            grid = grid,
            isCompleted = true,
            elapsedTimeSeconds = 120L,
        )
        assertEquals("Mejor tiempo debe conservarse en 65 segundos sin degradarse", 65L, dailyRepository.getTodayChallenge().bestTimeSeconds)
    }

    @Test
    fun `16 auditoria bestTimeSeconds - ausencia total de duplicacion de registros para la misma fecha`() = runBlocking {
        val todayKey = dateProvider.todayKey()
        val board = dailyRepository.getTodayChallenge().board!!
        val grid = CruciluxGridEngine.buildGrid(board)

        // Iniciar
        dailyRepository.startTodayChallenge()
        // Guardar progreso 1
        dailyRepository.saveDailySession(todayKey, board.id, emptyMap(), grid, elapsedTimeSeconds = 10L)
        // Guardar progreso 2
        dailyRepository.saveDailySession(todayKey, board.id, emptyMap(), grid, elapsedTimeSeconds = 20L)
        // Completar
        dailyRepository.completeTodayChallenge()

        // Verificar que solo existe exactamente 1 registro para la fecha
        val allChallenges = fakeDailyDao.getAllChallenges()
        val matchingToday = allChallenges.filter { it.dateKey == todayKey }
        assertEquals("Debe existir exactamente 1 registro en Room para la fecha del día", 1, matchingToday.size)
        assertEquals(1, allChallenges.size)
    }

    @Test
    fun `17 confirmacion exhaustiva - serie semanal de 7 dias, ruptura por salto y reinicio de racha`() = runBlocking {
        val baseDate = LocalDate.of(2026, 9, 1)

        // Simular 7 días consecutivos jugados y completados
        for (i in 0 until 7) {
            val date = baseDate.plusDays(i.toLong())
            dateProvider.setDate(date)
            dailyRepository.onBoardCompleted(dateProvider.todayKey(), "board_$i")
        }

        // En el día 7, la racha debe ser 7 y mejor racha 7
        dateProvider.setDate(baseDate.plusDays(6))
        val day7 = dailyRepository.getTodayChallenge()
        assertEquals("Racha de 7 días consecutivos completados", 7, day7.currentStreak)
        assertEquals("Mejor racha de 7 días", 7, day7.bestStreak)

        // Saltar día 8 y llegar al día 9 sin jugar el 8
        val day9Date = baseDate.plusDays(8)
        dateProvider.setDate(day9Date)
        val day9Before = dailyRepository.getTodayChallenge()
        assertEquals("Racha actual debe haberse roto a 0 por el día saltado", 0, day9Before.currentStreak)
        assertEquals("Mejor racha histórica debe conservar 7", 7, day9Before.bestStreak)

        // Completar el día 9
        dailyRepository.onBoardCompleted(dateProvider.todayKey(), "board_day9")
        val day9After = dailyRepository.getTodayChallenge()
        assertEquals("Nueva racha debe iniciar en 1", 1, day9After.currentStreak)
        assertEquals("Mejor racha histórica permanece intacta en 7", 7, day9After.bestStreak)
    }
}
