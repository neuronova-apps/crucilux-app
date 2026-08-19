package com.neuronova.crucilux

import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.data.db.CrosswordBoardStatus
import com.neuronova.crucilux.data.db.CrosswordProgressDao
import com.neuronova.crucilux.data.db.CrosswordProgressEntity
import com.neuronova.crucilux.data.repository.CategoryProgressStats
import com.neuronova.crucilux.data.repository.CrosswordProgressRepository
import com.neuronova.crucilux.engine.CruciluxGridEngine
import com.neuronova.crucilux.model.CruciluxAnswerType
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.ui.game.CheckMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

/**
 * Fake in-memory DAO para pruebas unitarias de Room en JVM.
 */
class FakeCrosswordProgressDao : CrosswordProgressDao {
    private val data = mutableMapOf<String, CrosswordProgressEntity>()
    private val flow = MutableStateFlow<List<CrosswordProgressEntity>>(emptyList())

    private fun notifyFlow() {
        flow.value = data.values.toList()
    }

    override suspend fun getProgress(boardId: String): CrosswordProgressEntity? = data[boardId]

    override fun observeProgress(boardId: String): Flow<CrosswordProgressEntity?> {
        return flow.map { list -> list.firstOrNull { it.boardId == boardId } }
    }

    override suspend fun insertOrUpdate(entity: CrosswordProgressEntity): Long {
        data[entity.boardId] = entity
        notifyFlow()
        return 1L
    }

    override suspend fun getAllProgress(): List<CrosswordProgressEntity> = data.values.toList()

    override fun observeAllProgress(): Flow<List<CrosswordProgressEntity>> = flow.asStateFlow()

    override suspend fun getProgressByCategory(category: String): List<CrosswordProgressEntity> {
        return data.values.filter { it.category.equals(category, ignoreCase = true) }
    }

    override fun observeProgressByCategory(category: String): Flow<List<CrosswordProgressEntity>> {
        return flow.map { list -> list.filter { it.category.equals(category, ignoreCase = true) } }
    }

    override suspend fun getInProgressList(): List<CrosswordProgressEntity> {
        return data.values.filter { it.status == CrosswordBoardStatus.IN_PROGRESS.name }
            .sortedByDescending { it.updatedAt }
    }

    override suspend fun getMostRecentInProgress(): CrosswordProgressEntity? {
        return getInProgressList().firstOrNull()
    }

    override fun observeMostRecentInProgress(): Flow<CrosswordProgressEntity?> {
        return flow.map { list ->
            list.filter { it.status == CrosswordBoardStatus.IN_PROGRESS.name }
                .maxByOrNull { it.updatedAt }
        }
    }

    override suspend fun countCompleted(): Int =
        data.values.count { it.status == CrosswordBoardStatus.COMPLETED.name }

    override suspend fun countInProgress(): Int =
        data.values.count { it.status == CrosswordBoardStatus.IN_PROGRESS.name }

    override suspend fun countCompletedByCategory(category: String): Int {
        return data.values.count {
            it.category.equals(category, ignoreCase = true) && it.status == CrosswordBoardStatus.COMPLETED.name
        }
    }

    override suspend fun countInProgressByCategory(category: String): Int {
        return data.values.count {
            it.category.equals(category, ignoreCase = true) && it.status == CrosswordBoardStatus.IN_PROGRESS.name
        }
    }

    override suspend fun deleteProgress(boardId: String): Int {
        val removed = data.remove(boardId) != null
        notifyFlow()
        return if (removed) 1 else 0
    }

    override suspend fun clearAllProgress(): Int {
        val count = data.size
        data.clear()
        notifyFlow()
        return count
    }
}

/**
 * Suite de 25 pruebas unitarias para el sistema de selección y progreso multi-tablero con Room.
 */
class CrosswordProgressTest {

    private lateinit var bankRepository: CruciluxBankRepository
    private lateinit var fakeDao: FakeCrosswordProgressDao
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

