package com.neuronovaapps.crucilux

import androidx.sqlite.db.SupportSQLiteDatabase
import com.neuronovaapps.crucilux.data.db.AchievementEntity
import com.neuronovaapps.crucilux.data.db.CrosswordBoardStatus
import com.neuronovaapps.crucilux.data.db.DailyChallengeEntity
import com.neuronovaapps.crucilux.data.db.DailyChallengeStatus
import com.neuronovaapps.crucilux.data.db.CruciluxDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Prueba unitaria específica de migración de base de datos Room de versión 3 a versión 4.
 *
 * Verifica exhaustivamente:
 * - Base de datos versión 3 con tablas 'crossword_progress', 'player_profile' y 'achievements' pobladas.
 * - Ejecución de [CruciluxDatabase.MIGRATION_3_4].
 * - Conservación íntegra de progreso, perfil, XP y logros sin borrado destructivo.
 * - Creación correcta de la tabla 'daily_challenge' con todas sus columnas y clave primaria 'dateKey'.
 * - Ausencia total de sentencias destructivas (sin DROP TABLE ni DELETE).
 * - Correcta inserción y lectura de [DailyChallengeEntity].
 */
class CruciluxDatabaseMigration3To4Test {

    private val executedSqlStatements = mutableListOf<String>()
    private val tablesInDb = mutableMapOf<String, MutableList<Map<String, Any?>>>()

    private lateinit var mockDb: SupportSQLiteDatabase

