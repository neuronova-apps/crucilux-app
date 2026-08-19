package com.neuronova.crucilux.progression

import com.neuronova.crucilux.ui.game.CheckMode
import com.neuronova.crucilux.data.db.CrosswordBoardStatus

object GameStartRules {
    fun shouldRequestMode(status: CrosswordBoardStatus): Boolean {
        return status == CrosswordBoardStatus.NOT_STARTED
    }
}

object HintRules {
    fun isProtected(isValidated: Boolean, isHintRevealed: Boolean): Boolean {
        return isValidated || isHintRevealed
    }

    fun canReveal(
        isPlayable: Boolean,
        currentLetter: Char?,
        expectedLetter: Char?,
        isValidated: Boolean,
        isAlreadyRevealed: Boolean,
    ): Boolean {
        return isPlayable && expectedLetter != null && !isProtected(isValidated, isAlreadyRevealed) &&
            currentLetter?.uppercaseChar() != expectedLetter.uppercaseChar()
    }

    fun requiresAdditionalConfirmation(hintsUsed: Int): Boolean = hintsUsed >= XpCalculator.INITIAL_HINTS
}

/** Reglas deterministas de XP basadas únicamente en respuestas, modo y pistas. */
object XpCalculator {
    const val INITIAL_HINTS = 3

    fun baseXp(entryCount: Int): Int = when {
        entryCount <= 6 -> 100
        entryCount <= 8 -> 120
        else -> 140
    }

    fun modeXp(entryCount: Int, mode: CheckMode): Int {
        val base = baseXp(entryCount)
        return if (mode == CheckMode.ASSISTED) (base * 80) / 100 else base
    }

    fun hintPenalty(hintsUsed: Int): Int {
        val normalized = hintsUsed.coerceAtLeast(0)
        val initialPenalty = minOf(normalized, INITIAL_HINTS) * 10
        val additionalPenalty = (normalized - INITIAL_HINTS).coerceAtLeast(0) * 20
        return initialPenalty + additionalPenalty
    }

    fun finalXp(entryCount: Int, mode: CheckMode, hintsUsed: Int): Int {
        return (modeXp(entryCount, mode) - hintPenalty(hintsUsed)).coerceAtLeast(0)
    }
}

/**
 * Umbrales diseñados sobre el máximo teórico de 32.000 XP del banco v1.37.
 * Leyenda Crucilux se alcanza con 29.000 XP (90,625 % del máximo), sin exigir
 * una finalización perfecta del 100 % de los 300 tableros.
 */
enum class PlayerLevel(
    val displayName: String,
    val minimumXp: Int,
) {
    NOVATO("Novato", 0),
    EXPLORADOR_VERBAL("Explorador verbal", 500),
    APRENDIZ_LEXICO("Aprendiz léxico", 1_500),
    RETADOR("Retador", 3_000),
    ESTRATEGA("Estratega", 5_000),
    ANALISTA_DE_PALABRAS("Analista de palabras", 8_000),
    EXPERTO_VERBAL("Experto verbal", 12_000),
    MAESTRO_DEL_CRUCIGRAMA("Maestro del crucigrama", 17_000),
    GRAN_MAESTRO_LEXICO("Gran Maestro léxico", 22_000),
    LEYENDA("Leyenda", 26_000),
    LEYENDA_CRUCILUX("Leyenda Crucilux", 29_000);

    val levelNumber: Int get() = ordinal + 1

    companion object {
        fun forXp(totalXp: Int): PlayerLevel {
            val normalized = totalXp.coerceAtLeast(0)
            return entries.last { normalized >= it.minimumXp }
        }
    }
}

data class PlayerProgress(
    val totalXp: Int = 0,
) {
    val level: PlayerLevel = PlayerLevel.forXp(totalXp)
    val nextLevel: PlayerLevel? = PlayerLevel.entries.getOrNull(level.ordinal + 1)
    val nextLevelXp: Int? = nextLevel?.minimumXp
    val xpWithinLevel: Int = totalXp - level.minimumXp
    val xpNeededWithinLevel: Int = nextLevel?.minimumXp?.minus(level.minimumXp) ?: 0
    val levelProgress: Float = if (nextLevel == null || xpNeededWithinLevel <= 0) {
        1f
    } else {
        (xpWithinLevel.toFloat() / xpNeededWithinLevel.toFloat()).coerceIn(0f, 1f)
    }
}
