package com.neuronova.crucilux.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import com.neuronova.crucilux.data.bank.CruciluxBankRepository
import com.neuronova.crucilux.model.CruciluxCategory

/**
 * Proveedor centralizado de configuración para Crucilux.
 * Deriva las categorías temáticas directamente desde el banco maestro validado (v1.37).
 *
 * El banco validado contiene exactamente 10 categorías temáticas reales.
 * Las dimensiones son dinámicas por tablero y no requieren selección de tamaño fija.
 */
object GameConfigProvider {

    private var bankRepository: CruciluxBankRepository? = null

    /**
     * Lista de las 10 categorías temáticas reales del banco maestro con sus iconos visuales.
     */
    val officialCategories: List<CruciluxCategory> = listOf(
        CruciluxCategory(id = "cultura_general", displayName = "Cultura general", icon = Icons.Default.Public),
        CruciluxCategory(id = "ciencia", displayName = "Ciencia", icon = Icons.Default.Science),
        CruciluxCategory(id = "naturaleza", displayName = "Naturaleza", icon = Icons.Default.Park),
        CruciluxCategory(id = "animales", displayName = "Animales", icon = Icons.Default.Pets),
        CruciluxCategory(id = "geografia", displayName = "Geografía", icon = Icons.Default.Terrain),
        CruciluxCategory(id = "historia", displayName = "Historia", icon = Icons.AutoMirrored.Filled.MenuBook),
        CruciluxCategory(id = "tecnologia", displayName = "Tecnología", icon = Icons.Default.Memory),
        CruciluxCategory(id = "cine", displayName = "Cine", icon = Icons.Default.Movie),
        CruciluxCategory(id = "biblia", displayName = "Biblia", icon = Icons.Default.AutoStories),
        CruciluxCategory(id = "peru", displayName = "Perú", icon = Icons.Default.Flag),
    )

    /**
     * Categorías activas disponibles para la interfaz de usuario.
     */
    val categories: List<CruciluxCategory>
        get() {
            val repo = bankRepository ?: CruciluxBankRepository.getInstance()
            if (repo.isReady() && repo.getAllBoards().isNotEmpty()) {
                return officialCategories
            }
            return officialCategories
        }

    val defaultCategory: CruciluxCategory
        get() = categories.firstOrNull() ?: officialCategories.first()

    /**
     * Inicializa el proveedor con el repositorio del banco maestro.
     */
    fun initialize(repository: CruciluxBankRepository) {
        this.bankRepository = repository
    }

    /**
     * Retorna el icono asociado a una categoría por nombre.
     */
    fun getIconForCategory(name: String): ImageVector {
        return when (name.lowercase().trim()) {
            "cultura general" -> Icons.Default.Public
            "ciencia" -> Icons.Default.Science
            "naturaleza" -> Icons.Default.Park
            "animales" -> Icons.Default.Pets
            "geografía", "geografia" -> Icons.Default.Terrain
            "historia" -> Icons.AutoMirrored.Filled.MenuBook
            "tecnología", "tecnologia" -> Icons.Default.Memory
            "cine" -> Icons.Default.Movie
            "biblia" -> Icons.Default.AutoStories
            "perú", "peru" -> Icons.Default.Flag
            else -> Icons.Default.Category
        }
    }
}
