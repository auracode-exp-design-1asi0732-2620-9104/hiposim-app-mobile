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

    /**
     * True when the [currentSimulation] was calculated with the session that is active now.
     *
     * The API only links a simulation to a buyer when the request carries that buyer's token, and it only accepts
     * a lead for a simulation of the same buyer. A buyer usually simulates first and signs in later, so the
     * simulation on screen may belong to nobody.
     */
    val currentBelongsToSession: Boolean

    /** Calculates a simulation and, on success, makes it the [currentSimulation]. */
    suspend fun simulate(params: SimulationParams): SimulationResult

    /**
     * Calculates the [currentSimulation] again with the same values, now under the active session. The figures
     * are the same; what changes is its id and that the API links it to the signed in buyer.
     */
    suspend fun recalculateCurrent(): SimulationResult
}
