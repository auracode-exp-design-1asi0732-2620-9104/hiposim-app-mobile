package com.auracode.hiposim.feature.leads.data

import com.auracode.hiposim.core.network.AuthInterceptor
import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.di.NetworkModule
import com.auracode.hiposim.feature.leads.domain.AgenciesResult
import com.auracode.hiposim.feature.leads.domain.Agency
import com.auracode.hiposim.feature.leads.domain.LeadFailure
import com.auracode.hiposim.feature.leads.domain.SendLeadResult
import com.auracode.hiposim.testing.contractResource
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.time.Instant

/** The repository against a local server that answers like the backend (src/HipoSim.Api/Leads). */
class DefaultLeadsRepositoryTest {
    private val server = MockWebServer()
    private val tokenStore = TokenStore()
    private lateinit var repository: DefaultLeadsRepository

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenStore)).build()
        val retrofit =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
                .build()
        repository =
            DefaultLeadsRepository(
                leadsService = retrofit.create(LeadsService::class.java),
                agenciesService = retrofit.create(AgenciesService::class.java),
            )
        tokenStore.token = "buyer.jwt.token"
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(
        code: Int,
        body: String = "",
        contentType: String = "application/json",
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .addHeader("Content-Type", contentType)
            .body(body)
            .build(),
    )

    private fun enqueueProblem(
        code: Int,
        resource: String,
    ) = enqueue(code, contractResource(resource), contentType = "application/problem+json")

    private suspend fun send() = repository.sendSimulation(AGENCY_ID, SIMULATION_ID, acceptedContact = true)

    @Test
    fun sendSimulation_postsTheLeadWithTheBuyerToken() =
        runTest {
            enqueue(201, contractResource("lead-response.json"))

            send()

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/leads", request.url.encodedPath)
            assertEquals("Bearer buyer.jwt.token", request.headers[AuthInterceptor.AUTHORIZATION])
            val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals(setOf("agencyId", "simulationId", "acceptedContact"), body.keys)
            assertEquals(AGENCY_ID, body.getValue("agencyId").jsonPrimitive.content)
            assertEquals(SIMULATION_ID, body.getValue("simulationId").jsonPrimitive.content)
            assertTrue(body.getValue("acceptedContact").jsonPrimitive.boolean)
        }

    @Test
    fun sendSimulation_created_returnsTheLead() =
        runTest {
            enqueue(201, contractResource("lead-response.json"))

            val lead = (send() as SendLeadResult.Success).lead

            assertEquals("5b0f2a51-93c1-4f6e-8a3d-0c7e9b1d2f44", lead.id)
            assertEquals(AGENCY_ID, lead.agencyId)
            assertEquals(SIMULATION_ID, lead.simulationId)
            assertEquals("New", lead.status)
            assertEquals(Instant.parse("2026-10-10T15:04:05.1234567Z"), lead.consentGrantedAt)
        }

    @Test
    fun sendSimulation_badRequest_isRejected() =
        runTest {
            enqueueProblem(400, "problem-lead-invalid.json")

            assertEquals(SendLeadResult.Failure(LeadFailure.Rejected), send())
        }

    @Test
    fun sendSimulation_notFound_forTheAgencyOrTheSimulation() =
        runTest {
            enqueueProblem(404, "problem-agency-not-found.json")

            assertEquals(SendLeadResult.Failure(LeadFailure.NotFound), send())
        }

    @Test
    fun sendSimulation_unauthorizedOrForbidden_isSessionExpired() =
        runTest {
            // The API answers 401 and 403 without a body.
            enqueue(401)
            enqueue(403)

            assertEquals(SendLeadResult.Failure(LeadFailure.SessionExpired), send())
            assertEquals(SendLeadResult.Failure(LeadFailure.SessionExpired), send())
        }

    @Test
    fun sendSimulation_serverErrorBadBodyAndNoConnection_areFailures() =
        runTest {
            enqueue(500)
            assertEquals(SendLeadResult.Failure(LeadFailure.ServiceUnavailable), send())

            enqueue(201, contractResource("lead-response.json").replace("2026-10-10T15:04:05.1234567+00:00", "today"))
            assertEquals(SendLeadResult.Failure(LeadFailure.Unexpected), send())

            server.close()
            assertEquals(SendLeadResult.Failure(LeadFailure.NoConnection), send())
        }

    @Test
    fun getAgencies_readsTheProposedShape() =
        runTest {
            enqueue(200, contractResource("agencies-response.json"))

            val result = repository.getAgencies()

            assertEquals("/api/agencies", server.takeRequest().url.encodedPath)
            assertEquals(AgenciesResult.Success(listOf(Agency(AGENCY_ID, "HipoSim Demo Agency"))), result)
        }

    @Test
    fun getAgencies_whileTheEndpointDoesNotExist_isAFailure() =
        runTest {
            enqueue(404)
            assertEquals(AgenciesResult.Failure(LeadFailure.NotFound), repository.getAgencies())

            server.close()
            assertEquals(AgenciesResult.Failure(LeadFailure.NoConnection), repository.getAgencies())
        }

    private companion object {
        const val AGENCY_ID = "a3e32c74-3e12-43e2-b609-3189294ff3e5"
        const val SIMULATION_ID = "3f9b8c2e-6a1d-4c58-9e0f-7d2b5a1c4e90"
    }
}
