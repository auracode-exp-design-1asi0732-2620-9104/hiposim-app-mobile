package com.auracode.hiposim.feature.simulation.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.component.HipoSimCard
import com.auracode.hiposim.core.designsystem.component.LoadingIndicator
import com.auracode.hiposim.core.designsystem.component.MetricTile
import com.auracode.hiposim.core.designsystem.theme.HipoSimTheme
import com.auracode.hiposim.core.designsystem.theme.Spacing
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Test tag of the list, so UI tests can scroll to any installment. */
const val SCHEDULE_LIST_TAG = "schedule_list"

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ScheduleContent(state = state, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleContent(
    state: ScheduleUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.schedule_full_title),
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
        when {
            state.isLoading ->
                LoadingIndicator(
                    description = stringResource(R.string.loading),
                    modifier = Modifier.padding(innerPadding),
                )

            state.isSimulationMissing ->
                MissingSimulation(onNewSimulation = onBack, modifier = Modifier.padding(innerPadding))

            else -> ScheduleList(state = state, modifier = Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun ScheduleList(
    state: ScheduleUiState,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }

    // Lazy on purpose: a 30-year loan has 360 rows and only the visible ones are composed.
    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .testTag(SCHEDULE_LIST_TAG),
        contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        item(key = "summary") { ScheduleSummary(state) }
        items(state.rows, key = { it.number }) { row ->
            ScheduleRow(row = row, date = row.paymentDate.format(dateFormatter))
        }
    }
}

@Composable
private fun ScheduleSummary(state: ScheduleUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.schedule_full_subtitle, state.rows.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            MetricTile(
                label = stringResource(R.string.total_interest_label),
                value = state.totalInterest,
                modifier = Modifier.weight(1f),
            )
            MetricTile(
                label = stringResource(R.string.total_paid_label),
                value = state.totalPaid,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScheduleRow(
    row: ScheduleRowUi,
    date: String,
) {
    // Read as one item by screen readers, in the order the buyer needs: month, payment, then how it splits.
    HipoSimCard(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.installment_number, row.number),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text =
                        if (row.isGracePeriod) {
                            stringResource(R.string.schedule_row_grace, date)
                        } else {
                            date
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = row.payment,
                style = MaterialTheme.typography.labelLarge,
                color = HipoSimTheme.extraColors.amount,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ScheduleLine(stringResource(R.string.installment_opening_balance), row.openingBalance)
            ScheduleLine(stringResource(R.string.installment_interest), row.interest)
            ScheduleLine(stringResource(R.string.installment_principal), row.principal)
            if (row.hasInsurance) ScheduleLine(stringResource(R.string.installment_insurance), row.insurance)
            ScheduleLine(stringResource(R.string.installment_closing_balance), row.closingBalance, emphasized = true)
        }
    }
}

@Composable
private fun ScheduleLine(
    label: String,
    value: String,
    emphasized: Boolean = false,
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
            color = if (emphasized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasized) FontWeight.SemiBold else null,
            modifier = Modifier.padding(start = Spacing.sm),
        )
    }
}
