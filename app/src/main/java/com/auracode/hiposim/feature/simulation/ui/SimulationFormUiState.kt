package com.auracode.hiposim.feature.simulation.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationForm
import com.auracode.hiposim.feature.simulation.domain.SimulationFormError

@Immutable
data class SimulationFormUiState(
    val form: SimulationForm = SimulationForm(),
    /** Client-side errors. Hidden until the buyer tries to submit, then they follow the edits. */
    val errors: Map<SimulationField, SimulationFormError> = emptyMap(),
    /** Messages the API attached to a field (400). Each one is cleared when its field is edited. */
    val serverErrors: Map<SimulationField, String> = emptyMap(),
    /** A failure that does not belong to a single field. */
    val generalError: SimulationGeneralError? = null,
    /** Down payment as a share of the price (`15%`), or null while it cannot be calculated. */
    val downPaymentPercent: String? = null,
    /** Reference rate used for the simulation, already formatted (`8.85%`). */
    val annualRate: String = "",
    val isSubmitting: Boolean = false,
    /** One-off request to open Results, cleared by [SimulationFormViewModel.onResultsNavigated]. */
    val navigateToResults: Boolean = false,
)

enum class SimulationGeneralError {
    NoConnection,
    ServiceUnavailable,

    /** The API rejected the request for a reason that is not one of the form fields. */
    Rejected,
    Unexpected,
}
