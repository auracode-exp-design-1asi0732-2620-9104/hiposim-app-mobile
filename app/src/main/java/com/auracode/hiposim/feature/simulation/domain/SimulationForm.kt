package com.auracode.hiposim.feature.simulation.domain

/** The simulation form as typed. Amounts are whole soles, digits only. */
data class SimulationForm(
    val propertyPrice: String = "",
    val downPayment: String = "",
    val termYears: String = "",
    val applyGoodPayerBonus: Boolean = false,
)

/** Form fields that can show an error. [requestProperty] is the name the API uses in its validation errors. */
enum class SimulationField(
    val requestProperty: String,
) {
    PropertyPrice("propertyPrice"),
    DownPayment("downPayment"),
    TermYears("termInMonths"),
    ;

    companion object {
        fun fromRequestProperty(name: String): SimulationField? = entries.firstOrNull { it.requestProperty == name }
    }
}

enum class SimulationFormError {
    PropertyPriceRequired,
    PropertyPriceOutOfRange,
    DownPaymentRequired,
    DownPaymentBelowMinimum,
    DownPaymentTooHigh,
    TermRequired,
    TermOutOfRange,
}

/** [params] is set only when the form is valid. */
data class SimulationFormValidation(
    val errors: Map<SimulationField, SimulationFormError>,
    val params: SimulationParams?,
) {
    val isValid: Boolean get() = params != null
}
