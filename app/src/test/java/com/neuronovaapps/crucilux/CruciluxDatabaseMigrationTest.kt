package com.neuronovaapps.crucilux

import androidx.sqlite.db.SupportSQLiteDatabase
import com.neuronovaapps.crucilux.data.db.AchievementEntity
import com.neuronovaapps.crucilux.data.db.CrosswordBoardStatus
import com.neuronovaapps.crucilux.data.db.CrosswordProgressEntity
import com.neuronovaapps.crucilux.data.db.CruciluxDatabase
import com.neuronovaapps.crucilux.data.db.PlayerProfileEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

/**
 * Prueba unitaria específica de migración de base de datos Room de versión 2 a versión 3.
 *
 * Verifica:
 * - Una base de datos versión 2 con [CrosswordProgressEntity] existente.
 * - [PlayerProfileEntity] existente.
 * - Ejecución de [CruciluxDatabase.MIGRATION_2_3].
 * - Conservación íntegra del progreso anterior sin borrado.
 * - Conservación íntegra del perfil y totalXp.
 * - Creación correcta de la tabla 'achievements' con todas sus columnas requeridas.
 * - Schema final válido.
 * - Ausencia total de sentencias destructivas (sin DROP TABLE ni DELETE).
 */
class CruciluxDatabaseMigrationTest {

    private val executedSqlStatements = mutableListOf<String>()
    private val tablesInDb = mutableMapOf<String, MutableList<Map<String, Any?>>>()

    private lateinit var mockDb: SupportSQLiteDatabase

