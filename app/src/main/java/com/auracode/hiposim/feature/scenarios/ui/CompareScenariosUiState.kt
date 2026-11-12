package com.auracode.hiposim.feature.scenarios.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.scenarios.domain.ComparedMetric
import com.auracode.hiposim.feature.scenarios.domain.MetricComparison
import com.auracode.hiposim.feature.scenarios.domain.ScenarioComparison
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure
import java.math.BigDecimal

/** Everything the comparison screen draws (US07). Money and rates are already formatted (es-PE). */
@Immutable
data class CompareScenariosUiState(
    val isLoading: Boolean = true,
    val comparison: ComparisonUi? = null,
    /** Why the comparison could not be built; null when it was. */
    val failure: ScenarioFailure? = null,
)

/** The scenarios as columns, in the order the buyer picked them, and the highlighted figures as rows. */
@Immutable
data class ComparisonUi(
    val scenarios: List<ScenarioUi>,
    val metrics: List<MetricRowUi>,
)

@Immutable
data class MetricRowUi(
    val metric: ComparedMetric,
    val cells: List<MetricCellUi>,
)

/**
 * One figure of one scenario. [isBest] marks the lowest of the row, and [extra] says how much a higher one is above
 * it (for example `+S/ 120`). The best one has no [extra], and neither has a figure the API did not send.
 */
@Immutable
data class MetricCellUi(
    val value: String?,
    val isBest: Boolean,
    val extra: String?,
)

fun ScenarioComparison.toUi(): ComparisonUi =
    ComparisonUi(
        scenarios = scenarios.map { it.toUi() },
        metrics = metrics.map { it.toUi() },
    )

private fun MetricComparison.toUi(): MetricRowUi =
    MetricRowUi(
        metric = metric,
        cells =
            values.indices.map { index ->
                val value = values[index]
                val difference = differences[index]
                val isBest = bestIndex != null && difference != null && difference.signum() == 0
                MetricCellUi(
                    value = value?.let { format(metric, it) },
                    isBest = isBest,
                    extra = difference?.takeIf { bestIndex != null && it.signum() > 0 }?.let { extra(metric, it) },
                )
            },
    )

private fun format(
    metric: ComparedMetric,
    value: BigDecimal,
): String =
    when (metric) {
        ComparedMetric.Tcea -> PeruFormat.percent(value)
        ComparedMetric.MonthlyPayment, ComparedMetric.TotalPaid -> PeruFormat.soles(value)
    }

/** A rate difference is in percentage points, not in percent: 8.85% against 9.20% is `+0.35 pp`. */
private fun extra(
    metric: ComparedMetric,
    difference: BigDecimal,
): String =
    when (metric) {
        ComparedMetric.Tcea -> "+${PeruFormat.percent(difference).removeSuffix("%")} pp"
        ComparedMetric.MonthlyPayment, ComparedMetric.TotalPaid -> "+${PeruFormat.soles(difference)}"
    }
