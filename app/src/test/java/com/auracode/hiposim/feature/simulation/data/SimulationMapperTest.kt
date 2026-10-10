package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.core.network.NetworkJson
import com.auracode.hiposim.feature.simulation.domain.SimulationRequest
import com.auracode.hiposim.testing.readTestResource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class SimulationMapperTest {
    private val request =
        SimulationRequest(
            propertyPrice = BigDecimal("280000"),
            downPayment = BigDecimal("42000"),
            termYears = 20,
            applyGoodPayerBonus = true,
        )

    private fun contractResponse(): SimulationResponseDto =
        NetworkJson.decodeFromString(readTestResource("contract/simulation-response-with-bonus.json"))

    /** BigDecimal.equals compares the scale too, so 1.50 and 1.5 differ. Compare by value instead. */
    private fun assertDecimal(
        expected: String,
        actual: BigDecimal,
    ) = assertEquals("expected $expected but was $actual", 0, BigDecimal(expected).compareTo(actual))

    @Test
    fun contractExample_isReadWithoutErrors() {
        val response = contractResponse()

        assertEquals("3f9b8c2e-6a1d-4c58-9e0f-7d2b5a1c4e90", response.simulationId)
        assertEquals(3, response.amortizationSchedule.size)
        assertEquals(240, response.amortizationSchedule.last().period)
    }

    @Test
    fun contractExample_mapsToTheFiguresTheResultsScreenShows() {
        val simulation = requireNotNull(contractResponse().toDomainOrNull(request))

        assertDecimal("280000", simulation.propertyPrice)
        assertDecimal("42000", simulation.downPayment)
        assertDecimal("0.15", simulation.downPaymentRate)
        assertDecimal("18200", simulation.goodPayerBonus)
        assertDecimal("219800", simulation.netLoan)
        assertEquals(20, simulation.termYears)
        assertEquals(240, simulation.termMonths)
        assertDecimal("1863.99", simulation.monthlyPayment)
        assertDecimal("0", simulation.monthlyInsurance)
        assertDecimal("0.085", simulation.tcea)
        assertDecimal("0.085", simulation.irr)
        assertDecimal("-20791.75", simulation.npv)
        assertEquals("3f9b8c2e-6a1d-4c58-9e0f-7d2b5a1c4e90", simulation.simulationId)
    }

    @Test
    fun contractExample_mapsTheScheduleRows() {
        val schedule = requireNotNull(contractResponse().toDomainOrNull(request)).initialSchedule

        assertEquals(listOf(1, 2, 240), schedule.map { it.number })
        val first = schedule.first()
        assertDecimal("1863.99", first.payment)
        assertDecimal("364.63", first.principal)
        assertDecimal("1499.36", first.interest)
        assertDecimal("0", first.insurance)
        assertDecimal("219435.37", first.remainingBalance)
        assertDecimal("0", schedule.last().remainingBalance)
    }

    @Test
    fun schedule_isCutToTheFirstYear() {
        val rows = (1..240).map { contractResponse().amortizationSchedule.first().copy(period = it) }
        val response = contractResponse().copy(amortizationSchedule = rows)

        val schedule = requireNotNull(response.toDomainOrNull(request)).initialSchedule

        assertEquals(INITIAL_SCHEDULE_ROWS, schedule.size)
        assertEquals((1..INITIAL_SCHEDULE_ROWS).toList(), schedule.map { it.number })
    }

    @Test
    fun insurances_areAddedAsDecimals() {
        val row =
            contractResponse().amortizationSchedule.first().copy(
                propertyInsurance = 20.1,
                creditLifeInsurance = 35.2,
                totalPayment = 1919.29,
            )
        val response = contractResponse().copy(amortizationSchedule = listOf(row))

        val simulation = requireNotNull(response.toDomainOrNull(request))

        assertDecimal("55.3", simulation.monthlyInsurance)
        assertDecimal("55.3", simulation.initialSchedule.single().insurance)
        assertDecimal("1919.29", simulation.monthlyPayment)
    }

    @Test
    fun emptySchedule_isNotAValidSimulation() {
        val response = contractResponse().copy(amortizationSchedule = emptyList())

        assertNull(response.toDomainOrNull(request))
    }

    @Test
    fun bonusRequestedButNotApplied_showsAZeroBonus() {
        val response =
            contractResponse().copy(
                benefit = BenefitDto("Good Payer Bonus", eligible = false, appliedAmount = 0.0),
            )

        val simulation = requireNotNull(response.toDomainOrNull(request))

        assertDecimal("0", simulation.goodPayerBonus)
    }

    @Test
    fun requestDto_usesTheContractFieldNamesAndTheReferenceRate() {
        val dto = SimulationRequest(BigDecimal("280000"), BigDecimal("42000"), termYears = 20).toDto()

        val json: JsonObject = Json.parseToJsonElement(NetworkJson.encodeToString(dto)).jsonObject

        assertEquals(
            setOf("propertyPrice", "downPayment", "annualEffectiveRate", "termInMonths", "applyGoodPayerBonus"),
            json.keys,
        )
        assertEquals(280000.0, json.getValue("propertyPrice").jsonPrimitive.double, 0.0)
        assertEquals(42000.0, json.getValue("downPayment").jsonPrimitive.double, 0.0)
        assertEquals(0.0885, json.getValue("annualEffectiveRate").jsonPrimitive.double, 0.0)
        assertEquals(240, json.getValue("termInMonths").jsonPrimitive.int)
        assertEquals(false, json.getValue("applyGoodPayerBonus").jsonPrimitive.boolean)
    }

    @Test
    fun requestDto_carriesTheBonusFlagWhenRequested() {
        assertEquals(true, request.toDto().applyGoodPayerBonus)
    }
}
