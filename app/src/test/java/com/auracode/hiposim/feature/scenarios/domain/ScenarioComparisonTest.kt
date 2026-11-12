package com.auracode.hiposim.feature.scenarios.domain

import com.auracode.hiposim.feature.simulation.domain.GraceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant

/** US07: which figure of each row is the lowest. */
class ScenarioComparisonTest {
    private fun scenario(
        id: String,
        payment: String? = "1000",
        tcea: String? = "0.09",
        total: String? = "300000",
    ) = SavedScenario(
        id = id,
        name = "Escenario $id",
        simulationId = "sim-$id",
        createdAt = Instant.EPOCH,
        propertyPrice = BigDecimal("400000"),
        downPayment = BigDecimal("80000"),
        termMonths = 240,
        graceType = GraceType.None,
        graceMonths = 0,
        bonusRequested = false,
        goodPayerBonus = null,
        netLoan = null,
        monthlyPayment = payment?.let(::BigDecimal),
        totalPaid = total?.let(::BigDecimal),
        tcea = tcea?.let(::BigDecimal),
    )

    private fun ScenarioComparison.row(metric: ComparedMetric) = metrics.first { it.metric == metric }

    @Test
    fun lowestValueOfEachRow_isTheBest_andKeepsTheColumnOrder() {
        val comparison =
            compareScenarios(
                listOf(
                    scenario("a", payment = "1200", tcea = "0.08", total = "310000"),
                    scenario("b", payment = "1100", tcea = "0.09", total = "290000"),
                    scenario("c", payment = "1300", tcea = "0.10", total = "320000"),
                ),
            )

        assertEquals(listOf("a", "b", "c"), comparison.scenarios.map { it.id })
        assertEquals(1, comparison.row(ComparedMetric.MonthlyPayment).bestIndex)
        assertEquals(0, comparison.row(ComparedMetric.Tcea).bestIndex)
        assertEquals(1, comparison.row(ComparedMetric.TotalPaid).bestIndex)
    }

    @Test
    fun differences_areMeasuredFromTheLowest() {
        val row =
            compareScenarios(listOf(scenario("a", payment = "1200"), scenario("b", payment = "1100")))
                .row(ComparedMetric.MonthlyPayment)

        assertEquals(0, BigDecimal("100").compareTo(row.differences[0]))
        assertEquals(0, BigDecimal.ZERO.compareTo(row.differences[1]))
    }

    @Test
    fun equalValues_haveNoBest() {
        val comparison = compareScenarios(listOf(scenario("a"), scenario("b")))

        assertNull(comparison.row(ComparedMetric.MonthlyPayment).bestIndex)
        assertNull(comparison.row(ComparedMetric.Tcea).bestIndex)
    }

    @Test
    fun numericallyEqualValuesWithDifferentScale_areStillEqual() {
        val comparison = compareScenarios(listOf(scenario("a", payment = "1000"), scenario("b", payment = "1000.00")))

        assertNull(comparison.row(ComparedMetric.MonthlyPayment).bestIndex)
    }

    @Test
    fun aMissingFigure_isNeverBest_andNeverInventsADifference() {
        val comparison =
            compareScenarios(
                listOf(
                    scenario("a", payment = null),
                    scenario("b", payment = "1100"),
                    scenario("c", payment = "1200"),
                ),
            )
        val row = comparison.row(ComparedMetric.MonthlyPayment)

        assertEquals(1, row.bestIndex)
        assertNull(row.differences[0])
    }

    @Test
    fun withASingleKnownFigure_nothingIsHighlighted() {
        val row =
            compareScenarios(listOf(scenario("a", payment = null), scenario("b", payment = "1100")))
                .row(ComparedMetric.MonthlyPayment)

        assertNull(row.bestIndex)
    }
}
