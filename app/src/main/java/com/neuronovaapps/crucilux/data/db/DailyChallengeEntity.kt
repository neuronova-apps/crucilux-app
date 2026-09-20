package com.neuronovaapps.crucilux.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.neuronovaapps.crucilux.data.GameSessionManager
import com.neuronovaapps.crucilux.model.CruciluxDirection
import com.neuronovaapps.crucilux.ui.game.CheckMode

/**
 * Estados posibles de un Desafío Diario.
 */
enum class DailyChallengeStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED;

    companion object {
        fun fromString(value: String?): DailyChallengeStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NOT_STARTED
        }
    }
}

/**
 * Entidad de Room para persistir el estado y la sesión aislada del Desafío Diario.
 *
 * Cada fecha local canónica (YYYY-MM-DD) posee exactamente un único registro.
 * Aísla conceptualmente la sesión diaria del progreso histórico general, permitiendo
 * rejugar tableros ya completados previamente sin alterar sus puntuaciones ni degradar
 * las estadísticas históricas de Crucilux.
 */
@Entity(tableName = "daily_challenge")
data class DailyChallengeEntity(
    @PrimaryKey
    val dateKey: String,
    val boardId: String,
    val status: String = DailyChallengeStatus.NOT_STARTED.name,
    val startedAt: Long? = null,
    val completedAt: Long? = null,
    val bestTimeSeconds: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val elapsedTimeSeconds: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val attemptCount: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val isRewardClaimed: Boolean = false,
    @ColumnInfo(defaultValue = "''")
    val userLetters: String = "",
    @ColumnInfo(defaultValue = "0")
    val progressPercent: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val hintsUsed: Int = 0,
    @ColumnInfo(defaultValue = "''")
    val hintRevealedCells: String = "",
    @ColumnInfo(defaultValue = "'CLASSIC'")
    val checkMode: String = "CLASSIC",
    @ColumnInfo(defaultValue = "0")
    val selectedRow: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val selectedCol: Int = 0,
    @ColumnInfo(defaultValue = "'H'")
    val selectedDirection: String = "H",
) {
    val challengeStatus: DailyChallengeStatus
        get() = DailyChallengeStatus.fromString(status)

    val isCompleted: Boolean
        get() = challengeStatus == DailyChallengeStatus.COMPLETED

    val isInProgress: Boolean
        get() = challengeStatus == DailyChallengeStatus.IN_PROGRESS

    val isNotStarted: Boolean
        get() = challengeStatus == DailyChallengeStatus.NOT_STARTED

    fun parseUserLetters(): Map<Pair<Int, Int>, Char> {
        return GameSessionManager.deserializeLetters(userLetters)
    }

    val direction: CruciluxDirection
        get() = if (selectedDirection.equals("V", ignoreCase = true)) {
            CruciluxDirection.VERTICAL
        } else {
            CruciluxDirection.HORIZONTAL
        }

    val parsedCheckMode: CheckMode
        get() = if (checkMode.equals("ASSISTED", ignoreCase = true)) {
            CheckMode.ASSISTED
        } else {
            CheckMode.CLASSIC
        }

    fun parseHintRevealedCells(): Set<Pair<Int, Int>> {
        return CrosswordProgressEntity.deserializePositions(hintRevealedCells)
    }
}
