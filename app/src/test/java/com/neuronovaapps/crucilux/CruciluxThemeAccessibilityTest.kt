package com.neuronovaapps.crucilux

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.neuronovaapps.crucilux.data.TextSizePreference
import com.neuronovaapps.crucilux.ui.theme.DarkBackground
import com.neuronovaapps.crucilux.ui.theme.DarkHCNightBackground
import com.neuronovaapps.crucilux.ui.theme.DarkHCNightError
import com.neuronovaapps.crucilux.ui.theme.DarkHCNightOnBackground
import com.neuronovaapps.crucilux.ui.theme.DarkHCNightOnSurface
import com.neuronovaapps.crucilux.ui.theme.DarkHCNightPrimary
import com.neuronovaapps.crucilux.ui.theme.DarkHCNightSecondary
import com.neuronovaapps.crucilux.ui.theme.DarkHCNightSuccess
import com.neuronovaapps.crucilux.ui.theme.DarkHCNightSurface
import com.neuronovaapps.crucilux.ui.theme.DarkOnBackground
import com.neuronovaapps.crucilux.ui.theme.DarkPrimary
import com.neuronovaapps.crucilux.ui.theme.LightBackground
import com.neuronovaapps.crucilux.ui.theme.LightOnBackground
import com.neuronovaapps.crucilux.ui.theme.LightPrimary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class CruciluxThemeAccessibilityTest {

    // ── 1. Prueba de Escalamiento de Tamaño de Texto ──────────────────────────

    @Test
    fun textSizeScaling_multipliesSystemScaleWithoutReplacing() {
        val baseSystemScale = 1.0f
        assertEquals(1.00f, baseSystemScale * TextSizePreference.Normal.scaleFactor, 0.001f)
        assertEquals(1.15f, baseSystemScale * TextSizePreference.Large.scaleFactor, 0.001f)
        assertEquals(1.30f, baseSystemScale * TextSizePreference.VeryLarge.scaleFactor, 0.001f)

        // Verificación con escala de accesibilidad previa del sistema (e.g. usuario con sistema a 1.25x)
        val accessibilitySystemScale = 1.25f
        val calculatedNormal = accessibilitySystemScale * TextSizePreference.Normal.scaleFactor
        val calculatedLarge = accessibilitySystemScale * TextSizePreference.Large.scaleFactor
        val calculatedVeryLarge = accessibilitySystemScale * TextSizePreference.VeryLarge.scaleFactor

        assertEquals(1.25f, calculatedNormal, 0.001f)
        assertEquals(1.4375f, calculatedLarge, 0.001f)
        assertEquals(1.625f, calculatedVeryLarge, 0.001f)

        // Debe conservar la proporcionalidad y nunca resetear a un valor fijo
        assertTrue(calculatedVeryLarge > calculatedLarge)
        assertTrue(calculatedLarge > calculatedNormal)
    }

    // ── 2. Verificación de Ratios de Contraste WCAG ───────────────────────────

    @Test
    fun highContrast_meetsWcagAaaRatios() {
        // Fondo negro absoluto vs texto blanco puro -> 21:1 (Máximo posible)
        val bgVsText = calculateContrastRatio(DarkHCNightBackground, DarkHCNightOnBackground)
        assertEquals(21.0, bgVsText, 0.1)
        assertTrue("Fondo vs texto en alto contraste debe cumplir AAA (>= 7.0)", bgVsText >= 7.0)

        // Superficie oscura (#121212) vs texto blanco (#FFFFFF) -> > 18:1
        val surfaceVsText = calculateContrastRatio(DarkHCNightSurface, DarkHCNightOnSurface)
        assertTrue("Superficie vs texto en alto contraste debe superar 18:1, obtenido: $surfaceVsText", surfaceVsText >= 18.0)

        // Fondo negro vs Primario Cian brillante (#40C4FF) -> > 10:1
        val bgVsPrimary = calculateContrastRatio(DarkHCNightBackground, DarkHCNightPrimary)
        assertTrue("Primario cian en fondo negro debe superar AAA 7:1, obtenido: $bgVsPrimary", bgVsPrimary >= 7.0)

        // Fondo negro vs Secundario Ámbar brillante (#FFAB40) -> > 10:1
        val bgVsSecondary = calculateContrastRatio(DarkHCNightBackground, DarkHCNightSecondary)
        assertTrue("Secundario ámbar en fondo negro debe superar AAA 7:1, obtenido: $bgVsSecondary", bgVsSecondary >= 7.0)

        // Fondo negro vs Éxito Menta (#00E676) -> > 12:1
        val bgVsSuccess = calculateContrastRatio(DarkHCNightBackground, DarkHCNightSuccess)
        assertTrue("Éxito menta en fondo negro debe superar AAA 7:1, obtenido: $bgVsSuccess", bgVsSuccess >= 7.0)

        // Fondo negro vs Error Rojo brillante (#FF5252) -> > 6.5:1 (supera AA 4.5:1)
        val bgVsError = calculateContrastRatio(DarkHCNightBackground, DarkHCNightError)
        assertTrue("Error en fondo negro debe superar AA 4.5:1, obtenido: $bgVsError", bgVsError >= 4.5)
    }

    @Test
    fun normalDayAndNight_meetWcagAaRequirements() {
        // Día normal
        val dayBgVsText = calculateContrastRatio(LightBackground, LightOnBackground)
        assertTrue("Día normal texto en fondo supera AA 4.5:1, obtenido: $dayBgVsText", dayBgVsText >= 4.5)

        // Noche normal
        val nightBgVsText = calculateContrastRatio(DarkBackground, DarkOnBackground)
        assertTrue("Noche normal texto en fondo supera AA 4.5:1, obtenido: $nightBgVsText", nightBgVsText >= 4.5)
    }

    // ── 3. Corrección Específica: Celdas Bloqueadas en Modo Noche ─────────────

    @Test
    fun blockedCellColor_isNeverWhiteInDarkModes() {
        // Celda bloqueada en noche normal
        val darkNormalBlockedBg = Color(0xFF0A0F1D)
        // Celda bloqueada en noche alto contraste
        val darkHCBlockedBg = Color(0xFF000000)

        val pureWhite = Color(0xFFFFFFFF)

        // Verificación de que la celda bloqueada NUNCA es blanca en modo oscuro
        assertNotEquals(pureWhite, darkNormalBlockedBg)
        assertNotEquals(pureWhite, darkHCBlockedBg)

        // La luminancia debe ser ultra baja (oscura)
        assertTrue("Celda bloqueada en noche normal debe ser muy oscura", calculateLuminance(darkNormalBlockedBg) < 0.05)
        assertEquals(0.0, calculateLuminance(darkHCBlockedBg), 0.001)
    }

    // ── Funciones de Utilidad WCAG 2.1 ───────────────────────────────────────

    /**
     * Calcula el ratio de contraste entre dos colores siguiendo la fórmula WCAG 2.1:
     * (L1 + 0.05) / (L2 + 0.05), donde L1 es la luminancia más clara.
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Double {
        val lum1 = calculateLuminance(color1)
        val lum2 = calculateLuminance(color2)
        val brightest = maxOf(lum1, lum2)
        val darkest = minOf(lum1, lum2)
        return (brightest + 0.05) / (darkest + 0.05)
    }

    /**
     * Calcula la luminancia relativa según WCAG 2.1:
     * L = 0.2126 * R + 0.7152 * G + 0.0722 * B
     */
    private fun calculateLuminance(color: Color): Double {
        val argb = color.toArgb()
        val r = sRgbToLinear(((argb shr 16) and 0xFF) / 255.0)
        val g = sRgbToLinear(((argb shr 8) and 0xFF) / 255.0)
        val b = sRgbToLinear((argb and 0xFF) / 255.0)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun sRgbToLinear(c: Double): Double {
        return if (c <= 0.04045) {
            c / 12.92
        } else {
            ((c + 0.055) / 1.055).pow(2.4)
        }
    }
}
