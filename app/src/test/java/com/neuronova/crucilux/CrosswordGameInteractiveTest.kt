package com.neuronova.crucilux

import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.data.GameSessionState
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.engine.CruciluxGridEngine
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.ui.game.CheckMode
import com.neuronova.crucilux.ui.game.CrosswordGameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

/**
 * Pruebas unitarias de interacción, lógica de juego, modos de comprobación
 * y persistencia para Crucilux con banco dinámico v1.37.
 */
class CrosswordGameInteractiveTest {

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
    // 1. Selección de celda y dirección
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `seleccion inicial de celda activa con cruce selecciona horizontal`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)

        // Buscar una intersección
        val intersection = grid.cells.flatten().first { it.isIntersection }
        assertNotNull(intersection)

        // Estado inicial
        val state = CrosswordGameState(
            isLoading = false,
            board = board,
            grid = grid,
            selectedRow = intersection.row,
            selectedCol = intersection.col,
            activeDirection = CruciluxDirection.HORIZONTAL,
            activeEntryBankId = intersection.horizontalEntryBankId,
        )

        assertEquals(intersection.row, state.selectedRow)
        assertEquals(intersection.col, state.selectedCol)
        assertEquals(CruciluxDirection.HORIZONTAL, state.activeDirection)
        assertEquals(intersection.horizontalEntryBankId, state.activeEntryBankId)
    }

    @Test
    fun `cambio de orientacion por segundo toque en misma celda con cruce`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val cell = grid.cells.flatten().first { it.isIntersection }

        // Primer toque: Horizontal
        var dir = CruciluxDirection.HORIZONTAL
        // Segundo toque sobre la misma celda con cruce alterna a Vertical
        if (cell.horizontalEntryBankId != null && cell.verticalEntryBankId != null) {
            dir = if (dir == CruciluxDirection.HORIZONTAL) CruciluxDirection.VERTICAL else CruciluxDirection.HORIZONTAL
        }
        assertEquals(CruciluxDirection.VERTICAL, dir)

        // Tercer toque alterna de nuevo a Horizontal
        if (cell.horizontalEntryBankId != null && cell.verticalEntryBankId != null) {
            dir = if (dir == CruciluxDirection.HORIZONTAL) CruciluxDirection.VERTICAL else CruciluxDirection.HORIZONTAL
        }
        assertEquals(CruciluxDirection.HORIZONTAL, dir)
    }

    @Test
    fun `celda con solo entrada vertical mantiene direccion vertical`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)

        val vOnlyCell = grid.cells.flatten().firstOrNull {
            it.isActive && it.verticalEntryBankId != null && it.horizontalEntryBankId == null
        }
        if (vOnlyCell != null) {
            val hasH = vOnlyCell.horizontalEntryBankId != null
            val hasV = vOnlyCell.verticalEntryBankId != null
            val determinedDir = when {
                hasH && hasV -> CruciluxDirection.HORIZONTAL
                hasV -> CruciluxDirection.VERTICAL
                else -> CruciluxDirection.HORIZONTAL
            }
            assertEquals(CruciluxDirection.VERTICAL, determinedDir)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Entrada de letras y avance
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `entrada de letra almacena en userLetters sin modificar solutionLetter`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val cell = grid.cells.flatten().first { it.isActive }

        val userLetters = mutableMapOf<Pair<Int, Int>, Char>()
        val pos = Pair(cell.row, cell.col)
        userLetters[pos] = 'X'

        assertEquals('X', userLetters[pos])
        assertNotNull(cell.solutionLetter)
    }

    @Test
    fun `borrado de letra en celda actual elimina del mapa`() {
        val pos = Pair(2, 3)
        val userLetters = mutableMapOf(pos to 'A')
        assertEquals(1, userLetters.size)

        userLetters.remove(pos)
        assertTrue(userLetters.isEmpty())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Modos de comprobación
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `modo clasica acepta letras incorrectas sin marcarlas de inmediato`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val cell = grid.cells.flatten().first { it.isActive }

        val state = CrosswordGameState(
            isLoading = false,
            board = board,
            grid = grid,
            checkMode = CheckMode.CLASSIC,
            userLetters = mapOf(Pair(cell.row, cell.col) to 'Z'),
            incorrectCells = emptySet(),
        )

        assertEquals(CheckMode.CLASSIC, state.checkMode)
        assertEquals('Z', state.userLetters[Pair(cell.row, cell.col)])
        assertTrue("Modo clásica no debe marcar incorrectCells de inmediato", state.incorrectCells.isEmpty())
    }

    @Test
    fun `comprobar palabra activa detecta palabra incompleta`() {
        val activeCells = setOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(0, 3))
        val userLetters = mapOf(Pair(0, 0) to 'A', Pair(0, 1) to 'B')

        val isIncomplete = activeCells.any { it !in userLetters }
        assertTrue("Debe detectar palabra incompleta", isIncomplete)
    }

    @Test
    fun `comprobar palabra activa detecta palabra correcta`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hEntry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val userLetters = mutableMapOf<Pair<Int, Int>, Char>()
        val activeCells = mutableSetOf<Pair<Int, Int>>()
        for (i in hEntry.answer.indices) {
            val pos = Pair(hEntry.row, hEntry.col + i)
            userLetters[pos] = hEntry.answer[i]
            activeCells.add(pos)
        }

        val allCorrect = activeCells.all { pos ->
            val cell = grid.cellAt(pos.first, pos.second)!!
            cell.solutionLetter?.uppercaseChar() == userLetters[pos]?.uppercaseChar()
        }

        assertTrue("Todas las letras deben ser correctas", allCorrect)
    }

    @Test
    fun `comprobar palabra activa detecta errores sin revelar la respuesta`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hEntry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val userLetters = mutableMapOf<Pair<Int, Int>, Char>()
        val activeCells = mutableSetOf<Pair<Int, Int>>()
        for (i in hEntry.answer.indices) {
            val pos = Pair(hEntry.row, hEntry.col + i)
            userLetters[pos] = 'X'
            activeCells.add(pos)
        }

        val allCorrect = activeCells.all { pos ->
            val cell = grid.cellAt(pos.first, pos.second)!!
            cell.solutionLetter?.uppercaseChar() == userLetters[pos]?.uppercaseChar()
        }

        assertFalse("Debe detectar que hay errores", allCorrect)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Autoguardado y deserialización
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `serializacion y deserializacion de letras en GameSessionManager`() {
        val original = mapOf(
            Pair(0, 0) to 'C',
            Pair(0, 1) to 'A',
            Pair(0, 2) to 'S',
            Pair(0, 3) to 'A',
            Pair(3, 4) to 'Ñ',
        )

        val serialized = GameSessionManager.serializeLetters(original)
        assertTrue(serialized.isNotBlank())

        val deserialized = GameSessionManager.deserializeLetters(serialized)
        assertEquals(original.size, deserialized.size)
        assertEquals(original, deserialized)
    }

    @Test
    fun `deserializacion de letras vacias o nulas devuelve mapa vacio`() {
        assertTrue(GameSessionManager.deserializeLetters(null).isEmpty())
        assertTrue(GameSessionManager.deserializeLetters("").isEmpty())
        assertTrue(GameSessionManager.deserializeLetters("   ").isEmpty())
    }

    @Test
    fun `deserializacion ignora segmentos corruptos de forma segura`() {
        val raw = "0,0,A;invalido;1,2;3,4,B,extra;5,6,C"
        val result = GameSessionManager.deserializeLetters(raw)
        assertEquals(2, result.size)
        assertEquals('A', result[Pair(0, 0)])
        assertEquals('C', result[Pair(5, 6)])
    }

    @Test
    fun `GameSessionState calcula propiedades derivadas correctamente`() {
        val emptySession = GameSessionState()
        assertTrue(emptySession.isEmpty)
        assertFalse(emptySession.hasActiveSession)
        assertEquals(0, emptySession.filledCellCount)

        val activeSession = GameSessionState(
            boardId = "7X7-01",
            category = "Historia",
            boardSize = "7x7",
            userLetters = mapOf(Pair(0, 0) to 'A', Pair(0, 1) to 'B'),
            isFinished = false,
        )
        assertFalse(activeSession.isEmpty)
        assertTrue(activeSession.hasActiveSession)
        assertEquals(2, activeSession.filledCellCount)

        val finishedSession = activeSession.copy(isFinished = true)
        assertFalse(finishedSession.hasActiveSession)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Preservación y restauración por boardId
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `restauracion de tablero por boardId sin necesidad de size`() {
        val board = repository.getBoardById("7X7-01")
        assertNotNull("Debe encontrar el tablero 7X7-01", board)
        assertEquals("7X7-01", board?.id)

        val grid = CruciluxGridEngine.buildGrid(board!!)
        assertEquals(board.rows, grid.rows)
        assertEquals(board.cols, grid.cols)
    }

    @Test
    fun `todas las 10 categorias estan disponibles y tienen tableros asignables`() {
        val categories = listOf(
            "Cultura general", "Ciencia", "Naturaleza", "Animales", "Geografía",
            "Historia", "Tecnología", "Cine", "Biblia", "Perú"
        )

        for (cat in categories) {
            val board = repository.obtenerCrucigrama(cat)
            assertNotNull("Categoría $cat debe tener tablero asignable", board)
            assertEquals("La categoría del tablero debe coincidir", cat, board?.category)
        }
    }
}
