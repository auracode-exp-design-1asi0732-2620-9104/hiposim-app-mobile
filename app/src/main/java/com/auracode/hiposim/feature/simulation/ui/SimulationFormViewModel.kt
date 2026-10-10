package com.auracode.hiposim.feature.simulation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.simulation.domain.SimulateLoanUseCase
import com.auracode.hiposim.feature.simulation.domain.SimulationConfig
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationField
import com.auracode.hiposim.feature.simulation.domain.SimulationForm
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import com.auracode.hiposim.feature.simulation.domain.ValidateSimulationFormUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.MathContext
import javax.inject.Inject

@HiltViewModel
class SimulationFormViewModel
    @Inject
    constructor(
        private val validateForm: ValidateSimulationFormUseCase,
        private val simulateLoan: SimulateLoanUseCase,
    ) : ViewModel() {
        private val _uiState =
            MutableStateFlow(
                SimulationFormUiState(
                    form = INITIAL_FORM,
                    downPaymentPercent = downPaymentPercent(INITIAL_FORM),
                    annualRate = PeruFormat.percent(SimulationConfig.ANNUAL_EFFECTIVE_RATE),
                ),
            )
        val uiState: StateFlow<SimulationFormUiState> = _uiState.asStateFlow()

        fun onPropertyPriceChange(value: String) =
            updateForm(SimulationField.PropertyPrice) { it.copy(propertyPrice = value.digits(MAX_AMOUNT_DIGITS)) }

        fun onDownPaymentChange(value: String) =
            updateForm(SimulationField.DownPayment) { it.copy(downPayment = value.digits(MAX_AMOUNT_DIGITS)) }

        fun onTermYearsChange(value: String) =
            updateForm(SimulationField.TermYears) { it.copy(termYears = value.digits(MAX_TERM_DIGITS)) }

        fun onBonusChange(apply: Boolean) = updateForm(edited = null) { it.copy(applyGoodPayerBonus = apply) }

        /** Validates the form and, if it is valid, asks for the simulation. */
        fun onSubmit() {
            val state = _uiState.value
            if (state.isSubmitting) return

            val validation = validateForm(state.form)
            _uiState.update { it.copy(errors = validation.errors, serverErrors = emptyMap(), generalError = null) }
            val params = validation.params ?: return

            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                when (val result = simulateLoan(params)) {
                    is SimulationResult.Success ->
                        _uiState.update { it.copy(isSubmitting = false, navigateToResults = true) }

                    is SimulationResult.Failure -> showFailure(result.reason)
                }
            }
        }

        fun onResultsNavigated() {
            _uiState.update { it.copy(navigateToResults = false) }
        }

        private fun showFailure(reason: SimulationFailure) {
            // The API text of a field is shown as it arrives, next to that field.
            val serverErrors = (reason as? SimulationFailure.Rejected)?.fieldMessages.orEmpty()
            val generalError =
                when (reason) {
                    // A 400 about something the form does not show (the rate, the bonus) has no field to sit next to.
                    is SimulationFailure.Rejected ->
                        if (serverErrors.isEmpty()) SimulationGeneralError.Rejected else null
                    SimulationFailure.NoConnection -> SimulationGeneralError.NoConnection
                    SimulationFailure.ServiceUnavailable -> SimulationGeneralError.ServiceUnavailable
                    SimulationFailure.Unexpected -> SimulationGeneralError.Unexpected
                }
            _uiState.update {
                it.copy(isSubmitting = false, serverErrors = serverErrors, generalError = generalError)
            }
        }

        private fun updateForm(
            edited: SimulationField?,
            transform: (SimulationForm) -> SimulationForm,
        ) {
            _uiState.update { state ->
                val form = transform(state.form)
                state.copy(
                    form = form,
                    errors = if (state.errors.isEmpty()) state.errors else validateForm(form).errors,
                    serverErrors = if (edited == null) state.serverErrors else state.serverErrors - edited,
                    generalError = null,
                    downPaymentPercent = downPaymentPercent(form),
                )
            }
        }

        private fun String.digits(maxLength: Int): String = filter { it.isDigit() }.take(maxLength)

        private companion object {
            /** Up to S/ 999,999,999: far above the allowed range, short enough to never overflow the field. */
            const val MAX_AMOUNT_DIGITS = 9
            const val MAX_TERM_DIGITS = 2

            /** Starting values of the Landing Page simulator: S/ 280,000, 15% down payment, 20 years. */
            val INITIAL_FORM = SimulationForm(propertyPrice = "280000", downPayment = "42000", termYears = "20")

            fun downPaymentPercent(form: SimulationForm): String? {
                val price = form.propertyPrice.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return null
                val downPayment = form.downPayment.toBigDecimalOrNull() ?: return null
                return PeruFormat.percent(downPayment.divide(price, MathContext.DECIMAL64), fractionDigits = 1)
            }
        }
    }
