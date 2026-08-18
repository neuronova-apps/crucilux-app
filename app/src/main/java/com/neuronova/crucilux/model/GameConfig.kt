package com.neuronova.crucilux.model

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Modelo de dominio para representar una categoría de crucigrama.
 *
 * @property id Identificador único interno de la categoría.
 * @property displayName Nombre legible para mostrar en la interfaz.
 * @property icon Icono asociado (opcional, para representación visual).
 */
data class CruciluxCategory(
    val id: String,
    val displayName: String,
    val icon: ImageVector? = null,
)

/**
 * Dimensiones y tamaños de cuadrícula soportados por el banco maestro de Crucilux:
 * 7x7, 10x10 y 15x15.
 *
 * @property label Etiqueta visual mostrada en la interfaz (ej. "7x7").
 * @property rows Cantidad de filas del tablero.
 * @property columns Cantidad de columnas del tablero.
 */
enum class CruciluxGridSize(
    val label: String,
    val rows: Int,
    val columns: Int,
) {
    SIZE_7X7(label = "7x7", rows = 7, columns = 7),
    SIZE_10X10(label = "10x10", rows = 10, columns = 10),
    SIZE_15X15(label = "15x15", rows = 15, columns = 15);

    companion object {
        fun fromLabel(label: String): CruciluxGridSize {
            return entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: SIZE_10X10
        }
    }
}

/**
 * Configuración de partida seleccionada por el usuario (Categoría y Tamaño).
 */
data class GameSetupConfig(
    val category: String,
    val size: CruciluxGridSize,
)
