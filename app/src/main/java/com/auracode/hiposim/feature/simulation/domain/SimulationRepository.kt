package com.auracode.hiposim.feature.simulation.domain

/** Source of mortgage simulations. The UI depends on this interface, never on an implementation. */
interface SimulationRepository {
    /** Calculates a simulation and keeps it as the current one when it succeeds. Works without an account. */
    suspend fun simulate(request: SimulationRequest): SimulationResult

    /**
     * The last successful simulation, or null when there is none yet. It is null after the system kills the app
     * process, because the simulation is only kept in memory.
     */
    suspend fun getCurrentSimulation(): LoanSimulation?
}
