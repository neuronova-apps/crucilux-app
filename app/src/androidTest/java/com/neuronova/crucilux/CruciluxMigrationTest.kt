package com.neuronova.crucilux

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.neuronova.crucilux.data.db.CrosswordBoardStatus
import com.neuronova.crucilux.data.db.CruciluxDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CruciluxMigrationTest {
    private val databaseName = "migration-1-2-test.db"
    private lateinit var context: Context

    @Before
    fun prepare() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(databaseName)
    }

    @After
    fun cleanUp() {
        context.deleteDatabase(databaseName)
    }

    @Test
    fun migration_1_2_preserves_progress_letters_and_mode_without_data_loss() = runBlocking {
        val legacy = context.openOrCreateDatabase(databaseName, Context.MODE_PRIVATE, null)
        legacy.execSQL(
            "CREATE TABLE crossword_progress (" +
                "boardId TEXT NOT NULL, category TEXT NOT NULL, status TEXT NOT NULL, " +
                "progressPercent INTEGER NOT NULL, userLetters TEXT NOT NULL, " +
                "selectedRow INTEGER NOT NULL, selectedCol INTEGER NOT NULL, " +
                "selectedDirection TEXT NOT NULL, checkMode TEXT NOT NULL, " +
                "updatedAt INTEGER NOT NULL, PRIMARY KEY(boardId))"
        )
        legacy.execSQL(
            "INSERT INTO crossword_progress VALUES " +
                "('7X7-01','Cultura general','IN_PROGRESS',42,'0,0,A;0,1,B',2,3,'V','ASSISTED',1000)," +
                "('7X7-02','Cultura general','COMPLETED',100,'0,0,C',0,0,'H','CLASSIC',2000)"
        )
        legacy.version = 1
        legacy.close()

        val migrated = Room.databaseBuilder(context, CruciluxDatabase::class.java, databaseName)
            .addMigrations(CruciluxDatabase.MIGRATION_1_2)
            .build()

        val inProgress = migrated.progressDao().getProgress("7X7-01")!!
        val completed = migrated.progressDao().getProgress("7X7-02")!!

        assertEquals(CrosswordBoardStatus.IN_PROGRESS, inProgress.boardStatus)
        assertEquals(mapOf(Pair(0, 0) to 'A', Pair(0, 1) to 'B'), inProgress.parseUserLetters())
        assertEquals("ASSISTED", inProgress.checkMode)
        assertEquals(0, inProgress.hintsUsed)
        assertEquals(0, inProgress.bestXpEarned)
        assertTrue(inProgress.parseHintRevealedCells().isEmpty())
        assertEquals(CrosswordBoardStatus.COMPLETED, completed.boardStatus)
        assertEquals(0, migrated.playerProfileDao().getProfile()?.totalXp)
        migrated.close()
    }
}
