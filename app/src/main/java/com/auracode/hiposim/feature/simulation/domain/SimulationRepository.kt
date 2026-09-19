package com.auracode.hiposim.feature.simulation.domain

/** Source of mortgage simulations. The UI depends on this interface, never on an implementation. */
interface SimulationRepository {
    suspend fun getCurrentSimulation(): LoanSimulation
}
