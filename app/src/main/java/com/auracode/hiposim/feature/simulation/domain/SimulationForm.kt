package com.auracode.hiposim.feature.simulation.domain

/** Raw values of the simulation form, as typed by the user. */
data class SimulationForm(
    /** Whole soles, digits only. Empty while the user has not typed anything. */
    val propertyPrice: String = DEFAULT_PROPERTY_PRICE,
    /** Whole soles, digits only. */
    val downPayment: String = DEFAULT_DOWN_PAYMENT,
    val termYears: Int = DEFAULT_TERM_YEARS,
) {
    companion object {
        // The Landing Page simulator starts with these values.
        const val DEFAULT_PROPERTY_PRICE = "280000"
        const val DEFAULT_DOWN_PAYMENT = "42000"
        const val DEFAULT_TERM_YEARS = 20
    }
}

enum class SimulationField { PropertyPrice, DownPayment, Term }

enum class SimulationFormError {
    PropertyPriceRequired,
    DownPaymentRequired,

    /** The down payment is below 10% of the price (US01). */
    DownPaymentTooLow,

    /** The down payment covers the whole price, so there would be nothing to finance. */
    DownPaymentTooHigh,
    TermOutOfRange,

    /** Reported by the API after submitting, not by local validation. */
    RejectedByServer,
}

data class SimulationFormValidation(
    val errors: Map<SimulationField, SimulationFormError>,
    /** The request to send, present only when there are no errors. */
    val request: SimulationRequest?,
) {
    val isValid: Boolean get() = request != null
}
