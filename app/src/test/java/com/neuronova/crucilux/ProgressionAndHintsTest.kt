package com.neuronova.crucilux

import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.data.db.CrosswordBoardStatus
import com.neuronova.crucilux.data.db.CrosswordProgressEntity
import com.neuronova.crucilux.data.db.PlayerProfileDao
import com.neuronova.crucilux.data.db.PlayerProfileEntity
import com.neuronova.crucilux.data.repository.CrosswordProgressRepository
import com.neuronova.crucilux.progression.GameStartRules
import com.neuronova.crucilux.progression.HintRules
import com.neuronova.crucilux.progression.PlayerLevel
import com.neuronova.crucilux.progression.XpCalculator
import com.neuronova.crucilux.ui.game.CheckMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

private class FakePlayerProfileDao : PlayerProfileDao {
    private val profile = MutableStateFlow<PlayerProfileEntity?>(null)

    override suspend fun getProfile(): PlayerProfileEntity? = profile.value
    override fun observeProfile(): Flow<PlayerProfileEntity?> = profile
    override suspend fun insertOrUpdate(profile: PlayerProfileEntity): Long {
        this.profile.value = profile
        return 1L
    }
}

class ProgressionAndHintsTest {
    private lateinit var bank: CruciluxBankRepository
    private lateinit var progressDao: FakeCrosswordProgressDao
    private lateinit var profileDao: FakePlayerProfileDao
    private lateinit var repository: CrosswordProgressRepository

    @Before
    fun setUp() {
        bank = CruciluxBankRepository.getInstance()
        val local = File("src/main/assets/crucilux_bank_v1_37.json")
        val asset = if (local.exists()) local else File("app/src/main/assets/crucilux_bank_v1_37.json")
        FileInputStream(asset).use(bank::loadFromStream)
        progressDao = FakeCrosswordProgressDao()
        profileDao = FakePlayerProfileDao()
        repository = CrosswordProgressRepository(
            dao = progressDao,
            bankRepository = bank,
            playerProfileDao = profileDao,
        )
    }

    @Test
    fun `banco v1_37 tiene distribucion real 200 de seis y 100 de ocho`() {
        val frequencies = bank.getAllBoards().groupingBy { it.entries.size }.eachCount()
        assertEquals(mapOf(6 to 200, 8 to 100), frequencies)
        assertEquals(6, frequencies.keys.min())
        assertEquals(8, frequencies.keys.max())
        assertEquals(2_000, bank.getAllBoards().sumOf { it.entries.size })
    }

    @Test
    fun `bandas XP respetan cantidad objetiva de respuestas`() {
        assertEquals(100, XpCalculator.baseXp(6))
        assertEquals(120, XpCalculator.baseXp(7))
        assertEquals(120, XpCalculator.baseXp(8))
        assertEquals(140, XpCalculator.baseXp(9))
    }

    @Test
    fun `XP maximo teorico v1_37 es 32000`() {
        assertEquals(32_000, bank.getAllBoards().sumOf { XpCalculator.baseXp(it.entries.size) })
    }

    @Test
    fun `clasica entrega cien por ciento y asistida ochenta por ciento`() {
        assertEquals(120, XpCalculator.modeXp(8, CheckMode.CLASSIC))
        assertEquals(96, XpCalculator.modeXp(8, CheckMode.ASSISTED))
    }

    @Test
    fun `pistas iniciales descuentan 10 y adicionales 20`() {
        assertEquals(0, XpCalculator.hintPenalty(0))
        assertEquals(10, XpCalculator.hintPenalty(1))
        assertEquals(30, XpCalculator.hintPenalty(3))
        assertEquals(50, XpCalculator.hintPenalty(4))
        assertEquals(70, XpCalculator.hintPenalty(5))
    }

    @Test
    fun `XP final nunca es negativo`() {
        assertEquals(0, XpCalculator.finalXp(6, CheckMode.ASSISTED, 20))
    }

    @Test
    fun `reglas de pista rechazan correcta validada o ya revelada`() {
        assertFalse(HintRules.canReveal(true, 'A', 'A', false, false))
        assertFalse(HintRules.canReveal(true, null, 'A', true, false))
        assertFalse(HintRules.canReveal(true, null, 'A', false, true))
        assertFalse(HintRules.canReveal(false, null, 'A', false, false))
        assertTrue(HintRules.canReveal(true, 'X', 'A', false, false))
        assertTrue(HintRules.isProtected(isValidated = false, isHintRevealed = true))
        assertTrue(HintRules.isProtected(isValidated = true, isHintRevealed = false))
        assertFalse(HintRules.isProtected(isValidated = false, isHintRevealed = false))
    }

    @Test
    fun `cuarta pista exige confirmacion y cancelar no cambia reglas`() {
        assertFalse(HintRules.requiresAdditionalConfirmation(2))
        assertTrue(HintRules.requiresAdditionalConfirmation(3))
        assertTrue(HintRules.requiresAdditionalConfirmation(4))
    }

