package com.auracode.hiposim.feature.leads.domain

import java.time.Instant

/** A real estate agency that can receive simulations. */
data class Agency(
    val id: String,
    val name: String,
)

/** A simulation the buyer chose to share with an agency, as the API registered it. */
data class Lead(
    val id: String,
    val agencyId: String,
    val simulationId: String,
    /** Follow-up status set by the agency. A new lead starts as `New`. */
    val status: String,
    /** When the buyer gave the consent, recorded by the server. */
    val consentGrantedAt: Instant,
)

enum class LeadFailure {
    /** The buyer did not authorize the sharing. Nothing was sent. */
    ConsentRequired,

    /** There is no simulation to send. */
    SimulationMissing,

    /** The API does not know the agency or the simulation (404). */
    NotFound,

    /** The API rejected the request (400). */
    Rejected,

    /** There is no valid buyer session (401 or 403). The session is closed when this happens. */
    SessionExpired,
    NoConnection,
    ServiceUnavailable,
    Unexpected,
}

sealed interface AgenciesResult {
    data class Success(
        val agencies: List<Agency>,
    ) : AgenciesResult

    data class Failure(
        val reason: LeadFailure,
    ) : AgenciesResult
}

sealed interface SendLeadResult {
    data class Success(
        val lead: Lead,
    ) : SendLeadResult

    data class Failure(
        val reason: LeadFailure,
    ) : SendLeadResult
}