    @Before
    fun setUp() {
        executedSqlStatements.clear()
        tablesInDb.clear()

        // 1. Simular base de datos en Versión 3 con datos existentes
        tablesInDb["crossword_progress"] = mutableListOf(
            mapOf(
                "boardId" to "7X7-01",
                "category" to "Cultura general",
                "status" to CrosswordBoardStatus.COMPLETED.name,
                "progressPercent" to 100,
                "userLetters" to "0,0:A;0,1:B",
                "selectedRow" to 0,
                "selectedCol" to 0,
                "selectedDirection" to "H",
                "checkMode" to "CLASSIC",
                "hintsUsed" to 0,
                "bestXpEarned" to 140,
                "hintRevealedCells" to "",
                "updatedAt" to 1700000000000L,
            ),
            mapOf(
                "boardId" to "10X10-05",
                "category" to "Ciencia",
                "status" to CrosswordBoardStatus.IN_PROGRESS.name,
                "progressPercent" to 50,
                "userLetters" to "1,1:C",
                "selectedRow" to 1,
                "selectedCol" to 1,
                "selectedDirection" to "V",
                "checkMode" to "ASSISTED",
                "hintsUsed" to 1,
                "bestXpEarned" to 0,
                "hintRevealedCells" to "1,1",
                "updatedAt" to 1700000050000L,
            ),
        )

        tablesInDb["player_profile"] = mutableListOf(
            mapOf(
                "id" to 1,
                "totalXp" to 650,
            ),
        )

        tablesInDb["achievements"] = mutableListOf(
            mapOf(
                "achievementId" to "first_crossword",
                "isUnlocked" to 1,
                "unlockedAt" to 1700000000000L,
                "currentProgress" to 1,
                "targetProgress" to 1,
                "isNotified" to 1,
            ),
            mapOf(
                "achievementId" to "word_master",
                "isUnlocked" to 0,
                "unlockedAt" to null,
                "currentProgress" to 25,
                "targetProgress" to 50,
                "isNotified" to 0,
            ),
        )

        val handler = java.lang.reflect.InvocationHandler { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    val sql = args[0] as String
                    executedSqlStatements.add(sql)

                    if (sql.contains("CREATE TABLE", ignoreCase = true) && sql.contains("daily_challenge", ignoreCase = true)) {
                        tablesInDb.getOrPut("daily_challenge") { mutableListOf() }
                    }
                    null
                }
                "getVersion" -> 3
                "isDatabaseIntegrityOk" -> true
                else -> null
            }
        }

        mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
            handler,
        ) as SupportSQLiteDatabase
    }

    @Test
    fun `MIGRATION_3_4 tiene versiones de inicio 3 y fin 4`() {
        assertEquals(3, CruciluxDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, CruciluxDatabase.MIGRATION_3_4.endVersion)
    }

    @Test
    fun `MIGRATION_3_4 crea la tabla daily_challenge con todas las columnas y sin destructividad`() {
        assertFalse("daily_challenge no debe existir antes de la migración", tablesInDb.containsKey("daily_challenge"))

        CruciluxDatabase.MIGRATION_3_4.migrate(mockDb)

        assertTrue("daily_challenge debe existir tras la migración", tablesInDb.containsKey("daily_challenge"))
        assertEquals(1, executedSqlStatements.size)

        val createSql = executedSqlStatements.first()
        assertTrue(createSql.contains("CREATE TABLE IF NOT EXISTS daily_challenge", ignoreCase = true))
        assertTrue(createSql.contains("dateKey TEXT NOT NULL PRIMARY KEY", ignoreCase = true))
        assertTrue(createSql.contains("boardId TEXT NOT NULL", ignoreCase = true))
        assertTrue(createSql.contains("status TEXT NOT NULL", ignoreCase = true))
        assertTrue(createSql.contains("startedAt INTEGER", ignoreCase = true))
        assertTrue(createSql.contains("completedAt INTEGER", ignoreCase = true))
        assertTrue(createSql.contains("bestTimeSeconds INTEGER", ignoreCase = true))
        assertTrue(createSql.contains("elapsedTimeSeconds INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue(createSql.contains("attemptCount INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue(createSql.contains("isRewardClaimed INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue(createSql.contains("userLetters TEXT NOT NULL DEFAULT ''", ignoreCase = true))
        assertTrue(createSql.contains("progressPercent INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue(createSql.contains("hintsUsed INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue(createSql.contains("hintRevealedCells TEXT NOT NULL DEFAULT ''", ignoreCase = true))
        assertTrue(createSql.contains("checkMode TEXT NOT NULL DEFAULT 'CLASSIC'", ignoreCase = true))
        assertTrue(createSql.contains("selectedRow INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue(createSql.contains("selectedCol INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue(createSql.contains("selectedDirection TEXT NOT NULL DEFAULT 'H'", ignoreCase = true))

        // Confirmar ausencia de sentencias destructivas
        for (sql in executedSqlStatements) {
            assertFalse(sql.contains("DROP TABLE", ignoreCase = true))
            assertFalse(sql.contains("DELETE FROM", ignoreCase = true))
        }
    }

    @Test
    fun `MIGRATION_3_4 conserva integramente el progreso existente sin alteraciones`() {
        CruciluxDatabase.MIGRATION_3_4.migrate(mockDb)

        val progressRows = tablesInDb["crossword_progress"]
        assertNotNull(progressRows)
        assertEquals(2, progressRows?.size)

        val completed = progressRows?.first { it["boardId"] == "7X7-01" }
        assertEquals("Cultura general", completed?.get("category"))
        assertEquals(CrosswordBoardStatus.COMPLETED.name, completed?.get("status"))
        assertEquals(100, completed?.get("progressPercent"))
        assertEquals(140, completed?.get("bestXpEarned"))
        assertEquals(0, completed?.get("hintsUsed"))

        val inProgress = progressRows?.first { it["boardId"] == "10X10-05" }
        assertEquals(CrosswordBoardStatus.IN_PROGRESS.name, inProgress?.get("status"))
        assertEquals(50, inProgress?.get("progressPercent"))
    }

    @Test
    fun `MIGRATION_3_4 conserva integramente el perfil de jugador y totalXp`() {
        CruciluxDatabase.MIGRATION_3_4.migrate(mockDb)

        val profile = tablesInDb["player_profile"]?.first()
        assertEquals(1, profile?.get("id"))
        assertEquals(650, profile?.get("totalXp"))
    }

    @Test
    fun `MIGRATION_3_4 conserva integramente los logros desbloqueados y su estado isNotified`() {
        CruciluxDatabase.MIGRATION_3_4.migrate(mockDb)

        val achievements = tablesInDb["achievements"]
        assertNotNull(achievements)
        assertEquals(2, achievements?.size)

        val firstCrossword = achievements?.first { it["achievementId"] == "first_crossword" }
        assertEquals(1, firstCrossword?.get("isUnlocked"))
        assertEquals(1700000000000L, firstCrossword?.get("unlockedAt"))
        assertEquals(1, firstCrossword?.get("isNotified"))

        val wordMaster = achievements?.first { it["achievementId"] == "word_master" }
        assertEquals(0, wordMaster?.get("isUnlocked"))
        assertEquals(25, wordMaster?.get("currentProgress"))
        assertEquals(0, wordMaster?.get("isNotified"))
    }

    @Test
    fun `tabla daily_challenge permite insercion y lectura valida de DailyChallengeEntity`() {
        CruciluxDatabase.MIGRATION_3_4.migrate(mockDb)

        val sample = DailyChallengeEntity(
            dateKey = "2026-09-18",
            boardId = "7X7-01",
            status = DailyChallengeStatus.COMPLETED.name,
            startedAt = 1700000000000L,
            completedAt = 1700000600000L,
            bestTimeSeconds = 600L,
            attemptCount = 1,
            isRewardClaimed = false,
            userLetters = "0,0:A;0,1:B",
            progressPercent = 100,
        )

        tablesInDb["daily_challenge"]?.add(
            mapOf(
                "dateKey" to sample.dateKey,
                "boardId" to sample.boardId,
                "status" to sample.status,
                "startedAt" to sample.startedAt,
                "completedAt" to sample.completedAt,
                "bestTimeSeconds" to sample.bestTimeSeconds,
                "attemptCount" to sample.attemptCount,
                "isRewardClaimed" to if (sample.isRewardClaimed) 1 else 0,
                "userLetters" to sample.userLetters,
                "progressPercent" to sample.progressPercent,
            )
        )

        val rows = tablesInDb["daily_challenge"]
        assertNotNull(rows)
        assertEquals(1, rows?.size)

        val inserted = rows?.first()
        assertEquals("2026-09-18", inserted?.get("dateKey"))
        assertEquals("7X7-01", inserted?.get("boardId"))
        assertEquals(DailyChallengeStatus.COMPLETED.name, inserted?.get("status"))
        assertEquals(1700000000000L, inserted?.get("startedAt"))
        assertEquals(1700000600000L, inserted?.get("completedAt"))
        assertEquals(100, inserted?.get("progressPercent"))
    }
}
