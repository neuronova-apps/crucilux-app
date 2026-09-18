package com.neuronova.crucilux

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.neuronova.crucilux.data.TextSizePreference
import com.neuronova.crucilux.data.UserPreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Fake in-memory DataStore<Preferences> para pruebas unitarias sin dependencias de I/O o Android context.
 */
class FakePreferenceDataStore(
    initialPreferences: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
    private val flow = MutableStateFlow(initialPreferences)

    override val data: Flow<Preferences> = flow.asStateFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(flow.value)
        flow.value = updated
        return updated
    }
}

class UserPreferencesManagerTest {

    private lateinit var fakeDataStore: FakePreferenceDataStore
    private lateinit var manager: UserPreferencesManager

    @Before
    fun setUp() {
        fakeDataStore = FakePreferenceDataStore()
        manager = UserPreferencesManager(fakeDataStore)
    }

    @Test
    fun textSizePreference_scaleFactorsAreExact() {
        assertEquals(1.00f, TextSizePreference.Normal.scaleFactor, 0.001f)
        assertEquals(1.15f, TextSizePreference.Large.scaleFactor, 0.001f)
        assertEquals(1.30f, TextSizePreference.VeryLarge.scaleFactor, 0.001f)
    }

    @Test
    fun textSizePreference_fromStoredValue() {
        assertEquals(TextSizePreference.Normal, TextSizePreference.fromStoredValue("normal"))
        assertEquals(TextSizePreference.Large, TextSizePreference.fromStoredValue("large"))
        assertEquals(TextSizePreference.VeryLarge, TextSizePreference.fromStoredValue("very_large"))

        // Valores nulos o no reconocidos deben hacer fallback seguro a Normal
        assertEquals(TextSizePreference.Normal, TextSizePreference.fromStoredValue(null))
        assertEquals(TextSizePreference.Normal, TextSizePreference.fromStoredValue(""))
        assertEquals(TextSizePreference.Normal, TextSizePreference.fromStoredValue("invalid_size"))
        assertEquals(TextSizePreference.Normal, TextSizePreference.fromStoredValue("huge"))
    }

    @Test
    fun defaultPreferences_containsNormalTextSize() = runBlocking {
        val prefs = manager.userPreferencesFlow.first()
        assertEquals(TextSizePreference.Normal, prefs.textSize)
        assertEquals("", prefs.userName)
        assertFalse(prefs.isDarkMode)
        assertFalse(prefs.isHighContrast)
        assertTrue(prefs.seasonalThemesEnabled)
    }

    @Test
    fun setTextSize_persistsLargeAndVeryLarge() = runBlocking {
        // Guardar Large
        manager.setTextSize(TextSizePreference.Large)
        val prefsLarge = manager.userPreferencesFlow.first()
        assertEquals(TextSizePreference.Large, prefsLarge.textSize)

        // Guardar VeryLarge
        manager.setTextSize(TextSizePreference.VeryLarge)
        val prefsVeryLarge = manager.userPreferencesFlow.first()
        assertEquals(TextSizePreference.VeryLarge, prefsVeryLarge.textSize)

        // Regresar a Normal
        manager.setTextSize(TextSizePreference.Normal)
        val prefsNormal = manager.userPreferencesFlow.first()
        assertEquals(TextSizePreference.Normal, prefsNormal.textSize)
    }

    @Test
    fun fallbackOnCorruptedDataStoreValue() = runBlocking {
        // Inyectar un valor desconocido directamente en el DataStore
        fakeDataStore.updateData { prefs ->
            val mutable = prefs.toMutablePreferences()
            mutable[stringPreferencesKey("text_size")] = "corrupted_or_future_format"
            mutable
        }

        val prefs = manager.userPreferencesFlow.first()
        assertEquals(TextSizePreference.Normal, prefs.textSize)
    }

    @Test
    fun compatibilityWithExistingPreferences() = runBlocking {
        // Configurar preferencias previas
        manager.setUserName("Gabriel")
        manager.setDarkMode(true)
        manager.setHighContrast(true)

        // Cambiar tamaño de texto no debe sobrescribir ni alterar los demás campos
        manager.setTextSize(TextSizePreference.Large)
        val prefs = manager.userPreferencesFlow.first()

        assertEquals("Gabriel", prefs.userName)
        assertTrue(prefs.isDarkMode)
        assertTrue(prefs.isHighContrast)
        assertEquals(TextSizePreference.Large, prefs.textSize)

        // Cambiar otra preferencia no debe alterar el tamaño de texto
        manager.setDarkMode(false)
        val prefsUpdated = manager.userPreferencesFlow.first()
        assertFalse(prefsUpdated.isDarkMode)
        assertEquals(TextSizePreference.Large, prefsUpdated.textSize)
    }
}
