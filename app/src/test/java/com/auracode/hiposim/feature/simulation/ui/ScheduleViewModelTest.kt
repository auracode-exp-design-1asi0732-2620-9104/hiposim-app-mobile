package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.feature.simulation.domain.ObserveCurrentSimulationUseCase
import com.auracode.hiposim.feature.simulation.domain.SimulationParams
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.fakeBackedSimulationRepository
import com.auracode.hiposim.testing.simulationRepositoryWithContractExample
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ScheduleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun stateOfContractExample(): ScheduleUiState {
        val repository = runBlocking { simulationRepositoryWithContractExample() }
        return ScheduleViewModel(ObserveCurrentSimulationUseCase(repository)).uiState.value
    }

    @Test
    fun showsEveryInstallment_notOnlyThePreviewOfResults() {
        val state = stateOfContractExample()

        assertFalse(state.isLoading)
        assertFalse(state.isSimulationMissing)
        assertEquals(240, state.rows.size)
        assertEquals((1..240).toList(), state.rows.map { it.number })
        assertEquals("S/ 447,358", state.totalPaid)
        assertEquals("S/ 227,558", state.totalInterest)
    }

    @Test
    fun firstRow_hasTheFiguresOfTheContractExample() {
        val first = stateOfContractExample().rows.first()

        assertEquals("S/ 219,800.00", first.openingBalance)
        assertEquals("S/ 1,863.99", first.payment)
        assertEquals("S/ 1,499.36", first.interest)
        assertEquals("S/ 364.63", first.principal)
        assertEquals("S/ 219,435.37", first.closingBalance)
        assertFalse(first.hasInsurance)
        assertFalse(first.isGracePeriod)
    }

    @Test
    fun eachRowOpensWithTheClosingBalanceOfThePreviousOne_andTheLastOneClosesAtZero() {
        val rows = stateOfContractExample().rows

        rows.zipWithNext().forEach { (previous, next) ->
            assertEquals("row ${next.number}", previous.closingBalance, next.openingBalance)
        }
        assertEquals("S/ 0.00", rows.last().closingBalance)
    }

    @Test
    fun paymentDates_areOneMonthApart() {
        val rows = stateOfContractExample().rows
        val first: LocalDate = rows.first().paymentDate

        assertEquals(first.plusMonths(1), rows[1].paymentDate)
        assertEquals(first.plusMonths(239), rows.last().paymentDate)
    }

    @Test
    fun thirtyYearLoan_hasThreeHundredSixtyRows() =
        runTest {
            val repository = fakeBackedSimulationRepository()
            repository.simulate(contractExampleParams.copy(termYears = 30))

            val state = ScheduleViewModel(ObserveCurrentSimulationUseCase(repository)).uiState.value

            assertEquals(360, state.rows.size)
            assertEquals(360, state.rows.last().number)
        }

    @Test
    fun withoutASimulation_showsTheMissingState() {
        val state = ScheduleViewModel(ObserveCurrentSimulationUseCase(fakeBackedSimulationRepository())).uiState.value

        assertFalse(state.isLoading)
        assertTrue(state.isSimulationMissing)
        assertTrue(state.rows.isEmpty())
    }

    @Test
    fun aNewSimulation_replacesTheSchedule() =
        runTest {
            val repository = fakeBackedSimulationRepository()
            val viewModel = ScheduleViewModel(ObserveCurrentSimulationUseCase(repository))

            repository.simulate(contractExampleParams)
            assertEquals(240, viewModel.uiState.value.rows.size)

            repository.simulate(
                SimulationParams(
                    propertyPrice = BigDecimal("300000"),
                    downPayment = BigDecimal("60000"),
                    termYears = 5,
                    applyGoodPayerBonus = false,
                ),
            )
            assertEquals(60, viewModel.uiState.value.rows.size)
            assertEquals(
                "S/ 240,000.00",
                viewModel.uiState.value.rows
                    .first()
                    .openingBalance,
            )
        }
}
