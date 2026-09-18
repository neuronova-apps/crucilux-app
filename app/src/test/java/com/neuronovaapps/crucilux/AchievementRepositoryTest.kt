package com.neuronovaapps.crucilux

import com.neuronovaapps.crucilux.achievements.AchievementRepository
import com.neuronovaapps.crucilux.achievements.CruciluxAchievements
import com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
import com.neuronovaapps.crucilux.data.db.AchievementDao
import com.neuronovaapps.crucilux.data.db.AchievementEntity
import com.neuronovaapps.crucilux.data.db.CrosswordBoardStatus
import com.neuronovaapps.crucilux.data.db.CrosswordProgressEntity
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

/**
 * Fake in-memory DAO para pruebas unitarias de AchievementDao.
 */
class FakeAchievementDao : AchievementDao {
    private val data = mutableMapOf<String, AchievementEntity>()
    private val flow = MutableStateFlow<List<AchievementEntity>>(emptyList())

    private fun notifyFlow() {
        flow.value = data.values.toList()
    }

    override fun observeAll(): Flow<List<AchievementEntity>> = flow.asStateFlow()

    override suspend fun getAll(): List<AchievementEntity> = data.values.toList()

    override suspend fun getById(id: String): AchievementEntity? = data[id]

    override fun observeById(id: String): Flow<AchievementEntity?> {
        return flow.map { list -> list.firstOrNull { it.achievementId == id } }
    }

    override suspend fun insertOrUpdate(entity: AchievementEntity): Long {
        data[entity.achievementId] = entity
        notifyFlow()
        return 1L
    }

    override suspend fun insertOrUpdateAll(entities: List<AchievementEntity>): List<Long> {
        entities.forEach { data[it.achievementId] = it }
        notifyFlow()
        return entities.map { 1L }
    }

    override suspend fun markAsNotified(id: String): Int {
        val existing = data[id] ?: return 0
        data[id] = existing.copy(isNotified = true)
        notifyFlow()
        return 1
    }

    override fun observeUnlockedCount(): Flow<Int> {
        return flow.map { list -> list.count { it.isUnlocked } }
    }

    override fun observeUnnotifiedUnlocked(): Flow<List<AchievementEntity>> {
        return flow.map { list -> list.filter { it.isUnlocked && !it.isNotified } }
    }

    override suspend fun clearAll(): Int {
        val count = data.size
        data.clear()
        notifyFlow()
        return count
    }
}

class AchievementRepositoryTest {

    private lateinit var bankRepository: CruciluxBankRepository
    private lateinit var fakeAchievementDao: FakeAchievementDao
    private lateinit var fakeProgressDao: FakeCrosswordProgressDao
    private lateinit var repository: AchievementRepository

