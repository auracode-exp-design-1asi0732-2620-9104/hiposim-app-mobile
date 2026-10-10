package com.auracode.hiposim.feature.leads.domain

/** Real estate agencies and the simulations shared with them. */
interface LeadsRepository {
    suspend fun getAgencies(): AgenciesResult

    /**
     * Shares a simulation with an agency. [acceptedContact] is the buyer's explicit consent (Ley N° 29733):
     * it travels as the buyer gave it and is never assumed.
     */
    suspend fun sendSimulation(
        agencyId: String,
        simulationId: String,
        acceptedContact: Boolean,
    ): SendLeadResult
}
