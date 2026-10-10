package com.auracode.hiposim.feature.simulation.domain

import java.math.BigDecimal

/** What a simulation is calculated from. Amounts are in soles (PEN). */
data class SimulationParams(
    val propertyPrice: BigDecimal,
    val downPayment: BigDecimal,
    val termYears: Int,
    val applyGoodPayerBonus: Boolean,
    /** Effective annual rate as a fraction. It is not typed by the buyer, see [SimulationConfig]. */
    val annualEffectiveRate: BigDecimal = SimulationConfig.ANNUAL_EFFECTIVE_RATE,
) {
    val termMonths: Int get() = termYears * SimulationConfig.MONTHS_PER_YEAR
}
