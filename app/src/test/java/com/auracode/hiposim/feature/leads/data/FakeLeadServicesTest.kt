package com.auracode.hiposim.feature.leads.data

import com.auracode.hiposim.core.network.ApiError
import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.safeApiCall
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class FakeLeadServicesTest {
    private val leads = FakeLeadsService()

    private val request =
        CreateLeadRequestDto(agencyId = DemoAgency.ID, simulationId = "sim-1", acceptedContact = true)

    private suspend fun errorOf(request: CreateLeadRequestDto): ApiError =
        (safeApiCall { leads.createLead(request) } as ApiResult.Failure).error

    @Test
    fun agencies_areTheDemoAgencyOfTheBackendSeed() =
        runTest {
            assertEquals(
                listOf(AgencyDto("a3e32c74-3e12-43e2-b609-3189294ff3e5", "HipoSim Demo Agency")),
                FakeAgenciesService().getAgencies(),
            )
        }

    @Test
    fun createLead_answersLikeTheBackend() =
        runTest {
            val lead = leads.createLead(request)

            assertEquals(DemoAgency.ID, lead.agencyId)
            assertEquals("sim-1", lead.simulationId)
            assertEquals("New", lead.status)
            assertTrue(lead.id.isNotBlank())
            // Dates must be readable as ISO 8601 with offset, like the real ones.
            OffsetDateTime.parse(lead.consentGrantedAt)
            OffsetDateTime.parse(lead.createdAt)
        }

    @Test
    fun createLead_withoutConsentOrIds_isABadRequest() =
        runTest {
            val expected = ApiError.Validation(emptyMap(), "Valid agency, simulation and consent are required.")

            assertEquals(expected, errorOf(request.copy(acceptedContact = false)))
            assertEquals(expected, errorOf(request.copy(simulationId = " ")))
            assertEquals(expected, errorOf(request.copy(agencyId = "")))
        }

    @Test
    fun createLead_forAnUnknownAgency_isNotFound() =
        runTest {
            assertEquals(ApiError.Client(404, "Agency not found."), errorOf(request.copy(agencyId = "another-agency")))
        }
}
