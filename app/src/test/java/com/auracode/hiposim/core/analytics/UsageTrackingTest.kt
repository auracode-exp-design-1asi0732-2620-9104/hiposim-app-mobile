package com.auracode.hiposim.core.analytics

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.leads.data.DefaultLeadsRepository
import com.auracode.hiposim.feature.leads.data.DemoAgency
import com.auracode.hiposim.feature.leads.data.FakeAgenciesService
import com.auracode.hiposim.feature.leads.domain.SendSimulationToAgencyUseCase
import com.auracode.hiposim.feature.scenarios.data.DefaultScenariosRepository
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioUseCase
import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.GraceType
import com.auracode.hiposim.feature.simulation.domain.ObserveCurrentSimulationUseCase
import com.auracode.hiposim.feature.simulation.domain.SimulateLoanUseCase
import com.auracode.hiposim.feature.simulation.domain.ValidateSimulationFormUseCase
import com.auracode.hiposim.feature.simulation.ui.ScheduleViewModel
import com.auracode.hiposim.feature.simulation.ui.SimulationFormViewModel
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.RecordingUsageTracker
import com.auracode.hiposim.testing.StubLeadsService
import com.auracode.hiposim.testing.StubScenariosService
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/** TS10b: which moments of the buyer's journey are measured, and that no personal data goes with them. */
class UsageTrackingTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tracker = RecordingUsageTracker()
    private val tokenStore = TokenStore()
    private val authRepository = DefaultAuthRepository(FakeAuthService(), tokenStore)
    private val simulationService = StubSimulationService()
    private val simulationRepository = DefaultSimulationRepository(simulationService, tokenStore)
    private val simulate = SimulateLoanUseCase(simulationRepository, tracker)

    private val personalKeys = listOf("email", "phone", "dni", "name", "password", "address")

    private fun assertNoPersonalData() =
        tracker.events.flatMap { it.second.keys }.forEach { key ->
            assertTrue(key, personalKeys.none { key.lowercase().contains(it) })
        }

    @Test
    fun completedSimulation_isMeasuredWithItsShape_andAFailedOneIsNot() =
        runTest {
            simulate(contractExampleParams.copy(graceType = GraceType.Total, graceMonths = 3))
            simulationService.answer = { throw IOException("offline") }
            simulate(contractExampleParams)

            assertEquals(listOf(UsageEvents.SIMULATION_COMPLETED), tracker.names)
            assertEquals(
                mapOf<String, Any>(
                    "termYears" to 20,
                    "graceType" to "total",
                    "graceMonths" to 3,
                    "goodPayerBonus" to true,
                    "downPaymentPercent" to 15,
                ),
                tracker.events.single().second,
            )
            assertNoPersonalData()
        }

    @Test
    fun theForm_measuresItsStartOnce() =
        runTest {
            val viewModel = SimulationFormViewModel(ValidateSimulationFormUseCase(), simulate, tracker)

            viewModel.onPropertyPriceChange("300000")
            viewModel.onTermYearsChange("25")
            viewModel.onSubmit()

            assertEquals(
                listOf(UsageEvents.SIMULATION_STARTED, UsageEvents.SIMULATION_COMPLETED),
                tracker.names,
            )
        }

    @Test
    fun submittingTheDefaults_alsoCountsAsAStart() =
        runTest {
            val viewModel = SimulationFormViewModel(ValidateSimulationFormUseCase(), simulate, tracker)

            viewModel.onSubmit()

            assertEquals(UsageEvents.SIMULATION_STARTED, tracker.names.first())
        }

    @Test
    fun openingTheSchedule_isMeasured_onlyWithASimulation() =
        runTest {
            ScheduleViewModel(ObserveCurrentSimulationUseCase(simulationRepository), tracker)
            assertTrue(tracker.events.isEmpty())

            simulationRepository.simulate(contractExampleParams)
            ScheduleViewModel(ObserveCurrentSimulationUseCase(simulationRepository), tracker)

            assertEquals(listOf(UsageEvents.SCHEDULE_OPENED), tracker.names)
        }

    @Test
    fun savedScenario_isMeasuredWithoutItsName() =
        runTest {
            authRepository.register(validRegistrationForm)
            simulationRepository.simulate(contractExampleParams)
            val save =
                SaveScenarioUseCase(
                    DefaultScenariosRepository(StubScenariosService()),
                    simulationRepository,
                    authRepository,
                    tracker,
                )

            save("Mi casa en Surco")

            assertEquals(listOf(UsageEvents.SCENARIO_SAVED), tracker.names)
            assertTrue(
                tracker.events
                    .single()
                    .second.values
                    .none { it == "Mi casa en Surco" },
            )
            assertNoPersonalData()
        }

    @Test
    fun sentLead_isMeasured_andARefusedOneIsNot() =
        runTest {
            authRepository.register(validRegistrationForm)
            simulationRepository.simulate(contractExampleParams)
            val send =
                SendSimulationToAgencyUseCase(
                    DefaultLeadsRepository(StubLeadsService(), FakeAgenciesService()),
                    simulationRepository,
                    authRepository,
                    tracker,
                )

            send(DemoAgency.ID, consentAccepted = false)
            send(DemoAgency.ID, consentAccepted = true)

            assertEquals(listOf(UsageEvents.LEAD_SENT), tracker.names)
            assertNoPersonalData()
        }
}
