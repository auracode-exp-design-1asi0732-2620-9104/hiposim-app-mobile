package com.auracode.hiposim.feature.simulation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AssuredWorkload
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.component.ErrorBanner
import com.auracode.hiposim.core.designsystem.component.HipoSimTextField
import com.auracode.hiposim.core.designsystem.component.LegalFooter
import com.auracode.hiposim.core.designsystem.component.PrimaryButton
import com.auracode.hiposim.core.designsystem.component.ThousandsSeparatorTransformation
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.simulation.domain.GraceType
import com.auracode.hiposim.feature.simulation.domain.SimulationConfig
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationFormError

@Composable
fun SimulationFormScreen(
    onSimulated: () -> Unit,
    viewModel: SimulationFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.navigateToResults) {
        if (state.navigateToResults) {
            // Cleared first so coming back from Results does not navigate again.
            viewModel.onResultsNavigated()
            onSimulated()
        }
    }

    SimulationFormContent(
        state = state,
        onPropertyPriceChange = viewModel::onPropertyPriceChange,
        onDownPaymentChange = viewModel::onDownPaymentChange,
        onTermYearsChange = viewModel::onTermYearsChange,
        onBonusChange = viewModel::onBonusChange,
        onGraceTypeChange = viewModel::onGraceTypeChange,
        onGraceMonthsChange = viewModel::onGraceMonthsChange,
        onSubmit = viewModel::onSubmit,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimulationFormContent(
    state: SimulationFormUiState,
    onPropertyPriceChange: (String) -> Unit,
    onDownPaymentChange: (String) -> Unit,
    onTermYearsChange: (String) -> Unit,
    onBonusChange: (Boolean) -> Unit,
    onGraceTypeChange: (GraceType) -> Unit,
    onGraceMonthsChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AssuredWorkload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.simulation_form_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.simulation_form_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SimulationFields(
                state = state,
                onPropertyPriceChange = onPropertyPriceChange,
                onDownPaymentChange = onDownPaymentChange,
                onTermYearsChange = onTermYearsChange,
            )

            BonusRow(checked = state.form.applyGoodPayerBonus, onChange = onBonusChange)

            GraceSection(
                state = state,
                onGraceTypeChange = onGraceTypeChange,
                onGraceMonthsChange = onGraceMonthsChange,
            )

            Text(
                text = stringResource(R.string.simulation_rate_note, state.annualRate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.generalError?.let { ErrorBanner(generalErrorMessage(it)) }

            PrimaryButton(
                text =
                    stringResource(
                        if (state.isSubmitting) R.string.simulation_submitting else R.string.simulation_submit,
                    ),
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onSubmit,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.register_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            LegalFooter()
        }
    }
}

@Composable
private fun SimulationFields(
    state: SimulationFormUiState,
    onPropertyPriceChange: (String) -> Unit,
    onDownPaymentChange: (String) -> Unit,
    onTermYearsChange: (String) -> Unit,
) {
    val form = state.form
    val currency = stringResource(R.string.currency_prefix)
    val minimumPercent = PeruFormat.percent(SimulationConfig.MIN_DOWN_PAYMENT_RATE, fractionDigits = 0)
    val amountKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        HipoSimTextField(
            value = form.propertyPrice,
            onValueChange = onPropertyPriceChange,
            label = stringResource(R.string.field_property_price),
            leadingIcon = Icons.Filled.Home,
            prefix = currency,
            supportingText =
                stringResource(
                    R.string.field_property_price_hint,
                    PeruFormat.soles(SimulationConfig.MIN_PROPERTY_PRICE),
                    PeruFormat.soles(SimulationConfig.MAX_PROPERTY_PRICE),
                ),
            errorText = fieldError(state, SimulationField.PropertyPrice),
            visualTransformation = ThousandsSeparatorTransformation,
            keyboardOptions = amountKeyboard,
            modifier = Modifier.fillMaxWidth(),
        )
        HipoSimTextField(
            value = form.downPayment,
            onValueChange = onDownPaymentChange,
            label = stringResource(R.string.field_down_payment),
            leadingIcon = Icons.Filled.Savings,
            prefix = currency,
            supportingText =
                state.downPaymentPercent?.let {
                    stringResource(R.string.field_down_payment_hint, it, minimumPercent)
                } ?: stringResource(R.string.field_down_payment_hint_minimum, minimumPercent),
            errorText = fieldError(state, SimulationField.DownPayment),
            visualTransformation = ThousandsSeparatorTransformation,
            keyboardOptions = amountKeyboard,
            modifier = Modifier.fillMaxWidth(),
        )
        HipoSimTextField(
            value = form.termYears,
            onValueChange = onTermYearsChange,
            label = stringResource(R.string.field_term_years),
            leadingIcon = Icons.Filled.CalendarMonth,
            suffix = stringResource(R.string.field_term_suffix),
            supportingText =
                stringResource(
                    R.string.field_term_hint,
                    SimulationConfig.MIN_TERM_YEARS,
                    SimulationConfig.MAX_TERM_YEARS,
                ),
            errorText = fieldError(state, SimulationField.TermYears),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BonusRow(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = Spacing.minTouchTarget)
                .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        // The whole row is the toggle target, so the checkbox itself does not handle clicks.
        Checkbox(checked = checked, onCheckedChange = null)
        Column {
            Text(
                text = stringResource(R.string.bonus_checkbox_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.bonus_checkbox_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Grace period of US03: none, partial (only interest) or total (nothing paid, interest capitalized). The months
 * field appears only when a grace type is chosen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GraceSection(
    state: SimulationFormUiState,
    onGraceTypeChange: (GraceType) -> Unit,
    onGraceMonthsChange: (String) -> Unit,
) {
    val form = state.form
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            text = stringResource(R.string.grace_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().testTag(GRACE_TYPE_TAG)) {
            GraceType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = form.graceType == type,
                    onClick = { onGraceTypeChange(type) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = GraceType.entries.size),
                    modifier = Modifier.heightIn(min = Spacing.minTouchTarget),
                ) {
                    Text(stringResource(graceTypeLabel(type)))
                }
            }
        }
        Text(
            text = stringResource(graceTypeExplanation(form.graceType)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (form.graceType != GraceType.None) {
            HipoSimTextField(
                value = form.graceMonths,
                onValueChange = onGraceMonthsChange,
                label = stringResource(R.string.field_grace_months),
                leadingIcon = Icons.Filled.HourglassEmpty,
                suffix = stringResource(R.string.field_grace_months_suffix),
                supportingText =
                    stringResource(
                        R.string.field_grace_months_hint,
                        SimulationConfig.MIN_GRACE_MONTHS,
                        SimulationConfig.MAX_GRACE_MONTHS,
                    ),
                errorText = fieldError(state, SimulationField.GraceMonths),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().testTag(GRACE_MONTHS_TAG),
            )
        }
    }
}

private fun graceTypeLabel(type: GraceType): Int =
    when (type) {
        GraceType.None -> R.string.grace_none
        GraceType.Partial -> R.string.grace_partial
        GraceType.Total -> R.string.grace_total
    }

private fun graceTypeExplanation(type: GraceType): Int =
    when (type) {
        GraceType.None -> R.string.grace_none_body
        GraceType.Partial -> R.string.grace_partial_body
        GraceType.Total -> R.string.grace_total_body
    }

/** Test tags of the grace controls, used by the UI tests. */
const val GRACE_TYPE_TAG = "grace_type"
const val GRACE_MONTHS_TAG = "grace_months"

/** The client message when there is one, otherwise what the API said about the field. */
@Composable
private fun fieldError(
    state: SimulationFormUiState,
    field: SimulationField,
): String? = state.errors[field]?.let { formErrorMessage(it) } ?: state.serverErrors[field]

@Composable
private fun formErrorMessage(error: SimulationFormError): String =
    when (error) {
        SimulationFormError.PropertyPriceRequired -> stringResource(R.string.error_property_price_required)
        SimulationFormError.PropertyPriceOutOfRange ->
            stringResource(
                R.string.error_property_price_range,
                PeruFormat.soles(SimulationConfig.MIN_PROPERTY_PRICE),
                PeruFormat.soles(SimulationConfig.MAX_PROPERTY_PRICE),
            )
        SimulationFormError.DownPaymentRequired -> stringResource(R.string.error_down_payment_required)
        SimulationFormError.DownPaymentBelowMinimum ->
            stringResource(
                R.string.error_down_payment_minimum,
                PeruFormat.percent(SimulationConfig.MIN_DOWN_PAYMENT_RATE, fractionDigits = 0),
            )
        SimulationFormError.DownPaymentTooHigh -> stringResource(R.string.error_down_payment_too_high)
        SimulationFormError.TermRequired -> stringResource(R.string.error_term_required)
        SimulationFormError.TermOutOfRange ->
            stringResource(
                R.string.error_term_range,
                SimulationConfig.MIN_TERM_YEARS,
                SimulationConfig.MAX_TERM_YEARS,
            )
        SimulationFormError.GraceMonthsRequired -> stringResource(R.string.error_grace_months_required)
        SimulationFormError.GraceMonthsOutOfRange ->
            stringResource(
                R.string.error_grace_months_range,
                SimulationConfig.MIN_GRACE_MONTHS,
                SimulationConfig.MAX_GRACE_MONTHS,
            )
    }

@Composable
private fun generalErrorMessage(error: SimulationGeneralError): String =
    stringResource(
        when (error) {
            SimulationGeneralError.NoConnection -> R.string.error_no_connection
            SimulationGeneralError.ServiceUnavailable -> R.string.error_service_unavailable
            SimulationGeneralError.Rejected -> R.string.error_request_rejected
            SimulationGeneralError.Unexpected -> R.string.error_unexpected
        },
    )
