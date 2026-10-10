package com.auracode.hiposim.feature.leads.domain

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.leads.data.DefaultLeadsRepository
import com.auracode.hiposim.feature.leads.data.DemoAgency
import com.auracode.hiposim.feature.leads.data.FakeAgenciesService
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.testing.StubLeadsService
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.httpException
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SendSimulationToAgencyUseCaseTest {
    private val tokenStore = TokenStore()
    private val authRepository = DefaultAuthRepository(FakeAuthService(), tokenStore)
    private val simulationService = StubSimulationService()
    private val simulationRepository = DefaultSimulationRepository(simulationService, tokenStore)
    private val leadsService = StubLeadsService()
    private val send =
        SendSimulationToAgencyUseCase(
            leadsRepository = DefaultLeadsRepository(leadsService, FakeAgenciesService()),
            simulationRepository = simulationRepository,
            authRepository = authRepository,
        )

    private suspend fun signIn() = authRepository.register(validRegistrationForm)

    private suspend fun simulate() = simulationRepository.simulate(contractExampleParams)

    private fun currentId() = requireNotNull(simulationRepository.currentSimulation.value).id

    @Test
    fun withoutConsent_nothingIsSentOrRecalculated() =
        runTest {
            simulate()
            signIn()

            val result = send(DemoAgency.ID, consentAccepted = false)

            assertEquals(SendLeadResult.Failure(LeadFailure.ConsentRequired), result)
            assertTrue(leadsService.requests.isEmpty())
            assertEquals(1, simulationService.requests.size)
        }

    @Test
    fun guest_isAskedForASession_andNothingIsSent() =
        runTest {
            simulate()

            val result = send(DemoAgency.ID, consentAccepted = true)

            assertEquals(SendLeadResult.Failure(LeadFailure.SessionExpired), result)
            assertTrue(leadsService.requests.isEmpty())
        }

    @Test
    fun withoutASimulation_nothingIsSent() =
        runTest {
            signIn()

            assertEquals(
                SendLeadResult.Failure(LeadFailure.SimulationMissing),
                send(DemoAgency.ID, consentAccepted = true),
            )
            assertTrue(leadsService.requests.isEmpty())
        }

    @Test
    fun simulationCalculatedAsGuest_isCalculatedAgainUnderTheSession_andThatOneIsSent() =
        runTest {
            simulate()
            val guestSimulationId = currentId()
            assertFalse(simulationRepository.currentBelongsToSession)
            signIn()
            assertFalse(simulationRepository.currentBelongsToSession)

            val result = send(DemoAgency.ID, consentAccepted = true)

            // Same values, calculated again with the token.
            assertEquals(2, simulationService.requests.size)
            assertEquals(simulationService.requests[0], simulationService.requests[1])
            assertTrue(simulationRepository.currentBelongsToSession)
            assertNotEquals(guestSimulationId, currentId())

            val request = leadsService.requests.single()
            assertEquals(DemoAgency.ID, request.agencyId)
            assertEquals(currentId(), request.simulationId)
            assertTrue(request.acceptedContact)
            assertEquals(currentId(), (result as SendLeadResult.Success).lead.simulationId)
        }

    @Test
    fun simulationCalculatedWithTheSession_isSentAsItIs() =
        runTest {
            signIn()
            simulate()
            val ownedId = currentId()

            send(DemoAgency.ID, consentAccepted = true)

            assertEquals(1, simulationService.requests.size)
            assertEquals(ownedId, leadsService.requests.single().simulationId)
        }

    @Test
    fun simulationOfAPreviousSession_isCalculatedAgainForTheNewOne() =
        runTest {
            signIn()
            simulate()
            authRepository.signOut()
            authRepository.register(validRegistrationForm.copy(email = "otra@ejemplo.com"))

            send(DemoAgency.ID, consentAccepted = true)

            assertEquals(2, simulationService.requests.size)
        }

    @Test
    fun whenTheRecalculationFails_nothingIsSent() =
        runTest {
            simulate()
            signIn()
            simulationService.answer = { throw IOException("offline") }

            val result = send(DemoAgency.ID, consentAccepted = true)

            assertEquals(SendLeadResult.Failure(LeadFailure.NoConnection), result)
            assertTrue(leadsService.requests.isEmpty())
        }

    @Test
    fun unauthorizedWhenSending_closesTheSession() =
        runTest {
            signIn()
            simulate()
            leadsService.onCreate = { throw httpException(401) }

            val result = send(DemoAgency.ID, consentAccepted = true)

            assertEquals(SendLeadResult.Failure(LeadFailure.SessionExpired), result)
            assertEquals(SessionState.Guest, authRepository.session.value)
            assertNull(tokenStore.token)
        }

    @Test
    fun notFoundWhenSending_keepsTheSession() =
        runTest {
            signIn()
            simulate()
            leadsService.onCreate = { throw httpException(404, """{"title":"Simulation not found.","status":404}""") }

            val result = send(DemoAgency.ID, consentAccepted = true)

            assertEquals(SendLeadResult.Failure(LeadFailure.NotFound), result)
            assertTrue(authRepository.session.value is SessionState.Authenticated)
        }

    @Test
    fun getAgencies_returnsTheDemoAgency() =
        runTest {
            val result = GetAgenciesUseCase(DefaultLeadsRepository(leadsService, FakeAgenciesService()))()

            assertEquals(AgenciesResult.Success(listOf(Agency(DemoAgency.ID, DemoAgency.NAME))), result)
        }
}
