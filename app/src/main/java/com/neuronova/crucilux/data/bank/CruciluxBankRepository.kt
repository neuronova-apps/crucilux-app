package com.neuronova.crucilux.data.bank

import android.content.Context
import com.neuronova.crucilux.model.CruciluxAnswerType
import com.neuronova.crucilux.model.CruciluxBankMetadata
import com.neuronova.crucilux.model.CruciluxBoard
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.model.CruciluxEntry
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter

enum class BankLoadStatus {
    NOT_LOADED,
    LOADING,
    LOADED,
    ERROR,
}

/**
 * Repositorio local para la lectura y consulta del banco maestro validado de Crucilux (v1.37).
 *
 * Características:
 * - Funciona 100% offline desde assets locales (`crucilux_bank_v1_37.json`).
 * - Soporta schemaVersion 2 y bankVersion 1.37 con dimensiones dinámicas [rows, cols].
 * - Soporta respuestas simples (SINGLE) y compuestas (COMPOUND).
 * - No utiliza bases de datos externas, APIs, Firebase ni Room.
 * - Mantiene los 300 tableros y 2.000 entradas cacheados en memoria de forma inmutable tras la carga.
 */
class CruciluxBankRepository private constructor() {

    private var metadata: CruciluxBankMetadata? = null
    private var boards: List<CruciluxBoard> = emptyList()
    private var boardsById: Map<String, CruciluxBoard> = emptyMap()
    private var boardsByCategory: Map<String, List<CruciluxBoard>> = emptyMap()

    @Volatile
    private var isLoaded: Boolean = false

    private val _loadStatus = MutableStateFlow(BankLoadStatus.NOT_LOADED)
    val loadStatus: StateFlow<BankLoadStatus> = _loadStatus.asStateFlow()

    @Volatile
    private var loadErrorMessage: String? = null

    /**
     * Carga el banco JSON desde los assets de la aplicación.
     */
    @Synchronized
    fun loadFromAssets(context: Context) {
        if (isLoaded) return
        _loadStatus.value = BankLoadStatus.LOADING
        loadErrorMessage = null
        try {
            context.assets.open(ASSET_FILE_NAME).use { inputStream ->
                loadFromStream(inputStream)
            }
        } catch (exception: Exception) {
            markInitialLoadFailed(exception)
            throw exception
        }
    }

    /**
     * Carga y parsea el banco desde un InputStream (compatible con tests unitarios y runtime).
     */
    @Synchronized
    fun loadFromStream(inputStream: InputStream) {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val jsonString = reader.readText()
        loadFromJsonString(jsonString)
    }

    /**
     * Parsea la cadena JSON e indexa los 300 tableros y 2.000 entradas en memoria.
     */
    @Synchronized
    fun loadFromJsonString(jsonString: String) {
        val wasLoaded = isLoaded
        if (!wasLoaded) {
            _loadStatus.value = BankLoadStatus.LOADING
            loadErrorMessage = null
        }

        try {
            parseAndPublish(jsonString)
        } catch (exception: Exception) {
            if (!wasLoaded) markInitialLoadFailed(exception)
            throw exception
        }
    }

