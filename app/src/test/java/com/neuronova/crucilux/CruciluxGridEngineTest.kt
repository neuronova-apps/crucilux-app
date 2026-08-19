package com.neuronova.crucilux

import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.engine.CruciluxGridEngine
import com.neuronova.crucilux.engine.CruciluxGridEngineException
import com.neuronova.crucilux.model.CruciluxDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

/**
 * Pruebas unitarias del motor de cuadrícula [CruciluxGridEngine].
 *
 * Las pruebas principales utilizan tableros REALES del banco maestro (v1.28).
 * Se prueban tableros 7x7, 10x10 y 15x15 de distintas categorías.
 *
 * Validan:
 * - Filas y columnas correctas.
 * - Celdas activas e inactivas.
 * - Intersecciones (celdas compartidas H y V).
 * - Numeración real del banco.
 * - Pistas horizontales y verticales (sin respuesta).
 * - Coordenadas dentro de límites.
 * - Longitudes correctas.
 * - Coherencia de letras en cruces.
 */
class CruciluxGridEngineTest {

    private lateinit var repository: CruciluxBankRepository

    @Before
    fun setUp() {
        repository = CruciluxBankRepository.getInstance()
        val assetFile = File("src/main/assets/crucilux_bank_v1_28.json")
        val finalFile = if (assetFile.exists()) assetFile
        else File("app/src/main/assets/crucilux_bank_v1_28.json")
        assertTrue("El archivo crucilux_bank_v1_28.json debe existir", finalFile.exists())
        FileInputStream(finalFile).use { stream ->
            repository.loadFromStream(stream)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Tablero 7x7 real
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `tablero 7x7 real tiene dimensiones correctas`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertEquals("Filas incorrectas", 7, grid.rows)
        assertEquals("Columnas incorrectas", 7, grid.cols)
    }

    @Test
    fun `tablero 7x7 real tiene celdas activas e inactivas`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertTrue("Debe haber celdas activas", grid.activeCellCount > 0)
        assertTrue("Debe haber celdas inactivas", grid.inactiveCellCount > 0)
        assertEquals("Total de celdas debe ser 7×7=49", 49, grid.activeCellCount + grid.inactiveCellCount)
    }

