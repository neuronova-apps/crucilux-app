package com.neuronova.crucilux.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "crucilux_user_preferences")

/**
 * Escala global de tamaño de texto para accesibilidad visual en Crucilux.
 *
 * Valores:
 * - Normal: 1.00f (valor predeterminado)
 * - Grande: 1.15f
 * - Muy grande: 1.30f
 *
 * Se aplica multiplicando la escala del sistema por [scaleFactor], sin sustituirla.
 */
enum class TextSizePreference(
    internal val storedValue: String,
    val scaleFactor: Float,
) {
    Normal(storedValue = "normal", scaleFactor = 1.0f),
    Large(storedValue = "large", scaleFactor = 1.15f),
    VeryLarge(storedValue = "very_large", scaleFactor = 1.3f),
    ;

    companion object {
        fun fromStoredValue(value: String?): TextSizePreference =
            entries.firstOrNull { it.storedValue == value } ?: Normal
    }
}

/**
 * Modelo inmutable para agrupar las preferencias del usuario.
 */
data class UserPreferences(
    val userName: String = "",
    val isDarkMode: Boolean = false,
    val isHighContrast: Boolean = false,
    val textSize: TextSizePreference = TextSizePreference.Normal,
    val seasonalThemesEnabled: Boolean = true,
    val defaultCheckMode: String = "CLASSIC",
)

/**
 * Gestor centralizado de persistencia local mediante Preferences DataStore.
 * Almacena exclusivamente:
 * 1. Nombre opcional del usuario (perfil local).
 * 2. Modo día / noche.
 * 3. Alto contraste.
 * 4. Habilitación de temas de temporada.
 */
class UserPreferencesManager internal constructor(
    private val preferencesDataStore: DataStore<Preferences>,
) {
    constructor(context: Context) : this(context.applicationContext.dataStore)

    companion object {
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        private val KEY_IS_HIGH_CONTRAST = booleanPreferencesKey("is_high_contrast")
        private val KEY_TEXT_SIZE = stringPreferencesKey("text_size")
        private val KEY_SEASONAL_THEMES_ENABLED = booleanPreferencesKey("seasonal_themes_enabled")
        private val KEY_DEFAULT_CHECK_MODE = stringPreferencesKey("default_check_mode")

        @Volatile
        private var INSTANCE: UserPreferencesManager? = null

        fun getInstance(context: Context): UserPreferencesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferencesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val userPreferencesFlow: Flow<UserPreferences> = preferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            UserPreferences(
                userName = preferences[KEY_USER_NAME] ?: "",
                isDarkMode = preferences[KEY_IS_DARK_MODE] ?: false, // Modo día por defecto
                isHighContrast = preferences[KEY_IS_HIGH_CONTRAST] ?: false,
                textSize = TextSizePreference.fromStoredValue(preferences[KEY_TEXT_SIZE]),
                seasonalThemesEnabled = preferences[KEY_SEASONAL_THEMES_ENABLED] ?: true, // Activado por defecto
                defaultCheckMode = preferences[KEY_DEFAULT_CHECK_MODE] ?: "CLASSIC",
            )
        }

    /**
     * Guarda el nombre del usuario normalizando únicamente espacios innecesarios.
     * Preserva mayúsculas y tildes originales.
     */
    suspend fun setUserName(name: String) {
        val normalized = name.trim().replace(Regex("\\s+"), " ")
        preferencesDataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = normalized
        }
    }

    /**
     * Elimina el nombre guardado.
     */
    suspend fun clearUserName() {
        preferencesDataStore.edit { preferences ->
            preferences.remove(KEY_USER_NAME)
        }
    }

    /**
     * Guarda la preferencia de tema (día = false, noche = true).
     */
    suspend fun setDarkMode(isDark: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[KEY_IS_DARK_MODE] = isDark
        }
    }

    /**
     * Guarda la preferencia de alto contraste.
     */
    suspend fun setHighContrast(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[KEY_IS_HIGH_CONTRAST] = enabled
        }
    }

    /**
     * Guarda la preferencia global de tamaño de texto.
     */
    suspend fun setTextSize(textSize: TextSizePreference) {
        preferencesDataStore.edit { preferences ->
            preferences[KEY_TEXT_SIZE] = textSize.storedValue
        }
    }

    /**
     * Guarda la preferencia de temas de temporada.
     */
    suspend fun setSeasonalThemesEnabled(enabled: Boolean) {
        preferencesDataStore.edit { preferences ->
            preferences[KEY_SEASONAL_THEMES_ENABLED] = enabled
        }
    }

    /**
     * Guarda el modo de comprobación de respuestas predeterminado ("CLASSIC" o "ASSISTED").
     */
    suspend fun setDefaultCheckMode(mode: String) {
        preferencesDataStore.edit { preferences ->
            preferences[KEY_DEFAULT_CHECK_MODE] = mode
        }
    }
}