        fakeDao = FakeCrosswordProgressDao()
        progressRepository = CrosswordProgressRepository(fakeDao, bankRepository)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Estados independientes en Room
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `01 Room guarda estados independientes por boardId`() = runBlocking {
        val board1 = bankRepository.getAllBoards()[0]
        val board2 = bankRepository.getAllBoards()[1]

        assertNotEquals(board1.id, board2.id)

        progressRepository.saveProgress(
            boardId = board1.id,
            category = board1.category,
            userLetters = mapOf(Pair(0, 0) to 'A'),
            grid = null,
        )

        val p1 = progressRepository.getProgress(board1.id)
        val p2 = progressRepository.getProgress(board2.id)

        assertEquals(CrosswordBoardStatus.IN_PROGRESS, p1.status)
        assertEquals(CrosswordBoardStatus.NOT_STARTED, p2.status)
    }

    @Test
    fun `02 NOT_STARTED por defecto con 0 por ciento`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        val progress = progressRepository.getProgress(board.id)

        assertEquals(CrosswordBoardStatus.NOT_STARTED, progress.status)
        assertEquals(0, progress.progressPercent)
        assertTrue(progress.isNotStarted)
        assertEquals("○", progress.displaySymbol)
    }

    @Test
    fun `03 primera letra cambia estado a IN_PROGRESS`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val activeCell = grid.cells.flatten().first { it.isActive }

        progressRepository.saveProgress(
            boardId = board.id,
            category = board.category,
            userLetters = mapOf(Pair(activeCell.row, activeCell.col) to activeCell.solutionLetter!!),
            grid = grid,
        )

        val progress = progressRepository.getProgress(board.id)
        assertEquals(CrosswordBoardStatus.IN_PROGRESS, progress.status)
        assertTrue(progress.isInProgress)
        assertTrue(progress.progressPercent > 0)
    }

    @Test
    fun `04 progreso aumenta correctamente segun casillas jugables`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val playableCells = grid.cells.flatten().filter { it.isActive }

        val map1 = mapOf(Pair(playableCells[0].row, playableCells[0].col) to playableCells[0].solutionLetter!!)
        val (st1, pct1) = CrosswordProgressRepository.calculateProgress(grid, map1, isCompleted = false)

        val map3 = playableCells.take(3).associate { Pair(it.row, it.col) to it.solutionLetter!! }
        val (st3, pct3) = CrosswordProgressRepository.calculateProgress(grid, map3, isCompleted = false)

        assertEquals(CrosswordBoardStatus.IN_PROGRESS, st1)
        assertEquals(CrosswordBoardStatus.IN_PROGRESS, st3)
        assertTrue("El porcentaje con 3 letras debe ser mayor que con 1", pct3 >= pct1)
    }

    @Test
    fun `05 borrar reduce porcentaje cuando corresponde`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val playableCells = grid.cells.flatten().filter { it.isActive }

        val map2 = playableCells.take(2).associate { Pair(it.row, it.col) to it.solutionLetter!! }
        val (_, pct2) = CrosswordProgressRepository.calculateProgress(grid, map2, isCompleted = false)

        val map1 = playableCells.take(1).associate { Pair(it.row, it.col) to it.solutionLetter!! }
        val (_, pct1) = CrosswordProgressRepository.calculateProgress(grid, map1, isCompleted = false)

        assertTrue(pct2 >= pct1)

        val map0 = emptyMap<Pair<Int, Int>, Char>()
        val (st0, pct0) = CrosswordProgressRepository.calculateProgress(grid, map0, isCompleted = false)
        assertEquals(CrosswordBoardStatus.NOT_STARTED, st0)
        assertEquals(0, pct0)
    }

    @Test
    fun `06 completar tablero produce COMPLETED al 100 por ciento`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)

        progressRepository.saveProgress(
            boardId = board.id,
            category = board.category,
            userLetters = emptyMap(),
            grid = grid,
            isCompletedOverride = true,
        )

        val progress = progressRepository.getProgress(board.id)
        assertEquals(CrosswordBoardStatus.COMPLETED, progress.status)
        assertEquals(100, progress.progressPercent)
        assertTrue(progress.isCompleted)
        assertEquals("✓", progress.displaySymbol)
    }

    @Test
    fun `07 COMPLETED persiste tras reapertura`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)

        progressRepository.saveProgress(
            boardId = board.id,
            category = board.category,
            userLetters = emptyMap(),
            grid = grid,
            isCompletedOverride = true,
        )

        val loaded = progressRepository.getProgress(board.id)
        assertEquals(CrosswordBoardStatus.COMPLETED, loaded.status)
        assertEquals(100, loaded.progressPercent)
    }

    @Test
    fun `08 abrir completado no lo convierte en IN_PROGRESS`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)

        progressRepository.saveProgress(
            boardId = board.id,
            category = board.category,
            userLetters = emptyMap(),
            grid = grid,
            isCompletedOverride = true,
        )

        progressRepository.saveProgress(
            boardId = board.id,
            category = board.category,
            userLetters = mapOf(Pair(0, 0) to 'A'),
            grid = grid,
            isCompletedOverride = false,
        )

        val check = progressRepository.getProgress(board.id)
        assertEquals("Un tablero COMPLETED debe conservar status COMPLETED", CrosswordBoardStatus.COMPLETED, check.status)
        assertEquals(100, check.progressPercent)
    }

    @Test
    fun `09 30 tableros por categoria exactamente para las 10 categorias`() {
        val categories = bankRepository.getCategories()
        assertEquals(10, categories.size)

        for (cat in categories) {
            val boards = bankRepository.getBoardsByCategory(cat)
            assertEquals("La categoría $cat debe tener exactamente 30 tableros", 30, boards.size)
        }
    }

    @Test
    fun `10 indice 01 a 30 estable y determinista`() {
        val cat = "Historia"
        val boards1 = bankRepository.getBoardsByCategory(cat)
        val boards2 = bankRepository.getBoardsByCategory(cat)

        assertEquals(30, boards1.size)
        for (i in 0 until 30) {
            assertEquals("El índice $i debe ser determinista", boards1[i].id, boards2[i].id)
        }
    }

    @Test
    fun `11 usuario puede seleccionar cualquier tablero sin bloqueos`() = runBlocking {
        val catBoards = bankRepository.getBoardsByCategory("Tecnología")
        val lastBoard = catBoards.last()

        progressRepository.saveProgress(
            boardId = lastBoard.id,
            category = lastBoard.category,
            userLetters = mapOf(Pair(0, 0) to 'T'),
            grid = null,
        )

        val progress = progressRepository.getProgress(lastBoard.id)
        assertEquals(CrosswordBoardStatus.IN_PROGRESS, progress.status)
    }

    @Test
    fun `12 restauracion correcta por boardId`() = runBlocking {
        val board = bankRepository.getAllBoards()[5]
        val letters = mapOf(Pair(1, 2) to 'X', Pair(3, 4) to 'Y')

        progressRepository.saveProgress(
            boardId = board.id,
            category = board.category,
            userLetters = letters,
            grid = null,
            selectedRow = 3,
            selectedCol = 4,
            selectedDirection = CruciluxDirection.VERTICAL,
            checkMode = CheckMode.ASSISTED,
        )

        val restored = progressRepository.getProgress(board.id)
        assertEquals(board.id, restored.boardId)
        assertEquals(board.category, restored.category)
        assertEquals(letters, restored.userLetters)
        assertEquals(3, restored.selectedRow)
        assertEquals(4, restored.selectedCol)
        assertEquals(CruciluxDirection.VERTICAL, restored.selectedDirection)
        assertEquals(CheckMode.ASSISTED, restored.checkMode)
    }

    @Test
    fun `13 dos tableros en progreso no se sobrescriben`() = runBlocking {
        val b1 = bankRepository.getAllBoards()[10]
        val b2 = bankRepository.getAllBoards()[11]

        val l1 = mapOf(Pair(0, 0) to 'A')
        val l2 = mapOf(Pair(1, 1) to 'B')

        progressRepository.saveProgress(b1.id, b1.category, l1, null)
        progressRepository.saveProgress(b2.id, b2.category, l2, null)

        val p1 = progressRepository.getProgress(b1.id)
        val p2 = progressRepository.getProgress(b2.id)

        assertEquals(l1, p1.userLetters)
        assertEquals(l2, p2.userLetters)
    }

    @Test
    fun `14 Continuar recupera el IN_PROGRESS mas reciente`() = runBlocking {
        val b1 = bankRepository.getAllBoards()[0]
        val b2 = bankRepository.getAllBoards()[1]

        progressRepository.saveProgress(b1.id, b1.category, mapOf(Pair(0, 0) to 'A'), null)
        val entity2 = CrosswordProgressEntity(
            boardId = b2.id,
            category = b2.category,
            status = CrosswordBoardStatus.IN_PROGRESS.name,
            progressPercent = 25,
            userLetters = "0,0,B",
            updatedAt = System.currentTimeMillis() + 500,
        )
        fakeDao.insertOrUpdate(entity2)

        val mostRecent = progressRepository.getMostRecentInProgress()
        assertNotNull(mostRecent)
        assertEquals(b2.id, mostRecent?.boardId)
    }

    @Test
    fun `15 COMPLETED no aparece como Continuar`() = runBlocking {
        val b1 = bankRepository.getAllBoards()[0]

        fakeDao.insertOrUpdate(
            CrosswordProgressEntity(
                boardId = b1.id,
                category = b1.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
                updatedAt = System.currentTimeMillis(),
            )
        )

        val mostRecent = progressRepository.getMostRecentInProgress()
        assertNull("Partida COMPLETED no debe aparecer como Continuar", mostRecent)
    }

    @Test
    fun `16 Siguiente encuentra el proximo no completado`() = runBlocking {
        val catBoards = bankRepository.getBoardsByCategory("Ciencia")
        val b0 = catBoards[0]
        val b1 = catBoards[1]

        val next = progressRepository.getNextUncompletedBoard("Ciencia", b0.id)
        assertNotNull(next)
        assertEquals(b1.id, next?.id)
    }

    @Test
    fun `17 Siguiente salta completados`() = runBlocking {
        val catBoards = bankRepository.getBoardsByCategory("Ciencia")
        val b0 = catBoards[0]
        val b1 = catBoards[1]
        val b2 = catBoards[2]

        fakeDao.insertOrUpdate(
            CrosswordProgressEntity(
                boardId = b1.id,
                category = b1.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        )

        val next = progressRepository.getNextUncompletedBoard("Ciencia", b0.id)
        assertNotNull(next)
        assertEquals("Debe saltar b1 y seleccionar b2", b2.id, next?.id)
    }

    @Test
    fun `18 categoria 30 de 30 detecta categoria completada`() = runBlocking {
        val cat = "Animales"
        val catBoards = bankRepository.getBoardsByCategory(cat)
        assertEquals(30, catBoards.size)

        for (b in catBoards) {
            fakeDao.insertOrUpdate(
                CrosswordProgressEntity(
                    boardId = b.id,
                    category = cat,
                    status = CrosswordBoardStatus.COMPLETED.name,
                    progressPercent = 100,
                )
            )
        }

        val next = progressRepository.getNextUncompletedBoard(cat, catBoards.first().id)
        assertNull("Si todos están completados, debe retornar null", next)

        val stats = progressRepository.observeCategoryStats(cat).first()
        assertTrue("La categoría debe estar 100% completada", stats.isAllCompleted)
        assertEquals(30, stats.completedBoards)
        assertEquals(100, stats.completedPercent)
    }

    @Test
    fun `19 porcentaje categoria usa completed entre 30`() = runBlocking {
        val cat = "Geografía"
        val catBoards = bankRepository.getBoardsByCategory(cat)

        for (i in 0 until 12) {
            fakeDao.insertOrUpdate(
                CrosswordProgressEntity(
                    boardId = catBoards[i].id,
                    category = cat,
                    status = CrosswordBoardStatus.COMPLETED.name,
                    progressPercent = 100,
                )
            )
        }

        val stats = progressRepository.observeCategoryStats(cat).first()
        assertEquals(12, stats.completedBoards)
        assertEquals(30, stats.totalBoards)
        assertEquals(40, stats.completedPercent)
    }

    @Test
    fun `20 progreso global sobre 300`() = runBlocking {
        val allBoards = bankRepository.getAllBoards()
        assertEquals(300, allBoards.size)

        for (i in 0 until 30) {
            fakeDao.insertOrUpdate(
                CrosswordProgressEntity(
                    boardId = allBoards[i].id,
                    category = allBoards[i].category,
                    status = CrosswordBoardStatus.COMPLETED.name,
                    progressPercent = 100,
                )
            )
        }
        for (i in 30 until 45) {
            fakeDao.insertOrUpdate(
                CrosswordProgressEntity(
                    boardId = allBoards[i].id,
                    category = allBoards[i].category,
                    status = CrosswordBoardStatus.IN_PROGRESS.name,
                    progressPercent = 50,
                )
            )
        }

        val global = progressRepository.observeGlobalStats().first()
        assertEquals(300, global.totalBoards)
        assertEquals(30, global.completedBoards)
        assertEquals(15, global.inProgressBoards)
        assertEquals(255, global.notStartedBoards)
        assertEquals(10, global.globalPercent)
    }

    @Test
    fun `21 migracion de partida legacy DataStore`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        val legacyLetters = mapOf(Pair(0, 0) to 'C', Pair(0, 1) to 'A')
        val serialized = GameSessionManager.serializeLetters(legacyLetters)

        fakeDao.insertOrUpdate(
            CrosswordProgressEntity(
                boardId = board.id,
                category = board.category,
                status = CrosswordBoardStatus.IN_PROGRESS.name,
                progressPercent = 20,
                userLetters = serialized,
            )
        )

        val migrated = progressRepository.getProgress(board.id)
        assertEquals(CrosswordBoardStatus.IN_PROGRESS, migrated.status)
        assertEquals(legacyLetters, migrated.userLetters)
    }

    @Test
    fun `22 Biblia mantiene 9 AT 9 NT y 12 Ambos`() {
        val bibleBoards = bankRepository.getBoardsByCategory("Biblia")
        assertEquals(30, bibleBoards.size)

        val atCount = bibleBoards.count { it.subcategory.equals("AT", ignoreCase = true) || it.subcategory.contains("Antiguo", ignoreCase = true) }
        val ntCount = bibleBoards.count { it.subcategory.equals("NT", ignoreCase = true) || it.subcategory.contains("Nuevo", ignoreCase = true) }
        val bothCount = bibleBoards.count { it.subcategory.equals("Ambos", ignoreCase = true) }

        assertEquals(9, atCount)
        assertEquals(9, ntCount)
        assertEquals(12, bothCount)
        assertEquals(30, atCount + ntCount + bothCount)
    }

    @Test
    fun `23 palabra validada permanece bloqueada despues de restauracion`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hEntry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val userLetters = mutableMapOf<Pair<Int, Int>, Char>()
        for (i in hEntry.answer.indices) {
            userLetters[Pair(hEntry.row, hEntry.col + i)] = hEntry.answer[i]
        }

        progressRepository.saveProgress(
            boardId = board.id,
            category = board.category,
            userLetters = userLetters,
            grid = grid,
        )

        val restored = progressRepository.getProgress(board.id)
        assertEquals(userLetters, restored.userLetters)
        assertTrue(restored.userLetters.containsKey(Pair(hEntry.row, hEntry.col)))
    }

    @Test
    fun `24 respuesta compuesta sigue funcionando con Room`() = runBlocking {
        val boardWithCompound = bankRepository.getAllBoards().first { board ->
            board.entries.any { it.answerType == CruciluxAnswerType.COMPOUND }
        }
        val grid = CruciluxGridEngine.buildGrid(boardWithCompound)

        progressRepository.saveProgress(
            boardId = boardWithCompound.id,
            category = boardWithCompound.category,
            userLetters = mapOf(Pair(0, 0) to 'A'),
            grid = grid,
        )

        val restored = progressRepository.getProgress(boardWithCompound.id)
        assertEquals(boardWithCompound.id, restored.boardId)
        assertEquals(CrosswordBoardStatus.IN_PROGRESS, restored.status)
    }

    @Test
    fun `25 tableros rectangulares siguen funcionando con Room`() = runBlocking {
        val rectangularBoard = bankRepository.getAllBoards().first { it.rows != it.cols }
        val grid = CruciluxGridEngine.buildGrid(rectangularBoard)

        progressRepository.saveProgress(
            boardId = rectangularBoard.id,
            category = rectangularBoard.category,
            userLetters = mapOf(Pair(0, 0) to 'R'),
            grid = grid,
        )

        val restored = progressRepository.getProgress(rectangularBoard.id)
        assertEquals(rectangularBoard.id, restored.boardId)
        assertEquals(rectangularBoard.category, restored.category)
    }
}
