package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.feature.simulation.data.DefaultSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.BonusStatus
import com.auracode.hiposim.feature.simulation.domain.SimulateLoanUseCase
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationFormError
import com.auracode.hiposim.feature.simulation.domain.ValidateSimulationFormUseCase
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.StubSimulationService
import com.auracode.hiposim.testing.httpException
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.math.BigDecimal

class SimulationFormViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var service: StubSimulationService
    private lateinit var repository: DefaultSimulationRepository
    private lateinit var viewModel: SimulationFormViewModel

    @Before
    fun setUp() {
        service = StubSimulationService()
        repository = DefaultSimulationRepository(service)
        viewModel = SimulationFormViewModel(ValidateSimulationFormUseCase(), SimulateLoanUseCase(repository))
    }

    private val state get() = viewModel.uiState.value

    @Test
    fun init_startsWithTheLandingValuesAndTheReferenceRate() {
        assertEquals("280000", state.form.propertyPrice)
        assertEquals("42000", state.form.downPayment)
        assertEquals("20", state.form.termYears)
        assertFalse(state.form.applyGoodPayerBonus)
        assertEquals("15.0%", state.downPaymentPercent)
        assertEquals("8.50%", state.annualRate)
        assertTrue(state.errors.isEmpty())
    }

    @Test
    fun amounts_keepOnlyDigits_andThePercentFollowsTheEdits() {
        viewModel.onPropertyPriceChange("S/ 300,000.x")
        viewModel.onDownPaymentChange("45 000")
        viewModel.onTermYearsChange("2x5y9")

        assertEquals("300000", state.form.propertyPrice)
        assertEquals("45000", state.form.downPayment)
        assertEquals("25", state.form.termYears)
        assertEquals("15.0%", state.downPaymentPercent)

        viewModel.onPropertyPriceChange("")
        assertNull(state.downPaymentPercent)
    }

    @Test
    fun submitValidForm_storesTheSimulationAndAsksToNavigateOnce() {
        viewModel.onBonusChange(true)

        viewModel.onSubmit()

        assertTrue(state.navigateToResults)
        assertFalse(state.isSubmitting)
        val simulation = requireNotNull(repository.currentSimulation.value)
        assertEquals(BigDecimal("1863.99"), simulation.monthlyPayment)
        assertEquals(BonusStatus.Applied, simulation.bonusStatus)

        viewModel.onResultsNavigated()
        assertFalse(state.navigateToResults)
    }

    @Test
    fun submit_sendsTheFormWithTheRateAsAFraction() {
        viewModel.onTermYearsChange("15")

        viewModel.onSubmit()

        val request = service.requests.single()
        assertEquals(280_000.0, request.propertyPrice, 0.0)
        assertEquals(42_000.0, request.downPayment, 0.0)
        assertEquals(180, request.termInMonths)
        assertEquals(0.085, request.annualEffectiveRate, 0.0)
        assertFalse(request.applyGoodPayerBonus)
    }

    @Test
    fun submitInvalidForm_showsErrorsAndDoesNotCallTheApi() {
        viewModel.onPropertyPriceChange("100000")
        viewModel.onDownPaymentChange("5000")
        viewModel.onTermYearsChange("40")

        viewModel.onSubmit()

        assertEquals(SimulationFormError.PropertyPriceOutOfRange, state.errors[SimulationField.PropertyPrice])
        assertEquals(SimulationFormError.DownPaymentBelowMinimum, state.errors[SimulationField.DownPayment])
        assertEquals(SimulationFormError.TermOutOfRange, state.errors[SimulationField.TermYears])
        assertFalse(state.navigateToResults)
        assertTrue(service.requests.isEmpty())
    }

    @Test
    fun errors_areHiddenUntilSubmitAndThenFollowTheEdits() {
        viewModel.onTermYearsChange("40")
        assertTrue(state.errors.isEmpty())

        viewModel.onSubmit()
        assertEquals(setOf(SimulationField.TermYears), state.errors.keys)

        viewModel.onTermYearsChange("30")
        assertTrue(state.errors.isEmpty())
    }

    @Test
    fun whileSubmitting_theButtonIsBusyAndASecondSubmitIsIgnored() {
        val gate = CompletableDeferred<Unit>()
        val answer = service.answer
        service.answer = {
            gate.await()
            answer(it)
        }

        viewModel.onSubmit()
        assertTrue(state.isSubmitting)
        viewModel.onSubmit()

        gate.complete(Unit)
        assertFalse(state.isSubmitting)
        assertEquals(1, service.requests.size)
    }

    @Test
    fun serverValidationError_isShownNextToItsField_andClearsWhenThatFieldIsEdited() {
        service.answer = {
            throw httpException(
                400,
                """
                {"title":"One or more validation errors occurred.","status":400,
                 "errors":{"downPayment":["The down payment must be at least 10% of the property price."],
                           "termInMonths":["The termInMonths must be between 60 and 360."]}}
                """.trimIndent(),
            )
        }

        viewModel.onSubmit()

        assertEquals(
            mapOf(
                SimulationField.DownPayment to "The down payment must be at least 10% of the property price.",
                SimulationField.TermYears to "The termInMonths must be between 60 and 360.",
            ),
            state.serverErrors,
        )
        assertNull(state.generalError)
        assertFalse(state.navigateToResults)
        assertFalse(state.isSubmitting)

        viewModel.onDownPaymentChange("50000")
        assertEquals(setOf(SimulationField.TermYears), state.serverErrors.keys)
    }

    @Test
    fun serverValidationErrorOutsideTheForm_isAGeneralRejection() {
        service.answer = {
            throw httpException(
                400,
                """{"title":"Invalid.","status":400,"errors":{"annualEffectiveRate":["Must be a fraction."]}}""",
            )
        }

        viewModel.onSubmit()

        assertTrue(state.serverErrors.isEmpty())
        assertEquals(SimulationGeneralError.Rejected, state.generalError)
    }

    @Test
    fun noConnection_showsAGeneralError_thatClearsOnTheNextEdit() {
        service.answer = { throw IOException("offline") }

        viewModel.onSubmit()

        assertEquals(SimulationGeneralError.NoConnection, state.generalError)
        assertFalse(state.navigateToResults)
        assertNull(repository.currentSimulation.value)

        viewModel.onBonusChange(true)
        assertNull(state.generalError)
    }

    @Test
    fun serverFailureAndOtherErrors_haveTheirOwnMessage() {
        service.answer = { throw httpException(503) }
        viewModel.onSubmit()
        assertEquals(SimulationGeneralError.ServiceUnavailable, state.generalError)

        service.answer = { throw httpException(401, """{"title":"Unauthorized.","status":401}""") }
        viewModel.onSubmit()
        assertEquals(SimulationGeneralError.Unexpected, state.generalError)
    }

    @Test
    fun retryAfterAFailure_succeeds() {
        service.answer = { throw IOException("offline") }
        viewModel.onSubmit()

        service.answer = StubSimulationService().answer
        viewModel.onSubmit()

        assertNull(state.generalError)
        assertTrue(state.navigateToResults)
    }
}
