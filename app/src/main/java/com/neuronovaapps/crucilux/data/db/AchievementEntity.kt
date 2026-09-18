package com.neuronovaapps.crucilux.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Room para la persistencia del estado de cada logro en Crucilux.
 *
 * @property achievementId Identificador único y estable del logro (ej. "first_crossword").
 * @property isUnlocked Indica si el logro ha sido desbloqueado.
 * @property unlockedAt Timestamp en milisegundos de cuando se desbloqueó por primera vez (null si está bloqueado).
 * @property currentProgress Progreso numérico actual del usuario hacia el objetivo.
 * @property targetProgress Meta numérica requerida para desbloquear el logro.
 * @property isNotified Indica si la notificación visual in-app ya fue presentada y confirmada al usuario.
 */
@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey
    val achievementId: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    @ColumnInfo(defaultValue = "0")
    val currentProgress: Int = 0,
    @ColumnInfo(defaultValue = "1")
    val targetProgress: Int = 1,
    @ColumnInfo(defaultValue = "0")
    val isNotified: Boolean = false,
)
