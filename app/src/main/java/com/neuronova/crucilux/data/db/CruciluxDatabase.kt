package com.neuronova.crucilux.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local SQLite de Crucilux implementada con Room.
 */
@Database(
    entities = [CrosswordProgressEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CruciluxDatabase : RoomDatabase() {

    abstract fun progressDao(): CrosswordProgressDao

    companion object {
        private const val DATABASE_NAME = "crucilux_progress.db"

        @Volatile
        private var instance: CruciluxDatabase? = null

        fun getInstance(context: Context): CruciluxDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    CruciluxDatabase::class.java,
                    DATABASE_NAME,
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
        }
    }
}
