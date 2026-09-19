package com.auracode.hiposim.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.auth.domain.RegisterResult
import com.auracode.hiposim.feature.auth.domain.RegisterUserUseCase
import com.auracode.hiposim.feature.auth.domain.RegistrationError
import com.auracode.hiposim.feature.auth.domain.RegistrationField
import com.auracode.hiposim.feature.auth.domain.RegistrationForm
import com.auracode.hiposim.feature.auth.domain.ValidateRegistrationFormUseCase
import com.auracode.hiposim.feature.simulation.domain.GetLoanSimulationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel
    @Inject
    constructor(
        private val getLoanSimulation: GetLoanSimulationUseCase,
        private val validateForm: ValidateRegistrationFormUseCase,
        private val registerUser: RegisterUserUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RegisterUiState())
        val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val simulation = getLoanSimulation()
                val quote =
                    QuoteSummaryUi(
                        monthlyPayment = PeruFormat.soles(simulation.monthlyPayment),
                        tcea = PeruFormat.percent(simulation.tcea),
                        propertyPrice = PeruFormat.soles(simulation.propertyPrice),
                    )
                _uiState.update { it.copy(quote = quote) }
            }
        }

        fun onFullNameChange(value: String) = updateForm { it.copy(fullName = value) }

        fun onEmailChange(value: String) = updateForm { it.copy(email = value) }

        fun onPhoneChange(value: String) =
            updateForm {
                it.copy(phone = value.filter { char -> char.isDigit() || char == ' ' }.take(MAX_PHONE_INPUT_LENGTH))
            }

        fun onPasswordChange(value: String) = updateForm { it.copy(password = value) }

        fun onConsentChange(accepted: Boolean) = updateForm { it.copy(consentAccepted = accepted) }

        fun onTogglePasswordVisibility() {
            _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
        }

        /** Validates the form and, if it is valid, creates the account. */
        fun onSubmit() {
            val state = _uiState.value
            if (state.isSubmitting) return

            val validation = validateForm(state.form)
            _uiState.update { it.copy(errors = validation.errors) }
            if (!validation.isValid) return

            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                when (registerUser(state.form)) {
                    is RegisterResult.Success ->
                        _uiState.update { it.copy(isSubmitting = false, isRegistered = true) }

                    RegisterResult.EmailAlreadyRegistered ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                errors = it.errors + EMAIL_TAKEN_ERROR,
                            )
                        }
                }
            }
        }

        private fun updateForm(transform: (RegistrationForm) -> RegistrationForm) {
            _uiState.update { state ->
                val form = transform(state.form)
                val errors = if (state.errors.isEmpty()) state.errors else revalidate(state, form)
                state.copy(form = form, errors = errors)
            }
        }

        /** Follows the edits, but keeps "email already registered" until the email itself changes. */
        private fun revalidate(
            state: RegisterUiState,
            form: RegistrationForm,
        ): Map<RegistrationField, RegistrationError> {
            val errors = validateForm(form).errors
            val emailTaken = state.errors[RegistrationField.Email] == RegistrationError.EmailAlreadyRegistered
            val keepEmailTaken =
                emailTaken && form.email == state.form.email && !errors.containsKey(RegistrationField.Email)
            return if (keepEmailTaken) errors + EMAIL_TAKEN_ERROR else errors
        }

        private companion object {
            /** Nine digits plus the two separating spaces of `987 654 321`. */
            const val MAX_PHONE_INPUT_LENGTH = 11

            val EMAIL_TAKEN_ERROR = RegistrationField.Email to RegistrationError.EmailAlreadyRegistered
        }
    }
