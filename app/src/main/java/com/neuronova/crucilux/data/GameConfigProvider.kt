package com.neuronova.crucilux.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import com.neuronova.crucilux.model.CruciluxCategory
import com.neuronova.crucilux.model.CruciluxDifficulty
import com.neuronova.crucilux.model.CruciluxGridSize

/**
 * Proveedor centralizado de configuración para Crucilux.
 *
 * NOTA DE ARQUITECTURA / INTEGRACIÓN DEL BANCO:
 * 1. Las categorías aquí listadas son EXCLUSIVAMENTE PLACEHOLDERS PROVISIONALES
 *    de interfaz y NO corresponden al contenido final.
 * 2. Están concentradas en este único punto para que, al integrar el archivo
 *    del banco maestro desde `app/src/main/assets/`, se reemplacen sin necesidad
 *    de reescribir ni tocar los componentes de la interfaz de usuario.
 * 3. Las pantallas de UI consumen sus opciones únicamente a través de este proveedor.
 */
object GameConfigProvider {

    /**
     * Categorías provisionales de prueba de interfaz.
     * Reemplazar cuando se conecte el parser del banco JSON maestro.
     */
    val provisionalCategories: List<CruciluxCategory> = listOf(
        CruciluxCategory(id = "general", displayName = "General", icon = Icons.Default.Dashboard),
        CruciluxCategory(id = "ciencia", displayName = "Ciencia", icon = Icons.Default.Science),
        CruciluxCategory(id = "historia", displayName = "Historia", icon = Icons.AutoMirrored.Filled.MenuBook),
        CruciluxCategory(id = "arte", displayName = "Arte", icon = Icons.Default.Palette),
        CruciluxCategory(id = "naturaleza", displayName = "Naturaleza", icon = Icons.Default.Park),
        CruciluxCategory(id = "geografia", displayName = "Geografía", icon = Icons.Default.Public),
    )

    /**
     * Tamaños de tablero reales soportados por el banco maestro de Crucilux:
     * 7x7, 10x10 y 15x15.
     */
    val availableSizes: List<CruciluxGridSize> = listOf(
        CruciluxGridSize.SIZE_7X7,
        CruciluxGridSize.SIZE_10X10,
        CruciluxGridSize.SIZE_15X15,
    )

    /**
     * Niveles de dificultad disponibles en Crucilux.
     */
    val availableDifficulties: List<CruciluxDifficulty> = listOf(
        CruciluxDifficulty.EASY,
        CruciluxDifficulty.MEDIUM,
        CruciluxDifficulty.HARD,
    )

    val defaultCategory: CruciluxCategory
        get() = provisionalCategories.first()

    val defaultSize: CruciluxGridSize
        get() = CruciluxGridSize.SIZE_10X10

    val defaultDifficulty: CruciluxDifficulty
        get() = CruciluxDifficulty.MEDIUM
}
