package com.auracode.hiposim.feature.simulation.domain

/**
 * Grace period at the start of the loan (US03). [apiValue] is the `graceType` of contract v0.1.0.
 *
 * - [Partial]: the buyer pays only the interest during the grace months.
 * - [Total]: the buyer pays nothing; the interest of each month is added to the balance.
 */
enum class GraceType(
    val apiValue: String,
) {
    None("none"),
    Partial("partial"),
    Total("total"),
    ;

    companion object {
        fun fromApiValue(value: String?): GraceType = entries.firstOrNull { it.apiValue == value } ?: None
    }
}
