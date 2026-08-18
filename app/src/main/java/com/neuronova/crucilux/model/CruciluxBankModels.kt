package com.neuronova.crucilux.model

/**
 * Orientación de las palabras dentro del tablero de crucigrama.
 * Coincide estrictamente con los valores "horizontal" y "vertical" de crucilux_bank_v1_28.json.
 */
enum class CruciluxDirection(val value: String) {
    HORIZONTAL("horizontal"),
    VERTICAL("vertical");

    companion object {
        fun fromValue(value: String): CruciluxDirection {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: HORIZONTAL
        }
    }
}

/**
 * Modelo de una entrada individual (palabra y pista) en el banco de Crucilux.
 * Representa fielmente los campos del JSON:
 * - number: Número de referencia de la pista en el crucigrama.
 * - direction: Orientación horizontal o vertical.
 * - answer: Palabra de respuesta en mayúsculas.
 * - length: Longitud en caracteres de la respuesta.
 * - row: Fila de inicio (base 0).
 * - col: Columna de inicio (base 0).
 * - bankId: Identificador único maestro de la palabra (ej. "CRU00359").
 * - clue: Texto de la definición o pista.
 */
data class CruciluxEntry(
    val number: Int,
    val direction: CruciluxDirection,
    val answer: String,
    val length: Int,
    val row: Int,
    val col: Int,
    val bankId: String,
    val clue: String,
)

/**
 * Modelo de un tablero completo de crucigrama en Crucilux.
 * Representa fielmente los campos del JSON:
 * - id: Identificador único del tablero (ej. "7X7-01", "10X10-45", "15X15-100").
 * - size: Etiqueta de dimensión ("7x7", "10x10", "15x15").
 * - rows: Cantidad de filas.
 * - cols: Cantidad de columnas.
 * - category: Categoría temática real del banco (ej. "Cultura general", "Historia", "Perú").
 * - subcategory: Subcategoría (ej. "No aplica", "AT", "NT", "Ambos").
 * - entries: Lista de entradas/palabras que conforman la cuadrícula.
 */
data class CruciluxBoard(
    val id: String,
    val size: String,
    val rows: Int,
    val cols: Int,
    val category: String,
    val subcategory: String,
    val entries: List<CruciluxEntry>,
)

/**
 * Metadatos de cabecera del banco maestro de Crucilux.
 */
data class CruciluxBankMetadata(
    val schemaVersion: String,
    val bankVersion: String,
    val app: String,
    val coordinateBase: Int,
    val totalBoards: Int,
    val totalEntries: Int,
    val sizes: Map<String, Int>,
    val categories: List<String>,
)
