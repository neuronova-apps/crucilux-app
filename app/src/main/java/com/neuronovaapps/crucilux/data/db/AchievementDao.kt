package com.neuronovaapps.crucilux.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para las operaciones sobre logros en Room.
 */
@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAll(): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE achievementId = :id")
    suspend fun getById(id: String): AchievementEntity?

    @Query("SELECT * FROM achievements WHERE achievementId = :id")
    fun observeById(id: String): Flow<AchievementEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: AchievementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<AchievementEntity>): List<Long>

    @Query("UPDATE achievements SET isNotified = 1 WHERE achievementId = :id")
    suspend fun markAsNotified(id: String): Int

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    fun observeUnlockedCount(): Flow<Int>

    @Query("SELECT * FROM achievements WHERE isUnlocked = 1 AND isNotified = 0 ORDER BY unlockedAt ASC")
    fun observeUnnotifiedUnlocked(): Flow<List<AchievementEntity>>

    @Query("DELETE FROM achievements")
    suspend fun clearAll(): Int
}
