package com.neuronova.crucilux.data.bank

import android.content.Context
import com.neuronova.crucilux.model.CruciluxBankMetadata
import com.neuronova.crucilux.model.CruciluxBoard
import com.neuronova.crucilux.model.CruciluxDirection
import com.neuronova.crucilux.model.CruciluxEntry
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Repositorio local para la lectura y consulta del banco maestro validado de Crucilux (v1.28).
 *
 * Características:
 * - Funciona 100% offline desde assets locales (`crucilux_bank_v1_28.json`).
 * - No utiliza bases de datos externas, APIs, Firebase ni Room.
 * - Mantiene los 300 tableros y 2.000 entradas cacheados en memoria de forma inmutable tras la carga.
 */
class CruciluxBankRepository private constructor() {

    private var metadata: CruciluxBankMetadata? = null
    private var boards: List<CruciluxBoard> = emptyList()
    private var boardsById: Map<String, CruciluxBoard> = emptyMap()
    private var boardsByCategoryAndSize: Map<Pair<String, String>, List<CruciluxBoard>> = emptyMap()

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
     * Parsea la cadena JSON e indexa los tableros en memoria.
     */
    @Synchronized
    fun loadFromJsonString(jsonString: String) {
        val root = JSONObject(jsonString)

        val schemaVersion = root.optString("schemaVersion", "1.0")
        val bankVersion = root.optString("bankVersion", "1.28")
        val app = root.optString("app", "Crucilux")
        val coordinateBase = root.optInt("coordinateBase", 0)
        val totalBoards = root.optInt("totalBoards", 0)
        val totalEntries = root.optInt("totalEntries", 0)

        // Parsear mapa de tamaños
        val sizesMap = mutableMapOf<String, Int>()
        val sizesObj = root.optJSONObject("sizes")
        if (sizesObj != null) {
            val keys = sizesObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                sizesMap[key] = sizesObj.getInt(key)
            }
        }

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
            sizes = sizesMap,
            categories = categoriesList,
        )

        // Parsear lista de tableros
        val parsedBoards = mutableListOf<CruciluxBoard>()
        val boardsArray = root.optJSONArray("boards")
        if (boardsArray != null) {
            for (i in 0 until boardsArray.length()) {
                val bObj = boardsArray.getJSONObject(i)
                val id = bObj.getString("id")
                val size = bObj.getString("size")
                val rows = bObj.getInt("rows")
                val cols = bObj.getInt("cols")
                val category = bObj.getString("category")
                val subcategory = bObj.optString("subcategory", "No aplica")

                val entriesList = mutableListOf<CruciluxEntry>()
                val entriesArray = bObj.optJSONArray("entries")
                if (entriesArray != null) {
                    for (j in 0 until entriesArray.length()) {
                        val eObj = entriesArray.getJSONObject(j)
                        val entry = CruciluxEntry(
                            number = eObj.getInt("number"),
                            direction = CruciluxDirection.fromValue(eObj.getString("direction")),
                            answer = eObj.getString("answer"),
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
                        size = size,
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
        this.boardsByCategoryAndSize = parsedBoards.groupBy { Pair(it.category.lowercase(), it.size.lowercase()) }
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
     * Retorna la lista oficial de tamaños de tablero disponibles en el banco maestro.
     */
    fun getSizes(): List<String> {
        return metadata?.sizes?.keys?.toList() ?: boards.map { it.size }.distinct()
    }

    /**
     * Retorna todos los tableros cargados en memoria.
     */
    fun getAllBoards(): List<CruciluxBoard> = boards

    /**
     * Retorna los tableros que pertenecen a una categoría específica.
     */
    fun getBoardsByCategory(category: String): List<CruciluxBoard> {
        return boards.filter { it.category.equals(category, ignoreCase = true) }
    }

    /**
     * Retorna los tableros que pertenecen a un tamaño específico ("7x7", "10x10", "15x15").
     */
    fun getBoardsBySize(size: String): List<CruciluxBoard> {
        return boards.filter { it.size.equals(size, ignoreCase = true) }
    }

    /**
     * Retorna la lista de tableros que coinciden exactamente con la categoría y el tamaño dados.
     * En el banco v1.28 existen exactamente 10 tableros para cada combinación.
     */
    fun getBoards(category: String, size: String): List<CruciluxBoard> {
        val key = Pair(category.trim().lowercase(), size.trim().lowercase())
        return boardsByCategoryAndSize[key] ?: boards.filter {
            it.category.equals(category, ignoreCase = true) && it.size.equals(size, ignoreCase = true)
        }
    }

    /**
     * Busca un tablero específico por su ID (ej. "7X7-01", "10X10-45").
     */
    fun getBoardById(id: String): CruciluxBoard? {
        return boardsById[id] ?: boards.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    /**
     * Obtiene un crucigrama para la partida según la categoría y tamaño seleccionados.
     * Retorna el primer tablero disponible para la combinación o busca por coincidencia aproximada.
     */
    fun obtenerCrucigrama(category: String, size: String): CruciluxBoard? {
        val matchingBoards = getBoards(category, size)
        return matchingBoards.firstOrNull()
            ?: getBoardsBySize(size).firstOrNull()
            ?: boards.firstOrNull()
    }

    /**
     * Indica si el repositorio ya ha cargado los datos en memoria.
     */
    fun isReady(): Boolean = isLoaded

    companion object {
        const val ASSET_FILE_NAME = "crucilux_bank_v1_28.json"

        @Volatile
        private var instance: CruciluxBankRepository? = null

        fun getInstance(): CruciluxBankRepository {
            return instance ?: synchronized(this) {
                instance ?: CruciluxBankRepository().also { instance = it }
            }
        }
    }
}
