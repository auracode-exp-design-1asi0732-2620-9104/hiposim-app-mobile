package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.feature.simulation.domain.SimulateLoanUseCase
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationFormError
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import com.auracode.hiposim.feature.simulation.domain.ValidateSimulationFormUseCase
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.StubSimulationRepository
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal

class SimulationFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: StubSimulationRepository
    private lateinit var viewModel: SimulationFormViewModel

    @Before
    fun setUp() {
        repository = StubSimulationRepository()
        viewModel =
            SimulationFormViewModel(
                validateForm = ValidateSimulationFormUseCase(),
                simulateLoan = SimulateLoanUseCase(repository),
            )
    }

    @Test
    fun init_startsWithTheLandingValuesAndTheirDownPaymentShare() {
        val state = viewModel.uiState.value

        assertEquals("280000", state.form.propertyPrice)
        assertEquals("42000", state.form.downPayment)
        assertEquals(20, state.form.termYears)
        assertEquals("15.0%", state.downPaymentPercent)
        assertTrue(state.errors.isEmpty())
        assertFalse(state.isSubmitting)
        assertFalse(state.isSimulated)
        assertNull(state.submitError)
    }

    @Test
    fun editingTheAmounts_keepsOnlyDigitsAndUpdatesTheShare() {
        viewModel.onPropertyPriceChange("S/ 300,000")
        viewModel.onDownPaymentChange("60a000")

        val state = viewModel.uiState.value
        assertEquals("300000", state.form.propertyPrice)
        assertEquals("60000", state.form.downPayment)
        assertEquals("20.0%", state.downPaymentPercent)
    }

    @Test
    fun amounts_areLimitedToNineDigits() {
        viewModel.onPropertyPriceChange("1234567890123")

        assertEquals("123456789", viewModel.uiState.value.form.propertyPrice)
    }

    @Test
    fun share_isHiddenWhileAnAmountIsMissingOrZero() {
        viewModel.onPropertyPriceChange("")
        assertNull(viewModel.uiState.value.downPaymentPercent)

        viewModel.onPropertyPriceChange("0")
        assertNull(viewModel.uiState.value.downPaymentPercent)

        viewModel.onPropertyPriceChange("280000")
        viewModel.onDownPaymentChange("")
        assertNull(viewModel.uiState.value.downPaymentPercent)
    }

    @Test
    fun term_followsTheSlider() {
        viewModel.onTermChange(25)

        assertEquals(25, viewModel.uiState.value.form.termYears)
    }

    @Test
    fun simulateWithTooLowDownPayment_showsTheErrorAndSendsNothing() {
        viewModel.onDownPaymentChange("10000")

        viewModel.onSimulate()

        val state = viewModel.uiState.value
        assertEquals(SimulationFormError.DownPaymentTooLow, state.errors[SimulationField.DownPayment])
        assertFalse(state.isSimulated)
        assertFalse(state.isSubmitting)
        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun errors_areHiddenUntilSimulateAndThenFollowTheEdits() {
        viewModel.onDownPaymentChange("10000")
        assertTrue(
            viewModel.uiState.value.errors
                .isEmpty(),
        )

        viewModel.onSimulate()
        assertEquals(1, viewModel.uiState.value.errors.size)

        viewModel.onDownPaymentChange("30000")
        assertTrue(
            viewModel.uiState.value.errors
                .isEmpty(),
        )
    }

    @Test
    fun simulateWithValidData_sendsTheRequestAndOpensTheResults() {
        viewModel.onPropertyPriceChange("350000")
        viewModel.onDownPaymentChange("70000")
        viewModel.onTermChange(15)

        viewModel.onSimulate()

        val request = repository.requests.single()
        assertEquals(0, BigDecimal("350000").compareTo(request.propertyPrice))
        assertEquals(0, BigDecimal("70000").compareTo(request.downPayment))
        assertEquals(180, request.termMonths)
        assertEquals(0, BigDecimal("0.0885").compareTo(request.annualEffectiveRate))
        assertFalse("The bonus option arrives with US05", request.applyGoodPayerBonus)
        val state = viewModel.uiState.value
        assertTrue(state.isSimulated)
        assertFalse(state.isSubmitting)
        assertNull(state.submitError)
    }

    @Test
    fun onNavigated_clearsTheFlagSoComingBackDoesNotOpenTheResultsAgain() {
        viewModel.onSimulate()
        assertTrue(viewModel.uiState.value.isSimulated)

        viewModel.onNavigated()

        assertFalse(viewModel.uiState.value.isSimulated)
    }

    @Test
    fun whileTheRequestIsInFlight_theFormIsBusyAndASecondTapIsIgnored() {
        repository.gate = CompletableDeferred()

        viewModel.onSimulate()
        viewModel.onSimulate()

        assertTrue(viewModel.uiState.value.isSubmitting)
        assertEquals(1, repository.requests.size)

        repository.gate?.complete(Unit)

        assertFalse(viewModel.uiState.value.isSubmitting)
        assertTrue(viewModel.uiState.value.isSimulated)
    }

    @Test
    fun noConnection_showsTheRetryMessageAndStaysOnTheForm() {
        repository.simulateResult = SimulationResult.Failure(SimulationFailure.NoConnection)

        viewModel.onSimulate()

        val state = viewModel.uiState.value
        assertEquals(SubmitError.NoConnection, state.submitError)
        assertFalse(state.isSimulated)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun serverError_showsTheGenericMessage() {
        repository.simulateResult = SimulationResult.Failure(SimulationFailure.ServerError)

        viewModel.onSimulate()

        assertEquals(SubmitError.ServerError, viewModel.uiState.value.submitError)
    }

    @Test
    fun submitError_clearsWhenTheUserEditsAField() {
        repository.simulateResult = SimulationResult.Failure(SimulationFailure.NoConnection)
        viewModel.onSimulate()

        viewModel.onTermChange(10)

        assertNull(viewModel.uiState.value.submitError)
    }

    @Test
    fun retryAfterAFailure_succeeds() {
        repository.simulateResult = SimulationResult.Failure(SimulationFailure.NoConnection)
        viewModel.onSimulate()
        repository.simulateResult = SimulationResult.Success(repository.current!!)

        viewModel.onSimulate()

        val state = viewModel.uiState.value
        assertTrue(state.isSimulated)
        assertNull(state.submitError)
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun apiRejectsAField_marksThatField() {
        repository.simulateResult =
            SimulationResult.Failure(SimulationFailure.Rejected(setOf(SimulationField.DownPayment)))

        viewModel.onSimulate()

        val state = viewModel.uiState.value
        assertEquals(SimulationFormError.RejectedByServer, state.errors[SimulationField.DownPayment])
        assertNull(state.submitError)
        assertFalse(state.isSimulated)
    }

    @Test
    fun apiRejectsWithoutNamingAField_showsTheGenericMessage() {
        repository.simulateResult = SimulationResult.Failure(SimulationFailure.Rejected(emptySet()))

        viewModel.onSimulate()

        val state = viewModel.uiState.value
        assertEquals(SubmitError.ServerError, state.submitError)
        assertTrue(state.errors.isEmpty())
    }
}
