package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.di.NetworkModule
import com.auracode.hiposim.feature.simulation.domain.BonusStatus
import com.auracode.hiposim.feature.simulation.domain.SimulationParams
import com.auracode.hiposim.testing.contractExampleParams
import com.auracode.hiposim.testing.contractResource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

/** Mapping between the contract examples (test/resources/contract) and the domain model. */
class SimulationMapperTest {
    private val json = NetworkModule.provideJson()

    private fun response(resource: String): SimulationResponseDto = json.decodeFromString(contractResource(resource))

    private val withBonus = response("simulation-response-with-bonus.json")

    @Test
    fun request_usesTheContractNames_theTermInMonthsAndTheRateAsAFraction() {
        val dto = contractExampleParams.toRequestDto()

        assertEquals(280_000.0, dto.propertyPrice, 0.0)
        assertEquals(42_000.0, dto.downPayment, 0.0)
        assertEquals(0.085, dto.annualEffectiveRate, 0.0)
        assertEquals(240, dto.termInMonths)
        assertTrue(dto.applyGoodPayerBonus)
        assertNull(dto.gracePeriodMonths)
        assertNull(dto.graceType)
        assertNull(dto.startDate)
    }

    @Test
    fun request_leavesOutTheOptionalFieldsTheAppDoesNotUse() {
        val body = json.encodeToString(SimulationRequestDto.serializer(), contractExampleParams.toRequestDto())

        assertFalse(body.contains("gracePeriodMonths"))
        assertFalse(body.contains("graceType"))
        assertFalse(body.contains("startDate"))
    }

    @Test
    fun response_keepsWhatTheBuyerAskedFor() {
        val simulation = requireNotNull(withBonus.toDomainOrNull(contractExampleParams))

        assertEquals(BigDecimal("280000"), simulation.propertyPrice)
        assertEquals(BigDecimal("42000"), simulation.downPayment)
        assertEquals(BigDecimal("0.150000"), simulation.downPaymentRate)
        assertEquals(240, simulation.termMonths)
        assertEquals(20, simulation.termYears)
        assertEquals(BigDecimal("0.085"), simulation.annualEffectiveRate)
    }

    @Test
    fun response_mapsTheContractNamesToTheModel() {
        val simulation = requireNotNull(withBonus.toDomainOrNull(contractExampleParams))

        assertEquals("3f9b8c2e-6a1d-4c58-9e0f-7d2b5a1c4e90", simulation.id)
        // financedAmount -> netLoan, monthlyInstallment -> monthlyPayment, benefit.appliedAmount -> goodPayerBonus
        assertEquals(BigDecimal("219800.00"), simulation.netLoan)
        assertEquals(BigDecimal("1863.99"), simulation.monthlyPayment)
        assertEquals(BigDecimal("18200.00"), simulation.goodPayerBonus)
        assertEquals(BigDecimal("447357.60"), simulation.totalPaid)
        assertEquals(BigDecimal("227557.97"), simulation.totalInterest)
        assertEquals(BigDecimal("0.00"), simulation.additionalCosts)
    }

    @Test
    fun response_keepsRatesAsFractionsWithSixDecimals_andTheNegativeNpv() {
        val simulation = requireNotNull(withBonus.toDomainOrNull(contractExampleParams))

        assertEquals(BigDecimal("0.085000"), simulation.tcea)
        assertEquals(BigDecimal("0.085000"), simulation.irr)
        assertEquals(BigDecimal("-20791.75"), simulation.npv)
        assertEquals(BigDecimal("0.100000"), simulation.assumptions.annualDiscountRate)
        assertEquals(BigDecimal("0.00"), simulation.monthlyInsurance)
    }

    @Test
    fun response_mapsEveryScheduleRow() {
        val schedule = requireNotNull(withBonus.toDomainOrNull(contractExampleParams)).schedule

        assertEquals(listOf(1, 2, 240), schedule.map { it.number })
        val first = schedule.first()
        assertEquals(LocalDate.parse("2026-11-01"), first.paymentDate)
        assertEquals(BigDecimal("219800.00"), first.openingBalance)
        assertEquals(BigDecimal("1863.99"), first.installment)
        // amortization -> principal, totalPayment -> payment
        assertEquals(BigDecimal("364.63"), first.principal)
        assertEquals(BigDecimal("1499.36"), first.interest)
        assertEquals(BigDecimal("1863.99"), first.payment)
        assertEquals(BigDecimal("219435.37"), first.remainingBalance)
        assertEquals(BigDecimal("0.00"), first.insurance)
        assertFalse(first.isGracePeriod)
        assertEquals(BigDecimal("0.00"), schedule.last().remainingBalance)
    }

    @Test
    fun insurance_isAddedAsDecimals() {
        val row = withBonus.amortizationSchedule.first().copy(propertyInsurance = 20.1, creditLifeInsurance = 35.2)
        val simulation =
            requireNotNull(withBonus.copy(amortizationSchedule = listOf(row)).toDomainOrNull(contractExampleParams))

        assertEquals(BigDecimal("55.30"), simulation.schedule.first().insurance)
    }

    @Test
    fun bonusStatus_followsTheRequestAndTheBenefit() {
        val outOfRange = response("simulation-response-out-of-range.json")
        val expensiveHome =
            SimulationParams(
                propertyPrice = BigDecimal("450000"),
                downPayment = BigDecimal("90000"),
                termYears = 20,
                applyGoodPayerBonus = true,
            )

        assertEquals(BonusStatus.Applied, withBonus.toDomainOrNull(contractExampleParams)?.bonusStatus)
        assertEquals(BonusStatus.NotEligible, outOfRange.toDomainOrNull(expensiveHome)?.bonusStatus)
        assertEquals(
            BonusStatus.NotRequested,
            outOfRange.toDomainOrNull(expensiveHome.copy(applyGoodPayerBonus = false))?.bonusStatus,
        )
    }

    @Test
    fun responseWithoutSchedule_isNotASimulation() {
        assertNull(withBonus.copy(amortizationSchedule = emptyList()).toDomainOrNull(contractExampleParams))
    }
}
