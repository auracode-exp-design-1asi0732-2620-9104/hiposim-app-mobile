package com.auracode.hiposim.core.legal

import org.junit.Assert.assertEquals
import org.junit.Test

class LegalLinksTest {
    private val landing = "https://example.org/hiposim-LandingPage/"

    @Test
    fun `terms link points to the terms section of the landing page`() {
        assertEquals("https://example.org/hiposim-LandingPage/#terminos-seccion", LegalLinks.termsUrl(landing))
    }

    @Test
    fun `privacy link points to the privacy section of the landing page`() {
        assertEquals("https://example.org/hiposim-LandingPage/#privacidad", LegalLinks.privacyUrl(landing))
    }

    @Test
    fun `an anchor or spaces in the configured address are ignored`() {
        assertEquals(
            "https://example.org/hiposim-LandingPage/#privacidad",
            LegalLinks.privacyUrl("  https://example.org/hiposim-LandingPage/#hero "),
        )
    }

    @Test
    fun `default links use the configured landing page over https`() {
        assert(LegalLinks.termsUrl().startsWith("https://"))
        assert(LegalLinks.termsUrl().endsWith("#terminos-seccion"))
        assert(LegalLinks.privacyUrl().endsWith("#privacidad"))
    }
}
