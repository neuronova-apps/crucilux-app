package com.neuronova.crucilux.model

/**
 * Modelo de pista para la interfaz de usuario.
 *
 * Contiene únicamente los datos necesarios para mostrar una pista en pantalla.
 * No incluye la respuesta. El [bankId] real de la entrada se conserva para
 * uso futuro (ej. selección de palabra activa).
 *
 * @property bankId Identificador único maestro de la entrada en el banco (ej. "CRU00359").
 * @property number Número de pista tal como aparece en el banco y en la cuadrícula.
 * @property clue Texto de la definición o pista. Apto para lectura por TalkBack.
 * @property direction Orientación de la palabra: [CruciluxDirection.HORIZONTAL] o [CruciluxDirection.VERTICAL].
 * @property length Longitud de la palabra en caracteres.
 * @property startRow Fila de inicio de la palabra (base 0).
 * @property startCol Columna de inicio de la palabra (base 0).
 */
data class CrosswordClue(
    val bankId: String,
    val number: Int,
    val clue: String,
    val direction: CruciluxDirection,
    val length: Int,
    val startRow: Int,
    val startCol: Int,
)