    private fun parseAndPublish(jsonString: String) {
        val root = JSONObject(jsonString)

        val schemaVersion = root.getInt("schemaVersion")
        val bankVersion = root.getString("bankVersion")
        val app = root.getString("app")
        val coordinateBase = root.getInt("coordinateBase")
        val totalBoards = root.getInt("totalBoards")
        val totalEntries = root.getInt("totalEntries")

        require(schemaVersion == 2) { "schemaVersion no compatible: $schemaVersion" }
        require(bankVersion == "1.37") { "bankVersion no compatible: $bankVersion" }
        require(app == "Crucilux") { "Banco destinado a otra aplicación: $app" }
        require(coordinateBase == 0) { "coordinateBase no compatible: $coordinateBase" }

        // Parsear lista de categorías
        val categoriesList = mutableListOf<String>()
        val categoriesArray = root.getJSONArray("categories")
        for (i in 0 until categoriesArray.length()) {
            categoriesList.add(categoriesArray.getString(i))
        }
        require(categoriesList.isNotEmpty()) { "El banco no declara categorías" }
        require(categoriesList.distinct().size == categoriesList.size) { "El banco declara categorías duplicadas" }

        // Parsear lista de tableros
        val parsedBoards = mutableListOf<CruciluxBoard>()
        val boardsArray = root.getJSONArray("boards")
        for (i in 0 until boardsArray.length()) {
                val bObj = boardsArray.getJSONObject(i)
                val id = bObj.getString("id")
                val rows = bObj.getInt("rows")
                val cols = bObj.getInt("cols")
                val category = bObj.getString("category")
                val subcategory = bObj.optString("subcategory", "No aplica")

                require(id.isNotBlank()) { "Tablero en índice $i sin ID" }
                require(rows > 0 && cols > 0) { "Tablero $id con dimensiones inválidas ${rows}x${cols}" }
                require(category in categoriesList) { "Tablero $id usa categoría no declarada: $category" }

                val entriesList = mutableListOf<CruciluxEntry>()
                val entriesArray = bObj.getJSONArray("entries")
                for (j in 0 until entriesArray.length()) {
                        val eObj = entriesArray.getJSONObject(j)
                        val directionStr = eObj.getString("direction")
                        val answerStr = eObj.getString("answer")
                        val displayAnswerStr = eObj.optString("displayAnswer", answerStr)
                        val answerTypeStr = eObj.getString("answerType")
                        val wordCountInt = eObj.getInt("wordCount")

                        val direction = CruciluxDirection.entries.firstOrNull {
                            it.value.equals(directionStr, ignoreCase = true)
                        } ?: throw IllegalArgumentException(
                            "Tablero $id: dirección inválida '$directionStr' en entrada $j"
                        )
                        val answerType = CruciluxAnswerType.entries.firstOrNull {
                            it.value.equals(answerTypeStr, ignoreCase = true)
                        } ?: throw IllegalArgumentException(
                            "Tablero $id: answerType inválido '$answerTypeStr' en entrada $j"
                        )

                        val wordLengthsList = mutableListOf<Int>()
                        val wordLengthsArray = eObj.optJSONArray("wordLengths")
                        if (wordLengthsArray != null) {
                            for (k in 0 until wordLengthsArray.length()) {
                                wordLengthsList.add(wordLengthsArray.getInt(k))
                            }
                        } else {
                            wordLengthsList.add(eObj.optInt("length", answerStr.length))
                        }

                        val entry = CruciluxEntry(
                            number = eObj.getInt("number"),
                            direction = direction,
                            answer = answerStr,
                            displayAnswer = displayAnswerStr,
                            answerType = answerType,
                            wordCount = wordCountInt,
                            wordLengths = wordLengthsList,
                            length = eObj.getInt("length"),
                            row = eObj.getInt("row"),
                            col = eObj.getInt("col"),
                            bankId = eObj.getString("bankId"),
                            clue = eObj.getString("clue"),
                        )
                        entriesList.add(entry)
                }
                require(entriesList.isNotEmpty()) { "Tablero $id sin entradas" }

                parsedBoards.add(
                    CruciluxBoard(
                        id = id,
                        rows = rows,
                        cols = cols,
                        category = category,
                        subcategory = subcategory,
                        entries = entriesList,
                    )
                )
        }

        require(parsedBoards.size == totalBoards) {
            "totalBoards=$totalBoards, pero se encontraron ${parsedBoards.size}"
        }
        require(parsedBoards.map { it.id.lowercase() }.distinct().size == parsedBoards.size) {
            "El banco contiene IDs de tablero duplicados"
        }
        val parsedEntryCount = parsedBoards.sumOf { it.entries.size }
        require(parsedEntryCount == totalEntries) {
            "totalEntries=$totalEntries, pero se encontraron $parsedEntryCount"
        }

        val parsedMetadata = CruciluxBankMetadata(
            schemaVersion = schemaVersion,
            bankVersion = bankVersion,
            app = app,
            coordinateBase = coordinateBase,
            totalBoards = totalBoards,
            totalEntries = totalEntries,
            categories = categoriesList.toList(),
        )

        this.metadata = parsedMetadata
        this.boards = parsedBoards
        this.boardsById = parsedBoards.associateBy { it.id }
        this.boardsByCategory = parsedBoards.groupBy { it.category.trim().lowercase() }
        this.isLoaded = true
        this.loadErrorMessage = null
        this._loadStatus.value = BankLoadStatus.LOADED
    }

    /**
     * Retorna los metadatos globales del banco JSON.
     */
    fun getMetadata(): CruciluxBankMetadata? = metadata

    /**
     * Retorna la lista oficial de categorías disponibles en el banco maestro.
     */
    fun getCategories(): List<String> {
        return metadata?.categories ?: boards.map { it.category }.distinct()
    }

    /**
     * Retorna todos los tableros cargados en memoria (exactamente 300).
     */
    fun getAllBoards(): List<CruciluxBoard> = boards

    /**
     * Retorna los tableros que pertenecen a una categoría específica (exactamente 30 por categoría).
     */
    fun getBoardsByCategory(category: String): List<CruciluxBoard> {
        val key = category.trim().lowercase()
        return boardsByCategory[key] ?: boards.filter { it.category.equals(category, ignoreCase = true) }
    }

    /**
     * Busca un tablero específico por su ID (ej. "7X7-01", "10X10-45").
     */
    fun getBoardById(id: String): CruciluxBoard? {
        return boardsById[id] ?: boards.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    /**
     * Obtiene un crucigrama para la partida según la categoría seleccionada.
     * Retorna el primer tablero disponible para la categoría.
     */
    fun obtenerCrucigrama(category: String): CruciluxBoard? {
        return getBoardsByCategory(category).firstOrNull()
    }

    /**
     * Sobrecarga de compatibilidad para llamadas históricas que especificaban tamaño.
     */
    fun obtenerCrucigrama(category: String, size: String?): CruciluxBoard? {
        return obtenerCrucigrama(category)
    }

    /**
     * Indica si el repositorio ya ha cargado los datos en memoria.
     */
    fun isReady(): Boolean = isLoaded

    fun getLoadErrorMessage(): String? = loadErrorMessage

    suspend fun awaitReady(): Boolean {
        if (isLoaded) return true
        val terminalStatus = loadStatus
            .filter { it == BankLoadStatus.LOADED || it == BankLoadStatus.ERROR }
            .first()
        return terminalStatus == BankLoadStatus.LOADED
    }

    private fun markInitialLoadFailed(exception: Exception) {
        if (isLoaded) return
        loadErrorMessage = exception.message ?: exception.javaClass.simpleName
        _loadStatus.value = BankLoadStatus.ERROR
    }

    companion object {
        const val ASSET_FILE_NAME = "crucilux_bank_v1_37.json"

        @Volatile
        private var instance: CruciluxBankRepository? = null

        fun getInstance(): CruciluxBankRepository {
            return instance ?: synchronized(this) {
                instance ?: CruciluxBankRepository().also { instance = it }
            }
        }
    }
}
