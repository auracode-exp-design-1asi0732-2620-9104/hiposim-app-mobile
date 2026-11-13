package com.auracode.hiposim.feature.scenarios.ui

import com.auracode.hiposim.feature.scenarios.domain.ComparedMetric
import com.auracode.hiposim.feature.scenarios.domain.SavedScenario
import com.auracode.hiposim.feature.scenarios.domain.compareScenarios
import com.auracode.hiposim.feature.simulation.domain.GraceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/** US07: visible figures must preserve the differences used to highlight the comparison. */
class CompareScenariosUiStateTest {
    @Test
    fun paymentsAndTotals_keepCents_andShowSubSolDifferences() {
        val ui =
            compareScenarios(
                listOf(
                    scenario("a", "1100.10", "300000.01"),
                    scenario("b", "1100.40", "300000.02"),
                ),
            ).toUi()

        val payments = ui.row(ComparedMetric.MonthlyPayment)
        assertEquals("S/ 1,100.10", payments[0].value)
        assertEquals("S/ 1,100.40", payments[1].value)
        assertEquals("+S/ 0.30", payments[1].extra)
        assertTrue(payments[0].isBest)
        assertFalse(payments[1].isBest)
        val totals = ui.row(ComparedMetric.TotalPaid)
        assertEquals("S/ 300,000.01", totals[0].value)
        assertEquals("S/ 300,000.02", totals[1].value)
        assertEquals("+S/ 0.01", totals[1].extra)
    }

    @Test
    fun tiedLowestValues_areBothHighlighted_andMissingValuesStayUnknown() {
        val ui =
            compareScenarios(
                listOf(scenario("a", "1100.10"), scenario("b", "1100.100"), scenario("c", "1100.40")),
            ).toUi()
        val payments = ui.row(ComparedMetric.MonthlyPayment)

        assertEquals(listOf(true, true, false), payments.map { it.isBest })
        assertNull(payments[0].extra)
        assertNull(payments[1].extra)
        assertEquals("+S/ 0.30", payments[2].extra)
        ui.row(ComparedMetric.TotalPaid).forEach {
            assertNull(it.value)
            assertNull(it.extra)
            assertFalse(it.isBest)
        }
    }

    @Test
    fun equalValues_haveNoHighlightOrDifference_andRatesUsePercentagePoints() {
        val ui =
            compareScenarios(
                listOf(scenario("a", "1100.10", tcea = "0.0885"), scenario("b", "1100.10", tcea = "0.0920")),
            ).toUi()

        ui.row(ComparedMetric.MonthlyPayment).forEach {
            assertFalse(it.isBest)
            assertNull(it.extra)
        }
        val rates = ui.row(ComparedMetric.Tcea)
        assertEquals("8.85%", rates[0].value)
        assertEquals("9.20%", rates[1].value)
        assertEquals("+0.35 pp", rates[1].extra)
    }

    private fun ComparisonUi.row(metric: ComparedMetric) = metrics.first { it.metric == metric }.cells

    private fun scenario(
        id: String,
        payment: String,
        total: String? = null,
        tcea: String = "0.0885",
    ) = SavedScenario(
        id = id,
        name = id,
        simulationId = "sim-$id",
        createdAt = Instant.EPOCH,
        propertyPrice = null,
        downPayment = null,
        termMonths = 240,
        graceType = GraceType.None,
        graceMonths = 0,
        bonusRequested = false,
        goodPayerBonus = null,
        netLoan = null,
        monthlyPayment = BigDecimal(payment),
        totalPaid = total?.let(::BigDecimal),
        tcea = BigDecimal(tcea),
    )
}
