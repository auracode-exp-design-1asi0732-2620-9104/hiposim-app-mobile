package com.auracode.hiposim.feature.simulation.domain

import java.math.BigDecimal

/** Values of the simulator that do not come from the buyer. Change them here and nowhere else. */
object SimulationConfig {
    /**
     * Effective annual rate (TEA) sent with every simulation, as a fraction: `0.085` means 8.5%.
     *
     * The buyer does not type a rate in this delivery, so the app uses the reference rate of the API contract
     * example (openapi.v0.yaml). PENDING TEAM DECISION: the Landing Page shows 8.85%; both must agree.
     */
    val ANNUAL_EFFECTIVE_RATE: BigDecimal = BigDecimal("0.085")

    // Form ranges, taken from the Landing Page sliders. They are a first filter for the buyer:
    // the API validates again and is the source of truth.
    val MIN_PROPERTY_PRICE: BigDecimal = BigDecimal("120000")
    val MAX_PROPERTY_PRICE: BigDecimal = BigDecimal("800000")

    /** Minimum down payment as a fraction of the property price. */
    val MIN_DOWN_PAYMENT_RATE: BigDecimal = BigDecimal("0.10")

    const val MIN_TERM_YEARS = 5
    const val MAX_TERM_YEARS = 30
    const val MONTHS_PER_YEAR = 12
}
