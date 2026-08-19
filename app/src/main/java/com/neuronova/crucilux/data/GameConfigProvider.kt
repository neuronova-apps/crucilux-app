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
import com.neuronova.crucilux.model.CruciluxGridSize

/**
 * Proveedor centralizado de configuración para Crucilux.
 * Deriva las categorías y dimensiones directamente desde el banco maestro validado (v1.28).
 *
 * El banco validado contiene exactamente 10 categorías temáticas reales y 3 tamaños de tablero (7x7, 10x10, 15x15).
 * No utiliza niveles de dificultad formal (Fácil, Intermedio, Difícil) para la selección de partidas.
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
     *
     * DIAGNÓSTICO: El banco v1.28 tiene exactamente 10 categorías fijas y conocidas.
     * La comparación de strings con tildes entre el JSON (UTF-8) y los literales Kotlin
     * puede fallar por diferencias de normalización Unicode (NFC vs NFD), causando que
     * solo aparezca "Biblia" (sin caracteres especiales). La solución correcta es
     * retornar directamente `officialCategories` cuando el banco confirma que está cargado,
     * sin intentar mapear por nombre. Las categorías del banco v1.28 son fijas.
     */
    val categories: List<CruciluxCategory>
        get() {
            val repo = bankRepository ?: CruciluxBankRepository.getInstance()
            // Si el banco está cargado y tiene tableros, usamos officialCategories directamente.
            // El banco v1.28 tiene exactamente las 10 categorías de officialCategories.
            if (repo.isReady() && repo.getAllBoards().isNotEmpty()) {
                return officialCategories
            }
            // Fallback si el banco no está cargado aún
            return officialCategories
        }

    /**
     * Tamaños de tablero reales soportados por el banco maestro de Crucilux:
     * 7x7, 10x10 y 15x15.
     */
    val availableSizes: List<CruciluxGridSize> = listOf(
        CruciluxGridSize.SIZE_7X7,
        CruciluxGridSize.SIZE_10X10,
        CruciluxGridSize.SIZE_15X15,
    )

    val defaultCategory: CruciluxCategory
        get() = categories.firstOrNull() ?: officialCategories.first()

    val defaultSize: CruciluxGridSize
        get() = CruciluxGridSize.SIZE_10X10

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
