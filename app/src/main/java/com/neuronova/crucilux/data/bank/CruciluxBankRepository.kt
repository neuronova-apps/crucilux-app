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

    /**
     * Carga el banco JSON desde los assets de la aplicación.
     */
    @Synchronized
    fun loadFromAssets(context: Context) {
        if (isLoaded) return
        context.assets.open(ASSET_FILE_NAME).use { inputStream ->
            loadFromStream(inputStream)
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
        val root = JSONObject(jsonString)

        val schemaVersion = root.optInt("schemaVersion", 2)
        val bankVersion = root.optString("bankVersion", "1.37")
        val app = root.optString("app", "Crucilux")
        val coordinateBase = root.optInt("coordinateBase", 0)
        val totalBoards = root.optInt("totalBoards", 300)
        val totalEntries = root.optInt("totalEntries", 2000)

        // Parsear lista de categorías
        val categoriesList = mutableListOf<String>()
        val categoriesArray = root.optJSONArray("categories")
        if (categoriesArray != null) {
            for (i in 0 until categoriesArray.length()) {
                categoriesList.add(categoriesArray.getString(i))
            }
        }

        this.metadata = CruciluxBankMetadata(
            schemaVersion = schemaVersion,
            bankVersion = bankVersion,
            app = app,
            coordinateBase = coordinateBase,
            totalBoards = totalBoards,
            totalEntries = totalEntries,
            categories = categoriesList,
        )

        // Parsear lista de tableros
        val parsedBoards = mutableListOf<CruciluxBoard>()
        val boardsArray = root.optJSONArray("boards")
        if (boardsArray != null) {
            for (i in 0 until boardsArray.length()) {
                val bObj = boardsArray.getJSONObject(i)
                val id = bObj.getString("id")
                val rows = bObj.getInt("rows")
                val cols = bObj.getInt("cols")
                val category = bObj.getString("category")
                val subcategory = bObj.optString("subcategory", "No aplica")

                val entriesList = mutableListOf<CruciluxEntry>()
                val entriesArray = bObj.optJSONArray("entries")
                if (entriesArray != null) {
                    for (j in 0 until entriesArray.length()) {
                        val eObj = entriesArray.getJSONObject(j)
                        val directionStr = eObj.getString("direction")
                        val answerStr = eObj.getString("answer")
                        val displayAnswerStr = eObj.optString("displayAnswer", answerStr)
                        val answerTypeStr = eObj.optString("answerType", "SINGLE")
                        val wordCountInt = eObj.optInt("wordCount", 1)

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
                            direction = CruciluxDirection.fromValue(directionStr),
                            answer = answerStr,
                            displayAnswer = displayAnswerStr,
                            answerType = CruciluxAnswerType.fromValue(answerTypeStr),
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
                }

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
        }

        this.boards = parsedBoards
        this.boardsById = parsedBoards.associateBy { it.id }
        this.boardsByCategory = parsedBoards.groupBy { it.category.trim().lowercase() }
        this.isLoaded = true
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
        return getBoardsByCategory(category).firstOrNull() ?: boards.firstOrNull()
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
