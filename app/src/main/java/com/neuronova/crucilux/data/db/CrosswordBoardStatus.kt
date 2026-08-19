package com.neuronova.crucilux.data.db

/**
 * Estados del ciclo de vida de un tablero de Crucilux.
 *
 * - [NOT_STARTED]: Tablero nunca iniciado, sin letras introducidas (0%).
 * - [IN_PROGRESS]: Tablero en curso, con al menos una letra introducida (1-99%).
 * - [COMPLETED]: Todas las respuestas validadas correctamente (100%).
 */
enum class CrosswordBoardStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED;

    companion object {
        fun fromString(value: String?): CrosswordBoardStatus {
            return when (value?.uppercase()?.trim()) {
                "COMPLETED" -> COMPLETED
                "IN_PROGRESS" -> IN_PROGRESS
                else -> NOT_STARTED
            }
        }
    }
}
