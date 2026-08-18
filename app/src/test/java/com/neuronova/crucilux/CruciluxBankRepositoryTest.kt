package com.neuronova.crucilux

import com.neuronova.crucilux.data.bank.CruciluxBankRepository
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
        val assetFile = File("src/main/assets/crucilux_bank_v1_28.json")
        val finalFile = if (assetFile.exists()) assetFile else File("app/src/main/assets/crucilux_bank_v1_28.json")
        assertTrue("El archivo crucilux_bank_v1_28.json debe existir", finalFile.exists())

        FileInputStream(finalFile).use { stream ->
            repository.loadFromStream(stream)
        }
    }

    @Test
    fun testBankJsonMetadata() {
        val metadata = repository.getMetadata()
        assertNotNull("Los metadatos no deben ser nulos", metadata)
        assertEquals("1.0", metadata?.schemaVersion)
        assertEquals("1.28", metadata?.bankVersion)
        assertEquals("Crucilux", metadata?.app)
        assertEquals(0, metadata?.coordinateBase)
        assertEquals(300, metadata?.totalBoards)
        assertEquals(2000, metadata?.totalEntries)
    }

    @Test
    fun testTotalBoardsAndUniqueIds() {
        val allBoards = repository.getAllBoards()
        assertEquals("Debe contener exactamente 300 tableros", 300, allBoards.size)

        val uniqueIds = allBoards.map { it.id }.toSet()
        assertEquals("Los 300 tableros deben tener IDs únicos", 300, uniqueIds.size)
    }

    @Test
    fun testSizesDistribution() {
        val boards7x7 = repository.getBoardsBySize("7x7")
        val boards10x10 = repository.getBoardsBySize("10x10")
        val boards15x15 = repository.getBoardsBySize("15x15")

        assertEquals("Debe haber 100 tableros de 7x7", 100, boards7x7.size)
        assertEquals("Debe haber 100 tableros de 10x10", 100, boards10x10.size)
        assertEquals("Debe haber 100 tableros de 15x15", 100, boards15x15.size)

        val sizes = repository.getSizes()
        assertTrue(sizes.contains("7x7"))
        assertTrue(sizes.contains("10x10"))
        assertTrue(sizes.contains("15x15"))
    }

    @Test
    fun testCategoriesDistribution() {
        val expectedCategories = listOf(
            "Cultura general",
            "Ciencia",
            "Naturaleza",
            "Animales",
            "Geografía",
            "Historia",
            "Tecnología",
            "Cine",
            "Biblia",
            "Perú"
        )

        val categories = repository.getCategories()
        assertEquals("Debe haber exactamente 10 categorías reales", 10, categories.size)

        for (cat in expectedCategories) {
            val count = repository.getBoardsByCategory(cat).size
            assertEquals("La categoría $cat debe tener 30 tableros", 30, count)

            val count7x7 = repository.getBoards(cat, "7x7").size
            val count10x10 = repository.getBoards(cat, "10x10").size
            val count15x15 = repository.getBoards(cat, "15x15").size

            assertEquals("La categoría $cat debe tener 10 tableros de 7x7", 10, count7x7)
            assertEquals("La categoría $cat debe tener 10 tableros de 10x10", 10, count10x10)
            assertEquals("La categoría $cat debe tener 10 tableros de 15x15", 10, count15x15)
        }
    }

    @Test
    fun testTotalEntriesCount() {
        val allBoards = repository.getAllBoards()
        val totalEntries = allBoards.sumOf { it.entries.size }
        assertEquals("Debe haber exactamente 2.000 entradas en total", 2000, totalEntries)

        val boards7x7 = repository.getBoardsBySize("7x7")
        assertTrue("Los tableros 7x7 deben tener 6 entradas cada uno", boards7x7.all { it.entries.size == 6 })

        val boards10x10 = repository.getBoardsBySize("10x10")
        assertTrue("Los tableros 10x10 deben tener 6 entradas cada uno", boards10x10.all { it.entries.size == 6 })

        val boards15x15 = repository.getBoardsBySize("15x15")
        assertTrue("Los tableros 15x15 deben tener 8 entradas cada uno", boards15x15.all { it.entries.size == 8 })
    }

    @Test
    fun testObtenerCrucigrama() {
        val board = repository.obtenerCrucigrama("Historia", "10x10")
        assertNotNull("Debe encontrar un tablero para Historia 10x10", board)
        assertEquals("Historia", board?.category)
        assertEquals("10x10", board?.size)
        assertEquals(10, board?.rows)
        assertEquals(10, board?.cols)
        assertEquals(6, board?.entries?.size)

        val peruBoard = repository.obtenerCrucigrama("Perú", "15x15")
        assertNotNull("Debe encontrar un tablero para Perú 15x15", peruBoard)
        assertEquals("Perú", peruBoard?.category)
        assertEquals("15x15", peruBoard?.size)
        assertEquals(15, peruBoard?.rows)
        assertEquals(15, peruBoard?.cols)
        assertEquals(8, peruBoard?.entries?.size)
    }

    @Test
    fun testEntryGeometryBounds() {
        for (board in repository.getAllBoards()) {
            for (entry in board.entries) {
                assertEquals(
                    "Longitud de la respuesta debe coincidir con length en tablero ${board.id}",
                    entry.length,
                    entry.answer.length
                )
                if (entry.direction.value == "horizontal") {
                    assertTrue(
                        "Entrada horizontal fuera de límites en tablero ${board.id}",
                        entry.col + entry.length <= board.cols
                    )
                    assertTrue("Fila fuera de límites en tablero ${board.id}", entry.row in 0 until board.rows)
                } else {
                    assertTrue(
                        "Entrada vertical fuera de límites en tablero ${board.id}",
                        entry.row + entry.length <= board.rows
                    )
                    assertTrue("Columna fuera de límites en tablero ${board.id}", entry.col in 0 until board.cols)
                }
            }
        }
    }
}
