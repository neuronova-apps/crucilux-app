package com.neuronova.crucilux.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para las operaciones de progreso multi-tablero en Room.
 */
@Dao
interface CrosswordProgressDao {

    @Query("SELECT * FROM crossword_progress WHERE boardId = :boardId")
    suspend fun getProgress(boardId: String): CrosswordProgressEntity?

    @Query("SELECT * FROM crossword_progress WHERE boardId = :boardId")
    fun observeProgress(boardId: String): Flow<CrosswordProgressEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: CrosswordProgressEntity): Long

    @Query("SELECT * FROM crossword_progress")
    suspend fun getAllProgress(): List<CrosswordProgressEntity>

    @Query("SELECT * FROM crossword_progress")
    fun observeAllProgress(): Flow<List<CrosswordProgressEntity>>

    @Query("SELECT * FROM crossword_progress WHERE LOWER(category) = LOWER(:category)")
    suspend fun getProgressByCategory(category: String): List<CrosswordProgressEntity>

    @Query("SELECT * FROM crossword_progress WHERE LOWER(category) = LOWER(:category)")
    fun observeProgressByCategory(category: String): Flow<List<CrosswordProgressEntity>>

    @Query("SELECT * FROM crossword_progress WHERE status = 'IN_PROGRESS' ORDER BY updatedAt DESC")
    suspend fun getInProgressList(): List<CrosswordProgressEntity>

    @Query("SELECT * FROM crossword_progress WHERE status = 'IN_PROGRESS' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getMostRecentInProgress(): CrosswordProgressEntity?

    @Query("SELECT * FROM crossword_progress WHERE status = 'IN_PROGRESS' ORDER BY updatedAt DESC LIMIT 1")
    fun observeMostRecentInProgress(): Flow<CrosswordProgressEntity?>

    @Query("SELECT COUNT(*) FROM crossword_progress WHERE status = 'COMPLETED'")
    suspend fun countCompleted(): Int

    @Query("SELECT COUNT(*) FROM crossword_progress WHERE status = 'IN_PROGRESS'")
    suspend fun countInProgress(): Int

    @Query("SELECT COUNT(*) FROM crossword_progress WHERE LOWER(category) = LOWER(:category) AND status = 'COMPLETED'")
    suspend fun countCompletedByCategory(category: String): Int

    @Query("SELECT COUNT(*) FROM crossword_progress WHERE LOWER(category) = LOWER(:category) AND status = 'IN_PROGRESS'")
    suspend fun countInProgressByCategory(category: String): Int

    @Query("DELETE FROM crossword_progress WHERE boardId = :boardId")
    suspend fun deleteProgress(boardId: String): Int

    @Query("DELETE FROM crossword_progress")
    suspend fun clearAllProgress(): Int
}
