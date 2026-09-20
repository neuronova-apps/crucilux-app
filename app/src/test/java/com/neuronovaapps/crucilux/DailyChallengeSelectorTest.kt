package com.neuronovaapps.crucilux

import com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
import com.neuronovaapps.crucilux.data.daily.DailyChallengeSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate

/**
 * Pruebas unitarias para la selección determinista del Desafío Diario.
 * Verifica la uniformidad, ausencia de repeticiones consecutivas y estabilidad temporal.
 */
class DailyChallengeSelectorTest {

    private lateinit var bankRepository: CruciluxBankRepository
    private lateinit var selector: DailyChallengeSelector

    @Before
    fun setUp() {
        bankRepository = CruciluxBankRepository.getInstance()
        val assetFile = File("src/main/assets/crucilux_bank_v1_37.json")
        val finalFile = if (assetFile.exists()) assetFile
        else File("app/src/main/assets/crucilux_bank_v1_37.json")
        assertTrue("El archivo crucilux_bank_v1_37.json debe existir", finalFile.exists())
        FileInputStream(finalFile).use { stream ->
            bankRepository.loadFromStream(stream)
        }
        selector = DailyChallengeSelector(bankRepository)
    }

    @Test
    fun `01 la misma fecha produce siempre el mismo boardId de forma determinista`() {
        val testDate = LocalDate.of(2026, 9, 18)
        val board1 = selector.selectBoardForDate(testDate)
        val board2 = selector.selectBoardForDate(testDate)
        val board3 = selector.selectBoardForDate(testDate)

        assertNotNull("El tablero no debe ser nulo", board1)
        assertEquals("Debe ser idéntico en múltiples llamadas", board1?.id, board2?.id)
        assertEquals("Debe ser idéntico en múltiples llamadas", board2?.id, board3?.id)
    }

    @Test
    fun `02 no hay repeticiones de tablero en dias consecutivos durante 10 anos`() {
        val startDate = LocalDate.of(2024, 1, 1)
        var previousBoardId: String? = null
        val totalDays = 3650 // 10 años consecutivos

        for (i in 0 until totalDays) {
            val date = startDate.plusDays(i.toLong())
            val currentBoard = selector.selectBoardForDate(date)
            assertNotNull("El tablero para $date no debe ser nulo", currentBoard)

            if (previousBoardId != null) {
                assertNotEquals(
                    "El tablero para $date (${currentBoard?.id}) no debe ser igual al del día anterior ($previousBoardId)",
                    previousBoardId,
                    currentBoard?.id
                )
            }
            previousBoardId = currentBoard?.id
        }
    }

    @Test
    fun `03 cobertura y distribucion uniforme de las 10 categorias en 300 dias`() {
        val startDate = LocalDate.of(2026, 1, 1)
        val categoryCounts = mutableMapOf<String, Int>()

        for (i in 0 until 300) {
            val date = startDate.plusDays(i.toLong())
            val board = selector.selectBoardForDate(date)
            assertNotNull(board)
            val category = board!!.category
            categoryCounts[category] = (categoryCounts[category] ?: 0) + 1
        }

        // Deben estar presentes las 10 categorías
        assertEquals("Las 10 categorías deben estar representadas en 300 días", 10, categoryCounts.size)

        // Ninguna categoría debe estar infra-representada (mínimo 15 apariciones en 300 días)
        for ((cat, count) in categoryCounts) {
            assertTrue("La categoría $cat debe aparecer al menos 15 veces (apareció $count)", count >= 15)
        }
    }

    @Test
    fun `04 manejo de anos bisiestos y transiciones fin de mes`() {
        // Año bisiesto 2024
        val feb28 = LocalDate.of(2024, 2, 28)
        val feb29 = LocalDate.of(2024, 2, 29)
        val mar01 = LocalDate.of(2024, 3, 1)

        val boardFeb28 = selector.selectBoardForDate(feb28)
        val boardFeb29 = selector.selectBoardForDate(feb29)
        val boardMar01 = selector.selectBoardForDate(mar01)

        assertNotNull(boardFeb28)
        assertNotNull(boardFeb29)
        assertNotNull(boardMar01)

        assertNotEquals(boardFeb28?.id, boardFeb29?.id)
        assertNotEquals(boardFeb29?.id, boardMar01?.id)

        // Fin de año a año nuevo
        val dec31 = LocalDate.of(2025, 12, 31)
        val jan01 = LocalDate.of(2026, 1, 1)

        val boardDec31 = selector.selectBoardForDate(dec31)
        val boardJan01 = selector.selectBoardForDate(jan01)

        assertNotNull(boardDec31)
        assertNotNull(boardJan01)
        assertNotEquals(boardDec31?.id, boardJan01?.id)
    }

    @Test
    fun `05 computeRawIndex maneja fechas historicas con epochDay negativo`() {
        val pre1970 = LocalDate.of(1950, 6, 15)
        val rawIndex = DailyChallengeSelector.computeRawIndex(pre1970.toEpochDay(), 300)
        assertTrue("El índice raw debe estar en el rango [0, 299]", rawIndex in 0 until 300)

        val farFuture = LocalDate.of(2150, 1, 1)
        val rawFuture = DailyChallengeSelector.computeRawIndex(farFuture.toEpochDay(), 300)
        assertTrue("El índice raw futuro debe estar en el rango [0, 299]", rawFuture in 0 until 300)
    }

    @Test
    fun `06 computeEffectiveIndex resuelve colision con el dia anterior deterministamente`() {
        val total = 300
        val yesterdayEffective = 42

        // Caso con colisión: todayRaw coincide con yesterdayEffective
        val resolved = DailyChallengeSelector.computeEffectiveIndex(
            todayRaw = 42,
            yesterdayEffective = yesterdayEffective,
            totalBoards = total
        )
        assertEquals("Debe avanzar al siguiente índice (43)", 43, resolved)

        // Caso con colisión en el límite superior (299)
        val resolvedWrap = DailyChallengeSelector.computeEffectiveIndex(
            todayRaw = 299,
            yesterdayEffective = 299,
            totalBoards = total
        )
        assertEquals("Debe reiniciar al índice 0 con wrap-around", 0, resolvedWrap)

        // Caso sin colisión: debe conservar el todayRaw
        val noCollision = DailyChallengeSelector.computeEffectiveIndex(
            todayRaw = 15,
            yesterdayEffective = yesterdayEffective,
            totalBoards = total
        )
        assertEquals("Sin colisión debe conservar el índice raw", 15, noCollision)
    }
}
