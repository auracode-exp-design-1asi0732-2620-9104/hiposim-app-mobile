package com.auracode.hiposim.feature.scenarios.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.component.ErrorBanner
import com.auracode.hiposim.core.designsystem.component.HipoSimCard
import com.auracode.hiposim.core.designsystem.component.LoadingIndicator
import com.auracode.hiposim.core.designsystem.component.OutlinedActionButton
import com.auracode.hiposim.core.designsystem.component.PrimaryButton
import com.auracode.hiposim.core.designsystem.theme.HipoSimTheme
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.feature.scenarios.domain.ScenarioFailure
import com.auracode.hiposim.feature.simulation.domain.GraceType
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Test tag of the list, used by the UI tests. */
const val SCENARIOS_LIST_TAG = "scenarios_list"

/** The buyer's saved scenarios (US06). */
@Composable
fun ScenariosScreen(
    onBack: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: ScenariosViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScenariosContent(state = state, onBack = onBack, onRetry = viewModel::onRetry, onSignIn = onSignIn)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScenariosContent(
    state: ScenariosUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.scenarios_title),
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
        when {
            state.isLoading ->
                LoadingIndicator(
                    description = stringResource(R.string.loading),
                    modifier = Modifier.padding(innerPadding),
                )

            failure != null ->
                ScenariosFailure(
                    failure = failure,
                    onRetry = onRetry,
                    onSignIn = onSignIn,
                    modifier = Modifier.padding(innerPadding),
                )

            state.isEmpty -> EmptyScenarios(onNewSimulation = onBack, modifier = Modifier.padding(innerPadding))

            else -> ScenariosList(state.scenarios, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun ScenariosList(
    scenarios: List<ScenarioUi>,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter =
        remember(locale) {
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale)
        }
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag(SCENARIOS_LIST_TAG),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "subtitle") {
            Text(
                text = pluralStringResource(R.plurals.scenarios_count, scenarios.size, scenarios.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(scenarios, key = { it.id }) { scenario ->
            ScenarioCard(
                scenario = scenario,
                savedAt = scenario.savedAt.atZone(ZoneId.systemDefault()).format(dateFormatter),
            )
        }
    }
}

@Composable
private fun ScenarioCard(
    scenario: ScenarioUi,
    savedAt: String,
) {
    // Read as one item by screen readers: name, date, then the figures.
    HipoSimCard(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Text(
            text = scenario.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(R.string.scenario_saved_on, savedAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        scenario.monthlyPayment?.let {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.monthly_payment_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleLarge,
                    color = HipoSimTheme.extraColors.amount,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            scenario.tcea?.let { ScenarioLine(stringResource(R.string.tcea_label), it) }
            scenario.totalPaid?.let { ScenarioLine(stringResource(R.string.total_paid_label), it) }
            scenario.propertyPrice?.let { ScenarioLine(stringResource(R.string.property_price_label), it) }
            scenario.downPayment?.let { ScenarioLine(stringResource(R.string.scenario_down_payment_label), it) }
            scenario.termYears?.let {
                ScenarioLine(
                    stringResource(R.string.term_label),
                    pluralStringResource(R.plurals.scenario_term_years, it, it),
                )
            }
            scenario.goodPayerBonus?.let { ScenarioLine(stringResource(R.string.scenario_bonus_label), it) }
            graceText(scenario)?.let { ScenarioLine(stringResource(R.string.grace_title), it) }
        }
    }
}

@Composable
private fun graceText(scenario: ScenarioUi): String? =
    when (scenario.graceType) {
        GraceType.None -> null
        GraceType.Partial ->
            pluralStringResource(R.plurals.scenario_grace_partial, scenario.graceMonths, scenario.graceMonths)
        GraceType.Total ->
            pluralStringResource(R.plurals.scenario_grace_total, scenario.graceMonths, scenario.graceMonths)
    }

@Composable
private fun ScenarioLine(
    label: String,
    value: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}

@Composable
private fun EmptyScenarios(
    onNewSimulation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.scenarios_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = stringResource(R.string.scenarios_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedActionButton(
            text = stringResource(R.string.scenarios_back_to_results),
            onClick = onNewSimulation,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ScenariosFailure(
    failure: ScenarioFailure,
    onRetry: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        ErrorBanner(stringResource(scenarioFailureMessage(failure)))
        if (failure == ScenarioFailure.SessionExpired) {
            PrimaryButton(
                text = stringResource(R.string.scenarios_sign_in),
                onClick = onSignIn,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Message for a failure to read or save scenarios. Shared with the save dialog of Results. */
fun scenarioFailureMessage(failure: ScenarioFailure): Int =
    when (failure) {
        ScenarioFailure.NameInvalid -> R.string.error_scenario_name
        ScenarioFailure.SimulationMissing, ScenarioFailure.NotFound -> R.string.error_scenario_not_found
        ScenarioFailure.SessionExpired -> R.string.error_scenario_session_expired
        ScenarioFailure.NoConnection -> R.string.error_no_connection
        ScenarioFailure.ServiceUnavailable -> R.string.error_service_unavailable
        ScenarioFailure.Unexpected -> R.string.error_unexpected
    }
