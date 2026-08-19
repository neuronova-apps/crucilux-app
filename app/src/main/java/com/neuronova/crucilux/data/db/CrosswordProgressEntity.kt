package com.neuronova.crucilux.data.db

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey
import com.neuronova.crucilux.data.GameSessionManager
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.ui.game.CheckMode

/**
 * Entidad de Room para persistir el progreso independiente de cada uno de los 300 tableros.
 *
 * Cumple estrictamente con las reglas de Crucilux:
 * - `boardId` como PRIMARY KEY.
 * - NO almacena respuestas, soluciones (`answer`, `displayAnswer`) ni pistas (`clue`).
 * - Mantiene el estado del ciclo de vida y porcentaje de progreso (0..100).
 */
@Entity(tableName = "crossword_progress")
data class CrosswordProgressEntity(
    @PrimaryKey
    val boardId: String,
    val category: String,
    val status: String = CrosswordBoardStatus.NOT_STARTED.name,
    val progressPercent: Int = 0,
    val userLetters: String = "",
    val selectedRow: Int = 0,
    val selectedCol: Int = 0,
    val selectedDirection: String = "H",
    val checkMode: String = "CLASSIC",
    @ColumnInfo(defaultValue = "0")
    val hintsUsed: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val bestXpEarned: Int = 0,
    @ColumnInfo(defaultValue = "''")
    val hintRevealedCells: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
) {
    /**
     * Retorna el estado tipado del tablero.
     */
    val boardStatus: CrosswordBoardStatus
        get() = CrosswordBoardStatus.fromString(status)

    /**
     * Retorna las letras introducidas deserializadas en un mapa posicional.
     */
    fun parseUserLetters(): Map<Pair<Int, Int>, Char> {
        return GameSessionManager.deserializeLetters(userLetters)
    }

    /**
     * Retorna la orientación activa de juego.
     */
    val direction: CruciluxDirection
        get() = if (selectedDirection.equals("V", ignoreCase = true)) {
            CruciluxDirection.VERTICAL
        } else {
            CruciluxDirection.HORIZONTAL
        }

    /**
     * Retorna el modo de comprobación.
     */
    val parsedCheckMode: CheckMode
        get() = if (checkMode.equals("ASSISTED", ignoreCase = true)) {
            CheckMode.ASSISTED
        } else {
            CheckMode.CLASSIC
        }

    fun parseHintRevealedCells(): Set<Pair<Int, Int>> {
        return deserializePositions(hintRevealedCells)
    }

    companion object {
        fun serializePositions(positions: Set<Pair<Int, Int>>): String {
            return positions
                .sortedWith(compareBy<Pair<Int, Int>> { it.first }.thenBy { it.second })
                .joinToString(";") { (row, col) -> "$row,$col" }
        }

        fun deserializePositions(raw: String?): Set<Pair<Int, Int>> {
            if (raw.isNullOrBlank()) return emptySet()
            return raw.split(';').mapNotNull { segment ->
                val parts = segment.split(',')
                if (parts.size != 2) return@mapNotNull null
                val row = parts[0].toIntOrNull() ?: return@mapNotNull null
                val col = parts[1].toIntOrNull() ?: return@mapNotNull null
                Pair(row, col)
            }.toSet()
        }
    }
}
