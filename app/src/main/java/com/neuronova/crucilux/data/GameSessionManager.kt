package com.neuronova.crucilux.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.gameSessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "crucilux_game_session")

/**
 * Estado persistente de una sesión de partida en curso.
 *
 * @property boardId ID del tablero (ej. "7X7-01", "10X10-15").
 * @property category Categoría del crucigrama.
 * @property boardSize Tamaño del tablero ("7x7", "10x10", "15x15").
 * @property selectedRow Fila de la celda con foco (-1 si ninguna).
 * @property selectedCol Columna de la celda con foco (-1 si ninguna).
 * @property activeDirection Dirección de la palabra activa ("H" o "V").
 * @property checkMode Modo de comprobación ("CLASSIC" o "ASSISTED").
 * @property userLetters Letras introducidas por el usuario: (row, col) -> Char.
 * @property isFinished `true` si la partida fue terminada.
 * @property lastUpdatedMs Timestamp de la última modificación en milisegundos.
 */
data class GameSessionState(
    val boardId: String = "",
    val category: String = "",
    val boardSize: String = "",
    val selectedRow: Int = -1,
    val selectedCol: Int = -1,
    val activeDirection: String = "H",
    val checkMode: String = "CLASSIC",
    val userLetters: Map<Pair<Int, Int>, Char> = emptyMap(),
    val isFinished: Boolean = false,
    val lastUpdatedMs: Long = 0L,
) {
    val isEmpty: Boolean get() = boardId.isBlank()
    val hasActiveSession: Boolean get() = !isEmpty && !isFinished
    val filledCellCount: Int get() = userLetters.size
}

/**
 * Gestor de persistencia local para partidas en curso mediante Preferences DataStore.
 * Almacena el progreso sin guardar nunca las respuestas del banco maestro.
 */
class GameSessionManager private constructor(private val context: Context) {

    companion object {
        private val KEY_BOARD_ID = stringPreferencesKey("session_board_id")
        private val KEY_CATEGORY = stringPreferencesKey("session_category")
        private val KEY_BOARD_SIZE = stringPreferencesKey("session_board_size")
        private val KEY_SELECTED_ROW = intPreferencesKey("session_selected_row")
        private val KEY_SELECTED_COL = intPreferencesKey("session_selected_col")
        private val KEY_ACTIVE_DIRECTION = stringPreferencesKey("session_active_direction")
        private val KEY_CHECK_MODE = stringPreferencesKey("session_check_mode")
        private val KEY_USER_LETTERS = stringPreferencesKey("session_user_letters")
        private val KEY_IS_FINISHED = booleanPreferencesKey("session_is_finished")
        private val KEY_LAST_UPDATED_MS = stringPreferencesKey("session_last_updated_ms")

        @Volatile
        private var instance: GameSessionManager? = null

        fun getInstance(context: Context): GameSessionManager {
            return instance ?: synchronized(this) {
                instance ?: GameSessionManager(context.applicationContext).also { instance = it }
            }
        }

        /**
         * Serializa el mapa de letras a formato compacto "row,col,char;...".
         */
        fun serializeLetters(letters: Map<Pair<Int, Int>, Char>): String {
            if (letters.isEmpty()) return ""
            return letters.entries.joinToString(";") { (pos, char) ->
                "${pos.first},${pos.second},$char"
            }
        }

        /**
         * Deserializa la cadena a mapa de letras. Ignora elementos corruptos.
         */
        fun deserializeLetters(raw: String?): Map<Pair<Int, Int>, Char> {
            if (raw.isNullOrBlank()) return emptyMap()
            val map = mutableMapOf<Pair<Int, Int>, Char>()
            val items = raw.split(";")
            for (item in items) {
                val parts = item.split(",")
                if (parts.size == 3) {
                    val r = parts[0].toIntOrNull()
                    val c = parts[1].toIntOrNull()
                    val ch = parts[2].firstOrNull()
                    if (r != null && c != null && ch != null) {
                        map[Pair(r, c)] = ch
                    }
                }
            }
            return map
        }
    }

    /**
     * Flujo reactivo con el estado de la sesión guardada.
     */
    val sessionFlow: Flow<GameSessionState> = context.gameSessionDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val boardId = prefs[KEY_BOARD_ID] ?: ""
            val category = prefs[KEY_CATEGORY] ?: ""
            val boardSize = prefs[KEY_BOARD_SIZE] ?: ""
            val selectedRow = prefs[KEY_SELECTED_ROW] ?: -1
            val selectedCol = prefs[KEY_SELECTED_COL] ?: -1
            val activeDirection = prefs[KEY_ACTIVE_DIRECTION] ?: "H"
            val checkMode = prefs[KEY_CHECK_MODE] ?: "CLASSIC"
            val rawLetters = prefs[KEY_USER_LETTERS] ?: ""
            val isFinished = prefs[KEY_IS_FINISHED] ?: false
            val lastUpdatedMs = prefs[KEY_LAST_UPDATED_MS]?.toLongOrNull() ?: 0L

            GameSessionState(
                boardId = boardId,
                category = category,
                boardSize = boardSize,
                selectedRow = selectedRow,
                selectedCol = selectedCol,
                activeDirection = activeDirection,
                checkMode = checkMode,
                userLetters = deserializeLetters(rawLetters),
                isFinished = isFinished,
                lastUpdatedMs = lastUpdatedMs,
            )
        }

    /**
     * Indica reactivamente si hay una partida activa no finalizada.
     */
    fun hasActiveSession(): Flow<Boolean> = sessionFlow.map { it.hasActiveSession }

    /**
     * Guarda el estado completo de la partida en curso.
     */
    suspend fun saveSession(state: GameSessionState) {
        context.gameSessionDataStore.edit { prefs ->
            prefs[KEY_BOARD_ID] = state.boardId
            prefs[KEY_CATEGORY] = state.category
            prefs[KEY_BOARD_SIZE] = state.boardSize
            prefs[KEY_SELECTED_ROW] = state.selectedRow
            prefs[KEY_SELECTED_COL] = state.selectedCol
            prefs[KEY_ACTIVE_DIRECTION] = state.activeDirection
            prefs[KEY_CHECK_MODE] = state.checkMode
            prefs[KEY_USER_LETTERS] = serializeLetters(state.userLetters)
            prefs[KEY_IS_FINISHED] = state.isFinished
            prefs[KEY_LAST_UPDATED_MS] = System.currentTimeMillis().toString()
        }
    }

    /**
     * Elimina completamente la sesión guardada.
     */
    suspend fun clearSession() {
        context.gameSessionDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    /**
     * Marca la sesión actual como terminada.
     */
    suspend fun markFinished() {
        context.gameSessionDataStore.edit { prefs ->
            prefs[KEY_IS_FINISHED] = true
            prefs[KEY_LAST_UPDATED_MS] = System.currentTimeMillis().toString()
        }
    }
}
