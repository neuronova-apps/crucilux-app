package com.neuronovaapps.crucilux.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para el Desafío Diario en Room.
 * Utiliza primitivas no destructivas de INSERT IGNORE y UPDATE
 * para evitar semánticas de reemplazo destructivo sobre registros diarios persistentes.
 */
@Dao
interface DailyChallengeDao {

    @Query("SELECT * FROM daily_challenge WHERE dateKey = :dateKey")
    suspend fun getChallenge(dateKey: String): DailyChallengeEntity?

    @Query("SELECT * FROM daily_challenge WHERE dateKey = :dateKey")
    fun observeChallenge(dateKey: String): Flow<DailyChallengeEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(entity: DailyChallengeEntity): Long

    @Update
    suspend fun update(entity: DailyChallengeEntity): Int

    @Query("SELECT * FROM daily_challenge WHERE status = 'COMPLETED' ORDER BY dateKey ASC")
    suspend fun getAllCompleted(): List<DailyChallengeEntity>

    @Query("SELECT * FROM daily_challenge WHERE status = 'COMPLETED' ORDER BY dateKey ASC")
    fun observeAllCompleted(): Flow<List<DailyChallengeEntity>>

    @Query("SELECT * FROM daily_challenge ORDER BY dateKey DESC")
    suspend fun getAllChallenges(): List<DailyChallengeEntity>

    @Query("SELECT * FROM daily_challenge ORDER BY dateKey DESC")
    fun observeAllChallenges(): Flow<List<DailyChallengeEntity>>

    @Query("SELECT COUNT(*) FROM daily_challenge WHERE status = 'COMPLETED'")
    suspend fun countCompleted(): Int

    @Query("SELECT COUNT(*) FROM daily_challenge WHERE status = 'COMPLETED'")
    fun observeCompletedCount(): Flow<Int>

    @Query("DELETE FROM daily_challenge WHERE dateKey = :dateKey")
    suspend fun deleteChallenge(dateKey: String): Int
}
