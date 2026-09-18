package com.neuronovaapps.crucilux.achievements

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Estado completo de un logro para ser consumido por la UI de Crucilux.
 *
 * @property definition Metadatos estáticos del logro.
 * @property currentProgress Progreso numérico alcanzado por el usuario.
 * @property isUnlocked Indica si el logro se encuentra desbloqueado.
 * @property unlockedAt Timestamp en milisegundos de cuándo se desbloqueó, o null si está bloqueado.
 * @property isNotified Indica si la notificación visual in-app ya fue mostrada y confirmada.
 */
data class AchievementState(
    val definition: AchievementDefinition,
    val currentProgress: Int = 0,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val isNotified: Boolean = false,
) {
    val id: String get() = definition.id
    val name: String get() = definition.name
    val description: String get() = definition.description
    val condition: String get() = definition.condition
    val targetProgress: Int get() = definition.targetProgress
    val initialLetter: String get() = definition.initialLetter

    /**
     * Progreso en rango 0f..1f para indicadores visuales.
     */
    val progressFraction: Float
        get() = if (targetProgress > 0) {
            (currentProgress.toFloat() / targetProgress.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    /**
     * Progreso porcentual entero 0..100.
     */
    val progressPercent: Int
        get() = (progressFraction * 100).toInt().coerceIn(0, 100)

    /**
     * Fecha formateada de desbloqueo (ej. "18/09/2026"), o null si está bloqueado.
     */
    val formattedUnlockDate: String?
        get() = unlockedAt?.let { timestamp ->
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.format(Date(timestamp))
        }

    /**
     * Texto semántico accesible para TalkBack y lectores de pantalla.
     * Ejemplo desbloqueado: "Primer Crucigrama, desbloqueado el 18/09/2026"
     * Ejemplo bloqueado: "Maestro de Letras, bloqueado, 12 de 30 crucigramas completados"
     */
    val contentDescription: String
        get() = if (isUnlocked) {
            val dateText = formattedUnlockDate?.let { " el $it" }.orEmpty()
            "${definition.name}, desbloqueado$dateText"
        } else {
            val unit = definition.unitLabel
            "${definition.name}, bloqueado, $currentProgress de $targetProgress $unit completados"
        }
}

/**
 * Resumen global del sistema de logros para Home y ProgressScreen.
 */
data class AchievementSummary(
    val unlockedCount: Int = 0,
    val totalCount: Int = CruciluxAchievements.ALL.size,
) {
    val progressPercent: Int
        get() = if (totalCount > 0) ((unlockedCount * 100) / totalCount).coerceIn(0, 100) else 0

    val isAllUnlocked: Boolean
        get() = totalCount > 0 && unlockedCount >= totalCount

    val labelText: String
        get() = "$unlockedCount de $totalCount desbloqueados"
}