    @Before
    fun setUp() {
        bankRepository = CruciluxBankRepository.getInstance()
        val localAsset = File("src/main/assets/crucilux_bank_v1_37.json")
        val finalAsset = if (localAsset.exists()) localAsset else File("app/src/main/assets/crucilux_bank_v1_37.json")
        FileInputStream(finalAsset).use { stream ->
            bankRepository.loadFromStream(stream)
        }

        fakeAchievementDao = FakeAchievementDao()
        fakeProgressDao = FakeCrosswordProgressDao()

        repository = AchievementRepository(
            achievementDao = fakeAchievementDao,
            progressDao = fakeProgressDao,
            bankRepository = bankRepository,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `01 estado inicial vacio retorna todos los logros bloqueados`() = runBlocking {
        val achievements = repository.getAchievements()
        assertEquals(4, achievements.size)
        assertTrue(achievements.all { !it.isUnlocked })

        val summary = repository.observeSummary().first()
        assertEquals(0, summary.unlockedCount)
        assertEquals(4, summary.totalCount)
        assertEquals(0, summary.progressPercent)
    }

    @Test
    fun `02 evaluateAndSync sincroniza progreso existente en Room y desbloquea Primer Crucigrama`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        fakeProgressDao.insertOrUpdate(
            CrosswordProgressEntity(
                boardId = board.id,
                category = board.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        )

        val updated = repository.evaluateAndSync()
        val firstCrossword = updated.first { it.id == CruciluxAchievements.ID_FIRST_CROSSWORD }
        assertTrue(firstCrossword.isUnlocked)
        assertNotNull(firstCrossword.unlockedAt)

        val summary = repository.observeSummary().first()
        assertEquals(1, summary.unlockedCount)
        assertEquals(4, summary.totalCount)
        assertEquals(25, summary.progressPercent)
    }

    @Test
    fun `03 notificacion de logro se emite solo si no ha sido notificado`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        fakeProgressDao.insertOrUpdate(
            CrosswordProgressEntity(
                boardId = board.id,
                category = board.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        )

        repository.evaluateAndSync()

        val unnotifiedBefore = repository.observeUnnotifiedUnlocked().first()
        assertEquals(1, unnotifiedBefore.size)
        assertEquals(CruciluxAchievements.ID_FIRST_CROSSWORD, unnotifiedBefore.first().id)

        // Marcar como presentado
        repository.markNotificationShown(CruciluxAchievements.ID_FIRST_CROSSWORD)

        val unnotifiedAfter = repository.observeUnnotifiedUnlocked().first()
        assertTrue("No debe haber notificaciones pendientes tras confirmación", unnotifiedAfter.isEmpty())
    }

    @Test
    fun `04 reinicio del repositorio conserva el estado persistido en Room`() = runBlocking {
        val board = bankRepository.getAllBoards().first()
        fakeProgressDao.insertOrUpdate(
            CrosswordProgressEntity(
                boardId = board.id,
                category = board.category,
                status = CrosswordBoardStatus.COMPLETED.name,
                progressPercent = 100,
            )
        )

        repository.evaluateAndSync()
        repository.markNotificationShown(CruciluxAchievements.ID_FIRST_CROSSWORD)

        // Instanciar un nuevo repositorio sobre los mismos DAOs (simulando reinicio de la app)
        val newRepository = AchievementRepository(
            achievementDao = fakeAchievementDao,
            progressDao = fakeProgressDao,
            bankRepository = bankRepository,
            ioDispatcher = Dispatchers.Unconfined,
        )

        val restoredAchievements = newRepository.getAchievements()
        val firstCrossword = restoredAchievements.first { it.id == CruciluxAchievements.ID_FIRST_CROSSWORD }
        assertTrue("Debe seguir desbloqueado tras reiniciar", firstCrossword.isUnlocked)
        assertTrue("Debe conservar que ya fue notificado", firstCrossword.isNotified)

        val pending = newRepository.observeUnnotifiedUnlocked().first()
        assertTrue("No debe generar notificaciones duplicadas tras reiniciar la app", pending.isEmpty())
    }

    @Test
    fun `05 desbloqueo multiple sincroniza todos los logros alcanzados simultaneamente`() = runBlocking {
        val allBoards = bankRepository.getAllBoards()
        val board15x15 = allBoards.first { it.rows >= 15 && it.cols >= 15 }

        // Insertar 30 tableros completados incluyendo el 15x15
        val boardsToComplete = (allBoards.take(29).filter { it.id != board15x15.id } + board15x15).take(30)
        for (b in boardsToComplete) {
            fakeProgressDao.insertOrUpdate(
                CrosswordProgressEntity(
                    boardId = b.id,
                    category = b.category,
                    status = CrosswordBoardStatus.COMPLETED.name,
                    progressPercent = 100,
                )
            )
        }

        repository.evaluateAndSync()

        val achievements = repository.getAchievements()
        val firstCrossword = achievements.first { it.id == CruciluxAchievements.ID_FIRST_CROSSWORD }
        val grandGrid = achievements.first { it.id == CruciluxAchievements.ID_GRAND_GRID }
        val masterSolver = achievements.first { it.id == CruciluxAchievements.ID_MASTER_SOLVER }
        val wordMaster = achievements.first { it.id == CruciluxAchievements.ID_WORD_MASTER }

        assertTrue(firstCrossword.isUnlocked)
        assertTrue(grandGrid.isUnlocked)
        assertTrue(masterSolver.isUnlocked)
        assertTrue("30 tableros con 6-8 palabras superan ampliamente las 50 palabras", wordMaster.isUnlocked)

        val summary = repository.observeSummary().first()
        assertEquals(4, summary.unlockedCount)
        assertEquals(100, summary.progressPercent)
        assertTrue(summary.isAllUnlocked)
    }
}
