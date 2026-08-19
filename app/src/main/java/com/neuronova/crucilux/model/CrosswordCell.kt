package com.neuronova.crucilux.model

/**
 * Modelo de dominio para una celda individual de la cuadrícula de crucigrama.
 *
 * Este modelo es independiente de Compose y representa el estado estructural
 * de una celda: si está activa, qué número de pista muestra, con qué entradas
 * se relaciona (horizontal, vertical) y si es una intersección.
 *
 * La letra interna ([solutionLetter]) existe únicamente para que el motor valide
 * intersecciones. No debe exponerse a la UI ni incluirse en contentDescription.
 *
 * @property row Fila de la celda (base 0).
 * @property col Columna de la celda (base 0).
 * @property isActive `true` si la celda pertenece al crucigrama (tiene una letra válida).
 * @property clueNumber Número de pista mostrado en la esquina superior izquierda, o null.
 * @property horizontalEntryBankId bankId de la entrada horizontal que pasa por esta celda, o null.
 * @property verticalEntryBankId bankId de la entrada vertical que pasa por esta celda, o null.
 * @property isWordStart `true` si la celda es el inicio de al menos una palabra.
 * @property solutionLetter Letra de la solución. SOLO para validación interna del motor.
 *   No debe renderizarse ni incluirse en texto de accesibilidad.
 */
data class CrosswordCell(
    val row: Int,
    val col: Int,
    val isActive: Boolean = false,
    val clueNumber: Int? = null,
    val horizontalEntryBankId: String? = null,
    val verticalEntryBankId: String? = null,
    val isWordStart: Boolean = false,
    val solutionLetter: Char? = null,
) {
    /** `true` si la celda es compartida por una entrada horizontal y una vertical. */
    val isIntersection: Boolean
        get() = horizontalEntryBankId != null && verticalEntryBankId != null
}
