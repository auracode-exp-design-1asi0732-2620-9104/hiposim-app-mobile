package com.auracode.hiposim.feature.leads.data

import com.auracode.hiposim.core.network.ApiError
import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.safeApiCall
import com.auracode.hiposim.feature.leads.domain.AgenciesResult
import com.auracode.hiposim.feature.leads.domain.Agency
import com.auracode.hiposim.feature.leads.domain.Lead
import com.auracode.hiposim.feature.leads.domain.LeadFailure
import com.auracode.hiposim.feature.leads.domain.LeadsRepository
import com.auracode.hiposim.feature.leads.domain.SendLeadResult
import java.time.OffsetDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** The only [LeadsRepository]. Real or in-memory answers are decided by the services that Hilt provides. */
@Singleton
class DefaultLeadsRepository
    @Inject
    constructor(
        private val leadsService: LeadsService,
        private val agenciesService: AgenciesService,
    ) : LeadsRepository {
        override suspend fun getAgencies(): AgenciesResult =
            when (val response = safeApiCall { agenciesService.getAgencies().map { Agency(it.id, it.name) } }) {
                is ApiResult.Success -> AgenciesResult.Success(response.data)
                is ApiResult.Failure -> AgenciesResult.Failure(response.error.toLeadFailure())
            }

        override suspend fun sendSimulation(
            agencyId: String,
            simulationId: String,
            acceptedContact: Boolean,
        ): SendLeadResult {
            val request = CreateLeadRequestDto(agencyId, simulationId, acceptedContact)
            // The mapping runs inside the call so an unreadable date is a failure, not a crash.
            return when (val response = safeApiCall { leadsService.createLead(request).toDomain() }) {
                is ApiResult.Success -> SendLeadResult.Success(response.data)
                is ApiResult.Failure -> SendLeadResult.Failure(response.error.toLeadFailure())
            }
        }

        private fun LeadResponseDto.toDomain() =
            Lead(
                id = id,
                agencyId = agencyId,
                simulationId = simulationId,
                status = status,
                consentGrantedAt = OffsetDateTime.parse(consentGrantedAt).toInstant(),
            )

        private fun ApiError.toLeadFailure(): LeadFailure =
            when (this) {
                ApiError.NoConnection -> LeadFailure.NoConnection
                is ApiError.Validation -> LeadFailure.Rejected
                is ApiError.Unauthorized -> LeadFailure.SessionExpired
                is ApiError.Client ->
                    when (code) {
                        HTTP_FORBIDDEN -> LeadFailure.SessionExpired
                        HTTP_NOT_FOUND -> LeadFailure.NotFound
                        else -> LeadFailure.Unexpected
                    }
                is ApiError.Server -> LeadFailure.ServiceUnavailable
                is ApiError.Conflict, is ApiError.Unexpected -> LeadFailure.Unexpected
            }

        private companion object {
            const val HTTP_FORBIDDEN = 403
            const val HTTP_NOT_FOUND = 404
        }
    }
