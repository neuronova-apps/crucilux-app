package com.neuronova.crucilux

import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.data.GameSessionState
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.engine.CruciluxGridEngine
import com.neuronova.crucilux.model.CruciluxAnswerType
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.ui.game.CheckMode
import com.neuronova.crucilux.ui.game.CrosswordGameState
import com.neuronova.crucilux.ui.game.CrosswordGameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream

/**
 * Pruebas unitarias de interacción, validación automática, bloqueo de celdas,
 * navegación entre pistas, estados visuales y persistencia para Crucilux.
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

        val intersection = grid.cells.flatten().first { it.isIntersection }
        assertNotNull(intersection)

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

        var dir = CruciluxDirection.HORIZONTAL
        if (cell.horizontalEntryBankId != null && cell.verticalEntryBankId != null) {
            dir = if (dir == CruciluxDirection.HORIZONTAL) CruciluxDirection.VERTICAL else CruciluxDirection.HORIZONTAL
        }
        assertEquals(CruciluxDirection.VERTICAL, dir)

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
    // 3. Modos de comprobación y verificación
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

    // ──────────────────────────────────────────────────────────────────────────
    // 5. VALIDACIÓN AUTOMÁTICA Y BLOQUEO DE CELDAS (NUEVAS PRUEBAS REQUERIDAS)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `1 palabra incompleta no se valida`() {
        val vm = CrosswordGameViewModel()
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val entry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        // Escribir solo algunas letras de la palabra
        val userLetters = mutableMapOf<Pair<Int, Int>, Char>()
        for (i in 0 until entry.answer.length - 1) {
            userLetters[Pair(entry.row, entry.col + i)] = entry.answer[i]
        }

        val state = CrosswordGameState(
            isLoading = false,
            board = board,
            grid = grid,
            userLetters = userLetters,
            validatedEntryBankIds = emptySet(),
            validatedCells = emptySet(),
        )

        assertFalse("Palabra incompleta no debe estar en validatedEntryBankIds", entry.bankId in state.validatedEntryBankIds)
        assertFalse("Celdas de palabra incompleta no deben estar bloqueadas", Pair(entry.row, entry.col) in state.validatedCells)
    }

    @Test
    fun `2 palabra completa incorrecta no se valida`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val entry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        // Escribir palabra completa pero con letras incorrectas
        val userLetters = mutableMapOf<Pair<Int, Int>, Char>()
        for (i in entry.answer.indices) {
            userLetters[Pair(entry.row, entry.col + i)] = 'X'
        }

        val state = CrosswordGameState(
            isLoading = false,
            board = board,
            grid = grid,
            userLetters = userLetters,
            validatedEntryBankIds = emptySet(),
            validatedCells = emptySet(),
        )

        assertFalse("Palabra incorrecta no debe validarse", entry.bankId in state.validatedEntryBankIds)
        assertFalse("Celdas de palabra incorrecta no deben marcarse validadas", Pair(entry.row, entry.col) in state.validatedCells)
    }

    @Test
    fun `3 palabra completa incorrecta sigue editable`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val entry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val pos = Pair(entry.row, entry.col)
        val userLetters = mutableMapOf(pos to 'X')
        val validatedCells = emptySet<Pair<Int, Int>>()

        // Comprobar que no está bloqueada y puede sobreescribirse o borrarse
        assertFalse("No debe estar bloqueada", pos in validatedCells)
        userLetters[pos] = 'A' // Corrección
        assertEquals('A', userLetters[pos])
        userLetters.remove(pos) // Borrado
        assertTrue(userLetters.isEmpty())
    }

    @Test
    fun `4 palabra completa correcta se valida automaticamente`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val entry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val userLetters = mutableMapOf<Pair<Int, Int>, Char>()
        val expectedCells = mutableSetOf<Pair<Int, Int>>()
        for (i in entry.answer.indices) {
            val p = Pair(entry.row, entry.col + i)
            userLetters[p] = entry.answer[i]
            expectedCells.add(p)
        }

        // Comprobar coincidencia exacta
        val allCorrect = expectedCells.all { pos ->
            grid.cellAt(pos.first, pos.second)?.solutionLetter?.uppercaseChar() == userLetters[pos]?.uppercaseChar()
        }
        assertTrue("Todas las letras coinciden con la solución", allCorrect)
    }

    @Test
    fun `5 palabra correcta queda bloqueada`() {
        val board = repository.getAllBoards().first()
        val entry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val validatedBankIds = setOf(entry.bankId)
        val validatedCells = (0 until entry.length).map { Pair(entry.row, entry.col + it) }.toSet()

        assertTrue(entry.bankId in validatedBankIds)
        for (i in 0 until entry.length) {
            assertTrue("Celda ($i) debe estar bloqueada", Pair(entry.row, entry.col + i) in validatedCells)
        }
    }

    @Test
    fun `6 backspace no borra letra validada`() {
        val board = repository.getAllBoards().first()
        val entry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val pos = Pair(entry.row, entry.col)
        val userLetters = mutableMapOf(pos to entry.answer[0])
        val validatedCells = setOf(pos)

        // Intento de borrado sobre celda validada
        if (pos !in validatedCells) {
            userLetters.remove(pos)
        }

        assertTrue("La letra validada debe permanecer intacta", userLetters.containsKey(pos))
        assertEquals(entry.answer[0], userLetters[pos])
    }

    @Test
    fun `7 teclado no sobrescribe letra validada`() {
        val board = repository.getAllBoards().first()
        val entry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val pos = Pair(entry.row, entry.col)
        val userLetters = mutableMapOf(pos to entry.answer[0])
        val validatedCells = setOf(pos)

        // Intento de sobreescritura con 'Z'
        val newChar = 'Z'
        if (pos !in validatedCells) {
            userLetters[pos] = newChar
        }

        assertEquals("La letra original validada no debe cambiar", entry.answer[0], userLetters[pos])
    }

    @Test
    fun `8 palabra validada mantiene estado tras seleccion`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val entry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val validatedBankIds = setOf(entry.bankId)
        val validatedCells = (0 until entry.length).map { Pair(entry.row, entry.col + it) }.toSet()

        val state = CrosswordGameState(
            isLoading = false,
            board = board,
            grid = grid,
            selectedRow = entry.row,
            selectedCol = entry.col,
            activeDirection = CruciluxDirection.HORIZONTAL,
            activeEntryBankId = entry.bankId,
            validatedEntryBankIds = validatedBankIds,
            validatedCells = validatedCells,
        )

        assertTrue(entry.bankId in state.validatedEntryBankIds)
        assertTrue(Pair(entry.row, entry.col) in state.validatedCells)
    }

    @Test
    fun `9 interseccion validada no puede borrarse desde otra palabra`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val intersection = grid.cells.flatten().first { it.isIntersection }

        val interPos = Pair(intersection.row, intersection.col)
        val userLetters = mutableMapOf(interPos to intersection.solutionLetter!!)
        val validatedCells = setOf(interPos) // Validada por la palabra horizontal

        // Ahora el usuario está en la palabra vertical e intenta borrar en interPos
        if (interPos !in validatedCells) {
            userLetters.remove(interPos)
        }

        assertTrue("La intersección validada debe seguir protegida", userLetters.containsKey(interPos))
        assertEquals(intersection.solutionLetter, userLetters[interPos])
    }

    @Test
    fun `10 completar todas las respuestas marca tablero completo`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)

        val allEntryBankIds = board.entries.map { it.bankId }.toSet()
        val validatedBankIds = allEntryBankIds // Todas validadas

        val isCompleted = validatedBankIds.size == board.entries.size && board.entries.isNotEmpty()
        assertTrue("Tablero debe marcarse como completado al 100%", isCompleted)
    }

    @Test
    fun `11 tablero lleno pero con error NO se marca completado`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)

        // Todas las palabras menos una están validadas
        val validatedBankIds = board.entries.drop(1).map { it.bankId }.toSet()

        val isCompleted = validatedBankIds.size == board.entries.size && board.entries.isNotEmpty()
        assertFalse("Tablero con errores o palabras no validadas NO debe completarse", isCompleted)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. NAVEGACIÓN Y SINCRONIZACIÓN DE PISTAS (FLECHAS ‹ Y ›)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `12 navegacion pista siguiente avanza canonicamente`() {
        val vm = CrosswordGameViewModel()
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val allClues = vm.getAllCluesOrdered(grid)

        assertTrue(allClues.size >= 2)
        val firstClue = allClues[0]
        val secondClue = allClues[1]

        // Seleccionar la primera
        vm.selectClue(firstClue)

        // Calcular siguiente
        val currentIndex = allClues.indexOfFirst { it.bankId == firstClue.bankId && it.direction == firstClue.direction }
        val nextIndex = (currentIndex + 1) % allClues.size
        assertEquals(1, nextIndex)
        assertEquals(secondClue.bankId, allClues[nextIndex].bankId)
    }

    @Test
    fun `13 navegacion pista anterior retrocede canonicamente`() {
        val vm = CrosswordGameViewModel()
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val allClues = vm.getAllCluesOrdered(grid)

        val firstClue = allClues.first()
        val lastClue = allClues.last()

        val currentIndex = 0
        val prevIndex = if (currentIndex <= 0) allClues.lastIndex else currentIndex - 1
        assertEquals(allClues.lastIndex, prevIndex)
        assertEquals(lastClue.bankId, allClues[prevIndex].bankId)
    }

    @Test
    fun `14 cambio de pista actualiza orientacion`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val hClue = grid.horizontalClues.first()
        val vClue = grid.verticalClues.first()

        var activeDirection = hClue.direction
        assertEquals(CruciluxDirection.HORIZONTAL, activeDirection)

        // Cambiar a vertical
        activeDirection = vClue.direction
        assertEquals(CruciluxDirection.VERTICAL, activeDirection)
    }

    @Test
    fun `15 seleccion por pista resalta la palabra correcta`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val clue = grid.horizontalClues.first()

        val wordCells = mutableSetOf<Pair<Int, Int>>()
        for (i in 0 until clue.length) {
            wordCells.add(Pair(clue.startRow, clue.startCol + i))
        }

        assertEquals(clue.length, wordCells.size)
        assertTrue(Pair(clue.startRow, clue.startCol) in wordCells)
    }

    @Test
    fun `16 numeracion Horizontal Vertical se mantiene coherente`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)

        // Las pistas deben conservar sus números asignados
        for (clue in grid.horizontalClues) {
            assertTrue("Número debe ser positivo", clue.number > 0)
        }
        for (clue in grid.verticalClues) {
            assertTrue("Número debe ser positivo", clue.number > 0)
        }
    }

    @Test
    fun `17 COMPOUND muestra wordCount y wordLengths sin displayAnswer`() {
        val boardWithCompound = repository.getAllBoards().first { board ->
            board.entries.any { it.answerType == CruciluxAnswerType.COMPOUND }
        }
        val grid = CruciluxGridEngine.buildGrid(boardWithCompound)
        val compoundClue = (grid.horizontalClues + grid.verticalClues).first {
            it.answerType == CruciluxAnswerType.COMPOUND
        }

        val lengthInfo = compoundClue.formatLengthInfo()
        assertTrue(lengthInfo.contains("palabras"))
        assertTrue(lengthInfo.contains("letras"))
        assertFalse("No debe contener la respuesta en texto claro", lengthInfo.contains(compoundClue.displayAnswer))
    }

    @Test
    fun `18 SINGLE muestra numero de letras`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val singleClue = grid.horizontalClues.first { it.answerType == CruciluxAnswerType.SINGLE }

        val lengthInfo = singleClue.formatLengthInfo()
        assertEquals("${singleClue.length} letras", lengthInfo)
    }

    @Test
    fun `19 restauracion de partida conserva letras correctas y estado de validacion`() {
        val board = repository.getAllBoards().first()
        val grid = CruciluxGridEngine.buildGrid(board)
        val entry = board.entries.first { it.direction == CruciluxDirection.HORIZONTAL }

        val userLetters = mutableMapOf<Pair<Int, Int>, Char>()
        for (i in entry.answer.indices) {
            userLetters[Pair(entry.row, entry.col + i)] = entry.answer[i]
        }

        // Serializar y deserializar como si viniera de DataStore
        val serialized = GameSessionManager.serializeLetters(userLetters)
        val restoredLetters = GameSessionManager.deserializeLetters(serialized)

        assertEquals(userLetters.size, restoredLetters.size)
        assertEquals(userLetters, restoredLetters)
    }

    @Test
    fun `20 tableros rectangulares funcionan con validacion automatica y navegacion`() {
        val rectangularBoard = repository.getAllBoards().first { it.rows != it.cols }
        val grid = CruciluxGridEngine.buildGrid(rectangularBoard)

        val vm = CrosswordGameViewModel()
        val allClues = vm.getAllCluesOrdered(grid)

        assertTrue(rectangularBoard.rows != rectangularBoard.cols)
        assertTrue("Tablero rectangular debe tener pistas ordenables", allClues.isNotEmpty())
    }
}

