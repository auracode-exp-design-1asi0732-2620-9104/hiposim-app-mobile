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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.component.AccentButton
import com.auracode.hiposim.core.designsystem.component.BarSegment
import com.auracode.hiposim.core.designsystem.component.HighlightBanner
import com.auracode.hiposim.core.designsystem.component.HipoSimCard
import com.auracode.hiposim.core.designsystem.component.HipoSimNavigationBar
import com.auracode.hiposim.core.designsystem.component.HipoSimTextField
import com.auracode.hiposim.core.designsystem.component.LegalFooter
import com.auracode.hiposim.core.designsystem.component.LoadingIndicator
import com.auracode.hiposim.core.designsystem.component.MetricTile
import com.auracode.hiposim.core.designsystem.component.NavBarItem
import com.auracode.hiposim.core.designsystem.component.OutlinedActionButton
import com.auracode.hiposim.core.designsystem.component.PrimaryButton
import com.auracode.hiposim.core.designsystem.component.SegmentedBar
import com.auracode.hiposim.core.designsystem.theme.HipoSimTheme
import com.auracode.hiposim.core.designsystem.theme.MetricDisplayStyle
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.core.navigation.MainDestination
import com.auracode.hiposim.feature.auth.ui.AccountSheet
import com.auracode.hiposim.feature.auth.ui.LoginRequiredSheet
import com.auracode.hiposim.feature.leads.ui.SendToAgencySheet
import com.auracode.hiposim.feature.report.ui.ReportSharer
import com.auracode.hiposim.feature.report.ui.messageRes
import com.auracode.hiposim.feature.scenarios.domain.MAX_SCENARIO_NAME_LENGTH
import com.auracode.hiposim.feature.scenarios.ui.scenarioFailureMessage
import com.auracode.hiposim.feature.simulation.domain.BonusStatus
import com.auracode.hiposim.feature.simulation.domain.GraceType
import kotlinx.coroutines.launch

