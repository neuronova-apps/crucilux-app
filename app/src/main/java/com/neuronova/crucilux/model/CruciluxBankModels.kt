package com.neuronova.crucilux.model

/**
 * Orientación de las palabras dentro del tablero de crucigrama.
 * Coincide con los valores "HORIZONTAL" y "VERTICAL" del banco JSON v1.37.
 */
enum class CruciluxDirection(val value: String) {
    HORIZONTAL("HORIZONTAL"),
    VERTICAL("VERTICAL");

    companion object {
        fun fromValue(value: String): CruciluxDirection {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: HORIZONTAL
        }
    }
}

/**
 * Tipo seguro para clasificar respuestas simples o compuestas en Crucilux (v1.37).
 * - [SINGLE]: Respuesta de una sola palabra (ej. "LAMPARA", wordCount = 1).
 * - [COMPOUND]: Respuesta compuesta de múltiples palabras (ej. "LEON MARINO", wordCount >= 2).
 */
enum class CruciluxAnswerType(val value: String) {
    SINGLE("SINGLE"),
    COMPOUND("COMPOUND");

    companion object {
        fun fromValue(value: String): CruciluxAnswerType {
            return entries.firstOrNull { it.value.equals(value, ignoreCase = true) } ?: SINGLE
        }
    }
}

/**
 * Modelo de una entrada individual (palabra y pista) en el banco de Crucilux (v1.37).
 * Representa fielmente los campos del contrato schemaVersion 2:
 * - number: Número de referencia de la pista en el crucigrama.
 * - direction: Orientación HORIZONTAL o VERTICAL.
 * - answer: Palabra de respuesta continua en mayúsculas sin espacios (ej. "LEONMARINO").
 * - displayAnswer: Respuesta formateada con espacios y tildes (ej. "LEÓN MARINO").
 * - answerType: Tipo de respuesta ([CruciluxAnswerType.SINGLE] o [CruciluxAnswerType.COMPOUND]).
 * - wordCount: Cantidad de palabras que conforman la respuesta.
 * - wordLengths: Lista con las longitudes de cada palabra individual (ej. [4, 6]).
 * - length: Longitud total en caracteres de la respuesta en la cuadrícula.
 * - row: Fila de inicio (base 0).
 * - col: Columna de inicio (base 0).
 * - bankId: Identificador único maestro de la palabra (ej. "CRU00359").
 * - clue: Texto de la definición o pista.
 */
data class CruciluxEntry(
    val number: Int,
    val direction: CruciluxDirection,
    val answer: String,
    val displayAnswer: String,
    val answerType: CruciluxAnswerType,
    val wordCount: Int,
    val wordLengths: List<Int>,
    val length: Int,
    val row: Int,
    val col: Int,
    val bankId: String,
    val clue: String,
)

/**
 * Modelo de un tablero completo de crucigrama en Crucilux (v1.37).
 * Las dimensiones autoritativas son [rows] y [cols] dinámicas.
 * No depende de campos fijos de tamaño ni asume cuadrículas cuadradas.
 *
 * @property id Identificador único del tablero (ej. "7X7-01", "10X10-45").
 * @property rows Cantidad de filas autoritativa.
 * @property cols Cantidad de columnas autoritativa.
 * @property category Categoría temática real del banco (ej. "Cultura general", "Historia", "Perú").
 * @property subcategory Subcategoría (ej. "No aplica", "AT", "NT", "Ambos").
 * @property entries Lista de entradas/palabras que conforman la cuadrícula.
 */
data class CruciluxBoard(
    val id: String,
    val rows: Int,
    val cols: Int,
    val category: String,
    val subcategory: String,
    val entries: List<CruciluxEntry>,
) {
    /**
     * Etiqueta de dimensión informativa basada en rows x cols (ej. "7x7", "7x6", "10x10").
     */
    val dimensionLabel: String
        get() = "${rows}x${cols}"

    /**
     * Compatibilidad legacy temporal para consultas que esperan `size`.
     */
    val size: String
        get() = dimensionLabel
}

/**
 * Metadatos de cabecera del banco maestro de Crucilux (schemaVersion 2, bankVersion 1.37).
 */
data class CruciluxBankMetadata(
    val schemaVersion: Int,
    val bankVersion: String,
    val app: String,
    val coordinateBase: Int,
    val totalBoards: Int,
    val totalEntries: Int,
    val categories: List<String>,
)
