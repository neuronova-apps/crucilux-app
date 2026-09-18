package com.neuronovaapps.crucilux

import com.neuronovaapps.crucilux.data.NeuroNovaLinks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeuroNovaLinksTest {

    @Test
    fun `todas las URLs comienzan por https y no contienen espacios`() {
        val urls = listOf(
            NeuroNovaLinks.CRUCILUX_OFFICIAL_URL,
            NeuroNovaLinks.REPOSITORY_URL,
            NeuroNovaLinks.NEURONOVA_APPS_URL,
            NeuroNovaLinks.MORE_APPS_URL,
            NeuroNovaLinks.PRIVACY_POLICY_URL,
            NeuroNovaLinks.TERMS_URL,
            NeuroNovaLinks.LICENSES_URL,
            NeuroNovaLinks.SUPPORT_URL,
            NeuroNovaLinks.REPORT_ISSUE_URL,
        )

        urls.forEach { url ->
            assertTrue("La URL debe iniciar con https://: $url", url.startsWith("https://"))
            assertFalse("La URL no debe tener espacios: $url", url.contains(" "))
        }
    }

    @Test
    fun `las URLs institucionales apuntan a los dominios oficiales de NeuroNova o GitHub`() {
        val validDomains = listOf("neuronova-apps.github.io", "github.com")

        val urls = listOf(
            NeuroNovaLinks.CRUCILUX_OFFICIAL_URL,
            NeuroNovaLinks.REPOSITORY_URL,
            NeuroNovaLinks.NEURONOVA_APPS_URL,
            NeuroNovaLinks.MORE_APPS_URL,
            NeuroNovaLinks.PRIVACY_POLICY_URL,
            NeuroNovaLinks.TERMS_URL,
            NeuroNovaLinks.LICENSES_URL,
            NeuroNovaLinks.SUPPORT_URL,
            NeuroNovaLinks.REPORT_ISSUE_URL,
        )

        urls.forEach { url ->
            assertTrue(
                "La URL no pertenece a un dominio oficial: $url",
                validDomains.any { domain -> url.startsWith("https://$domain") },
            )
        }
    }

    @Test
    fun `los enlaces de Crucilux corresponden a sus rutas especificas`() {
        assertEquals("https://neuronova-apps.github.io/crucilux-app/", NeuroNovaLinks.CRUCILUX_OFFICIAL_URL)
        assertEquals("https://github.com/neuronova-apps/crucilux-app", NeuroNovaLinks.REPOSITORY_URL)
        assertEquals("https://neuronova-apps.github.io/crucilux-app/privacy/", NeuroNovaLinks.PRIVACY_POLICY_URL)
    }

    @Test
    fun `los enlaces comunes corresponden al estandar del ecosistema`() {
        assertEquals("https://neuronova-apps.github.io/", NeuroNovaLinks.NEURONOVA_APPS_URL)
        assertEquals("https://neuronova-apps.github.io/apps/", NeuroNovaLinks.MORE_APPS_URL)
        assertEquals("https://neuronova-apps.github.io/terms/", NeuroNovaLinks.TERMS_URL)
        assertEquals("https://neuronova-apps.github.io/licenses/", NeuroNovaLinks.LICENSES_URL)
        assertEquals("https://neuronova-apps.github.io/support/", NeuroNovaLinks.SUPPORT_URL)
        assertEquals("https://neuronova-apps.github.io/support/#reportar-problema", NeuroNovaLinks.REPORT_ISSUE_URL)
    }
}
