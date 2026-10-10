package com.auracode.hiposim.feature.simulation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auracode.hiposim.R
import com.auracode.hiposim.core.designsystem.component.HipoSimTextField
import com.auracode.hiposim.core.designsystem.component.PrimaryButton
import com.auracode.hiposim.core.designsystem.component.ThousandsVisualTransformation
import com.auracode.hiposim.core.designsystem.theme.Spacing
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationFormError
import com.auracode.hiposim.feature.simulation.domain.ValidateSimulationFormUseCase
import kotlin.math.roundToInt

private const val MIN_DOWN_PAYMENT_PERCENT = ValidateSimulationFormUseCase.MIN_DOWN_PAYMENT_PERCENT

/** First screen of the app: the buyer enters the home price, the down payment and the term. No account needed. */
@Composable
fun SimulationFormScreen(
    onSimulated: () -> Unit,
    viewModel: SimulationFormViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSimulated) {
        if (state.isSimulated) {
            viewModel.onNavigated()
            onSimulated()
        }
    }

    SimulationFormContent(
        state = state,
        onPropertyPriceChange = viewModel::onPropertyPriceChange,
        onDownPaymentChange = viewModel::onDownPaymentChange,
        onTermChange = viewModel::onTermChange,
        onSimulate = viewModel::onSimulate,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimulationFormContent(
    state: SimulationFormUiState,
    onPropertyPriceChange: (String) -> Unit,
    onDownPaymentChange: (String) -> Unit,
    onTermChange: (Int) -> Unit,
    onSimulate: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.simulation_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
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
                    text = stringResource(R.string.simulation_headline),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.simulation_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                HipoSimTextField(
                    value = state.form.propertyPrice,
                    onValueChange = onPropertyPriceChange,
                    label = stringResource(R.string.field_property_price),
                    placeholder = stringResource(R.string.field_property_price_placeholder),
                    prefix = stringResource(R.string.currency_prefix),
                    leadingIcon = Icons.Filled.Home,
                    errorText = state.errors[SimulationField.PropertyPrice]?.let { errorMessage(it) },
                    visualTransformation = ThousandsVisualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                HipoSimTextField(
                    value = state.form.downPayment,
                    onValueChange = onDownPaymentChange,
                    label = stringResource(R.string.field_down_payment),
                    prefix = stringResource(R.string.currency_prefix),
                    leadingIcon = Icons.Filled.Savings,
                    errorText = state.errors[SimulationField.DownPayment]?.let { errorMessage(it) },
                    helperText =
                        state.downPaymentPercent?.let {
                            stringResource(R.string.down_payment_hint, it, MIN_DOWN_PAYMENT_PERCENT)
                        },
                    visualTransformation = ThousandsVisualTransformation,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                TermSelector(years = state.form.termYears, onYearsChange = onTermChange)
            }

            state.submitError?.let { error ->
                Text(
                    text = submitErrorMessage(error),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            val submitLabel = if (state.isSubmitting) R.string.simulation_submitting else R.string.simulation_submit
            PrimaryButton(
                text = stringResource(submitLabel),
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                onClick = onSimulate,
                enabled = !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TermSelector(
    years: Int,
    onYearsChange: (Int) -> Unit,
) {
    val termText = stringResource(R.string.term_value, years, years * MONTHS_PER_YEAR)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.term_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = termText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = years.toFloat(),
            onValueChange = { onYearsChange(it.roundToInt()) },
            valueRange = MIN_TERM_YEARS.toFloat()..MAX_TERM_YEARS.toFloat(),
            steps = TERM_STEPS,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { stateDescription = termText },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = stringResource(R.string.years_short, MIN_TERM_YEARS),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.years_short, MAX_TERM_YEARS),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun submitErrorMessage(error: SubmitError): String =
    stringResource(
        when (error) {
            SubmitError.NoConnection -> R.string.error_no_connection
            SubmitError.ServerError -> R.string.error_server
        },
    )

@Composable
private fun errorMessage(error: SimulationFormError): String =
    when (error) {
        SimulationFormError.PropertyPriceRequired -> stringResource(R.string.error_property_price_required)
        SimulationFormError.DownPaymentRequired -> stringResource(R.string.error_down_payment_required)
        SimulationFormError.DownPaymentTooLow ->
            stringResource(R.string.error_down_payment_too_low, MIN_DOWN_PAYMENT_PERCENT)
        SimulationFormError.DownPaymentTooHigh -> stringResource(R.string.error_down_payment_too_high)
        SimulationFormError.TermOutOfRange -> stringResource(R.string.error_term_out_of_range)
        SimulationFormError.RejectedByServer -> stringResource(R.string.error_rejected_by_server)
    }

private const val MONTHS_PER_YEAR = 12
private const val MIN_TERM_YEARS = ValidateSimulationFormUseCase.MIN_TERM_YEARS
private const val MAX_TERM_YEARS = ValidateSimulationFormUseCase.MAX_TERM_YEARS

/** The slider moves in 5 year steps (5, 10, ... 30), like the Landing Page simulator: 4 stops between the ends. */
private const val TERM_STEPS = 4
