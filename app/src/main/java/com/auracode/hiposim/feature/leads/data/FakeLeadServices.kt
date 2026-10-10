package com.auracode.hiposim.feature.leads.data

import com.auracode.hiposim.core.network.problemException
import kotlinx.coroutines.delay
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/** The demo agency of the backend development seed (`DatabaseSeed.DemoAgencyId`). */
object DemoAgency {
    const val ID = "a3e32c74-3e12-43e2-b609-3189294ff3e5"
    const val NAME = "HipoSim Demo Agency"
}

/** Answers with the demo agency of the backend seed, which is the only one that exists today. */
class FakeAgenciesService(
    private val latencyMillis: Long = 0,
) : AgenciesService {
    override suspend fun getAgencies(): List<AgencyDto> {
        if (latencyMillis > 0) delay(latencyMillis)
        return listOf(AgencyDto(id = DemoAgency.ID, name = DemoAgency.NAME))
    }
}

/**
 * Stand-in for POST /api/leads (`BuildConfig.USE_FAKE_API`). It only simulates the 201: nothing is stored and no
 * agency receives anything. It fails like the server for a missing consent (400) and an unknown agency (404).
 */
class FakeLeadsService(
    private val latencyMillis: Long = 0,
) : LeadsService {
    override suspend fun createLead(request: CreateLeadRequestDto): LeadResponseDto {
        if (latencyMillis > 0) delay(latencyMillis)

        if (request.agencyId.isBlank() || request.simulationId.isBlank() || !request.acceptedContact) {
            throw problemException(BAD_REQUEST, "Valid agency, simulation and consent are required.")
        }
        if (request.agencyId != DemoAgency.ID) throw problemException(NOT_FOUND, "Agency not found.")

        val now = OffsetDateTime.now(ZoneOffset.UTC).toString()
        return LeadResponseDto(
            id = UUID.randomUUID().toString(),
            buyerId = FAKE_BUYER_ID,
            agencyId = request.agencyId,
            simulationId = request.simulationId,
            status = "New",
            consentGrantedAt = now,
            createdAt = now,
        )
    }

    companion object {
        /** Simulated network delay of the app, so the loading state can be seen. */
        const val DEFAULT_LATENCY_MILLIS = 400L

        private const val FAKE_BUYER_ID = "00000000-0000-0000-0000-000000000000"
        private const val BAD_REQUEST = 400
        private const val NOT_FOUND = 404
    }
}
