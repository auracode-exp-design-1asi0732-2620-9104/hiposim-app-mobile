package com.auracode.hiposim.feature.simulation.domain

/** Outcome of asking for a simulation, in terms of the domain: the UI never sees HTTP or network types. */
sealed interface SimulationResult {
    data class Success(
        val simulation: LoanSimulation,
    ) : SimulationResult

    data class Failure(
        val reason: SimulationFailure,
    ) : SimulationResult
}

sealed interface SimulationFailure {
    /**
     * The API rejected the request (400). [fieldMessages] has the API message of each form field it named;
     * it is empty when the rejection is about something the form does not show, such as the rate.
     */
    data class Rejected(
        val fieldMessages: Map<SimulationField, String>,
    ) : SimulationFailure

    /** The API could not be reached: no network, timeout or server down. */
    data object NoConnection : SimulationFailure

    /** The API failed on its side (5xx). */
    data object ServiceUnavailable : SimulationFailure

    /** Anything else, including an answer that does not match the contract. */
    data object Unexpected : SimulationFailure
}