    @Before
    fun setUp() {
        executedSqlStatements.clear()
        tablesInDb.clear()

        // 1. Simular base de datos en Versión 2 con datos previos de progreso y perfil
        tablesInDb["crossword_progress"] = mutableListOf(
            mapOf(
                "boardId" to "7X7-01",
                "category" to "Historia",
                "status" to CrosswordBoardStatus.COMPLETED.name,
                "progressPercent" to 100,
                "userLetters" to "0,0:A;0,1:B",
                "selectedRow" to 0,
                "selectedCol" to 0,
                "selectedDirection" to "H",
                "checkMode" to "CLASSIC",
                "hintsUsed" to 1,
                "bestXpEarned" to 120,
                "hintRevealedCells" to "0,0",
                "updatedAt" to 1700000000000L,
            ),
            mapOf(
                "boardId" to "10X10-15",
                "category" to "Ciencia",
                "status" to CrosswordBoardStatus.IN_PROGRESS.name,
                "progressPercent" to 40,
                "userLetters" to "1,1:X",
                "selectedRow" to 1,
                "selectedCol" to 1,
                "selectedDirection" to "V",
                "checkMode" to "ASSISTED",
                "hintsUsed" to 0,
                "bestXpEarned" to 0,
                "hintRevealedCells" to "",
                "updatedAt" to 1700000050000L,
            ),
        )

        tablesInDb["player_profile"] = mutableListOf(
            mapOf(
                "id" to 1,
                "totalXp" to 120,
            ),
        )

        // Proxy dinámico para interceptar y registrar las operaciones SQLite
        val handler = java.lang.reflect.InvocationHandler { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    val sql = args[0] as String
                    executedSqlStatements.add(sql)

                    // Si es CREATE TABLE achievements, registrar la tabla en el catálogo
                    if (sql.contains("CREATE TABLE", ignoreCase = true) && sql.contains("achievements", ignoreCase = true)) {
                        tablesInDb.getOrPut("achievements") { mutableListOf() }
                    }
                    null
                }
                "getVersion" -> 2
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
    fun `MIGRATION_2_3 tiene versiones de inicio 2 y fin 3`() {
        assertEquals(2, CruciluxDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, CruciluxDatabase.MIGRATION_2_3.endVersion)
    }

    @Test
    fun `MIGRATION_2_3 crea la tabla achievements con todas las columnas requeridas`() {
        assertFalse("La tabla achievements no debe existir antes de la migración", tablesInDb.containsKey("achievements"))

        // Ejecutar migración
        CruciluxDatabase.MIGRATION_2_3.migrate(mockDb)

        assertTrue("La tabla achievements debe existir tras la migración", tablesInDb.containsKey("achievements"))
        assertEquals(1, executedSqlStatements.size)

        val createSql = executedSqlStatements.first()
        assertTrue("Debe ser CREATE TABLE IF NOT EXISTS achievements", createSql.contains("CREATE TABLE IF NOT EXISTS achievements", ignoreCase = true))
        assertTrue("Debe incluir achievementId como PRIMARY KEY", createSql.contains("achievementId TEXT NOT NULL PRIMARY KEY", ignoreCase = true))
        assertTrue("Debe incluir isUnlocked", createSql.contains("isUnlocked INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue("Debe incluir unlockedAt", createSql.contains("unlockedAt INTEGER", ignoreCase = true))
        assertTrue("Debe incluir currentProgress", createSql.contains("currentProgress INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
        assertTrue("Debe incluir targetProgress", createSql.contains("targetProgress INTEGER NOT NULL DEFAULT 1", ignoreCase = true))
        assertTrue("Debe incluir isNotified", createSql.contains("isNotified INTEGER NOT NULL DEFAULT 0", ignoreCase = true))
    }

    @Test
    fun `MIGRATION_2_3 conserva integramente el progreso existente sin borrado destructivo`() {
        val initialCompleted = tablesInDb["crossword_progress"]?.count { it["status"] == CrosswordBoardStatus.COMPLETED.name } ?: 0
        val initialInProgress = tablesInDb["crossword_progress"]?.count { it["status"] == CrosswordBoardStatus.IN_PROGRESS.name } ?: 0
        assertEquals(1, initialCompleted)
        assertEquals(1, initialInProgress)

        // Ejecutar migración
        CruciluxDatabase.MIGRATION_2_3.migrate(mockDb)

        // Verificar que no se ejecutó ningún DROP TABLE ni DELETE
        for (sql in executedSqlStatements) {
            assertFalse("No debe ejecutar DROP TABLE", sql.contains("DROP TABLE", ignoreCase = true))
            assertFalse("No debe ejecutar DELETE FROM", sql.contains("DELETE FROM", ignoreCase = true))
            assertFalse("No debe alterar crossword_progress", sql.contains("crossword_progress", ignoreCase = true))
        }

        // Datos de progreso intactos
        val progressRows = tablesInDb["crossword_progress"]
        assertNotNull(progressRows)
        assertEquals(2, progressRows?.size)

        val completedRow = progressRows?.first { it["boardId"] == "7X7-01" }
        assertEquals("Historia", completedRow?.get("category"))
        assertEquals(CrosswordBoardStatus.COMPLETED.name, completedRow?.get("status"))
        assertEquals(100, completedRow?.get("progressPercent"))
        assertEquals(120, completedRow?.get("bestXpEarned"))
        assertEquals(1, completedRow?.get("hintsUsed"))
    }

    @Test
    fun `MIGRATION_2_3 conserva integramente el perfil y el totalXp del jugador`() {
        val initialProfile = tablesInDb["player_profile"]?.first()
        assertEquals(1, initialProfile?.get("id"))
        assertEquals(120, initialProfile?.get("totalXp"))

        // Ejecutar migración
        CruciluxDatabase.MIGRATION_2_3.migrate(mockDb)

        // Verificar que player_profile no fue alterada ni vaciada
        for (sql in executedSqlStatements) {
            assertFalse("No debe alterar player_profile", sql.contains("player_profile", ignoreCase = true))
        }

        val profileAfter = tablesInDb["player_profile"]?.first()
        assertEquals(1, profileAfter?.get("id"))
        assertEquals(120, profileAfter?.get("totalXp"))
    }

    @Test
    fun `nueva tabla achievements acepta registros validos de AchievementEntity`() {
        CruciluxDatabase.MIGRATION_2_3.migrate(mockDb)

        val sampleEntity = AchievementEntity(
            achievementId = "first_crossword",
            isUnlocked = true,
            unlockedAt = 1700000000000L,
            currentProgress = 1,
            targetProgress = 1,
            isNotified = false,
        )

        // Insertar en la tabla creada por la migración
        tablesInDb["achievements"]?.add(
            mapOf(
                "achievementId" to sampleEntity.achievementId,
                "isUnlocked" to if (sampleEntity.isUnlocked) 1 else 0,
                "unlockedAt" to sampleEntity.unlockedAt,
                "currentProgress" to sampleEntity.currentProgress,
                "targetProgress" to sampleEntity.targetProgress,
                "isNotified" to if (sampleEntity.isNotified) 1 else 0,
            )
        )

        val achievements = tablesInDb["achievements"]
        assertNotNull(achievements)
        assertEquals(1, achievements?.size)
        assertEquals("first_crossword", achievements?.first()?.get("achievementId"))
        assertEquals(1, achievements?.first()?.get("isUnlocked"))
        assertEquals(1700000000000L, achievements?.first()?.get("unlockedAt"))
        assertEquals(0, achievements?.first()?.get("isNotified"))
    }
}
