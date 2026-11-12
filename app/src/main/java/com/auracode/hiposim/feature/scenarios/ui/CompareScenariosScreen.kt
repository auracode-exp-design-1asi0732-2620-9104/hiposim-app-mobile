package com.auracode.hiposim.feature.scenarios.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.component.ErrorBanner
import com.auracode.hiposim.core.designsystem.component.HipoSimCard
import com.auracode.hiposim.core.designsystem.component.LoadingIndicator
import com.auracode.hiposim.core.designsystem.component.OutlinedActionButton
import com.auracode.hiposim.core.designsystem.component.PrimaryButton
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.feature.scenarios.domain.ComparedMetric
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure

/** Test tag of the comparison table, used by the UI tests. */
const val COMPARISON_TABLE_TAG = "comparison_table"

/** Weight of the label column against each scenario column, which weighs 1. */
private const val LABEL_WEIGHT = 0.9f

/** Two or three saved scenarios side by side, with the lowest figure of each row highlighted (US07). */
@Composable
fun CompareScenariosScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: CompareScenariosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CompareScenariosContent(state = state, onBack = onBack, onRetry = viewModel::onRetry, onSignIn = onSignIn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompareScenariosContent(
    state: CompareScenariosUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.compare_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val failure = state.failure
        val comparison = state.comparison
        when {
            state.isLoading ->
                LoadingIndicator(
                    description = stringResource(R.string.loading),
                    modifier = Modifier.padding(innerPadding),
                )

            failure != null ->
                CompareFailure(
                    failure = failure,
                    onRetry = onRetry,
                    onSignIn = onSignIn,
                    onBack = onBack,
                    modifier = Modifier.padding(innerPadding),
                )

            comparison != null -> ComparisonTable(comparison, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun ComparisonTable(
    comparison: ComparisonUi,
    modifier: Modifier = Modifier,
) {
    val count = comparison.scenarios.size
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.compare_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HipoSimCard(modifier = Modifier.fillMaxWidth().testTag(COMPARISON_TABLE_TAG)) {
            ComparisonRow(label = null, count = count) { index ->
                Text(
                    text = comparison.scenarios[index].name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            comparison.metrics.forEach { row ->
                ComparisonRow(label = stringResource(row.metric.labelRes()), count = count) { index ->
                    MetricCell(row.cells[index])
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
            DetailRow(R.string.property_price_label, comparison.scenarios) { it.propertyPrice }
            DetailRow(R.string.scenario_down_payment_label, comparison.scenarios) { it.downPayment }
            ComparisonRow(label = stringResource(R.string.term_label), count = count) { index ->
                val years = comparison.scenarios[index].termYears
                DetailValue(years?.let { pluralStringResource(R.plurals.scenario_term_years, it, it) })
            }
            ComparisonRow(label = stringResource(R.string.grace_title), count = count) { index ->
                // "None" is a real answer, not a missing figure: say it instead of showing a dash.
                DetailValue(graceText(comparison.scenarios[index]) ?: stringResource(R.string.compare_no_grace))
            }
            ComparisonRow(label = stringResource(R.string.scenario_bonus_label), count = count) { index ->
                DetailValue(comparison.scenarios[index].goodPayerBonus ?: stringResource(R.string.compare_no_bonus))
            }
        }
    }
}

/** One row of the table: the label, then one cell for each scenario. A row without label is the header. */
@Composable
private fun ComparisonRow(
    label: String?,
    count: Int,
    cell: @Composable (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(modifier = Modifier.weight(LABEL_WEIGHT)) {
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        repeat(count) { index -> Box(modifier = Modifier.weight(1f)) { cell(index) } }
    }
}

@Composable
private fun DetailRow(
    labelRes: Int,
    scenarios: List<ScenarioUi>,
    value: (ScenarioUi) -> String?,
) {
    ComparisonRow(label = stringResource(labelRes), count = scenarios.size) { index ->
        DetailValue(value(scenarios[index]))
    }
}

@Composable
private fun DetailValue(text: String?) {
    Text(
        text = text ?: stringResource(R.string.compare_no_value),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

/**
 * A highlighted figure. The lowest one has a filled background and the word "Lowest", so the highlight does not rely on
 * colour alone; a higher one says how much more it is.
 */
@Composable
private fun MetricCell(cell: MetricCellUi) {
    val value = cell.value ?: stringResource(R.string.compare_no_value)
    val lowest = stringResource(R.string.compare_lowest)
    val colors = MaterialTheme.colorScheme
    val background = if (cell.isBest) colors.primaryContainer else colors.surface
    val textColor = if (cell.isBest) colors.onPrimaryContainer else colors.onSurface
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(color = background, shape = MaterialTheme.shapes.small)
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs)
                .semantics(mergeDescendants = true) {
                    if (cell.isBest) contentDescription = "$value, $lowest"
                },
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (cell.isBest) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
        )
        if (cell.isBest) {
            Text(
                text = lowest,
                style = MaterialTheme.typography.labelSmall,
                color = textColor,
            )
        } else if (cell.extra != null) {
            Text(
                text = stringResource(R.string.compare_higher, cell.extra),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompareFailure(
    failure: ScenarioFailure,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ErrorBanner(stringResource(scenarioFailureMessage(failure)))
        when (failure) {
            ScenarioFailure.SessionExpired ->
                PrimaryButton(
                    text = stringResource(R.string.scenarios_sign_in),
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                )

            // The picked scenarios cannot be compared as they are: the buyer has to pick again.
            ScenarioFailure.SelectionInvalid, ScenarioFailure.NotFound ->
                OutlinedActionButton(
                    text = stringResource(R.string.compare_back_to_list),
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                )

            else ->
                PrimaryButton(
                    text = stringResource(R.string.action_retry),
                    onClick = onRetry,
                    modifier = Modifier.fillMaxWidth(),
                )
        }
    }
}

private fun ComparedMetric.labelRes(): Int =
    when (this) {
        ComparedMetric.MonthlyPayment -> R.string.monthly_payment_label
        ComparedMetric.Tcea -> R.string.tcea_label
        ComparedMetric.TotalPaid -> R.string.total_paid_label
    }
