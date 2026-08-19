package com.neuronova.crucilux

import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.model.CruciluxAnswerType
import com.neuronova.crucilux.model.CruciluxDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class CruciluxBankRepositoryTest {

    private lateinit var repository: CruciluxBankRepository

    @Before
    fun setUp() {
        repository = CruciluxBankRepository.getInstance()
        val assetFile = File("src/main/assets/crucilux_bank_v1_37.json")
        val finalFile = if (assetFile.exists()) assetFile else File("app/src/main/assets/crucilux_bank_v1_37.json")
        assertTrue("El archivo crucilux_bank_v1_37.json debe existir", finalFile.exists())

        FileInputStream(finalFile).use { stream ->
            repository.loadFromStream(stream)
        }
    }

    // 1. schemaVersion = 2
    @Test
    fun testSchemaVersionIs2() {
        val metadata = repository.getMetadata()
        assertNotNull("Metadatos no deben ser nulos", metadata)
        assertEquals("schemaVersion debe ser 2", 2, metadata?.schemaVersion)
    }

    // 2. bankVersion = 1.37
    @Test
    fun testBankVersionIs137() {
        val metadata = repository.getMetadata()
        assertNotNull("Metadatos no deben ser nulos", metadata)
        assertEquals("bankVersion debe ser 1.37", "1.37", metadata?.bankVersion)
    }

    // 3. 300 tableros
    @Test
    fun testTotalBoardsIs300() {
        val allBoards = repository.getAllBoards()
        assertEquals("Debe contener exactamente 300 tableros", 300, allBoards.size)
        assertEquals("Metadatos debe reportar 300 tableros", 300, repository.getMetadata()?.totalBoards)
    }

    // 4. 2.000 entradas
    @Test
    fun testTotalEntriesIs2000() {
        val allBoards = repository.getAllBoards()
        val totalEntries = allBoards.sumOf { it.entries.size }
        assertEquals("Debe haber exactamente 2.000 entradas en total", 2000, totalEntries)
        assertEquals("Metadatos debe reportar 2.000 entradas", 2000, repository.getMetadata()?.totalEntries)
    }

    // 5. 10 categorías
    @Test
    fun testExact10Categories() {
        val categories = repository.getCategories()
        assertEquals("Debe haber exactamente 10 categorías reales", 10, categories.size)
    }

    // 6. 30 tableros por categoría
    @Test
    fun test30BoardsPerCategory() {
        val expectedCategories = listOf(
            "Cultura general", "Ciencia", "Naturaleza", "Animales", "Geografía",
            "Historia", "Tecnología", "Cine", "Biblia", "Perú"
        )
        for (cat in expectedCategories) {
            val boardsInCat = repository.getBoardsByCategory(cat)
            assertEquals("La categoría $cat debe tener exactamente 30 tableros", 30, boardsInCat.size)
        }
    }

    // 7. todos los boards usan rows/cols válidos
    @Test
    fun testAllBoardsHaveValidRowsAndCols() {
        for (board in repository.getAllBoards()) {
            assertTrue("Rows debe ser >= 5 en ${board.id}", board.rows >= 5)
            assertTrue("Cols debe ser >= 5 en ${board.id}", board.cols >= 5)
        }
    }

    // 8. no existe dependencia funcional de size
    @Test
    fun testNoFunctionalDependencyOnSize() {
        for (board in repository.getAllBoards()) {
            assertTrue("Rows debe ser positivo", board.rows > 0)
            assertTrue("Cols debe ser positivo", board.cols > 0)
            assertEquals("dimensionLabel debe ser rows x cols", "${board.rows}x${board.cols}", board.dimensionLabel)
        }
    }

    // 10. soportar tableros rectangulares
    @Test
    fun testRectangularBoardsExistAndSupported() {
        val rectangularBoards = repository.getAllBoards().filter { it.rows != it.cols }
        assertTrue("Debe haber tableros rectangulares en v1.37", rectangularBoards.isNotEmpty())
        for (board in rectangularBoards) {
            assertTrue("Tablero rectangular debe tener rows != cols", board.rows != board.cols)
        }
    }

    // 11. coordenadas válidas en 300/300
    @Test
    fun testCoordinatesValidInAll300Boards() {
        for (board in repository.getAllBoards()) {
            for (entry in board.entries) {
                if (entry.direction == CruciluxDirection.HORIZONTAL) {
                    assertTrue("Fila dentro de límite rows en ${board.id}", entry.row in 0 until board.rows)
                    assertTrue("Columna inicio >= 0 en ${board.id}", entry.col >= 0)
                    assertTrue("Columna fin <= cols en ${board.id}", entry.col + entry.length <= board.cols)
                } else {
                    assertTrue("Columna dentro de límite cols en ${board.id}", entry.col in 0 until board.cols)
                    assertTrue("Fila inicio >= 0 en ${board.id}", entry.row >= 0)
                    assertTrue("Fila fin <= rows en ${board.id}", entry.row + entry.length <= board.rows)
                }
            }
        }
    }

    // 12. SINGLE = 1.945 ocurrencias
    @Test
    fun testSingleOccurrencesIs1945() {
        val allEntries = repository.getAllBoards().flatMap { it.entries }
        val singleEntries = allEntries.filter { it.answerType == CruciluxAnswerType.SINGLE }
        assertEquals("SINGLE debe tener exactamente 1.945 ocurrencias", 1945, singleEntries.size)
    }

    // 13. COMPOUND = 55 ocurrencias
    @Test
    fun testCompoundOccurrencesIs55() {
        val allEntries = repository.getAllBoards().flatMap { it.entries }
        val compoundEntries = allEntries.filter { it.answerType == CruciluxAnswerType.COMPOUND }
        assertEquals("COMPOUND debe tener exactamente 55 ocurrencias", 55, compoundEntries.size)
    }

    // 14. wordCount coherente con wordLengths
    @Test
    fun testWordCountCoherentWithWordLengths() {
        val allEntries = repository.getAllBoards().flatMap { it.entries }
        for (entry in allEntries) {
            assertEquals(
                "wordCount debe ser igual a wordLengths.size en ${entry.bankId}",
                entry.wordCount,
                entry.wordLengths.size
            )
        }
    }

    // 15. sum(wordLengths) == length
    @Test
    fun testSumWordLengthsEqualsLength() {
        val allEntries = repository.getAllBoards().flatMap { it.entries }
        for (entry in allEntries) {
            val sum = entry.wordLengths.sum()
            assertEquals("sum(wordLengths) debe ser igual a length en ${entry.bankId}", entry.length, sum)
            assertEquals("length debe ser igual a answer.length en ${entry.bankId}", entry.length, entry.answer.length)
        }
    }

    // 16. SINGLE tiene wordCount == 1
    @Test
    fun testSingleHasWordCount1() {
        val singleEntries = repository.getAllBoards().flatMap { it.entries }
            .filter { it.answerType == CruciluxAnswerType.SINGLE }
        for (entry in singleEntries) {
            assertEquals("SINGLE debe tener wordCount == 1 en ${entry.bankId}", 1, entry.wordCount)
            assertEquals("SINGLE debe tener 1 elemento en wordLengths", 1, entry.wordLengths.size)
            assertEquals("SINGLE wordLengths[0] debe ser length", entry.length, entry.wordLengths[0])
        }
    }

    // 17. COMPOUND tiene wordCount >= 2
    @Test
    fun testCompoundHasWordCountGte2() {
        val compoundEntries = repository.getAllBoards().flatMap { it.entries }
            .filter { it.answerType == CruciluxAnswerType.COMPOUND }
        for (entry in compoundEntries) {
            assertTrue("COMPOUND debe tener wordCount >= 2 en ${entry.bankId}", entry.wordCount >= 2)
            assertTrue("COMPOUND debe tener >= 2 elementos en wordLengths", entry.wordLengths.size >= 2)
        }
    }

    // 18. displayAnswer presente
    @Test
    fun testDisplayAnswerPresentInAllEntries() {
        val allEntries = repository.getAllBoards().flatMap { it.entries }
        for (entry in allEntries) {
            assertTrue("displayAnswer no debe estar vacío en ${entry.bankId}", entry.displayAnswer.isNotBlank())
        }
    }

    // 19. boardId únicos
    @Test
    fun testAllBoardIdsAreUnique() {
        val allBoards = repository.getAllBoards()
        val uniqueIds = allBoards.map { it.id }.toSet()
        assertEquals("Los 300 IDs de tableros deben ser únicos", 300, uniqueIds.size)
    }

    // 20. consulta por boardId y obtención por categoría
    @Test
    fun testGetBoardByIdAndObtenerCrucigrama() {
        val board = repository.getBoardById("7X7-01")
        assertNotNull("Debe encontrar tablero 7X7-01", board)
        assertEquals("7X7-01", board?.id)

        val histBoard = repository.obtenerCrucigrama("Historia")
        assertNotNull("Debe encontrar un tablero para Historia", histBoard)
        assertEquals("Historia", histBoard?.category)
    }
}
