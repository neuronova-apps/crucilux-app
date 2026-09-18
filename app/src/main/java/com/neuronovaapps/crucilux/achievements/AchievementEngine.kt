package com.neuronovaapps.crucilux.achievements

import com.neuronovaapps.crucilux.data.db.AchievementEntity
import com.neuronovaapps.crucilux.data.db.CrosswordBoardStatus
import com.neuronovaapps.crucilux.data.db.CrosswordProgressEntity
import com.neuronovaapps.crucilux.model.CruciluxBoard
import com.neuronovaapps.crucilux.model.CruciluxDirection

/**
 * Motor puro y determinista de cálculo y evaluación de logros en Crucilux.
 * No depende del framework de Android ni de Compose, facilitando pruebas unitarias exhaustivas.
 */
object AchievementEngine {

    /**
     * Evalúa el estado de todos los logros definidos con base en el progreso actual de tableros
     * y el estado previamente persistido de los logros.
     *
     * Reglas garantizadas:
     * 1. Un logro desbloqueado nunca vuelve a bloquearse.
     * 2. El timestamp de desbloqueo [unlockedAt] se fija en el primer desbloqueo y nunca se altera.
     * 3. El progreso [currentProgress] es monotónico (nunca decrece por reinicios accidentales).
     * 4. Si el logro ya fue desbloqueado previamente, conserva su estado de notificación [isNotified].
     * 5. Si el logro se desbloquea por primera vez en esta evaluación, [isNotified] se inicializa en false.
     *
     * @param definitions Lista de logros a evaluar (por defecto [CruciluxAchievements.ALL]).
     * @param progressList Lista completa de entidades de progreso de tableros en Room.
     * @param existingEntities Mapa de entidades de logros previamente persistidas en Room.
     * @param getBoard Función para resolver los metadatos de un tablero (filas, columnas, palabras).
     * @param nowMs Timestamp actual en milisegundos para registrar la fecha de nuevo desbloqueo.
     * @return Lista de [AchievementEntity] actualizadas listas para ser persistidas.
     */
    fun evaluate(
        definitions: List<AchievementDefinition> = CruciluxAchievements.ALL,
        progressList: List<CrosswordProgressEntity>,
        existingEntities: Map<String, AchievementEntity> = emptyMap(),
        getBoard: (String) -> CruciluxBoard?,
        nowMs: Long = System.currentTimeMillis(),
    ): List<AchievementEntity> {
        val completedEntities = progressList.filter { it.status == CrosswordBoardStatus.COMPLETED.name }
        val completedBoardsCount = completedEntities.size

        // Detección de tablero 15x15 completado (ej. 15X15-01 o dimensiones >= 15)
        val hasCompleted15x15 = completedEntities.any { entity ->
            val board = getBoard(entity.boardId)
            board != null && (board.id.startsWith("15X15", ignoreCase = true) || board.rows >= 15 || board.cols >= 15)
        }

        // Conteo de palabras correctas acumuladas
        val accumulatedCorrectWords = calculateTotalCorrectWords(progressList, getBoard)

        return definitions.map { def ->
            val existing = existingEntities[def.id]
            val wasUnlocked = existing?.isUnlocked == true

            val (evaluatedProgress, conditionMet) = when (def.id) {
                CruciluxAchievements.ID_FIRST_CROSSWORD -> {
                    Pair(completedBoardsCount, completedBoardsCount >= 1)
                }
                CruciluxAchievements.ID_WORD_MASTER -> {
                    Pair(accumulatedCorrectWords, accumulatedCorrectWords >= 50)
                }
                CruciluxAchievements.ID_GRAND_GRID -> {
                    Pair(if (hasCompleted15x15) 1 else 0, hasCompleted15x15)
                }
                CruciluxAchievements.ID_MASTER_SOLVER -> {
                    Pair(completedBoardsCount, completedBoardsCount >= 30)
                }
                else -> {
                    Pair(existing?.currentProgress ?: 0, wasUnlocked)
                }
            }

            val isNowUnlocked = wasUnlocked || conditionMet

            // El timestamp se mantiene si ya estaba desbloqueado; si es nuevo, se toma nowMs.
            val unlockedAt = when {
                wasUnlocked -> existing?.unlockedAt ?: nowMs
                isNowUnlocked -> nowMs
                else -> null
            }

            // Progreso monotónico que no retrocede y se acota a la meta si ya está desbloqueado
            val previousProgress = existing?.currentProgress ?: 0
            val effectiveProgress = if (isNowUnlocked) {
                maxOf(def.targetProgress, evaluatedProgress, previousProgress)
            } else {
                maxOf(previousProgress, evaluatedProgress).coerceAtMost(def.targetProgress)
            }

            val isNotified = if (wasUnlocked) {
                existing?.isNotified ?: false
            } else {
                false
            }

            AchievementEntity(
                achievementId = def.id,
                isUnlocked = isNowUnlocked,
                unlockedAt = unlockedAt,
                currentProgress = effectiveProgress,
                targetProgress = def.targetProgress,
                isNotified = isNotified,
            )
        }
    }

    /**
     * Calcula la cantidad total de palabras resueltas correctamente acumuladas entre:
     * 1. Todas las entradas de cada tablero con estado [COMPLETED].
     * 2. Entradas individuales completamente resueltas y correctas en tableros [IN_PROGRESS].
     */
    fun calculateTotalCorrectWords(
        progressList: List<CrosswordProgressEntity>,
        getBoard: (String) -> CruciluxBoard?,
    ): Int {
        var totalWords = 0

        for (progress in progressList) {
            val board = getBoard(progress.boardId) ?: continue

            when (progress.status) {
                CrosswordBoardStatus.COMPLETED.name -> {
                    // En un tablero completado, todas las palabras están resueltas.
                    totalWords += board.entries.size
                }
                CrosswordBoardStatus.IN_PROGRESS.name -> {
                    // En tableros en progreso, verificar qué palabras completas están correctamente rellenadas.
                    val userLetters = progress.parseUserLetters()
                    if (userLetters.isNotEmpty()) {
                        for (entry in board.entries) {
                            val isWordCompleteAndCorrect = (0 until entry.length).all { offset ->
                                val r = if (entry.direction == CruciluxDirection.VERTICAL) entry.row + offset else entry.row
                                val c = if (entry.direction == CruciluxDirection.HORIZONTAL) entry.col + offset else entry.col
                                val entered = userLetters[Pair(r, c)]?.uppercaseChar()
                                val expected = entry.answer.getOrNull(offset)?.uppercaseChar()
                                entered != null && expected != null && entered == expected
                            }
                            if (isWordCompleteAndCorrect) {
                                totalWords++
                            }
                        }
                    }
                }
            }
        }

        return totalWords
    }

    /**
     * Convierte una lista de [AchievementEntity] y el catálogo de [AchievementDefinition]
     * en modelos de estado [AchievementState] listos para la UI.
     */
    fun toStates(
        definitions: List<AchievementDefinition> = CruciluxAchievements.ALL,
        entities: List<AchievementEntity>,
    ): List<AchievementState> {
        val entityMap = entities.associateBy { it.achievementId }

        return definitions.map { def ->
            val entity = entityMap[def.id]
            AchievementState(
                definition = def,
                currentProgress = entity?.currentProgress ?: 0,
                isUnlocked = entity?.isUnlocked ?: false,
                unlockedAt = entity?.unlockedAt,
                isNotified = entity?.isNotified ?: false,
            )
        }
    }
}
