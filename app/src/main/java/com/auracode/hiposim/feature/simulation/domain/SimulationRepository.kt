package com.auracode.hiposim.feature.simulation.domain

import kotlinx.coroutines.flow.StateFlow

/** Source of mortgage simulations. The UI depends on this interface, never on an implementation. */
interface SimulationRepository {
    /**
     * The last simulation calculated in this session, or null when there is none.
     *
     * It lives in memory: Results, Registration and (later) sending it to an agency all read it from here.
     */
    val currentSimulation: StateFlow<LoanSimulation?>

    /** Calculates a simulation and, on success, makes it the [currentSimulation]. */
    suspend fun simulate(params: SimulationParams): SimulationResult
}
