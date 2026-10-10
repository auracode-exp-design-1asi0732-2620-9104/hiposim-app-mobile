package com.auracode.hiposim.core.legal

import com.auracode.hiposim.BuildConfig

/**
 * Legal documents of HipoSim. They are sections of the published Landing Page, whose address comes from
 * `BuildConfig.LANDING_BASE_URL` (Gradle property `hiposim.landingBaseUrl`).
 */
object LegalLinks {
    private const val TERMS_ANCHOR = "terminos-seccion"
    private const val PRIVACY_ANCHOR = "privacidad"

    fun termsUrl(landingBaseUrl: String = BuildConfig.LANDING_BASE_URL): String = section(landingBaseUrl, TERMS_ANCHOR)

    fun privacyUrl(landingBaseUrl: String = BuildConfig.LANDING_BASE_URL): String =
        section(landingBaseUrl, PRIVACY_ANCHOR)

    private fun section(
        landingBaseUrl: String,
        anchor: String,
    ): String {
        val page = landingBaseUrl.trim().substringBefore('#')
        return "$page#$anchor"
    }
}
