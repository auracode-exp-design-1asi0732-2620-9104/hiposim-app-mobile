package com.auracode.hiposim.feature.leads.data

import kotlinx.serialization.Serializable

// POST /api/leads follows the backend contract (hiposim-backEnd, src/HipoSim.Api/Leads/LeadContracts.cs).

@Serializable
data class CreateLeadRequestDto(
    val agencyId: String,
    val simulationId: String,
    /** Must be true: explicit consent to share the simulation and the contact details. */
    val acceptedContact: Boolean,
)

/** `LeadResponse` of the backend. Dates are ISO 8601 with offset. */
@Serializable
data class LeadResponseDto(
    val id: String,
    val buyerId: String,
    val agencyId: String,
    val simulationId: String,
    val status: String,
    val consentGrantedAt: String,
    val createdAt: String,
)

/**
 * `AgencyResponse` of GET /api/agencies (hiposim-backEnd, docs/api-contract/openapi.v0.yaml).
 */
@Serializable
data class AgencyDto(
    val id: String,
    val name: String,
)
