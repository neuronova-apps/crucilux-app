package com.neuronova.crucilux.model

/**
 * Cuadrícula completa de un crucigrama de Crucilux.
 *
 * Modelo independiente de Compose que encapsula la estructura del tablero:
 * la matriz de celdas y las listas de pistas para la UI.
 *
 * Creado por [com.neuronova.crucilux.engine.CruciluxGridEngine] a partir de
 * un [CruciluxBoard] real del banco maestro.
 *
 * @property rows Número de filas del tablero.
 * @property cols Número de columnas del tablero.
 * @property cells Matriz de celdas [rows × cols]. Acceso: `cells[row][col]`.
 * @property horizontalClues Pistas horizontales ordenadas por número.
 * @property verticalClues Pistas verticales ordenadas por número.
 */
data class CrosswordGrid(
    val rows: Int,
    val cols: Int,
    val cells: List<List<CrosswordCell>>,
    val horizontalClues: List<CrosswordClue>,
    val verticalClues: List<CrosswordClue>,
) {
    /** Número total de celdas activas (pertenecen al crucigrama). */
    val activeCellCount: Int
        get() = cells.sumOf { row -> row.count { it.isActive } }

    /** Número total de celdas inactivas (negras). */
    val inactiveCellCount: Int
        get() = cells.sumOf { row -> row.count { !it.isActive } }

    /** Número total de intersecciones (celdas compartidas por horizontal y vertical). */
    val intersectionCount: Int
        get() = cells.sumOf { row -> row.count { it.isIntersection } }

    /**
     * Retorna la celda en la posición dada, o null si las coordenadas están fuera de límites.
     */
    fun cellAt(row: Int, col: Int): CrosswordCell? {
        if (row < 0 || row >= rows || col < 0 || col >= cols) return null
        return cells[row][col]
    }
}