    @Test
    fun `tablero 7x7 real tiene intersecciones`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertTrue("Debe haber al menos una intersección", grid.intersectionCount > 0)
    }

    @Test
    fun `tablero 7x7 real tiene pistas horizontales y verticales`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hEntries = board.entries.count { it.direction == CruciluxDirection.HORIZONTAL }
        val vEntries = board.entries.count { it.direction == CruciluxDirection.VERTICAL }
        assertEquals("Pistas horizontales incorrectas", hEntries, grid.horizontalClues.size)
        assertEquals("Pistas verticales incorrectas", vEntries, grid.verticalClues.size)
    }

    @Test
    fun `tablero 7x7 real pistas ordenadas por numero`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hNumbers = grid.horizontalClues.map { it.number }
        val vNumbers = grid.verticalClues.map { it.number }
        assertEquals("Horizontales deben estar ordenadas", hNumbers.sorted(), hNumbers)
        assertEquals("Verticales deben estar ordenadas", vNumbers.sorted(), vNumbers)
    }

    @Test
    fun `tablero 7x7 real pistas no tienen respuesta`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        // CrosswordClue no tiene campo answer — la clase ni lo define
        // Verificamos que los datos de la pista estén presentes
        grid.horizontalClues.forEach { clue ->
            assertTrue("Pista debe tener texto", clue.clue.isNotBlank())
            assertTrue("Pista debe tener bankId", clue.bankId.isNotBlank())
            assertEquals("Pista horizontal debe ser HORIZONTAL", CruciluxDirection.HORIZONTAL, clue.direction)
        }
        grid.verticalClues.forEach { clue ->
            assertTrue("Pista debe tener texto", clue.clue.isNotBlank())
            assertEquals("Pista vertical debe ser VERTICAL", CruciluxDirection.VERTICAL, clue.direction)
        }
    }

    @Test
    fun `tablero 7x7 real numeracion asignada desde banco`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        // Verificar que los números en las pistas coinciden con los del banco
        val bankNumbers = board.entries.map { it.number }.toSet()
        grid.horizontalClues.forEach { clue ->
            assertTrue("Número ${clue.number} debe existir en el banco", clue.number in bankNumbers)
        }
        grid.verticalClues.forEach { clue ->
            assertTrue("Número ${clue.number} debe existir en el banco", clue.number in bankNumbers)
        }
    }

    @Test
    fun `tablero 7x7 real celdas de inicio tienen numero correcto`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        for (entry in board.entries) {
            val startCell = grid.cellAt(entry.row, entry.col)
            assertNotNull("Celda de inicio debe existir", startCell)
            assertTrue("Celda de inicio debe ser activa", startCell!!.isActive)
            assertTrue("Celda de inicio debe ser wordStart", startCell.isWordStart)
            assertNotNull("Celda de inicio debe tener número", startCell.clueNumber)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Tablero 10x10 real
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `tablero 10x10 real tiene dimensiones correctas`() {
        val board = repository.getBoardsBySize("10x10").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertEquals("Filas incorrectas", 10, grid.rows)
        assertEquals("Columnas incorrectas", 10, grid.cols)
    }

    @Test
    fun `tablero 10x10 real tiene celdas activas e inactivas`() {
        val board = repository.getBoardsBySize("10x10").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertTrue("Debe haber celdas activas", grid.activeCellCount > 0)
        assertTrue("Debe haber celdas inactivas", grid.inactiveCellCount > 0)
        assertEquals("Total de celdas debe ser 10×10=100", 100, grid.activeCellCount + grid.inactiveCellCount)
    }

    @Test
    fun `tablero 10x10 real tiene intersecciones`() {
        val board = repository.getBoardsBySize("10x10").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertTrue("Debe haber al menos una intersección", grid.intersectionCount > 0)
    }

    @Test
    fun `tablero Historia 10x10 real construye correctamente`() {
        val board = repository.obtenerCrucigrama("Historia", "10x10")
        assertNotNull("Debe encontrar tablero Historia 10x10", board)
        val grid = CruciluxGridEngine.buildGrid(board!!)
        assertEquals(10, grid.rows)
        assertEquals(10, grid.cols)
        assertTrue(grid.horizontalClues.isNotEmpty())
        assertTrue(grid.verticalClues.isNotEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Tablero 15x15 real
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `tablero 15x15 real tiene dimensiones correctas`() {
        val board = repository.getBoardsBySize("15x15").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertEquals("Filas incorrectas", 15, grid.rows)
        assertEquals("Columnas incorrectas", 15, grid.cols)
    }

    @Test
    fun `tablero 15x15 real tiene celdas activas e inactivas`() {
        val board = repository.getBoardsBySize("15x15").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertTrue("Debe haber celdas activas", grid.activeCellCount > 0)
        assertTrue("Debe haber celdas inactivas", grid.inactiveCellCount > 0)
        assertEquals("Total de celdas debe ser 15×15=225", 225, grid.activeCellCount + grid.inactiveCellCount)
    }

    @Test
    fun `tablero Peru 15x15 real construye correctamente`() {
        val board = repository.obtenerCrucigrama("Perú", "15x15")
        assertNotNull("Debe encontrar tablero Perú 15x15", board)
        val grid = CruciluxGridEngine.buildGrid(board!!)
        assertEquals(15, grid.rows)
        assertEquals(15, grid.cols)
        assertTrue(grid.horizontalClues.isNotEmpty())
        assertTrue(grid.verticalClues.isNotEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Validación de coordenadas y longitudes
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `todas las celdas activas estan dentro de limites en tableros de muestra`() {
        // Probamos un tablero de cada tamaño
        val boards = listOf(
            repository.getBoardsBySize("7x7").first(),
            repository.getBoardsBySize("10x10").first(),
            repository.getBoardsBySize("15x15").first(),
        )
        for (board in boards) {
            val grid = CruciluxGridEngine.buildGrid(board)
            for (r in 0 until grid.rows) {
                for (c in 0 until grid.cols) {
                    val cell = grid.cellAt(r, c)
                    assertNotNull("cellAt($r, $c) no debe ser null", cell)
                }
            }
            // Coordenadas fuera de rango deben retornar null
            assertNull("cellAt fuera de límite debe ser null", grid.cellAt(-1, 0))
            assertNull("cellAt fuera de límite debe ser null", grid.cellAt(grid.rows, 0))
            assertNull("cellAt fuera de límite debe ser null", grid.cellAt(0, grid.cols))
        }
    }

    @Test
    fun `celdas activas con entrada horizontal tienen horizontalEntryBankId`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hEntries = board.entries.filter { it.direction == CruciluxDirection.HORIZONTAL }
        for (entry in hEntries) {
            val startCell = grid.cellAt(entry.row, entry.col)!!
            assertNotNull("Celda inicio horizontal debe tener horizontalEntryBankId", startCell.horizontalEntryBankId)
            assertEquals("BankId de entrada horizontal incorrecto", entry.bankId, startCell.horizontalEntryBankId)
        }
    }

    @Test
    fun `celdas activas con entrada vertical tienen verticalEntryBankId`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val vEntries = board.entries.filter { it.direction == CruciluxDirection.VERTICAL }
        for (entry in vEntries) {
            val startCell = grid.cellAt(entry.row, entry.col)!!
            assertNotNull("Celda inicio vertical debe tener verticalEntryBankId", startCell.verticalEntryBankId)
            assertEquals("BankId de entrada vertical incorrecto", entry.bankId, startCell.verticalEntryBankId)
        }
    }

    @Test
    fun `intersecciones tienen ambas relaciones`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        for (row in grid.cells) {
            for (cell in row) {
                if (cell.isIntersection) {
                    assertNotNull("Intersección debe tener horizontalEntryBankId", cell.horizontalEntryBankId)
                    assertNotNull("Intersección debe tener verticalEntryBankId", cell.verticalEntryBankId)
                }
            }
        }
    }

    @Test
    fun `celdas inactivas no tienen asociaciones de entrada`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        for (row in grid.cells) {
            for (cell in row) {
                if (!cell.isActive) {
                    assertNull("Celda inactiva no debe tener horizontalEntryBankId", cell.horizontalEntryBankId)
                    assertNull("Celda inactiva no debe tener verticalEntryBankId", cell.verticalEntryBankId)
                    assertNull("Celda inactiva no debe tener número", cell.clueNumber)
                    assertNull("Celda inactiva no debe tener letra", cell.solutionLetter)
                    assertFalse("Celda inactiva no debe ser intersección", cell.isIntersection)
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Coherencia de letras en intersecciones (todos los tableros de muestra)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `letras en intersecciones son coherentes en todos los tableros de muestra`() {
        // Una muestra representativa: primeros 3 tableros de cada tamaño
        val sampleBoards = listOf("7x7", "10x10", "15x15").flatMap { size ->
            repository.getBoardsBySize(size).take(3)
        }
        for (board in sampleBoards) {
            // Si el motor no lanza excepción, las intersecciones son coherentes
            val grid = CruciluxGridEngine.buildGrid(board)
            // Verificar que cada celda activa tiene una letra
            for (row in grid.cells) {
                for (cell in row) {
                    if (cell.isActive) {
                        assertNotNull(
                            "Celda activa en ${board.id} (${cell.row},${cell.col}) debe tener letra",
                            cell.solutionLetter,
                        )
                    }
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. CrosswordGrid.cellAt() y propiedades derivadas
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `crosswordGrid cellAt retorna null fuera de limites`() {
        val board = repository.getBoardsBySize("7x7").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertNull(grid.cellAt(-1, 0))
        assertNull(grid.cellAt(0, -1))
        assertNull(grid.cellAt(7, 0))
        assertNull(grid.cellAt(0, 7))
    }

    @Test
    fun `crosswordGrid activeCellCount mas inactiveCellCount es igual a total de celdas`() {
        val board = repository.getBoardsBySize("10x10").first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertEquals(grid.rows * grid.cols, grid.activeCellCount + grid.inactiveCellCount)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Prueba completa de los 300 tableros del banco
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `los 300 tableros del banco construyen correctamente sin errores`() {
        val allBoards = repository.getAllBoards()
        assertEquals("Debe haber 300 tableros", 300, allBoards.size)

        val errors = mutableListOf<String>()
        for (board in allBoards) {
            try {
                val grid = CruciluxGridEngine.buildGrid(board)

                // Validar dimensiones
                if (grid.rows != board.rows) {
                    errors.add("${board.id}: filas incorrectas ${grid.rows} vs ${board.rows}")
                }
                if (grid.cols != board.cols) {
                    errors.add("${board.id}: columnas incorrectas ${grid.cols} vs ${board.cols}")
                }

                // Validar que el total de celdas es correcto
                val expectedTotal = board.rows * board.cols
                val actualTotal = grid.activeCellCount + grid.inactiveCellCount
                if (actualTotal != expectedTotal) {
                    errors.add("${board.id}: total de celdas incorrecto $actualTotal vs $expectedTotal")
                }

                // Validar que todas las celdas activas tienen letra
                for (row in grid.cells) {
                    for (cell in row) {
                        if (cell.isActive && cell.solutionLetter == null) {
                            errors.add("${board.id}: celda activa (${cell.row},${cell.col}) sin letra")
                        }
                    }
                }

            } catch (e: CruciluxGridEngineException) {
                errors.add("${board.id}: ${e.message}")
            } catch (e: Exception) {
                errors.add("${board.id}: excepción inesperada ${e.javaClass.simpleName}: ${e.message}")
            }
        }

        assertTrue(
            "Los siguientes tableros fallaron:\n${errors.joinToString("\n")}",
            errors.isEmpty(),
        )
    }
}
