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
 * Configuración de partida seleccionada por el usuario (Categoría temática).
 */
data class GameSetupConfig(
    val category: String,
)
