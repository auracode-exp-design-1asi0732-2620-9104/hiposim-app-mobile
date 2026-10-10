package com.auracode.hiposim.feature.simulation.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationForm
import com.auracode.hiposim.feature.simulation.domain.SimulationFormError

@Immutable
data class SimulationFormUiState(
    val form: SimulationForm = SimulationForm(),
    /** Errors are hidden until the user tries to simulate, then they follow the edits. */
    val errors: Map<SimulationField, SimulationFormError> = emptyMap(),
    /** Down payment as a share of the price, already formatted (`15.0%`). Null until both amounts are valid numbers. */
    val downPaymentPercent: String? = null,
    val isSubmitting: Boolean = false,
    /** The API call failed in a way the buyer cannot fix by editing a field. */
    val submitError: SubmitError? = null,
    /** The simulation succeeded, so the screen should open the results. Cleared by [SimulationFormViewModel.onNavigated]. */
    val isSimulated: Boolean = false,
)

enum class SubmitError { NoConnection, ServerError }
