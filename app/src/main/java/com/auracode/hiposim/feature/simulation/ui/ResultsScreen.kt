package com.auracode.hiposim.feature.simulation.ui

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.component.AccentButton
import com.auracode.hiposim.core.designsystem.component.BarSegment
import com.auracode.hiposim.core.designsystem.component.HighlightBanner
import com.auracode.hiposim.core.designsystem.component.HipoSimCard
import com.auracode.hiposim.core.designsystem.component.HipoSimNavigationBar
import com.auracode.hiposim.core.designsystem.component.LoadingIndicator
import com.auracode.hiposim.core.designsystem.component.MetricTile
import com.auracode.hiposim.core.designsystem.component.NavBarItem
import com.auracode.hiposim.core.designsystem.component.OutlinedActionButton
import com.auracode.hiposim.core.designsystem.component.SegmentedBar
import com.auracode.hiposim.core.designsystem.theme.HipoSimTheme
import com.auracode.hiposim.core.designsystem.theme.MetricDisplayStyle
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.core.navigation.MainDestination
import com.auracode.hiposim.feature.auth.ui.AccountSheet
import com.auracode.hiposim.feature.auth.ui.LoginRequiredSheet
import kotlinx.coroutines.launch

@Composable
fun ResultsScreen(
    onNavigateToRegister: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val comingSoon = stringResource(R.string.coming_soon)
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    LaunchedEffect(state.showComingSoon) {
        if (state.showComingSoon) {
            viewModel.onComingSoonShown()
            // Own scope: clearing the flag restarts this effect, which would cancel the snackbar.
            scope.launch { snackbarHostState.showSnackbar(comingSoon) }
        }
    }

    ResultsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = { backDispatcher?.onBackPressed() },
        onSendQuote = viewModel::onSendQuoteClick,
        onDestinationClick = viewModel::onDestinationClick,
        onDownloadPdf = viewModel::onDownloadPdfClick,
    )

    if (state.isUnlockSheetVisible) {
        LoginRequiredSheet(
            onContinueWithEmail = {
                viewModel.onUnlockSheetDismiss()
                onNavigateToRegister()
            },
            onDismiss = viewModel::onUnlockSheetDismiss,
        )
    }

    state.account?.let { account ->
        if (state.isAccountSheetVisible) {
            AccountSheet(
                account = account,
                onSignOut = viewModel::onSignOutClick,
                onDismiss = viewModel::onAccountSheetDismiss,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultsContent(
    state: ResultsUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSendQuote: () -> Unit,
    onDestinationClick: (MainDestination) -> Unit,
    onDownloadPdf: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.results_title),
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
        bottomBar = {
            ResultsBottomBar(
                isAuthenticated = state.isAuthenticated,
                onSendQuote = onSendQuote,
                onDownloadPdf = onDownloadPdf,
                onDestinationClick = onDestinationClick,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val summary = state.summary
        if (state.isLoading || summary == null) {
            LoadingIndicator(
                description = stringResource(R.string.loading),
                modifier = Modifier.padding(innerPadding),
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                SubsidyBanner(summary)
                SummaryCard(summary)
                ScheduleSection(summary)
            }
        }
    }
}

@Composable
private fun SubsidyBanner(summary: ResultsSummaryUi) {
    val bonus = summary.goodPayerBonus
    val text = stringResource(R.string.subsidy_banner_body, bonus)
    val highlight = MaterialTheme.colorScheme.secondary
    val body =
        buildAnnotatedString {
            val start = text.indexOf(bonus)
            append(text)
            if (start >= 0) {
                addStyle(
                    SpanStyle(fontWeight = FontWeight.Bold, color = highlight),
                    start,
                    start + bonus.length,
                )
            }
        }
    HighlightBanner(
        icon = Icons.Filled.Eco,
        title = stringResource(R.string.subsidy_banner_title),
        body = body,
    )
}

@Composable
private fun SummaryCard(summary: ResultsSummaryUi) {
    HipoSimCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.monthly_payment_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    Text(
                        text = summary.monthlyPayment,
                        style = MetricDisplayStyle,
                        color = HipoSimTheme.extraColors.amount,
                    )
                    Text(
                        text = stringResource(R.string.per_month),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = Spacing.xs),
                    )
                }
                Text(
                    text = stringResource(R.string.payment_includes_insurance),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TceaBadge(summary.tcea, modifier = Modifier.padding(start = Spacing.sm))
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))

        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            TileRow(
                first = {
                    MetricTile(stringResource(R.string.property_price_label), summary.propertyPrice, it)
                },
                second = {
                    MetricTile(
                        label = stringResource(R.string.down_payment_label, summary.downPaymentPercent),
                        value = summary.downPayment,
                        modifier = it,
                    )
                },
            )
            TileRow(
                first = { MetricTile(stringResource(R.string.net_loan_label), summary.netLoan, it) },
                second = {
                    MetricTile(
                        label = stringResource(R.string.term_label),
                        value = stringResource(R.string.term_value, summary.termYears, summary.termMonths),
                        modifier = it,
                    )
                },
            )
            TileRow(
                first = {
                    MetricTile(
                        label = stringResource(R.string.npv_label),
                        value = summary.npv,
                        modifier = it,
                        detail = stringResource(R.string.example_value),
                    )
                },
                second = {
                    MetricTile(
                        label = stringResource(R.string.irr_label),
                        value = summary.irr,
                        modifier = it,
                        detail = stringResource(R.string.example_value),
                    )
                },
            )
            MetricTile(
                label = stringResource(R.string.monthly_insurance_label),
                value = summary.monthlyInsurance,
                modifier = Modifier.fillMaxWidth(),
                detail = stringResource(R.string.monthly_insurance_detail),
            )
        }

        CompositionSection(summary.composition)
    }
}

