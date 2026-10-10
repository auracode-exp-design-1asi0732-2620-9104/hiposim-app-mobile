package com.auracode.hiposim.feature.simulation.data

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The in-memory service must answer like the real engine, because the app uses it by default (`USE_FAKE_API`).
 *
 * `engine-parity/fake-vs-engine-cases.json` holds answers of `POST /api/simulations` of the real HipoSim engine
 * (see the README of that folder). Each case carries its own rate, so changing the app's reference rate does not
 * touch these cases. A failure here means the fake and the engine disagree: fix the fake, or refresh the file if the
 * engine changed on purpose.
 */
class FakeSimulationParityTest {
    private val cases: List<JsonObject> =
        Json.parseToJsonElement(readParityCases()).jsonArray.map { it.jsonObject }

    @Test
    fun theFileKeepsAllTheCases() {
        assertEquals(EXPECTED_CASES, cases.size)
    }

    @Test
    fun everyCase_reproducesTheEngineFigures() =
        runTest {
            cases.forEach { case ->
                val name = case.getValue("name").jsonPrimitive.content
                val expected = case.getValue("expected").jsonObject
                val actual = FakeSimulationService().createSimulation(case.getValue("request").jsonObject.toDto())
                val check = { field: String, value: Double, tolerance: Double ->
                    assertEquals("$name: $field", expected.number(field), value, tolerance)
                }

                check("financedAmount", actual.financedAmount, CENT)
                check("monthlyInstallment", actual.monthlyInstallment, CENT)
                check("totalPaid", actual.totalPaid, CENT)
                check("totalInterest", actual.totalInterest, CENT)
                check("additionalCosts", actual.additionalCosts, CENT)
                check("tcea", actual.tcea, RATE)
                check("irr", actual.irr, RATE)
                check("npv", actual.npv, CENT)

                val benefit = expected.getValue("benefit").jsonObject
                assertEquals("$name: benefit.eligible", benefit.flag("eligible"), actual.benefit.eligible)
                assertEquals(
                    "$name: benefit.amount",
                    benefit.number("appliedAmount"),
                    actual.benefit.appliedAmount,
                    CENT,
                )

                val schedule = actual.amortizationSchedule
                assertEquals("$name: rows", expected.getValue("rows").jsonPrimitive.int, schedule.size)
                assertRow("$name: first row", expected.getValue("firstRow").jsonObject, schedule.first())
                assertRow("$name: last row", expected.getValue("lastRow").jsonObject, schedule.last())
            }
        }

    private fun assertRow(
        label: String,
        expected: JsonObject,
        actual: AmortizationEntryDto,
    ) {
        assertEquals("$label: period", expected.getValue("period").jsonPrimitive.int, actual.period)
        assertEquals("$label: installment", expected.number("installment"), actual.installment, CENT)
        assertEquals("$label: interest", expected.number("interest"), actual.interest, CENT)
        assertEquals("$label: amortization", expected.number("amortization"), actual.amortization, CENT)
        assertEquals("$label: remainingBalance", expected.number("remainingBalance"), actual.remainingBalance, CENT)
        assertEquals("$label: totalPayment", expected.number("totalPayment"), actual.totalPayment, CENT)
    }

    private fun JsonObject.toDto() =
        SimulationRequestDto(
            propertyPrice = number("propertyPrice"),
            downPayment = number("downPayment"),
            annualEffectiveRate = number("annualEffectiveRate"),
            termInMonths = getValue("termInMonths").jsonPrimitive.int,
            applyGoodPayerBonus = flag("applyGoodPayerBonus"),
        )

    private fun JsonObject.number(key: String): Double = getValue(key).jsonPrimitive.double

    private fun JsonObject.flag(key: String): Boolean = getValue(key).jsonPrimitive.boolean

    private fun readParityCases(): String =
        requireNotNull(javaClass.getResource("/engine-parity/fake-vs-engine-cases.json")) {
            "Missing test resource engine-parity/fake-vs-engine-cases.json"
        }.readText()

    private companion object {
        const val EXPECTED_CASES = 11

        /** Money is rounded to cents on both sides, so half a cent means "the same cent". */
        const val CENT = 0.005

        /** Rates have 6 decimals. */
        const val RATE = 0.0000005
    }
}
