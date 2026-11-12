package com.auracode.hiposim.feature.scenarios.domain

import com.auracode.hiposim.core.analytics.UsageEvents
import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.scenarios.data.DefaultScenariosRepository
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.testing.RecordingUsageTracker
import com.auracode.hiposim.testing.StubScenariosService
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** US07: comparing two or three saved scenarios. */
class CompareScenariosUseCaseTest {
    private val tokenStore = TokenStore()
    private val authRepository = DefaultAuthRepository(FakeAuthService(), tokenStore)
    private val simulationRepository = DefaultSimulationRepository(StubSimulationService(), tokenStore)
    private val scenariosService = StubScenariosService()
    private val scenariosRepository = DefaultScenariosRepository(scenariosService)
    private val tracker = RecordingUsageTracker()
    private val getScenarios = GetScenariosUseCase(scenariosRepository, authRepository)
    private val compare = CompareScenariosUseCase(getScenarios, tracker)

    /** Signs in and saves one scenario for each name. Returns their ids, in the same order. */
    private suspend fun saved(vararg names: String): List<String> {
        authRepository.register(validRegistrationForm)
        simulationRepository.simulate(contractExampleParams)
        val save = SaveScenarioUseCase(scenariosRepository, simulationRepository, authRepository)
        names.forEach { save(it) }
        val byName = (getScenarios() as ScenariosResult.Success).scenarios.associateBy { it.name }
        return names.map { byName.getValue(it).id }
    }

    @Test
    fun twoOrThreeScenarios_areComparedInThePickedOrder() =
        runTest {
            val (a, b, c) = saved("Uno", "Dos", "Tres")

            val two = compare(listOf(b, a)) as CompareScenariosResult.Success
            assertEquals(listOf("Dos", "Uno"), two.comparison.scenarios.map { it.name })

            val three = compare(listOf(c, a, b)) as CompareScenariosResult.Success
            assertEquals(listOf("Tres", "Uno", "Dos"), three.comparison.scenarios.map { it.name })
        }

    @Test
    fun fewerThanTwoOrMoreThanThree_isRejected_withoutCallingTheApi() =
        runTest {
            val ids = saved("Uno", "Dos", "Tres", "Cuatro")
            val callsBefore = scenariosService.getCalls

            assertEquals(CompareScenariosResult.Failure(ScenarioFailure.SelectionInvalid), compare(ids.take(1)))
            assertEquals(CompareScenariosResult.Failure(ScenarioFailure.SelectionInvalid), compare(ids))
            assertEquals(
                CompareScenariosResult.Failure(ScenarioFailure.SelectionInvalid),
                compare(listOf(ids[0], ids[0])),
            )
            assertEquals(callsBefore, scenariosService.getCalls)
        }

    @Test
    fun aScenarioThatIsNoLongerSaved_isNotFound() =
        runTest {
            val (a) = saved("Uno", "Dos")

            assertEquals(
                CompareScenariosResult.Failure(ScenarioFailure.NotFound),
                compare(listOf(a, "no-such-id")),
            )
            assertTrue(tracker.events.isEmpty())
        }

    @Test
    fun aGuest_isAskedToSignIn() =
        runTest {
            assertEquals(
                CompareScenariosResult.Failure(ScenarioFailure.SessionExpired),
                compare(listOf("a", "b")),
            )
        }

    @Test
    fun aSuccessfulComparison_measuresOnlyHowManyWereCompared() =
        runTest {
            val ids = saved("Uno", "Dos", "Tres")

            compare(ids)

            assertEquals(listOf(UsageEvents.SCENARIOS_COMPARED), tracker.names)
            assertEquals(mapOf<String, Any>("count" to 3), tracker.events.single().second)
        }
}
