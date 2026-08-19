package com.neuronova.crucilux.model

/**
 * Modelo de pista para la interfaz de usuario.
 *
 * Contiene los datos necesarios para mostrar una pista en pantalla sin revelar
 * la respuesta durante el juego activo.
 *
 * @property bankId Identificador único maestro de la entrada en el banco (ej. "CRU00359").
 * @property number Número de pista tal como aparece en el banco y en la cuadrícula.
 * @property clue Texto de la definición o pista. Apto para lectura por TalkBack.
 * @property direction Orientación de la palabra: [CruciluxDirection.HORIZONTAL] o [CruciluxDirection.VERTICAL].
 * @property length Longitud total de la palabra en caracteres.
 * @property startRow Fila de inicio de la palabra (base 0).
 * @property startCol Columna de inicio de la palabra (base 0).
 * @property answerType Tipo de respuesta ([CruciluxAnswerType.SINGLE] o [CruciluxAnswerType.COMPOUND]).
 * @property wordCount Cantidad de palabras individuales.
 * @property wordLengths Lista con la longitud de cada palabra individual.
 * @property displayAnswer Respuesta formateada (SOLO para uso seguro posterior a resolver).
 */
data class CrosswordClue(
    val bankId: String,
    val number: Int,
    val clue: String,
    val direction: CruciluxDirection,
    val length: Int,
    val startRow: Int,
    val startCol: Int,
    val answerType: CruciluxAnswerType = CruciluxAnswerType.SINGLE,
    val wordCount: Int = 1,
    val wordLengths: List<Int> = listOf(length),
    val displayAnswer: String = "",
) {
    /**
     * Formato de longitud seguro para mostrar durante la partida sin revelar la solución.
     * Ejemplo SINGLE: "7 letras"
     * Ejemplo COMPOUND: "2 palabras · 6 + 9 letras"
     * Ejemplo COMPOUND de 3 palabras: "3 palabras · 5 + 2 + 7 letras"
     */
    fun formatLengthInfo(): String {
        return if (answerType == CruciluxAnswerType.COMPOUND && wordCount > 1) {
            val lengthsStr = wordLengths.joinToString(" + ")
            "$wordCount palabras · $lengthsStr letras"
        } else {
            "$length letras"
        }
    }
}
