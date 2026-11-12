package com.auracode.hiposim.feature.scenarios.domain

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.scenarios.data.DefaultScenariosRepository
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.testing.StubScenariosService
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.httpException
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** US06: saving the simulation on screen and reading the saved scenarios. */
class ScenarioUseCasesTest {
    private val tokenStore = TokenStore()
    private val authRepository = DefaultAuthRepository(FakeAuthService(), tokenStore)
    private val simulationService = StubSimulationService()
    private val simulationRepository = DefaultSimulationRepository(simulationService, tokenStore)
    private val scenariosService = StubScenariosService()
    private val scenariosRepository = DefaultScenariosRepository(scenariosService)
    private val save = SaveScenarioUseCase(scenariosRepository, simulationRepository, authRepository)
    private val getScenarios = GetScenariosUseCase(scenariosRepository, authRepository)

    private suspend fun signIn() = authRepository.register(validRegistrationForm)

    private suspend fun simulate() = simulationRepository.simulate(contractExampleParams)

    private fun currentId() = requireNotNull(simulationRepository.currentSimulation.value).id

    @Test
    fun guest_isAskedForASession_andNothingIsSaved() =
        runTest {
            simulate()

            assertEquals(SaveScenarioResult.Failure(ScenarioFailure.SessionExpired), save("Mío"))
            assertTrue(scenariosService.requests.isEmpty())
        }

    @Test
    fun blankOrLongName_isRejectedBeforeCallingTheApi() =
        runTest {
            simulate()
            signIn()

            assertEquals(SaveScenarioResult.Failure(ScenarioFailure.NameInvalid), save("   "))
            assertEquals(
                SaveScenarioResult.Failure(ScenarioFailure.NameInvalid),
                save("a".repeat(MAX_SCENARIO_NAME_LENGTH + 1)),
            )
            assertTrue(scenariosService.requests.isEmpty())
        }

    @Test
    fun withoutASimulation_nothingIsSaved() =
        runTest {
            signIn()

            assertEquals(SaveScenarioResult.Failure(ScenarioFailure.SimulationMissing), save("Mío"))
        }

    @Test
    fun simulationCalculatedAsGuest_isRecalculatedUnderTheSessionAndThatOneIsSaved() =
        runTest {
            simulate()
            val guestId = currentId()
            signIn()

            val result = save("  Plazo 20 años ")

            assertTrue(result is SaveScenarioResult.Success)
            assertEquals(2, simulationService.requests.size)
            val request = scenariosService.requests.single()
            assertNotEquals(guestId, request.simulationId)
            assertEquals(currentId(), request.simulationId)
            assertEquals("Plazo 20 años", request.name)
        }

    @Test
    fun simulationOfTheSession_isSavedWithoutRecalculating() =
        runTest {
            signIn()
            simulate()

            save("Mío")

            assertEquals(1, simulationService.requests.size)
            assertEquals(currentId(), scenariosService.requests.single().simulationId)
        }

    @Test
    fun expiredToken_signsOut() =
        runTest {
            signIn()
            simulate()
            scenariosService.onCreate = { throw httpException(401) }

            assertEquals(SaveScenarioResult.Failure(ScenarioFailure.SessionExpired), save("Mío"))
            assertEquals(SessionState.Guest, authRepository.session.value)
        }

    @Test
    fun savedScenarios_areListedForTheSession_andAGuestIsAskedToSignIn() =
        runTest {
            assertEquals(ScenariosResult.Failure(ScenarioFailure.SessionExpired), getScenarios())
            assertEquals(0, scenariosService.getCalls)

            signIn()
            simulate()
            save("Primero")
            save("Segundo")

            val scenarios = (getScenarios() as ScenariosResult.Success).scenarios
            assertEquals(listOf("Segundo", "Primero"), scenarios.map { it.name })
        }
}
