package com.auracode.hiposim.feature.simulation.domain

import java.math.BigDecimal

/** What the buyer asks the API to simulate. Amounts are in soles (PEN). */
data class SimulationRequest(
    val propertyPrice: BigDecimal,
    val downPayment: BigDecimal,
    val termYears: Int,
    /** Effective annual rate (TEA) as a fraction, `0.0885` for 8.85%. */
    val annualEffectiveRate: BigDecimal = REFERENCE_ANNUAL_RATE,
    /** Ask the API to subtract the Bono del Buen Pagador when the home qualifies (US05). */
    val applyGoodPayerBonus: Boolean = false,
) {
    val termMonths: Int get() = termYears * MONTHS_PER_YEAR

    companion object {
        private const val MONTHS_PER_YEAR = 12

        /**
         * Reference TEA agreed with the team so the Landing Page, the app and the API contract show the same figures.
         * The API requires the rate in every request.
         */
        val REFERENCE_ANNUAL_RATE: BigDecimal = BigDecimal("0.0885")
    }
}
