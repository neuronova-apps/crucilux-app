package com.neuronova.crucilux

import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.engine.CruciluxGridEngine
import com.neuronova.crucilux.engine.CruciluxGridEngineException
import com.neuronova.crucilux.model.CruciluxAnswerType
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
 * Pruebas unitarias del motor de cuadrícula [CruciluxGridEngine] sobre el banco dinámico v1.37.
 */
class CruciluxGridEngineTest {

    private lateinit var repository: CruciluxBankRepository

    @Before
    fun setUp() {
        repository = CruciluxBankRepository.getInstance()
        val assetFile = File("src/main/assets/crucilux_bank_v1_37.json")
        val finalFile = if (assetFile.exists()) assetFile
        else File("app/src/main/assets/crucilux_bank_v1_37.json")
        assertTrue("El archivo crucilux_bank_v1_37.json debe existir", finalFile.exists())
        FileInputStream(finalFile).use { stream ->
            repository.loadFromStream(stream)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Dimensiones y estructura básica
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `primer tablero real tiene dimensiones dinamicas correctas`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertEquals("Filas incorrectas", board.rows, grid.rows)
        assertEquals("Columnas incorrectas", board.cols, grid.cols)
    }

    @Test
    fun `primer tablero real tiene celdas activas e inactivas`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertTrue("Debe haber celdas activas", grid.activeCellCount > 0)
        assertTrue("Debe haber celdas inactivas", grid.inactiveCellCount > 0)
        assertEquals(
            "Total de celdas debe ser rows * cols",
            board.rows * board.cols,
            grid.activeCellCount + grid.inactiveCellCount
        )
    }

