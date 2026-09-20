package com.neuronovaapps.crucilux

import com.neuronovaapps.crucilux.achievements.AchievementNotificationManager
import com.neuronovaapps.crucilux.achievements.AchievementRepository
import com.neuronovaapps.crucilux.achievements.CruciluxAchievements
import com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
import com.neuronovaapps.crucilux.data.db.AchievementEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class AchievementNotificationManagerTest {

    private lateinit var bankRepository: CruciluxBankRepository
    private lateinit var fakeDao: FakeAchievementDao
    private lateinit var fakeProgressDao: FakeCrosswordProgressDao
    private lateinit var repository: AchievementRepository

    @Before
    fun setUp() {
        bankRepository = CruciluxBankRepository.getInstance()
        val localAsset = File("src/main/assets/crucilux_bank_v1_37.json")
        val finalAsset = if (localAsset.exists()) localAsset else File("app/src/main/assets/crucilux_bank_v1_37.json")
        if (finalAsset.exists()) {
            FileInputStream(finalAsset).use { stream ->
                bankRepository.loadFromStream(stream)
            }
        }

        fakeDao = FakeAchievementDao()
        fakeProgressDao = FakeCrosswordProgressDao()
        repository = AchievementRepository(
            achievementDao = fakeDao,
            progressDao = fakeProgressDao,
            bankRepository = bankRepository,
            ioDispatcher = Dispatchers.Unconfined,
        )
    }

    @Test
    fun `01 un logro se muestra como activo inmediatamente y marca presentacion en Room`() = runBlocking {
        val unlockTime = 1700000000000L
        fakeDao.insertOrUpdate(
            AchievementEntity(
                achievementId = CruciluxAchievements.ID_FIRST_CROSSWORD,
                isUnlocked = true,
                unlockedAt = unlockTime,
                isNotified = false,
            )
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 200L,
            transitionDelayMs = 20L,
        )

        delay(30L)
        val active = manager.activeAchievement.value
        assertNotNull("El logro debe estar activo", active)
        assertEquals(CruciluxAchievements.ID_FIRST_CROSSWORD, active?.id)

        val entityInDb = fakeDao.getById(CruciluxAchievements.ID_FIRST_CROSSWORD)
        assertNotNull(entityInDb)
        assertTrue("Debe marcarse como notificado en Room al iniciar la presentación", entityInDb!!.isNotified)
        assertEquals("Debe conservar unlockedAt", unlockTime, entityInDb.unlockedAt)

        scope.cancel()
    }

    @Test
    fun `02 desaparicion automatica tras tiempo de presentacion sin dejar rastro visual`() = runBlocking {
        fakeDao.insertOrUpdate(
            AchievementEntity(
                achievementId = CruciluxAchievements.ID_FIRST_CROSSWORD,
                isUnlocked = true,
                unlockedAt = System.currentTimeMillis(),
                isNotified = false,
            )
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 60L,
            transitionDelayMs = 20L,
        )

        delay(20L)
        assertNotNull("Debe estar visible", manager.activeAchievement.value)

        // Esperar que transcurra el tiempo de presentación (60ms) + margen
        delay(90L)
        assertNull("El estado visual debe limpiarse automáticamente", manager.activeAchievement.value)
        assertEquals("La cola debe quedar vacía", 0, manager.getQueuedCount())

        scope.cancel()
    }

    @Test
    fun `03 cierre manual por boton o gesto cancela temporizador y limpia banner de inmediato`() = runBlocking {
        fakeDao.insertOrUpdate(
            AchievementEntity(
                achievementId = CruciluxAchievements.ID_FIRST_CROSSWORD,
                isUnlocked = true,
                unlockedAt = System.currentTimeMillis(),
                isNotified = false,
            )
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 5000L, // Duración larga que no debe esperar
            transitionDelayMs = 20L,
        )

        delay(20L)
        assertNotNull("Debe estar visible inicialmente", manager.activeAchievement.value)

        // Descarte manual
        manager.dismissCurrent()
        delay(20L)

        assertNull("Debe cerrarse de inmediato sin esperar 5000ms", manager.activeAchievement.value)
        scope.cancel()
    }

    @Test
    fun `04 dos logros simultaneos se muestran en cola estrictamente secuencial sin superposicion`() = runBlocking {
        fakeDao.insertOrUpdateAll(
            listOf(
                AchievementEntity(
                    achievementId = CruciluxAchievements.ID_FIRST_CROSSWORD,
                    isUnlocked = true,
                    unlockedAt = 1000L,
                    isNotified = false,
                ),
                AchievementEntity(
                    achievementId = CruciluxAchievements.ID_GRAND_GRID,
                    isUnlocked = true,
                    unlockedAt = 1001L,
                    isNotified = false,
                )
            )
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 70L,
            transitionDelayMs = 25L,
        )

        // 1. Debe aparecer first_crossword
        delay(25L)
        assertEquals(CruciluxAchievements.ID_FIRST_CROSSWORD, manager.activeAchievement.value?.id)
        assertEquals("El segundo logro debe estar en cola de espera", 1, manager.getQueuedCount())

        // 2. Esperar fin de first_crossword y transición
        delay(80L) // 25 + 80 = 105ms (first_crossword expiró a los 70ms)
        // Durante o inmediatamente tras la transición de 25ms
        delay(25L)
        assertEquals("Debe aparecer el segundo logro (grand_grid)", CruciluxAchievements.ID_GRAND_GRID, manager.activeAchievement.value?.id)

        // 3. Esperar fin de grand_grid
        delay(85L)
        assertNull("Ambos deben haber concluido", manager.activeAchievement.value)
        assertEquals(0, manager.getQueuedCount())

        val firstInDb = fakeDao.getById(CruciluxAchievements.ID_FIRST_CROSSWORD)
        val secondInDb = fakeDao.getById(CruciluxAchievements.ID_GRAND_GRID)
        assertTrue(firstInDb!!.isNotified)
        assertTrue(secondInDb!!.isNotified)

        scope.cancel()
    }

    @Test
    fun `05 cuatro logros simultaneos se procesan todos ordenadamente sin superponerse`() = runBlocking {
        fakeDao.insertOrUpdateAll(
            listOf(
                AchievementEntity(CruciluxAchievements.ID_FIRST_CROSSWORD, isUnlocked = true, unlockedAt = 1000L, isNotified = false),
                AchievementEntity(CruciluxAchievements.ID_WORD_MASTER, isUnlocked = true, unlockedAt = 1001L, isNotified = false),
                AchievementEntity(CruciluxAchievements.ID_GRAND_GRID, isUnlocked = true, unlockedAt = 1002L, isNotified = false),
                AchievementEntity(CruciluxAchievements.ID_MASTER_SOLVER, isUnlocked = true, unlockedAt = 1003L, isNotified = false),
            )
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 40L,
            transitionDelayMs = 15L,
        )

        val presentedOrder = mutableListOf<String>()
        var lastSeen: String? = null

        // Muestreo continuo durante el ciclo de vida de los 4 banners (aprox 4 * 55ms = 220ms)
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 400L) {
            val current = manager.activeAchievement.value?.id
            if (current != null && current != lastSeen) {
                presentedOrder.add(current)
                lastSeen = current
            }
            delay(5L)
        }

        assertEquals(4, presentedOrder.size)
        assertEquals(CruciluxAchievements.ID_FIRST_CROSSWORD, presentedOrder[0])
        assertEquals(CruciluxAchievements.ID_WORD_MASTER, presentedOrder[1])
        assertEquals(CruciluxAchievements.ID_GRAND_GRID, presentedOrder[2])
        assertEquals(CruciluxAchievements.ID_MASTER_SOLVER, presentedOrder[3])

        assertNull("Debe culminar con estado visual limpio", manager.activeAchievement.value)
        assertEquals(0, manager.getQueuedCount())

        val allEntities = fakeDao.getAll()
        assertTrue("Todos deben quedar notificados en base de datos", allEntities.all { it.isNotified })

        scope.cancel()
    }

    @Test
    fun `06 cierre manual del primer logro avanza de inmediato al segundo logro`() = runBlocking {
        fakeDao.insertOrUpdateAll(
            listOf(
                AchievementEntity(CruciluxAchievements.ID_FIRST_CROSSWORD, isUnlocked = true, unlockedAt = 1000L, isNotified = false),
                AchievementEntity(CruciluxAchievements.ID_GRAND_GRID, isUnlocked = true, unlockedAt = 1001L, isNotified = false),
            )
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 5000L, // Duración larga
            transitionDelayMs = 20L,
        )

        delay(25L)
        assertEquals(CruciluxAchievements.ID_FIRST_CROSSWORD, manager.activeAchievement.value?.id)

        // Cierre manual de first_crossword
        manager.dismissCurrent()

        // Debe transicionar al segundo logro casi de inmediato
        delay(40L)
        assertEquals("Debe avanzar de inmediato a grand_grid tras descarte manual", CruciluxAchievements.ID_GRAND_GRID, manager.activeAchievement.value?.id)

        // Cierre manual de grand_grid
        manager.dismissCurrent()
        delay(30L)
        assertNull("Debe limpiar estado visual tras descartar el último", manager.activeAchievement.value)

        scope.cancel()
    }

    @Test
    fun `07 actualizacion reactiva de Room durante temporizador no cancela el logro activo`() = runBlocking {
        fakeDao.insertOrUpdate(
            AchievementEntity(CruciluxAchievements.ID_FIRST_CROSSWORD, isUnlocked = true, unlockedAt = 1000L, isNotified = false)
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 120L,
            transitionDelayMs = 20L,
        )

        delay(25L)
        assertEquals(CruciluxAchievements.ID_FIRST_CROSSWORD, manager.activeAchievement.value?.id)

        // Mientras el logro 1 está activo, Room emite un nuevo logro
        fakeDao.insertOrUpdate(
            AchievementEntity(CruciluxAchievements.ID_GRAND_GRID, isUnlocked = true, unlockedAt = 1001L, isNotified = false)
        )

        // Verificar que el logro activo NO fue cancelado por la emisión de Room
        delay(40L)
        assertEquals("first_crossword debe seguir activo y no ser cancelado por Room", CruciluxAchievements.ID_FIRST_CROSSWORD, manager.activeAchievement.value?.id)

        // Esperar que termine first_crossword y empiece grand_grid
        delay(85L)
        assertEquals("Ahora sí debe transicionar al segundo logro", CruciluxAchievements.ID_GRAND_GRID, manager.activeAchievement.value?.id)

        scope.cancel()
    }

    @Test
    fun `08 cancelacion de corrutina nunca deja el banner permanentemente visible`() = runBlocking {
        fakeDao.insertOrUpdate(
            AchievementEntity(CruciluxAchievements.ID_FIRST_CROSSWORD, isUnlocked = true, unlockedAt = 1000L, isNotified = false)
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 5000L,
        )

        delay(25L)
        assertNotNull("Debe estar visible", manager.activeAchievement.value)

        // Cancelar el scope abruptamente (simulando destrucción de Activity o ciclo de vida)
        scope.cancel()
        delay(30L)

        // El finally con NonCancellable garantiza que activeAchievement se limpie a null
        assertNull("La cancelación de corrutina jamás debe dejar el banner visible", manager.activeAchievement.value)
    }

    @Test
    fun `09 recomposicion de Compose mantiene estabilidad del temporizador y flujo`() = runBlocking {
        fakeDao.insertOrUpdate(
            AchievementEntity(CruciluxAchievements.ID_FIRST_CROSSWORD, isUnlocked = true, unlockedAt = 1000L, isNotified = false)
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 100L,
            transitionDelayMs = 20L,
        )

        delay(20L)
        // Simular múltiples recomposiciones leyendo repetidamente el StateFlow
        for (i in 1..5) {
            val state = manager.activeAchievement.value
            assertEquals(CruciluxAchievements.ID_FIRST_CROSSWORD, state?.id)
            delay(10L)
        }

        // El temporizador original debe continuar sin resetearse
        delay(70L)
        assertNull("El banner debe desaparecer al cumplirse el tiempo original sin reiniciarse por recomposiciones", manager.activeAchievement.value)

        scope.cancel()
    }

    @Test
    fun `10 simulacion de reinicio de la app no vuelve a mostrar logros ya notificados`() = runBlocking {
        // Sesión 1: Se desbloquean dos logros
        fakeDao.insertOrUpdateAll(
            listOf(
                AchievementEntity(CruciluxAchievements.ID_FIRST_CROSSWORD, isUnlocked = true, unlockedAt = 1000L, isNotified = false),
                AchievementEntity(CruciluxAchievements.ID_GRAND_GRID, isUnlocked = true, unlockedAt = 1001L, isNotified = false),
            )
        )

        val session1Scope = CoroutineScope(Job() + Dispatchers.Default)
        val session1Manager = AchievementNotificationManager(
            repository = repository,
            scope = session1Scope,
            displayDurationMs = 60L,
            transitionDelayMs = 20L,
        )

        // Esperar que first_crossword aparezca y sea marcado en Room
        delay(25L)
        assertEquals(CruciluxAchievements.ID_FIRST_CROSSWORD, session1Manager.activeAchievement.value?.id)

        // Simular interrupción / cierre abrupto de la app antes de que se muestre grand_grid
        session1Scope.cancel()
        delay(20L)

        // Verificar estado de persistencia en Room tras cierre abrupto:
        // first_crossword ya fue marcado (isNotified = true)
        // grand_grid no ha sido presentado aún (isNotified = false)
        val firstCrosswordEntity = fakeDao.getById(CruciluxAchievements.ID_FIRST_CROSSWORD)
        val grandGridEntity = fakeDao.getById(CruciluxAchievements.ID_GRAND_GRID)
        assertTrue(firstCrosswordEntity!!.isNotified)
        assertFalse(grandGridEntity!!.isNotified)

        // Sesión 2: Usuario reabre la app
        val session2Scope = CoroutineScope(Job() + Dispatchers.Default)
        val session2Manager = AchievementNotificationManager(
            repository = repository,
            scope = session2Scope,
            displayDurationMs = 60L,
            transitionDelayMs = 20L,
        )

        delay(25L)
        // first_crossword NO debe volver a aparecer; únicamente grand_grid
        assertEquals("En la nueva sesión solo debe aparecer el logro no notificado (grand_grid)", CruciluxAchievements.ID_GRAND_GRID, session2Manager.activeAchievement.value?.id)

        // Dejar que termine grand_grid
        delay(80L)
        assertNull(session2Manager.activeAchievement.value)
        session2Scope.cancel()

        // Sesión 3: Reabrir de nuevo -> 0 notificaciones
        val unnotified = repository.observeUnnotifiedUnlocked().first()
        assertTrue("No deben quedar notificaciones pendientes en Room", unnotified.isEmpty())
    }

    @Test
    fun `11 ausencia de banners duplicados ante emisiones repetidas de Room`() = runBlocking {
        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 80L,
            transitionDelayMs = 20L,
        )

        // Emisiones repetidas de la misma entidad
        for (i in 1..5) {
            fakeDao.insertOrUpdate(
                AchievementEntity(CruciluxAchievements.ID_FIRST_CROSSWORD, isUnlocked = true, unlockedAt = 1000L, isNotified = false)
            )
            delay(5L)
        }

        delay(20L)
        assertEquals(CruciluxAchievements.ID_FIRST_CROSSWORD, manager.activeAchievement.value?.id)
        assertEquals("No debe haber duplicados en cola", 0, manager.getQueuedCount())

        delay(90L)
        assertNull(manager.activeAchievement.value)
        assertEquals("No deben surgir banners adicionales de la emisión repetida", 0, manager.getQueuedCount())

        scope.cancel()
    }

    @Test
    fun `12 ausencia de notificaciones atascadas y cola siempre vacia al finalizar`() = runBlocking {
        fakeDao.insertOrUpdateAll(
            listOf(
                AchievementEntity(CruciluxAchievements.ID_FIRST_CROSSWORD, isUnlocked = true, unlockedAt = 1000L, isNotified = false),
                AchievementEntity(CruciluxAchievements.ID_GRAND_GRID, isUnlocked = true, unlockedAt = 1001L, isNotified = false),
            )
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 30L,
            transitionDelayMs = 10L,
        )

        // Esperar que complete todo el ciclo
        delay(120L)

        assertNull("El estado visual debe ser nulo", manager.activeAchievement.value)
        assertEquals("La cola debe quedar completamente drenada", 0, manager.getQueuedCount())

        scope.cancel()
    }

    @Test
    fun `13 preservacion de timestamp unlockedAt y reglas de XP intactas`() = runBlocking {
        val fixedTimestamp = 1695000000000L
        fakeDao.insertOrUpdate(
            AchievementEntity(
                achievementId = CruciluxAchievements.ID_FIRST_CROSSWORD,
                isUnlocked = true,
                unlockedAt = fixedTimestamp,
                currentProgress = 1,
                targetProgress = 1,
                isNotified = false,
            )
        )

        val scope = CoroutineScope(Job() + Dispatchers.Default)
        val manager = AchievementNotificationManager(
            repository = repository,
            scope = scope,
            displayDurationMs = 40L,
            transitionDelayMs = 10L,
        )

        delay(60L)

        val entity = fakeDao.getById(CruciluxAchievements.ID_FIRST_CROSSWORD)
        assertNotNull(entity)
        assertEquals("Timestamp original debe preservarse exactamente", fixedTimestamp, entity?.unlockedAt)
        assertTrue("isUnlocked debe seguir siendo true", entity?.isUnlocked == true)
        assertTrue("isNotified debe ser true tras ser mostrado", entity?.isNotified == true)
        assertEquals("Las reglas de XP de los logros se mantienen en 0", 0, CruciluxAchievements.FIRST_CROSSWORD.xpReward)

        scope.cancel()
    }
}
