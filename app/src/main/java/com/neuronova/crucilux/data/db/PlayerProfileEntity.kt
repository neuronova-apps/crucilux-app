package com.neuronova.crucilux.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Perfil global único; se actualiza en la misma transacción que bestXpEarned. */
@Entity(tableName = "player_profile")
data class PlayerProfileEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    val totalXp: Int = 0,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