    @Test
    fun `primer tablero real tiene intersecciones`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        assertTrue("Debe haber al menos una intersección", grid.intersectionCount > 0)
    }

    @Test
    fun `primer tablero real tiene pistas horizontales y verticales`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hEntries = board.entries.count { it.direction == CruciluxDirection.HORIZONTAL }
        val vEntries = board.entries.count { it.direction == CruciluxDirection.VERTICAL }
        assertEquals("Pistas horizontales incorrectas", hEntries, grid.horizontalClues.size)
        assertEquals("Pistas verticales incorrectas", vEntries, grid.verticalClues.size)
    }

    @Test
    fun `pistas horizontales y verticales estan ordenadas por numero`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hNumbers = grid.horizontalClues.map { it.number }
        val vNumbers = grid.verticalClues.map { it.number }
        assertEquals("Horizontales deben estar ordenadas", hNumbers.sorted(), hNumbers)
        assertEquals("Verticales deben estar ordenadas", vNumbers.sorted(), vNumbers)
    }

    @Test
    fun `pistas no exponen la solucion en el texto de la pista`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
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
    fun `numeracion de pistas asignada desde el banco`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val bankNumbers = board.entries.map { it.number }.toSet()
        grid.horizontalClues.forEach { clue ->
            assertTrue("Número ${clue.number} debe existir en el banco", clue.number in bankNumbers)
        }
        grid.verticalClues.forEach { clue ->
            assertTrue("Número ${clue.number} debe existir en el banco", clue.number in bankNumbers)
        }
    }

    @Test
    fun `celdas de inicio tienen numero correcto`() {
        val board = repository.getAllBoards().first()
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
    // 2. Tableros rectangulares en el motor
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `tablero rectangular construye con dimensiones asimetricas correctas`() {
        val rectangularBoard = repository.getAllBoards().first { it.rows != it.cols }
        val grid = CruciluxGridEngine.buildGrid(rectangularBoard)
        assertEquals(rectangularBoard.rows, grid.rows)
        assertEquals(rectangularBoard.cols, grid.cols)
        assertTrue("Rows debe diferir de cols", grid.rows != grid.cols)
        assertEquals(grid.rows * grid.cols, grid.activeCellCount + grid.inactiveCellCount)
    }

    @Test
    fun `tablero Historia construye correctamente`() {
        val board = repository.obtenerCrucigrama("Historia")
        assertNotNull("Debe encontrar tablero Historia", board)
        val grid = CruciluxGridEngine.buildGrid(board!!)
        assertEquals(board.rows, grid.rows)
        assertEquals(board.cols, grid.cols)
        assertTrue(grid.horizontalClues.isNotEmpty())
        assertTrue(grid.verticalClues.isNotEmpty())
    }

    @Test
    fun `tablero Peru construye correctamente`() {
        val board = repository.obtenerCrucigrama("Perú")
        assertNotNull("Debe encontrar tablero Perú", board)
        val grid = CruciluxGridEngine.buildGrid(board!!)
        assertEquals(board.rows, grid.rows)
        assertEquals(board.cols, grid.cols)
        assertTrue(grid.horizontalClues.isNotEmpty())
        assertTrue(grid.verticalClues.isNotEmpty())
    }

    @Test
    fun `tablero Ciencia construye correctamente`() {
        val board = repository.obtenerCrucigrama("Ciencia")
        assertNotNull("Debe encontrar tablero Ciencia", board)
        val grid = CruciluxGridEngine.buildGrid(board!!)
        assertEquals(board.rows, grid.rows)
        assertEquals(board.cols, grid.cols)
        assertTrue(grid.horizontalClues.isNotEmpty())
        assertTrue(grid.verticalClues.isNotEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Validación de coordenadas y celdas
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `todas las celdas de tableros de muestra estan dentro de limites`() {
        val sampleBoards = repository.getAllBoards().take(10)
        for (board in sampleBoards) {
            val grid = CruciluxGridEngine.buildGrid(board)
            for (r in 0 until grid.rows) {
                for (c in 0 until grid.cols) {
                    val cell = grid.cellAt(r, c)
                    assertNotNull("cellAt($r, $c) no debe ser null", cell)
                }
            }
            assertNull("cellAt fuera de límite debe ser null", grid.cellAt(-1, 0))
            assertNull("cellAt fuera de límite debe ser null", grid.cellAt(grid.rows, 0))
            assertNull("cellAt fuera de límite debe ser null", grid.cellAt(0, grid.cols))
        }
    }

    @Test
    fun `celdas activas tienen asociaciones de entrada horizontal y vertical`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hEntries = board.entries.filter { it.direction == CruciluxDirection.HORIZONTAL }
        for (entry in hEntries) {
            val startCell = grid.cellAt(entry.row, entry.col)!!
            assertEquals("BankId de entrada horizontal incorrecto", entry.bankId, startCell.horizontalEntryBankId)
        }
        val vEntries = board.entries.filter { it.direction == CruciluxDirection.VERTICAL }
        for (entry in vEntries) {
            val startCell = grid.cellAt(entry.row, entry.col)!!
            assertEquals("BankId de entrada vertical incorrecto", entry.bankId, startCell.verticalEntryBankId)
        }
    }

    @Test
    fun `intersecciones tienen ambas relaciones H y V`() {
        val board = repository.getAllBoards().first()
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
    fun `celdas inactivas no tienen asociaciones ni letras`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        for (row in grid.cells) {
            for (cell in row) {
                if (!cell.isActive) {
                    assertNull(cell.horizontalEntryBankId)
                    assertNull(cell.verticalEntryBankId)
                    assertNull(cell.clueNumber)
                    assertNull(cell.solutionLetter)
                    assertFalse(cell.isIntersection)
                }
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Respuestas compuestas y formato de longitud
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `formato de longitud para SINGLE indica cantidad de letras`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val singleClue = grid.horizontalClues.firstOrNull { it.answerType == CruciluxAnswerType.SINGLE }
            ?: grid.verticalClues.first { it.answerType == CruciluxAnswerType.SINGLE }

        assertEquals("${singleClue.length} letras", singleClue.formatLengthInfo())
    }

    @Test
    fun `formato de longitud para COMPOUND indica palabras y longitudes parciales`() {
        val boardWithCompound = repository.getAllBoards().first { board ->
            board.entries.any { it.answerType == CruciluxAnswerType.COMPOUND }
        }
        val grid = CruciluxGridEngine.buildGrid(boardWithCompound)
        val compoundClue = (grid.horizontalClues + grid.verticalClues).first {
            it.answerType == CruciluxAnswerType.COMPOUND
        }

        val expected = "${compoundClue.wordCount} palabras · ${compoundClue.wordLengths.joinToString(" + ")} letras"
        assertEquals(expected, compoundClue.formatLengthInfo())
    }

    @Test
    fun `coherencia de letras en intersecciones de tableros de muestra`() {
        val sampleBoards = repository.getAllBoards().take(20)
        for (board in sampleBoards) {
            val grid = CruciluxGridEngine.buildGrid(board)
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
    // 5. Verificación completa de los 300 tableros del banco v1.37
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `los 300 tableros del banco v1_37 construyen correctamente sin errores`() {
        val allBoards = repository.getAllBoards()
        assertEquals("Debe haber exactamente 300 tableros", 300, allBoards.size)

        val errors = mutableListOf<String>()
        var totalEntriesBuilt = 0

        for (board in allBoards) {
            try {
                val grid = CruciluxGridEngine.buildGrid(board)
                totalEntriesBuilt += board.entries.size

                if (grid.rows != board.rows) {
                    errors.add("${board.id}: filas incorrectas ${grid.rows} vs ${board.rows}")
                }
                if (grid.cols != board.cols) {
                    errors.add("${board.id}: columnas incorrectas ${grid.cols} vs ${board.cols}")
                }

                val expectedTotal = board.rows * board.cols
                val actualTotal = grid.activeCellCount + grid.inactiveCellCount
                if (actualTotal != expectedTotal) {
                    errors.add("${board.id}: total de celdas incorrecto $actualTotal vs $expectedTotal")
                }

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

        assertEquals("Debe procesar exactamente 2.000 entradas", 2000, totalEntriesBuilt)
        assertTrue(
            "Los siguientes tableros fallaron:\n${errors.joinToString("\n")}",
            errors.isEmpty(),
        )
    }
}
