package com.auracode.hiposim.feature.simulation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.simulation.domain.SimulateLoanUseCase
import com.auracode.hiposim.feature.simulation.domain.SimulationFailure
import com.auracode.hiposim.feature.simulation.domain.SimulationForm
import com.auracode.hiposim.feature.simulation.domain.SimulationFormError
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import com.auracode.hiposim.feature.simulation.domain.ValidateSimulationFormUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.RoundingMode
import javax.inject.Inject

@HiltViewModel
class SimulationFormViewModel
    @Inject
    constructor(
        private val validateForm: ValidateSimulationFormUseCase,
        private val simulateLoan: SimulateLoanUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SimulationFormUiState().withPercent())
        val uiState: StateFlow<SimulationFormUiState> = _uiState.asStateFlow()

        fun onPropertyPriceChange(value: String) = updateForm { it.copy(propertyPrice = value.digitsOnly()) }

        fun onDownPaymentChange(value: String) = updateForm { it.copy(downPayment = value.digitsOnly()) }

        fun onTermChange(years: Int) = updateForm { it.copy(termYears = years) }

        /** Validates the form and, if it is valid, asks the API for the simulation. Works without an account. */
        fun onSimulate() {
            val state = _uiState.value
            if (state.isSubmitting) return

            val validation = validateForm(state.form)
            _uiState.update { it.copy(errors = validation.errors, submitError = null) }
            val request = validation.request ?: return

            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                when (val result = simulateLoan(request)) {
                    is SimulationResult.Success ->
                        _uiState.update { it.copy(isSubmitting = false, isSimulated = true) }

                    is SimulationResult.Failure -> _uiState.update { it.withFailure(result.reason) }
                }
            }
        }

        /** The results were opened, so coming back to this screen must not open them again. */
        fun onNavigated() {
            _uiState.update { it.copy(isSimulated = false) }
        }

        private fun updateForm(transform: (SimulationForm) -> SimulationForm) {
            _uiState.update { state ->
                val form = transform(state.form)
                val errors = if (state.errors.isEmpty()) state.errors else validateForm(form).errors
                state.copy(form = form, errors = errors, submitError = null).withPercent()
            }
        }

        private fun SimulationFormUiState.withFailure(reason: SimulationFailure): SimulationFormUiState =
            when (reason) {
                is SimulationFailure.Rejected ->
                    if (reason.fields.isEmpty()) {
                        copy(isSubmitting = false, submitError = SubmitError.ServerError)
                    } else {
                        copy(
                            isSubmitting = false,
                            errors = reason.fields.associateWith { SimulationFormError.RejectedByServer },
                        )
                    }

                SimulationFailure.NoConnection -> copy(isSubmitting = false, submitError = SubmitError.NoConnection)

                SimulationFailure.ServerError -> copy(isSubmitting = false, submitError = SubmitError.ServerError)
            }

        private fun SimulationFormUiState.withPercent(): SimulationFormUiState =
            copy(downPaymentPercent = form.downPaymentPercent())

        private fun SimulationForm.downPaymentPercent(): String? {
            val price = propertyPrice.toBigDecimalOrNull()
            val down = downPayment.toBigDecimalOrNull()
            if (price == null || down == null || price.signum() <= 0) return null
            return PeruFormat.percent(down.divide(price, RATE_SCALE, RoundingMode.HALF_UP), fractionDigits = 1)
        }

        private fun String.digitsOnly(): String = filter { it.isDigit() }.take(MAX_AMOUNT_DIGITS)

        private companion object {
            /** Up to 999,999,999 soles: enough for any home, and it keeps the amount well inside a Double. */
            const val MAX_AMOUNT_DIGITS = 9
            const val RATE_SCALE = 6
        }
    }
