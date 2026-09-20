package com.neuronovaapps.crucilux.data.daily

import java.time.LocalDate
import java.time.ZoneId

/**
 * Abstracción de fecha local para Crucilux.
 * Centraliza la obtención de la fecha del día y permite simular fechas en pruebas unitarias.
 */
interface DateProvider {
    /** Retorna la fecha local actual. */
    fun today(): LocalDate

    /** Retorna la clave canónica de fecha en formato YYYY-MM-DD. */
    fun todayKey(): String = today().toString()
}

/**
 * Proveedor de fecha estándar basado en la zona horaria del sistema.
 */
class DefaultDateProvider(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
) : DateProvider {
    override fun today(): LocalDate = LocalDate.now(zoneId)
}

/**
 * Proveedor de fecha para pruebas unitarias con fecha mutable.
 */
class FakeDateProvider(
    private var currentDate: LocalDate = LocalDate.of(2026, 9, 18),
) : DateProvider {
    override fun today(): LocalDate = currentDate

    fun setDate(date: LocalDate) {
        currentDate = date
    }

    fun advanceDays(days: Long) {
        currentDate = currentDate.plusDays(days)
    }
}