@Composable
private fun TceaBadge(
    tcea: String,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.tcea_description, tcea)
    Surface(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.sm + Spacing.xs, vertical = Spacing.sm),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = stringResource(R.string.tcea_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = tcea,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TileRow(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        first(Modifier.weight(1f).fillMaxHeight())
        second(Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun CompositionSection(composition: PaymentCompositionUi) {
    val extra = HipoSimTheme.extraColors
    Column(
        modifier = Modifier.padding(top = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.composition_title),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text =
                stringResource(
                    R.string.composition_summary,
                    composition.interestPercent,
                    composition.principalPercent,
                    composition.insurancePercent,
                ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        SegmentedBar(
            segments =
                listOf(
                    BarSegment(composition.interestPercent.toFloat(), MaterialTheme.colorScheme.primary),
                    BarSegment(composition.principalPercent.toFloat(), extra.turquoise),
                    BarSegment(composition.insurancePercent.toFloat(), extra.insuranceSegment),
                ),
        )
    }
}

@Composable
private fun ScheduleSection(summary: ResultsSummaryUi) {
    val listState = rememberLazyListState()
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.schedule_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.schedule_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.schedule_range, 1, summary.schedule.size),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(start = Spacing.sm),
            )
        }
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState),
            contentPadding = PaddingValues(vertical = Spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            items(summary.schedule, key = { it.number }) { installment ->
                InstallmentCard(installment, modifier = Modifier.width(260.dp))
            }
        }
    }
}

@Composable
private fun InstallmentCard(
    installment: InstallmentUi,
    modifier: Modifier = Modifier,
) {
    HipoSimCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.installment_number, installment.number),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = installment.payment,
                style = MaterialTheme.typography.labelMedium,
                color = HipoSimTheme.extraColors.amount,
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            InstallmentLine(stringResource(R.string.installment_principal), installment.principal)
            InstallmentLine(stringResource(R.string.installment_interest), installment.interest)
            InstallmentLine(stringResource(R.string.installment_insurance), installment.insurance)
            HorizontalDivider()
            InstallmentLine(
                label = stringResource(R.string.installment_balance),
                value = installment.remainingBalance,
                emphasized = true,
            )
        }
    }
}

@Composable
private fun InstallmentLine(
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

@Composable
private fun ResultsBottomBar(
    isAuthenticated: Boolean,
    onSendQuote: () -> Unit,
    onDownloadPdf: () -> Unit,
    onDestinationClick: (MainDestination) -> Unit,
) {
    val items =
        MainDestination.entries.map { destination ->
            NavBarItem(
                id = destination.name,
                label = stringResource(destination.labelRes),
                icon = destination.icon,
                locked = destination.requiresAccount && !isAuthenticated,
            )
        }
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLowest, shadowElevation = 8.dp) {
        Column {
            Column(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                AccentButton(
                    text = stringResource(R.string.send_quote),
                    supportingText = stringResource(R.string.send_quote_hint),
                    icon = Icons.AutoMirrored.Filled.Send,
                    onClick = onSendQuote,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedActionButton(
                    text = stringResource(R.string.download_pdf),
                    icon = Icons.Filled.PictureAsPdf,
                    onClick = onDownloadPdf,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            HipoSimNavigationBar(
                items = items,
                selectedId = MainDestination.Simulate.name,
                onItemClick = { item -> onDestinationClick(MainDestination.valueOf(item.id)) },
            )
        }
    }
}
