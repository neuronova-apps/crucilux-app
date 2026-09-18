package com.neuronovaapps.crucilux

import com.neuronovaapps.crucilux.data.bank.BankLoadStatus
import com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
import com.neuronovaapps.crucilux.model.CruciluxDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.text.Normalizer
import java.util.Locale

class CruciluxBankIntegrityTest {

    private lateinit var repository: CruciluxBankRepository

    @Before
    fun setUp() {
        repository = CruciluxBankRepository.getInstance()
        val localAsset = File("src/main/assets/crucilux_bank_v1_37.json")
        val asset = if (localAsset.exists()) {
            localAsset
        } else {
            File("app/src/main/assets/crucilux_bank_v1_37.json")
        }
        FileInputStream(asset).use(repository::loadFromStream)
    }

    @Test
    fun `las referencias maestras conservan respuesta pista y presentacion`() {
        val entriesByBankId = repository.getAllBoards()
            .flatMap { it.entries }
            .groupBy { it.bankId }

        assertEquals(1_311, entriesByBankId.size)
        entriesByBankId.forEach { (bankId, occurrences) ->
            assertTrue("bankId inválido: $bankId", bankId.matches(Regex("CRU\\d{5}")))
            assertEquals("Respuesta inconsistente para $bankId", 1, occurrences.map { it.answer }.distinct().size)
            assertEquals(
                "Presentación inconsistente para $bankId",
                1,
                occurrences.map { it.displayAnswer }.distinct().size,
            )
            assertEquals("Pista inconsistente para $bankId", 1, occurrences.map { it.clue }.distinct().size)
        }
    }

    @Test
    fun `tildes espacios y eñe de presentacion normalizan a la respuesta jugable`() {
        repository.getAllBoards().flatMap { it.entries }.forEach { entry ->
            assertEquals(
                "displayAnswer incompatible en ${entry.bankId}",
                entry.answer,
                normalizeForGrid(entry.displayAnswer),
            )
            assertTrue("Pista vacía en ${entry.bankId}", entry.clue.isNotBlank())
            assertTrue("Carácter Unicode corrupto en ${entry.bankId}", '\uFFFD' !in entry.clue)
        }
    }

    @Test
    fun `todas las entradas de cada tablero forman una red conectada por cruces`() {
        repository.getAllBoards().forEach { board ->
            val cellsByEntry = board.entries.associate { entry ->
                entry.bankId to (0 until entry.length).map { offset ->
                    val row = if (entry.direction == CruciluxDirection.VERTICAL) entry.row + offset else entry.row
                    val col = if (entry.direction == CruciluxDirection.HORIZONTAL) entry.col + offset else entry.col
                    row to col
                }.toSet()
            }
            val adjacency = board.entries.associate { it.bankId to mutableSetOf<String>() }

            board.entries.forEachIndexed { index, entry ->
                board.entries.drop(index + 1).forEach { other ->
                    if (cellsByEntry.getValue(entry.bankId).intersect(cellsByEntry.getValue(other.bankId)).isNotEmpty()) {
                        adjacency.getValue(entry.bankId).add(other.bankId)
                        adjacency.getValue(other.bankId).add(entry.bankId)
                    }
                }
            }

            val visited = mutableSetOf(board.entries.first().bankId)
            val pending = ArrayDeque(visited)
            while (pending.isNotEmpty()) {
                adjacency.getValue(pending.removeFirst()).forEach { neighbour ->
                    if (visited.add(neighbour)) pending.addLast(neighbour)
                }
            }
            assertEquals("Entradas desconectadas en ${board.id}", board.entries.size, visited.size)
        }
    }

    @Test
    fun `categoria inexistente no sustituye silenciosamente otro tablero`() {
        assertTrue(repository.getBoardsByCategory("Categoría inexistente").isEmpty())
        assertNull(repository.obtenerCrucigrama("Categoría inexistente"))
    }

    @Test
    fun `una recarga corrupta se rechaza sin reemplazar el banco valido`() {
        val malformed = """
            {
              "schemaVersion": 99,
              "bankVersion": "1.37",
              "app": "Crucilux",
              "coordinateBase": 0,
              "totalBoards": 0,
              "totalEntries": 0,
              "categories": [],
              "boards": []
            }
        """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            repository.loadFromJsonString(malformed)
        }
        assertEquals(BankLoadStatus.LOADED, repository.loadStatus.value)
        assertEquals(300, repository.getAllBoards().size)
        assertEquals("1.37", repository.getMetadata()?.bankVersion)
    }

    private fun normalizeForGrid(value: String): String {
        val protectedEnye = value.replace("Ñ", "\uE000").replace("ñ", "\uE000")
        return Normalizer.normalize(protectedEnye, Normalizer.Form.NFD)
            .filter { Character.getType(it) != Character.NON_SPACING_MARK.toInt() }
            .uppercase(Locale.ROOT)
            .replace("\uE000", "Ñ")
            .filter { it in 'A'..'Z' || it == 'Ñ' }
    }
}
