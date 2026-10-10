package com.auracode.hiposim.feature.simulation.domain

/** Outcome of asking the API for a simulation. */
sealed interface SimulationResult {
    data class Success(
        val simulation: LoanSimulation,
    ) : SimulationResult

    data class Failure(
        val reason: SimulationFailure,
    ) : SimulationResult
}

sealed interface SimulationFailure {
    /** The API rejected these fields (HTTP 400). The local validation normally catches them first. */
    data class Rejected(
        val fields: Set<SimulationField>,
    ) : SimulationFailure

    /** The API could not be reached: no connection, timeout or server down. */
    data object NoConnection : SimulationFailure

    /** The API answered with an error of its own or with a body the app cannot read. */
    data object ServerError : SimulationFailure
}
