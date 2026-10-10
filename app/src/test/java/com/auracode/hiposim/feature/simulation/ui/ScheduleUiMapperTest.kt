package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.di.NetworkModule
import com.auracode.hiposim.feature.simulation.data.SimulationResponseDto
import com.auracode.hiposim.feature.simulation.data.toDomainOrNull
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.contractResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Mapping of the contract example (3 rows of the schedule) to the rows of the screen. */
class ScheduleUiMapperTest {
    private val response: SimulationResponseDto =
        NetworkModule.provideJson().decodeFromString(contractResource("simulation-response-with-bonus.json"))

    @Test
    fun mapsEveryRowOfTheApi_withTheFourFiguresOfTheStory() {
        val state = requireNotNull(response.toDomainOrNull(contractExampleParams)).toScheduleUiState()

        assertFalse(state.isLoading)
        assertEquals(listOf(1, 2, 240), state.rows.map { it.number })
        val second = state.rows[1]
        assertEquals(LocalDate.parse("2026-12-01"), second.paymentDate)
        // US02: opening balance, amortization, interest and closing balance of each month.
        assertEquals("S/ 219,435.37", second.openingBalance)
        assertEquals("S/ 367.11", second.principal)
        assertEquals("S/ 1,496.88", second.interest)
        assertEquals("S/ 219,068.26", second.closingBalance)
        assertEquals("S/ 1,863.99", second.payment)
    }

    @Test
    fun insurance_isShownOnlyWhenTheSimulationHasIt() {
        val withoutInsurance = requireNotNull(response.toDomainOrNull(contractExampleParams)).toScheduleUiState()
        assertFalse(withoutInsurance.rows.first().hasInsurance)

        val insured = response.amortizationSchedule.first().copy(propertyInsurance = 20.1, creditLifeInsurance = 35.2)
        val withInsurance =
            requireNotNull(
                response.copy(amortizationSchedule = listOf(insured)).toDomainOrNull(contractExampleParams),
            ).toScheduleUiState()

        assertTrue(withInsurance.rows.first().hasInsurance)
        assertEquals("S/ 55.30", withInsurance.rows.first().insurance)
    }

    @Test
    fun graceRows_areFlagged() {
        val grace = response.amortizationSchedule.first().copy(isGracePeriod = true)
        val state =
            requireNotNull(
                response.copy(amortizationSchedule = listOf(grace)).toDomainOrNull(contractExampleParams),
            ).toScheduleUiState()

        assertTrue(state.rows.single().isGracePeriod)
    }
}