@Composable
fun ResultsScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSchedule: () -> Unit,
    onNavigateToScenarios: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

    val scenarioSaved = stringResource(R.string.scenario_saved)
    val viewScenarios = stringResource(R.string.scenario_saved_action)

    LaunchedEffect(state.scenarioSaved) {
        if (state.scenarioSaved) {
            viewModel.onScenarioSavedShown()
            // Own scope, like the "coming soon" message: clearing the flag must not cancel the snackbar.
            scope.launch {
                val result = snackbarHostState.showSnackbar(scenarioSaved, actionLabel = viewScenarios)
                if (result == SnackbarResult.ActionPerformed) viewModel.onViewScenariosClick()
            }
        }
    }

    LaunchedEffect(state.navigateToScenarios) {
        if (state.navigateToScenarios) {
            viewModel.onScenariosNavigated()
            onNavigateToScenarios()
        }
    }

    // US08: the PDF arrived. Own scope, like the messages above: clearing the flag restarts this effect.
    val pdfOpenFailed = stringResource(R.string.pdf_open_failed)
    LaunchedEffect(state.pdfReport) {
        val report = state.pdfReport
        if (report != null) {
            viewModel.onPdfReportHandled()
            scope.launch {
                if (!ReportSharer.open(context, report)) snackbarHostState.showSnackbar(pdfOpenFailed)
            }
        }
    }

    val pdfFailureMessage = state.pdfFailure?.let { stringResource(it.messageRes()) }
    LaunchedEffect(state.pdfFailure) {
        if (pdfFailureMessage != null) {
            viewModel.onPdfFailureShown()
            scope.launch { snackbarHostState.showSnackbar(pdfFailureMessage) }
        }
    }

    ResultsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = { backDispatcher?.onBackPressed() },
        onSendQuote = viewModel::onSendQuoteClick,
        onDestinationClick = viewModel::onDestinationClick,
        onDownloadPdf = viewModel::onDownloadPdfClick,
        onSaveScenario = viewModel::onSaveScenarioClick,
        onViewSchedule = onNavigateToSchedule,
    )

    state.saveScenario?.let { dialog ->
        SaveScenarioDialog(
            state = dialog,
            onNameChange = viewModel::onScenarioNameChange,
            onConfirm = viewModel::onSaveScenarioConfirm,
            onDismiss = viewModel::onSaveScenarioDismiss,
        )
    }

    if (state.isUnlockSheetVisible) {
        LoginRequiredSheet(
            onContinueWithEmail = {
                viewModel.onUnlockSheetDismiss()
                onNavigateToRegister()
            },
            onSignIn = {
                viewModel.onUnlockSheetDismiss()
                onNavigateToLogin()
            },
            onDismiss = viewModel::onUnlockSheetDismiss,
        )
    }

    if (state.isSendSheetVisible) {
        SendToAgencySheet(
            onDismiss = viewModel::onSendSheetDismiss,
            onSignIn = {
                viewModel.onSendSheetDismiss()
                onNavigateToLogin()
            },
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
    onSaveScenario: (String) -> Unit,
    onViewSchedule: () -> Unit,
) {
    val suggestedName =
        state.summary?.let { stringResource(R.string.scenario_default_name, it.propertyPrice, it.termYears) }.orEmpty()
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
                showActions = state.summary != null,
                isExportingPdf = state.isExportingPdf,
                onSendQuote = onSendQuote,
                onDownloadPdf = onDownloadPdf,
                onSaveScenario = { onSaveScenario(suggestedName) },
                onNewSimulation = onBack,
                onDestinationClick = onDestinationClick,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        val summary = state.summary
        if (summary == null && state.isSimulationMissing) {
            MissingSimulation(onNewSimulation = onBack, modifier = Modifier.padding(innerPadding))
        } else if (summary == null) {
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
                when (summary.bonusStatus) {
                    BonusStatus.Applied -> SubsidyBanner(summary)
                    BonusStatus.NotEligible -> BonusNotEligibleBanner()
                    BonusStatus.NotRequested -> Unit
                }
                SummaryCard(summary)
                ScheduleSection(summary, onViewSchedule)
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

/** Nothing to show: the buyer goes back to the form, which is the previous screen. */
@Composable
internal fun MissingSimulation(
    onNewSimulation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Spacing.minTouchTarget),
        )
        Text(
            text = stringResource(R.string.results_missing_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.results_missing_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            text = stringResource(R.string.results_new_simulation),
            onClick = onNewSimulation,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** The bonus was requested but the home price is outside its range: a normal result, explained, not an error. */
@Composable
private fun BonusNotEligibleBanner() {
    HighlightBanner(
        icon = Icons.Filled.Info,
        title = stringResource(R.string.bonus_not_eligible_title),
        body = AnnotatedString(stringResource(R.string.bonus_not_eligible_body)),
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
                    text = stringResource(R.string.payment_excludes_insurance),
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
            GraceNote(summary)
            TileRow(
                first = {
                    MetricTile(
                        label = stringResource(R.string.npv_label),
                        value = summary.npv,
                        modifier = it,
                        detail = stringResource(R.string.npv_detail, summary.npvDiscountRate),
                    )
                },
                second = {
                    MetricTile(
                        label = stringResource(R.string.irr_label),
                        value = summary.irr,
                        modifier = it,
                        detail = stringResource(R.string.irr_detail),
                    )
                },
            )
            MetricTile(
                label = stringResource(R.string.monthly_insurance_label),
                value = summary.monthlyInsurance,
                modifier = Modifier.fillMaxWidth(),
                detail = stringResource(R.string.monthly_insurance_detail),
            )
            TileRow(
                first = { MetricTile(stringResource(R.string.total_interest_label), summary.totalInterest, it) },
                second = { MetricTile(stringResource(R.string.total_paid_label), summary.totalPaid, it) },
            )
            // PENDING TEAM DECISION (contract v0, open question 6): how to present the NPV to a buyer.
            // The API calculates it from the lender's side, so it is negative for a normal loan. Until the
            // team decides, the label says whose side it is and this note says it is not a loss.
            Text(
                text = stringResource(R.string.npv_explanation),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** Says which grace the buyer chose (US03); the schedule marks those months. Nothing without grace. */
@Composable
private fun GraceNote(summary: ResultsSummaryUi) {
    val text =
        when (summary.graceType) {
            GraceType.None -> return
            GraceType.Partial ->
                pluralStringResource(
                    R.plurals.grace_note_partial,
                    summary.graceMonths,
                    summary.graceMonths,
                )
            GraceType.Total ->
                pluralStringResource(
                    R.plurals.grace_note_total,
                    summary.graceMonths,
                    summary.graceMonths,
                )
        }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
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
private fun ScheduleSection(
    summary: ResultsSummaryUi,
    onViewSchedule: () -> Unit,
) {
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
        // The strip above is only a preview: every installment is on its own screen (US02).
        OutlinedActionButton(
            text = stringResource(R.string.schedule_full_action),
            icon = Icons.AutoMirrored.Filled.ListAlt,
            onClick = onViewSchedule,
            modifier = Modifier.fillMaxWidth(),
        )
        LegalFooter()
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
    showActions: Boolean,
    isExportingPdf: Boolean,
    onSendQuote: () -> Unit,
    onDownloadPdf: () -> Unit,
    onSaveScenario: () -> Unit,
    onNewSimulation: () -> Unit,
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
            // Sending or downloading only makes sense when there is a simulation on screen.
            if (showActions) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        OutlinedActionButton(
                            text = stringResource(R.string.save_scenario),
                            icon = Icons.Filled.BookmarkAdd,
                            onClick = onSaveScenario,
                            modifier = Modifier.weight(1f).testTag(SAVE_SCENARIO_TAG),
                        )
                        OutlinedActionButton(
                            text =
                                stringResource(
                                    if (isExportingPdf) R.string.pdf_generating else R.string.download_pdf,
                                ),
                            icon = Icons.Filled.PictureAsPdf,
                            onClick = onDownloadPdf,
                            enabled = !isExportingPdf,
                            modifier = Modifier.weight(1f).testTag(DOWNLOAD_PDF_TAG),
                        )
                    }
                    // Back to the form with the values still there, to try another price, down payment or term.
                    OutlinedActionButton(
                        text = stringResource(R.string.results_new_simulation),
                        icon = Icons.Filled.Refresh,
                        onClick = onNewSimulation,
                        modifier = Modifier.fillMaxWidth().testTag(NEW_SIMULATION_TAG),
                    )
                }
            }
            HipoSimNavigationBar(
                items = items,
                selectedId = MainDestination.Simulate.name,
                onItemClick = { item -> onDestinationClick(MainDestination.valueOf(item.id)) },
            )
        }
    }
}

/** Test tag of the "Save scenario" button, used by the UI tests. */
const val SAVE_SCENARIO_TAG = "save_scenario"

/** Test tag of the "Simulate again" button, used by the UI tests. */
const val NEW_SIMULATION_TAG = "new_simulation"

/** Test tag of the "Download PDF" button, used by the UI tests. */
const val DOWNLOAD_PDF_TAG = "download_pdf"

/** Names the simulation on screen and saves it as a scenario (US06). */
@Composable
private fun SaveScenarioDialog(
    state: SaveScenarioUi,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_scenario_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.save_scenario_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HipoSimTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    label = stringResource(R.string.field_scenario_name),
                    leadingIcon = Icons.Filled.BookmarkAdd,
                    supportingText = stringResource(R.string.field_scenario_name_hint, MAX_SCENARIO_NAME_LENGTH),
                    errorText = state.failure?.let { stringResource(scenarioFailureMessage(it)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !state.isSaving && state.name.isNotBlank()) {
                Text(
                    stringResource(
                        if (state.isSaving) R.string.save_scenario_saving else R.string.save_scenario_confirm,
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !state.isSaving) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
