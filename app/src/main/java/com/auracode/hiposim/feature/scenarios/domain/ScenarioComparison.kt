package com.auracode.hiposim.feature.scenarios.domain

import java.math.BigDecimal

/** Fewest and most saved scenarios that can be compared side by side (US07). */
const val MIN_COMPARED_SCENARIOS = 2
const val MAX_COMPARED_SCENARIOS = 3

/** The figures the comparison highlights: for all of them, lower is better for the buyer. */
enum class ComparedMetric {
    MonthlyPayment,
    Tcea,
    TotalPaid,
}

/**
 * One highlighted figure across the compared scenarios, in the same order as [ScenarioComparison.scenarios].
 *
 * [bestIndex] is the scenario with the lowest value, or null when there is nothing to tell apart: every value is
 * equal, or fewer than two scenarios have the figure. [differences] says how much each one is above the lowest
 * (zero for the best one), and is null where the API did not send the figure.
 */
data class MetricComparison(
    val metric: ComparedMetric,
    val values: List<BigDecimal?>,
    val bestIndex: Int?,
    val differences: List<BigDecimal?>,
)

data class ScenarioComparison(
    val scenarios: List<SavedScenario>,
    val metrics: List<MetricComparison>,
)

/** Puts the highlighted figures of [scenarios] side by side. The order of the list is the order of the columns. */
fun compareScenarios(scenarios: List<SavedScenario>): ScenarioComparison =
    ScenarioComparison(
        scenarios = scenarios,
        metrics =
            listOf(
                compareMetric(ComparedMetric.MonthlyPayment, scenarios.map { it.monthlyPayment }),
                compareMetric(ComparedMetric.Tcea, scenarios.map { it.tcea }),
                compareMetric(ComparedMetric.TotalPaid, scenarios.map { it.totalPaid }),
            ),
    )

private fun compareMetric(
    metric: ComparedMetric,
    values: List<BigDecimal?>,
): MetricComparison {
    val known = values.filterNotNull()
    val lowest = known.minOrNull()
    // With a single known value, or all equal, nothing is "better": highlighting one would be inventing a difference.
    val hasDifference = known.size >= MIN_COMPARED_SCENARIOS && known.any { it.compareTo(lowest) != 0 }
    return MetricComparison(
        metric = metric,
        values = values,
        bestIndex = if (hasDifference) values.indexOfFirst { it != null && it.compareTo(lowest) == 0 } else null,
        differences = values.map { value -> if (value == null || lowest == null) null else value - lowest },
    )
}
