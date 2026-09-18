package com.neuronovaapps.crucilux.achievements

import android.content.Context
import android.util.Log
import com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
import com.neuronovaapps.crucilux.data.db.AchievementDao
import com.neuronovaapps.crucilux.data.db.CrosswordProgressDao
import com.neuronovaapps.crucilux.data.db.CruciluxDatabase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Repositorio centralizado de Logros y Medallas de Crucilux respaldado por Room.
 * Actúa como la única fuente de verdad para el estado de desbloqueo, progreso y notificaciones.
 */
class AchievementRepository(
    private val achievementDao: AchievementDao,
    private val progressDao: CrosswordProgressDao,
    private val bankRepository: CruciluxBankRepository = CruciluxBankRepository.getInstance(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val evaluateMutex = Mutex()

    /**
     * Observa en tiempo real la lista completa de logros con su estado de desbloqueo y progreso.
     */
    fun observeAchievements(): Flow<List<AchievementState>> {
        return achievementDao.observeAll().map { entities ->
            AchievementEngine.toStates(CruciluxAchievements.ALL, entities)
        }.distinctUntilChanged()
    }

    /**
     * Observa el resumen global de logros (X de 4 desbloqueados y porcentaje).
     */
    fun observeSummary(): Flow<AchievementSummary> {
        return achievementDao.observeUnlockedCount().map { count ->
            AchievementSummary(
                unlockedCount = count,
                totalCount = CruciluxAchievements.ALL.size,
            )
        }.distinctUntilChanged()
    }

    /**
     * Observa los logros que se encuentran desbloqueados pero cuya notificación
     * aún no ha sido presentada al usuario.
     */
    fun observeUnnotifiedUnlocked(): Flow<List<AchievementState>> {
        return achievementDao.observeUnnotifiedUnlocked().map { entities ->
            AchievementEngine.toStates(CruciluxAchievements.ALL, entities)
                .filter { it.isUnlocked && !it.isNotified }
        }.distinctUntilChanged()
    }

    /**
     * Obtiene de forma síncrona/suspendida la lista actual de logros.
     */
    suspend fun getAchievements(): List<AchievementState> = withContext(ioDispatcher) {
        val entities = achievementDao.getAll()
        AchievementEngine.toStates(CruciluxAchievements.ALL, entities)
    }

    /**
     * Evalúa el progreso actual en Room frente al catálogo de logros y actualiza la base de datos.
     * Es idempotente y previene condiciones de carrera mediante [evaluateMutex].
     */
    suspend fun evaluateAndSync(): List<AchievementState> = withContext(ioDispatcher) {
        evaluateMutex.withLock {
            try {
                val progressList = progressDao.getAllProgress()
                val existingEntities = achievementDao.getAll().associateBy { it.achievementId }

                val updatedEntities = AchievementEngine.evaluate(
                    definitions = CruciluxAchievements.ALL,
                    progressList = progressList,
                    existingEntities = existingEntities,
                    getBoard = { boardId -> bankRepository.getBoardById(boardId) },
                )

                achievementDao.insertOrUpdateAll(updatedEntities)
                AchievementEngine.toStates(CruciluxAchievements.ALL, updatedEntities)
            } catch (exception: Exception) {
                Log.e(TAG, "Error evaluando logros", exception)
                getAchievements()
            }
        }
    }

    /**
     * Marca un logro como notificado después de que su presentación visual haya iniciado
     * o concluido exitosamente en la UI.
     */
    suspend fun markNotificationShown(achievementId: String) = withContext(ioDispatcher) {
        try {
            achievementDao.markAsNotified(achievementId)
        } catch (exception: Exception) {
            Log.w(TAG, "No se pudo marcar como notificado el logro $achievementId", exception)
        }
    }

    companion object {
        private const val TAG = "AchievementRepository"

        @Volatile
        private var instance: AchievementRepository? = null

        fun getInstance(context: Context): AchievementRepository {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val db = CruciluxDatabase.getInstance(context)
                    AchievementRepository(
                        achievementDao = db.achievementDao(),
                        progressDao = db.progressDao(),
                        bankRepository = CruciluxBankRepository.getInstance(),
                    ).also { instance = it }
                }
            }
        }
    }
}
