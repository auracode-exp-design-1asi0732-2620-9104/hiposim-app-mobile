package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.core.navigation.MainDestination
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.domain.ObserveSessionUseCase
import com.auracode.hiposim.feature.auth.domain.RegistrationForm
import com.auracode.hiposim.feature.auth.domain.SignOutUseCase
import com.auracode.hiposim.feature.scenarios.data.DefaultScenariosRepository
import com.auracode.hiposim.feature.scenarios.domain.MAX_SCENARIO_NAME_LENGTH
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioUseCase
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure
import com.auracode.hiposim.feature.simulation.domain.BonusStatus
import com.auracode.hiposim.feature.simulation.domain.ObserveCurrentSimulationUseCase
import com.auracode.hiposim.feature.simulation.domain.SimulationParams
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.StubScenariosService
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.fakeBackedAuthRepository
import com.auracode.hiposim.testing.fakeBackedSimulationRepository
import com.auracode.hiposim.testing.simulationRepositoryWithContractExample
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

class ResultsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: DefaultAuthRepository
    private lateinit var viewModel: ResultsViewModel

    @Before
    fun setUp() {
        authRepository = fakeBackedAuthRepository()
        viewModel = viewModelWith(runBlocking { simulationRepositoryWithContractExample() })
    }

    private val scenariosService = StubScenariosService()

    private fun viewModelWith(simulationRepository: SimulationRepository) =
        ResultsViewModel(
            observeCurrentSimulation = ObserveCurrentSimulationUseCase(simulationRepository),
            observeSession = ObserveSessionUseCase(authRepository),
            signOut = SignOutUseCase(authRepository),
            saveScenario =
                SaveScenarioUseCase(DefaultScenariosRepository(scenariosService), simulationRepository, authRepository),
        )

    private suspend fun signIn() {
        authRepository.register(
            RegistrationForm(
                fullName = "Carlos Mendoza",
                email = "carlos@ejemplo.com",
                phone = "987654321",
                password = "secret123",
                termsAccepted = true,
                consentAccepted = true,
            ),
        )
    }

    @Test
    fun init_showsTheSimulationCalculatedFromTheForm() {
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isUnlockSheetVisible)
        assertFalse(state.isAuthenticated)
        val summary = requireNotNull(state.summary)
        assertEquals("S/ 280,000", summary.propertyPrice)
        assertEquals("S/ 42,000", summary.downPayment)
        assertEquals("15%", summary.downPaymentPercent)
        assertEquals("S/ 18,200", summary.goodPayerBonus)
        assertEquals("S/ 219,800", summary.netLoan)
        assertEquals(20, summary.termYears)
        assertEquals(240, summary.termMonths)
        assertEquals("S/ 1,864", summary.monthlyPayment)
        assertEquals("8.50%", summary.tcea)
        assertEquals(BonusStatus.Applied, summary.bonusStatus)
    }

    @Test
    fun init_showsTheRealIndicatorsOfTheContractExample() {
        val state = viewModel.uiState.value
        val summary = requireNotNull(state.summary)

        assertFalse(state.isSimulationMissing)
        assertEquals("8.50%", summary.tcea)
        assertEquals("8.50%", summary.irr)
        assertEquals("-S/ 20,792", summary.npv)
        assertEquals("10.00%", summary.npvDiscountRate)
        assertEquals("S/ 447,358", summary.totalPaid)
        assertEquals("S/ 227,558", summary.totalInterest)
        assertEquals("S/ 0.00", summary.monthlyInsurance)
    }

    @Test
    fun outOfRangeExample_showsItsIndicatorsAndTheBonusAsNotEligible() =
        runTest {
            val repository = fakeBackedSimulationRepository()
            repository.simulate(
                SimulationParams(
                    propertyPrice = BigDecimal("450000"),
                    downPayment = BigDecimal("90000"),
                    termYears = 20,
                    applyGoodPayerBonus = true,
                    // Rate of the contract example, not the app's reference rate.
                    annualEffectiveRate = BigDecimal("0.085"),
                ),
            )

            val summary = requireNotNull(viewModelWith(repository).uiState.value.summary)

            assertEquals(BonusStatus.NotEligible, summary.bonusStatus)
            assertEquals("S/ 360,000", summary.netLoan)
            assertEquals("S/ 3,053", summary.monthlyPayment)
            assertEquals("8.50%", summary.tcea)
            assertEquals("8.50%", summary.irr)
            assertEquals("-S/ 34,054", summary.npv)
        }

    @Test
    fun withoutASimulation_showsTheMissingStateInsteadOfLoadingForever() {
        val state = viewModelWith(fakeBackedSimulationRepository()).uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.isSimulationMissing)
        assertNull(state.summary)
    }

    @Test
    fun aNewSimulation_replacesTheOneOnScreen() =
        runTest {
            val repository = fakeBackedSimulationRepository()
            val results = viewModelWith(repository)
            assertTrue(results.uiState.value.isSimulationMissing)

            repository.simulate(contractExampleParams)
            assertEquals(
                "S/ 1,864",
                results.uiState.value.summary
                    ?.monthlyPayment,
            )
            assertFalse(results.uiState.value.isSimulationMissing)

            repository.simulate(contractExampleParams.copy(termYears = 10))
            assertEquals(
                120,
                results.uiState.value.summary
                    ?.termMonths,
            )
        }

    @Test
    fun init_formatsScheduleRows() {
        val schedule = requireNotNull(viewModel.uiState.value.summary).schedule

        assertEquals(4, schedule.size)
        val first = schedule.first()
        assertEquals(1, first.number)
        assertEquals("S/ 1,863.99", first.payment)
        assertEquals("S/ 364.63", first.principal)
        assertEquals("S/ 1,499.36", first.interest)
        assertEquals("S/ 0.00", first.insurance)
        assertEquals("S/ 219,435.37", first.remainingBalance)
    }

    @Test
    fun init_compositionAddsUpToOneHundred() {
        val composition = requireNotNull(viewModel.uiState.value.summary).composition

        assertEquals(
            100,
            composition.interestPercent + composition.principalPercent + composition.insurancePercent,
        )
        assertEquals(80, composition.interestPercent)
        assertEquals(20, composition.principalPercent)
        assertEquals(0, composition.insurancePercent)
    }

    @Test
    fun guest_sendQuote_opensUnlockSheet() {
        viewModel.onSendQuoteClick()

        assertTrue(viewModel.uiState.value.isUnlockSheetVisible)
        assertFalse(viewModel.uiState.value.showComingSoon)
    }

    @Test
    fun guest_lockedDestinations_openUnlockSheet() {
        listOf(MainDestination.Realtors, MainDestination.History, MainDestination.Profile).forEach { destination ->
            viewModel.onUnlockSheetDismiss()

            viewModel.onDestinationClick(destination)

            assertTrue(destination.name, viewModel.uiState.value.isUnlockSheetVisible)
        }
    }

    @Test
    fun simulateDestination_doesNothing() {
        viewModel.onDestinationClick(MainDestination.Simulate)

        assertFalse(viewModel.uiState.value.isUnlockSheetVisible)
        assertFalse(viewModel.uiState.value.showComingSoon)
    }

    @Test
    fun dismiss_hidesUnlockSheet() {
        viewModel.onSendQuoteClick()
        viewModel.onUnlockSheetDismiss()

        assertFalse(viewModel.uiState.value.isUnlockSheetVisible)
    }

    @Test
    fun downloadPdf_showsComingSoonUntilConsumed() {
        viewModel.onDownloadPdfClick()
        assertTrue(viewModel.uiState.value.showComingSoon)

        viewModel.onComingSoonShown()
        assertFalse(viewModel.uiState.value.showComingSoon)
    }

    @Test
    fun signingIn_exposesTheAccountAndClosesTheUnlockSheet() =
        runTest {
            viewModel.onSendQuoteClick()

            signIn()

            val state = viewModel.uiState.value
            assertTrue(state.isAuthenticated)
            assertEquals("Carlos Mendoza", state.account?.name)
            assertEquals("carlos@ejemplo.com", state.account?.email)
            assertFalse(state.isUnlockSheetVisible)
        }

    @Test
    fun signedIn_sendQuoteAndAgenciesOpenTheSendSheet_andHistoryOpensMyScenarios() =
        runTest {
            signIn()

            viewModel.onSendQuoteClick()
            assertTrue(viewModel.uiState.value.isSendSheetVisible)
            assertFalse(viewModel.uiState.value.isUnlockSheetVisible)
            assertFalse(viewModel.uiState.value.showComingSoon)

            viewModel.onSendSheetDismiss()
            assertFalse(viewModel.uiState.value.isSendSheetVisible)

            viewModel.onDestinationClick(MainDestination.Realtors)
            assertTrue(viewModel.uiState.value.isSendSheetVisible)

            viewModel.onSendSheetDismiss()
            viewModel.onDestinationClick(MainDestination.History)
            assertTrue(viewModel.uiState.value.navigateToScenarios)
            assertFalse(viewModel.uiState.value.showComingSoon)
            assertFalse(viewModel.uiState.value.isSendSheetVisible)

            viewModel.onScenariosNavigated()
            assertFalse(viewModel.uiState.value.navigateToScenarios)
        }

    @Test
    fun guest_sendQuote_neverOpensTheSendSheet() {
        viewModel.onSendQuoteClick()

        assertFalse(viewModel.uiState.value.isSendSheetVisible)
        assertTrue(viewModel.uiState.value.isUnlockSheetVisible)
    }

    @Test
    fun signedInWithoutASimulation_cannotOpenTheSendSheet() =
        runTest {
            val withoutSimulation = viewModelWith(fakeBackedSimulationRepository())
            signIn()

            withoutSimulation.onSendQuoteClick()

            assertFalse(withoutSimulation.uiState.value.isSendSheetVisible)
        }

    @Test
    fun signedIn_profileOpensAccountSheet_andSignOutReturnsToGuest() =
        runTest {
            signIn()

            viewModel.onDestinationClick(MainDestination.Profile)
            assertTrue(viewModel.uiState.value.isAccountSheetVisible)

            viewModel.onSignOutClick()

            val state = viewModel.uiState.value
            assertFalse(state.isAccountSheetVisible)
            assertFalse(state.isAuthenticated)
            assertNull(state.account)
        }

    // US06: save the simulation as a named scenario.

    @Test
    fun guest_saveScenario_opensTheUnlockSheetInsteadOfTheDialog() {
        viewModel.onSaveScenarioClick("S/ 280,000 a 20 años")

        assertTrue(viewModel.uiState.value.isUnlockSheetVisible)
        assertNull(viewModel.uiState.value.saveScenario)
    }

    @Test
    fun signedIn_savesTheScenarioWithTheTypedName_andConfirmsOnce() =
        runTest {
            signIn()

            viewModel.onSaveScenarioClick("S/ 280,000 a 20 años")
            assertEquals(
                "S/ 280,000 a 20 años",
                viewModel.uiState.value.saveScenario
                    ?.name,
            )

            viewModel.onScenarioNameChange("Mi primera casa")
            viewModel.onSaveScenarioConfirm()

            assertNull(viewModel.uiState.value.saveScenario)
            assertTrue(viewModel.uiState.value.scenarioSaved)
            assertEquals("Mi primera casa", scenariosService.requests.single().name)

            viewModel.onScenarioSavedShown()
            assertFalse(viewModel.uiState.value.scenarioSaved)

            viewModel.onViewScenariosClick()
            assertTrue(viewModel.uiState.value.navigateToScenarios)
        }

    @Test
    fun saveFailure_staysInTheDialogWithTheReason() =
        runTest {
            signIn()
            scenariosService.onCreate = { throw IOException("offline") }

            viewModel.onSaveScenarioClick("Mío")
            viewModel.onSaveScenarioConfirm()

            val dialog = requireNotNull(viewModel.uiState.value.saveScenario)
            assertEquals(ScenarioFailure.NoConnection, dialog.failure)
            assertFalse(dialog.isSaving)
            assertFalse(viewModel.uiState.value.scenarioSaved)

            viewModel.onScenarioNameChange("Mío 2")
            assertNull(
                viewModel.uiState.value.saveScenario
                    ?.failure,
            )

            viewModel.onSaveScenarioDismiss()
            assertNull(viewModel.uiState.value.saveScenario)
        }

    @Test
    fun scenarioName_isCutAtTheLimitOfTheApi() =
        runTest {
            signIn()
            viewModel.onSaveScenarioClick("Mío")

            viewModel.onScenarioNameChange("a".repeat(MAX_SCENARIO_NAME_LENGTH + 10))

            assertEquals(
                MAX_SCENARIO_NAME_LENGTH,
                viewModel.uiState.value.saveScenario
                    ?.name
                    ?.length,
            )
        }
}
