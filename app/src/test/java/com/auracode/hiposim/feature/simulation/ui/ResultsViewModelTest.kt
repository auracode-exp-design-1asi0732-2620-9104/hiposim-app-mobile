package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.feature.simulation.data.FakeSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.GetLoanSimulationUseCase
import com.auracode.hiposim.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ResultsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: ResultsViewModel

    @Before
    fun setUp() {
        viewModel = ResultsViewModel(GetLoanSimulationUseCase(FakeSimulationRepository()))
    }

    @Test
    fun init_loadsSimulationWithLandingFigures() {
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.isUnlockSheetVisible)
        val summary = requireNotNull(state.summary)
        assertEquals("S/ 280,000", summary.propertyPrice)
        assertEquals("S/ 42,000", summary.downPayment)
        assertEquals("15%", summary.downPaymentPercent)
        assertEquals("S/ 18,200", summary.goodPayerBonus)
        assertEquals("S/ 219,800", summary.netLoan)
        assertEquals(20, summary.termYears)
        assertEquals(240, summary.termMonths)
        assertEquals("S/ 1,909", summary.monthlyPayment)
        assertEquals("8.85%", summary.tcea)
    }

    @Test
    fun init_formatsScheduleRows() {
        val schedule = requireNotNull(viewModel.uiState.value.summary).schedule

        assertEquals(4, schedule.size)
        val first = schedule.first()
        assertEquals(1, first.number)
        assertEquals("S/ 1,909.00", first.payment)
        assertEquals("S/ 385.49", first.principal)
        assertEquals("S/ 1,417.71", first.interest)
        assertEquals("S/ 105.80", first.insurance)
        assertEquals("S/ 219,414.51", first.remainingBalance)
    }

    @Test
    fun init_compositionAddsUpToOneHundred() {
        val composition = requireNotNull(viewModel.uiState.value.summary).composition

        assertEquals(
            100,
            composition.interestPercent + composition.principalPercent + composition.insurancePercent,
        )
        assertEquals(74, composition.interestPercent)
        assertEquals(20, composition.principalPercent)
        assertEquals(6, composition.insurancePercent)
    }

    @Test
    fun sendQuoteClick_opensUnlockSheet() {
        viewModel.onSendQuoteClick()

        assertTrue(viewModel.uiState.value.isUnlockSheetVisible)
        assertNotNull(viewModel.uiState.value.summary)
    }

    @Test
    fun lockedDestinationClick_opensUnlockSheet() {
        viewModel.onLockedDestinationClick()

        assertTrue(viewModel.uiState.value.isUnlockSheetVisible)
    }

    @Test
    fun dismiss_hidesUnlockSheet() {
        viewModel.onSendQuoteClick()
        viewModel.onUnlockSheetDismiss()

        assertFalse(viewModel.uiState.value.isUnlockSheetVisible)
    }
}
