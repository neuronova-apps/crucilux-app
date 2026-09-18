package com.neuronovaapps.crucilux

import com.neuronovaapps.crucilux.achievements.AchievementEngine
import com.neuronovaapps.crucilux.achievements.CruciluxAchievements
import com.neuronovaapps.crucilux.data.GameSessionManager
import com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
import com.neuronovaapps.crucilux.data.db.AchievementEntity
import com.neuronovaapps.crucilux.data.db.CrosswordBoardStatus
import com.neuronovaapps.crucilux.data.db.CrosswordProgressEntity
import com.neuronovaapps.crucilux.model.CruciluxAnswerType
import com.neuronovaapps.crucilux.model.CruciluxBoard
import com.neuronovaapps.crucilux.model.CruciluxDirection
import com.neuronovaapps.crucilux.model.CruciluxEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class AchievementEngineTest {

    private lateinit var bankRepository: CruciluxBankRepository

    @Before
    fun setUp() {
        bankRepository = CruciluxBankRepository.getInstance()
        val localAsset = File("src/main/assets/crucilux_bank_v1_37.json")
        val finalAsset = if (localAsset.exists()) localAsset else File("app/src/main/assets/crucilux_bank_v1_37.json")
        assertTrue("El archivo crucilux_bank_v1_37.json debe existir", finalAsset.exists())
        FileInputStream(finalAsset).use { stream ->
            bankRepository.loadFromStream(stream)
        }
    }

    @Test
    fun `01 todos los logros inician bloqueados con progreso cero cuando no hay partidas`() {
        val result = AchievementEngine.evaluate(
            progressList = emptyList(),
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 1000L,
        )

        assertEquals(4, result.size)
        result.forEach { entity ->
            assertFalse("El logro ${entity.achievementId} debe iniciar bloqueado", entity.isUnlocked)
            assertNull("El timestamp de desbloqueo debe ser null", entity.unlockedAt)
            assertEquals(0, entity.currentProgress)
            assertFalse(entity.isNotified)
        }
    }

    @Test
    fun `02 Primer Crucigrama se desbloquea al completar un tablero`() {
        val board = bankRepository.getAllBoards().first()
        val progress = listOf(
            CrosswordProgressEntity(
                boardId = board.id,
                category = board.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        )

        val result = AchievementEngine.evaluate(
            progressList = progress,
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 2000L,
        )

        val firstCrossword = result.first { it.achievementId == CruciluxAchievements.ID_FIRST_CROSSWORD }
        assertTrue(firstCrossword.isUnlocked)
        assertEquals(2000L, firstCrossword.unlockedAt)
        assertEquals(1, firstCrossword.currentProgress)
        assertEquals(1, firstCrossword.targetProgress)
    }

    @Test
    fun `03 Primer Crucigrama no se desbloquea si el tablero esta solo IN_PROGRESS`() {
        val board = bankRepository.getAllBoards().first()
        val progress = listOf(
            CrosswordProgressEntity(
                boardId = board.id,
                category = board.category,
                status = CrosswordBoardStatus.IN_PROGRESS.name,
                progressPercent = 90,
            )
        )

        val result = AchievementEngine.evaluate(
            progressList = progress,
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 2000L,
        )

        val firstCrossword = result.first { it.achievementId == CruciluxAchievements.ID_FIRST_CROSSWORD }
        assertFalse(firstCrossword.isUnlocked)
        assertEquals(0, firstCrossword.currentProgress)
    }

    @Test
    fun `04 Maestro de Letras muestra progreso parcial y se desbloquea exactamente a 30 tableros`() {
        val allBoards = bankRepository.getAllBoards()

        // 1. Con 12 tableros completados -> Progreso 12/30, bloqueado
        val progress12 = allBoards.take(12).map { b ->
            CrosswordProgressEntity(
                boardId = b.id,
                category = b.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        }

        val result12 = AchievementEngine.evaluate(
            progressList = progress12,
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 3000L,
        )

        val masterSolver12 = result12.first { it.achievementId == CruciluxAchievements.ID_MASTER_SOLVER }
        assertFalse(masterSolver12.isUnlocked)
        assertEquals(12, masterSolver12.currentProgress)
        assertEquals(30, masterSolver12.targetProgress)

        // 2. Con 29 tableros completados -> Bloqueado
        val progress29 = allBoards.take(29).map { b ->
            CrosswordProgressEntity(
                boardId = b.id,
                category = b.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        }

        val result29 = AchievementEngine.evaluate(
            progressList = progress29,
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 4000L,
        )

        val masterSolver29 = result29.first { it.achievementId == CruciluxAchievements.ID_MASTER_SOLVER }
        assertFalse(masterSolver29.isUnlocked)
        assertEquals(29, masterSolver29.currentProgress)

        // 3. Con 30 tableros completados -> Desbloqueado
        val progress30 = allBoards.take(30).map { b ->
            CrosswordProgressEntity(
                boardId = b.id,
                category = b.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        }

        val result30 = AchievementEngine.evaluate(
            progressList = progress30,
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 5000L,
        )

        val masterSolver30 = result30.first { it.achievementId == CruciluxAchievements.ID_MASTER_SOLVER }
        assertTrue(masterSolver30.isUnlocked)
        assertEquals(30, masterSolver30.currentProgress)
        assertEquals(5000L, masterSolver30.unlockedAt)
    }

    @Test
    fun `05 Gran Tablero se desbloquea al completar un crucigrama de 15x15`() {
        // Encontrar un tablero de 15x15 en el banco maestro (ej. 15X15-01)
        val board15x15 = bankRepository.getBoardById("15X15-01")
            ?: bankRepository.getAllBoards().first { it.id.startsWith("15X15") }
        assertNotNull(board15x15)
        assertTrue(board15x15.id.startsWith("15X15"))

        // 1. Completar un tablero de otro tamaño (ej. 7x7) no desbloquea Gran Tablero
        val smallBoard = bankRepository.getAllBoards().first { it.rows < 15 }
        val smallProgress = listOf(
            CrosswordProgressEntity(
                boardId = smallBoard.id,
                category = smallBoard.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        )

        val resultSmall = AchievementEngine.evaluate(
            progressList = smallProgress,
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 1000L,
        )
        val grandGridBefore = resultSmall.first { it.achievementId == CruciluxAchievements.ID_GRAND_GRID }
        assertFalse(grandGridBefore.isUnlocked)

        // 2. Completar 15X15-01 desbloquea Gran Tablero
        val fullProgress = smallProgress + CrosswordProgressEntity(
            boardId = board15x15.id,
            category = board15x15.category,
            status = CrosswordBoardStatus.COMPLETED.name,
            progressPercent = 100,
        )

        val result15 = AchievementEngine.evaluate(
            progressList = fullProgress,
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 6000L,
        )
        val grandGridAfter = result15.first { it.achievementId == CruciluxAchievements.ID_GRAND_GRID }
        assertTrue(grandGridAfter.isUnlocked)
        assertEquals(6000L, grandGridAfter.unlockedAt)
        assertEquals(1, grandGridAfter.currentProgress)
    }

    @Test
    fun `06 Vocabulario de Oro acumula palabras y desbloquea al llegar a 50 palabras correctas`() {
        val boards = bankRepository.getAllBoards()

        // Cada tablero tiene 6 u 8 palabras. Tomemos tableros hasta alcanzar 48 palabras (~7 tableros)
        var accumulatedWords = 0
        val selectedBoards = mutableListOf<CruciluxBoard>()
        for (b in boards) {
            if (accumulatedWords + b.entries.size < 50) {
                selectedBoards.add(b)
                accumulatedWords += b.entries.size
            } else {
                break
            }
        }

        val partialProgress = selectedBoards.map { b ->
            CrosswordProgressEntity(
                boardId = b.id,
                category = b.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        }

        val resultPartial = AchievementEngine.evaluate(
            progressList = partialProgress,
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 1000L,
        )

        val wordMasterPartial = resultPartial.first { it.achievementId == CruciluxAchievements.ID_WORD_MASTER }
        assertFalse(wordMasterPartial.isUnlocked)
        assertEquals(accumulatedWords, wordMasterPartial.currentProgress)

        // Añadir suficientes tableros para superar 50 palabras
        val additionalBoard = boards.first { it !in selectedBoards }
        val fullProgress = partialProgress + CrosswordProgressEntity(
            boardId = additionalBoard.id,
            category = additionalBoard.category,
            status = CrosswordBoardStatus.COMPLETED.name,
            progressPercent = 100,
        )

        val resultFull = AchievementEngine.evaluate(
            progressList = fullProgress,
            existingEntities = emptyMap(),
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 7000L,
        )

        val wordMasterFull = resultFull.first { it.achievementId == CruciluxAchievements.ID_WORD_MASTER }
        assertTrue(wordMasterFull.isUnlocked)
        assertTrue(wordMasterFull.currentProgress >= 50)
        assertEquals(7000L, wordMasterFull.unlockedAt)
    }

    @Test
    fun `07 Vocabulario de Oro contabiliza palabras correctas de tableros IN_PROGRESS`() {
        val board = bankRepository.getAllBoards().first()
        val entry1 = board.entries[0]
        val entry2 = board.entries[1]

        // Construir userLetters con las dos primeras palabras completas y correctas
        val lettersMap = mutableMapOf<Pair<Int, Int>, Char>()
        for (i in 0 until entry1.length) {
            val r = if (entry1.direction == CruciluxDirection.VERTICAL) entry1.row + i else entry1.row
            val c = if (entry1.direction == CruciluxDirection.HORIZONTAL) entry1.col + i else entry1.col
            lettersMap[Pair(r, c)] = entry1.answer[i]
        }
        for (i in 0 until entry2.length) {
            val r = if (entry2.direction == CruciluxDirection.VERTICAL) entry2.row + i else entry2.row
            val c = if (entry2.direction == CruciluxDirection.HORIZONTAL) entry2.col + i else entry2.col
            lettersMap[Pair(r, c)] = entry2.answer[i]
        }

        val inProgressEntity = CrosswordProgressEntity(
            boardId = board.id,
            category = board.category,
            status = CrosswordBoardStatus.IN_PROGRESS.name,
            progressPercent = 30,
            userLetters = GameSessionManager.serializeLetters(lettersMap),
        )

        val words = AchievementEngine.calculateTotalCorrectWords(
            progressList = listOf(inProgressEntity),
            getBoard = { bankRepository.getBoardById(it) },
        )

        assertTrue("Debe detectar al menos 2 palabras completas resueltas", words >= 2)
    }

    @Test
    fun `08 no duplicacion de desbloqueo conserva timestamp original`() {
        val board = bankRepository.getAllBoards().first()
        val progress = listOf(
            CrosswordProgressEntity(
                boardId = board.id,
                category = board.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        )

        val originalTimestamp = 10000000L
        val existingMap = mapOf(
            CruciluxAchievements.ID_FIRST_CROSSWORD to AchievementEntity(
                achievementId = CruciluxAchievements.ID_FIRST_CROSSWORD,
                isUnlocked = true,
                unlockedAt = originalTimestamp,
                currentProgress = 1,
                targetProgress = 1,
                isNotified = true,
            )
        )

        // Evaluar nuevamente tiempo después
        val result = AchievementEngine.evaluate(
            progressList = progress,
            existingEntities = existingMap,
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 20000000L,
        )

        val evaluated = result.first { it.achievementId == CruciluxAchievements.ID_FIRST_CROSSWORD }
        assertTrue(evaluated.isUnlocked)
        assertEquals("El timestamp no debe cambiar", originalTimestamp, evaluated.unlockedAt)
        assertTrue("El estado isNotified debe conservarse", evaluated.isNotified)
    }

    @Test
    fun `09 progreso es monotonico y no retrocede tras reinicio de tablero`() {
        val existingMap = mapOf(
            CruciluxAchievements.ID_WORD_MASTER to AchievementEntity(
                achievementId = CruciluxAchievements.ID_WORD_MASTER,
                isUnlocked = false,
                unlockedAt = null,
                currentProgress = 35,
                targetProgress = 50,
                isNotified = false,
            )
        )

        // Si por alguna razón la lista de progreso temporalmente tiene 0
        val result = AchievementEngine.evaluate(
            progressList = emptyList(),
            existingEntities = existingMap,
            getBoard = { bankRepository.getBoardById(it) },
            nowMs = 5000L,
        )

        val wordMaster = result.first { it.achievementId == CruciluxAchievements.ID_WORD_MASTER }
        assertEquals("El progreso no debe retroceder", 35, wordMaster.currentProgress)
    }

    @Test
    fun `10 toStates genera descripciones accesibles para TalkBack validas`() {
        val entities = listOf(
            AchievementEntity(
                achievementId = CruciluxAchievements.ID_FIRST_CROSSWORD,
                isUnlocked = true,
                unlockedAt = 1700000000000L,
                currentProgress = 1,
                targetProgress = 1,
            ),
            AchievementEntity(
                achievementId = CruciluxAchievements.ID_MASTER_SOLVER,
                isUnlocked = false,
                currentProgress = 12,
                targetProgress = 30,
            ),
        )

        val states = AchievementEngine.toStates(CruciluxAchievements.ALL, entities)
        val firstState = states.first { it.id == CruciluxAchievements.ID_FIRST_CROSSWORD }
        val masterState = states.first { it.id == CruciluxAchievements.ID_MASTER_SOLVER }

        assertTrue(firstState.contentDescription.contains("Primer Crucigrama, desbloqueado"))
        assertTrue(masterState.contentDescription.contains("Maestro de Letras, bloqueado, 12 de 30 crucigramas completados"))
    }
}
