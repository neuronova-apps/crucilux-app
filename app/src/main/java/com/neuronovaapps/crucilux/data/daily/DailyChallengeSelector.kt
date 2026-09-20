package com.neuronovaapps.crucilux.data.daily

import com.neuronovaapps.crucilux.data.bank.CruciluxBankRepository
import com.neuronovaapps.crucilux.model.CruciluxBoard
import java.time.LocalDate

/**
 * Selector determinista de tableros para el Desafío Diario de Crucilux.
 *
 * Principios del algoritmo:
 * 1. Mapeo determinista puro: misma fecha local (YYYY-MM-DD) produce SIEMPRE el mismo boardId.
 * 2. Ausencia de repetición inmediata: el desafío de hoy NUNCA repite el del día inmediatamente anterior.
 * 3. Si el índice calculado para hoy coincide con el tablero efectivo del día anterior, avanza
 *    determinísticamente al siguiente tablero elegible: `(indice + 1) % totalEligible`.
 * 4. Dispersión uniforme: utiliza una mezcla pseudoaleatoria SplitMix64 sobre el epochDay para
 *    garantizar que días consecutivos alternen entre distintas categorías y tamaños de tablero.
 * 5. Cero dependencias externas: 100% offline, sin Random inestable, sin servicios de red y sin
 *    calendarios cableados manualmente.
 */
class DailyChallengeSelector(
    private val bankRepository: CruciluxBankRepository = CruciluxBankRepository.getInstance(),
) {

    /**
     * Retorna la lista canónica y estable de tableros elegibles para el Desafío Diario.
     * Ordenados lexicográficamente por su identificador único para garantizar consistencia.
     */
    fun getEligibleBoards(): List<CruciluxBoard> {
        return bankRepository.getAllBoards()
            .filter { isValidDailyBoard(it) }
            .sortedBy { it.id }
    }

    /**
     * Valida que un tablero cumpla los requisitos técnicos para ser desafío diario.
     */
    fun isValidDailyBoard(board: CruciluxBoard): Boolean {
        return board.id.isNotBlank() &&
            board.rows >= 5 &&
            board.cols >= 5 &&
            board.entries.isNotEmpty() &&
            board.category.isNotBlank()
    }

    /**
     * Selecciona el tablero correspondiente a la fecha dada.
     *
     * @param date Fecha local a consultar.
     * @return El [CruciluxBoard] seleccionado, o null si no hay tableros elegibles cargados.
     */
    fun selectBoardForDate(date: LocalDate): CruciluxBoard? {
        val eligible = getEligibleBoards()
        if (eligible.isEmpty()) return null
        val index = computeEffectiveIndex(date, eligible.size)
        return eligible[index]
    }

    /**
     * Sobrecarga de conveniencia que acepta una clave de fecha en formato YYYY-MM-DD.
     */
    fun selectBoardForDate(dateKey: String): CruciluxBoard? {
        val date = LocalDate.parse(dateKey)
        return selectBoardForDate(date)
    }

    companion object {

        fun computeRawIndex(epochDay: Long, totalBoards: Int): Int {
            if (totalBoards <= 1) return 0
            var x = epochDay
            x = (x xor (x ushr 30)) * -4658895280553007687L
            x = (x xor (x ushr 27)) * -7723592293113705631L
            x = x xor (x ushr 31)
            val positive = x and Long.MAX_VALUE
            return (positive % totalBoards).toInt()
        }

        /**
         * Calcula el índice base pseudoaleatorio determinista para una fecha y tamaño dado
         * utilizando la función de mezcla SplitMix64 sobre el epochDay.
         */
        fun computeRawIndex(date: LocalDate, totalBoards: Int): Int {
            return computeRawIndex(date.toEpochDay(), totalBoards)
        }

        fun computeEffectiveIndex(todayRaw: Int, yesterdayEffective: Int, totalBoards: Int): Int {
            if (totalBoards <= 1) return 0
            return if (todayRaw == yesterdayEffective) {
                (todayRaw + 1) % totalBoards
            } else {
                todayRaw
            }
        }

        /**
         * Calcula el índice efectivo para la fecha actual asegurando que NUNCA sea igual
         * al del día anterior (evita repetición inmediata de tableros).
         */
        fun computeEffectiveIndex(date: LocalDate, totalBoards: Int): Int {
            if (totalBoards <= 1) return 0
            val prevPrevRaw = computeRawIndex(date.minusDays(2), totalBoards)
            val prevRaw = computeRawIndex(date.minusDays(1), totalBoards)
            val yesterdayEffective = computeEffectiveIndex(prevRaw, prevPrevRaw, totalBoards)
            val todayRaw = computeRawIndex(date, totalBoards)
            return computeEffectiveIndex(todayRaw, yesterdayEffective, totalBoards)
        }
    }
}
