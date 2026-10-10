package com.auracode.hiposim.feature.leads.ui

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.leads.data.AgencyDto
import com.auracode.hiposim.feature.leads.data.DefaultLeadsRepository
import com.auracode.hiposim.feature.leads.data.DemoAgency
import com.auracode.hiposim.feature.leads.domain.GetAgenciesUseCase
import com.auracode.hiposim.feature.leads.domain.LeadFailure
import com.auracode.hiposim.feature.leads.domain.SendSimulationToAgencyUseCase
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.StubAgenciesService
import com.auracode.hiposim.testing.StubLeadsService
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.httpException
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class SendToAgencyViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tokenStore = TokenStore()
    private val authRepository = DefaultAuthRepository(FakeAuthService(), tokenStore)
    private val simulationRepository = DefaultSimulationRepository(StubSimulationService(), tokenStore)
    private val leadsService = StubLeadsService()
    private val agenciesService = StubAgenciesService()
    private lateinit var viewModel: SendToAgencyViewModel

    @Before
    fun setUp() {
        // The usual order: the buyer simulates as a guest and creates the account afterwards.
        runBlocking {
            simulationRepository.simulate(contractExampleParams)
            authRepository.register(validRegistrationForm)
        }
        val leadsRepository = DefaultLeadsRepository(leadsService, agenciesService)
        viewModel =
            SendToAgencyViewModel(
                getAgencies = GetAgenciesUseCase(leadsRepository),
                sendSimulation = SendSimulationToAgencyUseCase(leadsRepository, simulationRepository, authRepository),
            )
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun open_listsTheAgencies_withTheOnlyOneSelected_andTheConsentUnchecked() {
        viewModel.onOpen()

        assertFalse(state.isLoadingAgencies)
        assertEquals(listOf(AgencyUi(DemoAgency.ID, DemoAgency.NAME)), state.agencies)
        assertEquals(DemoAgency.ID, state.selectedAgencyId)
        assertFalse(state.consentAccepted)
        assertFalse(state.isSent)
    }

    @Test
    fun withSeveralAgencies_noneIsSelectedUntilTheBuyerChooses() {
        agenciesService.onGet = { listOf(AgencyDto(DemoAgency.ID, DemoAgency.NAME), AgencyDto("b", "Otra")) }

        viewModel.onOpen()
        assertNull(state.selectedAgencyId)

        viewModel.onConsentChange(true)
        viewModel.onSend()
        assertTrue(state.showAgencyError)
        assertTrue(leadsService.requests.isEmpty())

        viewModel.onAgencySelected(DemoAgency.ID)
        assertFalse(state.showAgencyError)
    }

    @Test
    fun sendWithoutConsent_showsTheErrorAndSharesNothing() {
        viewModel.onOpen()

        viewModel.onSend()

        assertTrue(state.showConsentError)
        assertFalse(state.isSent)
        assertTrue(leadsService.requests.isEmpty())

        viewModel.onConsentChange(true)
        assertFalse(state.showConsentError)
    }

    @Test
    fun sendWithConsent_sharesTheSimulationAndConfirms() {
        viewModel.onOpen()
        viewModel.onConsentChange(true)

        viewModel.onSend()

        assertEquals(DemoAgency.NAME, state.sentToAgency)
        assertTrue(state.isSent)
        assertFalse(state.isSending)
        val request = leadsService.requests.single()
        assertEquals(DemoAgency.ID, request.agencyId)
        assertEquals(simulationRepository.currentSimulation.value?.id, request.simulationId)
        assertTrue(request.acceptedContact)
    }

    @Test
    fun whileSending_andAfterSending_anotherSendIsIgnored() {
        val gate = CompletableDeferred<Unit>()
        val create = leadsService.onCreate
        leadsService.onCreate = {
            gate.await()
            create(it)
        }
        viewModel.onOpen()
        viewModel.onConsentChange(true)

        viewModel.onSend()
        assertTrue(state.isSending)
        viewModel.onSend()
        gate.complete(Unit)
        viewModel.onSend()

        assertEquals(1, leadsService.requests.size)
        assertTrue(state.isSent)
    }

    @Test
    fun reopening_startsCleanAndAsksForTheConsentAgain() {
        viewModel.onOpen()
        viewModel.onConsentChange(true)
        viewModel.onSend()

        viewModel.onOpen()

        assertFalse(state.isSent)
        assertFalse(state.consentAccepted)
        assertEquals(2, agenciesService.calls)
    }

    @Test
    fun agenciesFailure_canBeRetried() {
        agenciesService.onGet = { throw IOException("offline") }

        viewModel.onOpen()
        assertEquals(LeadFailure.NoConnection, state.agenciesFailure)
        assertTrue(state.agencies.isEmpty())

        agenciesService.onGet = { listOf(AgencyDto(DemoAgency.ID, DemoAgency.NAME)) }
        viewModel.onRetryAgencies()

        assertNull(state.agenciesFailure)
        assertEquals(1, state.agencies.size)
    }

    @Test
    fun notFoundNoConnectionAndServerFailure_areShown_andTheBuyerCanRetry() {
        viewModel.onOpen()
        viewModel.onConsentChange(true)

        leadsService.onCreate = { throw httpException(404, """{"title":"Agency not found.","status":404}""") }
        viewModel.onSend()
        assertEquals(LeadFailure.NotFound, state.sendFailure)
        assertFalse(state.isSent)

        leadsService.onCreate = { throw IOException("offline") }
        viewModel.onSend()
        assertEquals(LeadFailure.NoConnection, state.sendFailure)

        leadsService.onCreate = { throw httpException(500) }
        viewModel.onSend()
        assertEquals(LeadFailure.ServiceUnavailable, state.sendFailure)

        leadsService.onCreate = StubLeadsService().onCreate
        viewModel.onSend()
        assertNull(state.sendFailure)
        assertTrue(state.isSent)
    }

    @Test
    fun unauthorized_closesTheSessionAndAsksToSignInAgain() {
        leadsService.onCreate = { throw httpException(401) }
        viewModel.onOpen()
        viewModel.onConsentChange(true)

        viewModel.onSend()

        assertTrue(state.isSessionExpired)
        assertFalse(state.isSent)
        assertEquals(SessionState.Guest, authRepository.session.value)
        assertNull(tokenStore.token)
    }
}