    @Test
    fun `posiciones reveladas se serializan sin guardar soluciones`() {
        val positions = setOf(Pair(4, 2), Pair(0, 1), Pair(2, 3))
        val raw = CrosswordProgressEntity.serializePositions(positions)
        assertEquals("0,1;2,3;4,2", raw)
        assertEquals(positions, CrosswordProgressEntity.deserializePositions(raw))
        assertFalse(raw.contains("answer", ignoreCase = true))
    }

    @Test
    fun `NOT_STARTED solicita modo e IN_PROGRESS no lo solicita`() {
        assertTrue(GameStartRules.shouldRequestMode(CrosswordBoardStatus.NOT_STARTED))
        assertFalse(GameStartRules.shouldRequestMode(CrosswordBoardStatus.IN_PROGRESS))
        assertFalse(GameStartRules.shouldRequestMode(CrosswordBoardStatus.COMPLETED))
    }

    @Test
    fun `modo clasica persiste al iniciar`() = runBlocking {
        val board = bank.getAllBoards()[0]
        val progress = repository.startBoard(board.id, board.category, CheckMode.CLASSIC)
        assertEquals(CrosswordBoardStatus.IN_PROGRESS, progress.status)
        assertEquals(CheckMode.CLASSIC, repository.getProgress(board.id).checkMode)
    }

    @Test
    fun `modo asistida persiste y no cambia durante partida`() = runBlocking {
        val board = bank.getAllBoards()[1]
        repository.startBoard(board.id, board.category, CheckMode.ASSISTED)
        repository.startBoard(board.id, board.category, CheckMode.CLASSIC)
        assertEquals(CheckMode.ASSISTED, repository.getProgress(board.id).checkMode)
    }

    @Test
    fun `pistas y celdas reveladas persisten`() = runBlocking {
        val board = bank.getAllBoards()[2]
        repository.startBoard(board.id, board.category, CheckMode.CLASSIC)
        repository.saveProgress(
            boardId = board.id,
            category = board.category,
            userLetters = mapOf(Pair(0, 0) to 'A'),
            grid = null,
            hintsUsed = 3,
            hintRevealedCells = setOf(Pair(0, 0), Pair(2, 1)),
        )
        val restored = repository.getProgress(board.id)
        assertEquals(3, restored.hintsUsed)
        assertEquals(setOf(Pair(0, 0), Pair(2, 1)), restored.hintRevealedCells)
    }

    @Test
    fun `completar acredita una vez y mejor marca solo diferencia`() = runBlocking {
        val board = bank.getAllBoards()[3]
        repository.startBoard(board.id, board.category, CheckMode.CLASSIC)

        val first = repository.saveProgress(
            board.id, board.category, emptyMap(), null,
            isCompletedOverride = true, awardCompletion = true, xpFinal = 80,
        )!!
        val better = repository.saveProgress(
            board.id, board.category, emptyMap(), null,
            isCompletedOverride = true, awardCompletion = true, xpFinal = 105,
        )!!
        val lower = repository.saveProgress(
            board.id, board.category, emptyMap(), null,
            isCompletedOverride = true, awardCompletion = true, xpFinal = 90,
        )!!

        assertEquals(80, first.xpAwarded)
        assertEquals(25, better.xpAwarded)
        assertEquals(0, lower.xpAwarded)
        assertEquals(105, repository.getProgress(board.id).bestXpEarned)
        assertEquals(105, repository.getPlayerProgress().totalXp)
    }

    @Test
    fun `reinicio conserva best XP y permite escoger nuevo modo`() = runBlocking {
        val board = bank.getAllBoards()[4]
        repository.startBoard(board.id, board.category, CheckMode.CLASSIC)
        repository.saveProgress(
            board.id, board.category, emptyMap(), null,
            isCompletedOverride = true, awardCompletion = true, xpFinal = 100,
        )
        repository.resetBoardProgress(board.id, board.category)
        val reset = repository.getProgress(board.id)
        assertEquals(CrosswordBoardStatus.NOT_STARTED, reset.status)
        assertEquals(100, reset.bestXpEarned)

        repository.startBoard(board.id, board.category, CheckMode.ASSISTED)
        assertEquals(CheckMode.ASSISTED, repository.getProgress(board.id).checkMode)
    }

    @Test
    fun `niveles se calculan en todos los umbrales`() {
        PlayerLevel.entries.forEach { level ->
            assertEquals(level, PlayerLevel.forXp(level.minimumXp))
        }
        assertEquals(PlayerLevel.NOVATO, PlayerLevel.forXp(-1))
        assertEquals(PlayerLevel.LEYENDA_CRUCILUX, PlayerLevel.forXp(32_000))
    }
}
