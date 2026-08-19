package com.neuronova.crucilux.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Base de datos local SQLite de Crucilux implementada con Room.
 */
@Database(
    entities = [CrosswordProgressEntity::class, PlayerProfileEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class CruciluxDatabase : RoomDatabase() {

    abstract fun progressDao(): CrosswordProgressDao
    abstract fun playerProfileDao(): PlayerProfileDao

    companion object {
        private const val DATABASE_NAME = "crucilux_progress.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE crossword_progress ADD COLUMN hintsUsed INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE crossword_progress ADD COLUMN bestXpEarned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE crossword_progress ADD COLUMN hintRevealedCells TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS player_profile " +
                        "(id INTEGER NOT NULL, totalXp INTEGER NOT NULL, PRIMARY KEY(id))"
                )
                db.execSQL("INSERT OR IGNORE INTO player_profile(id, totalXp) VALUES(1, 0)")
            }
        }

        @Volatile
        private var instance: CruciluxDatabase? = null

        fun getInstance(context: Context): CruciluxDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CruciluxDatabase::class.java,
                    DATABASE_NAME,
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}
