package com.neuronovaapps.crucilux.achievements

/**
 * Definición estática de un logro dentro del ecosistema Crucilux.
 *
 * @property id Identificador único y estable (ej. "first_crossword").
 * @property name Nombre visible del logro (ej. "Primer Crucigrama").
 * @property description Descripción de lo que representa el logro.
 * @property condition Descripción clara de la condición requerida.
 * @property targetProgress Meta numérica necesaria para el desbloqueo.
 * @property initialLetter Letra representativa para el glifo o medalla visual.
 * @property unitLabel Etiqueta de la unidad de progreso (ej. "crucigrama", "palabras").
 * @property xpReward XP simbólica asociada al logro (0 en esta fase para no alterar la progresión).
 */
data class AchievementDefinition(
    val id: String,
    val name: String,
    val description: String,
    val condition: String,
    val targetProgress: Int,
    val initialLetter: String,
    val unitLabel: String = "crucigramas",
    val xpReward: Int = 0,
)

/**
 * Catálogo maestro de logros oficiales iniciales de Crucilux.
 */
object CruciluxAchievements {

    const val ID_FIRST_CROSSWORD = "first_crossword"
    const val ID_WORD_MASTER = "word_master"
    const val ID_GRAND_GRID = "grand_grid"
    const val ID_MASTER_SOLVER = "master_solver"

    val FIRST_CROSSWORD = AchievementDefinition(
        id = ID_FIRST_CROSSWORD,
        name = "Primer Crucigrama",
        description = "Completa tu primer crucigrama",
        condition = "completedBoards >= 1",
        targetProgress = 1,
        initialLetter = "P",
        unitLabel = "crucigrama",
        xpReward = 0,
    )

    val WORD_MASTER = AchievementDefinition(
        id = ID_WORD_MASTER,
        name = "Vocabulario de Oro",
        description = "Encuentra 50 palabras correctas",
        condition = "50 palabras correctas acumuladas",
        targetProgress = 50,
        initialLetter = "V",
        unitLabel = "palabras",
        xpReward = 0,
    )

    val GRAND_GRID = AchievementDefinition(
        id = ID_GRAND_GRID,
        name = "Gran Tablero",
        description = "Resuelve un crucigrama de 15×15",
        condition = "completar un tablero 15x15",
        targetProgress = 1,
        initialLetter = "G",
        unitLabel = "tablero 15×15",
        xpReward = 0,
    )

    val MASTER_SOLVER = AchievementDefinition(
        id = ID_MASTER_SOLVER,
        name = "Maestro de Letras",
        description = "Completa 30 crucigramas",
        condition = "completedBoards >= 30",
        targetProgress = 30,
        initialLetter = "L",
        unitLabel = "crucigramas",
        xpReward = 0,
    )

    val ALL: List<AchievementDefinition> = listOf(
        FIRST_CROSSWORD,
        WORD_MASTER,
        GRAND_GRID,
        MASTER_SOLVER,
    )

    fun getById(id: String): AchievementDefinition? = ALL.firstOrNull { it.id == id }
}
