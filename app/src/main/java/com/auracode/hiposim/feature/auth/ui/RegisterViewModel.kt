package com.auracode.hiposim.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.auth.domain.AuthFailure
import com.auracode.hiposim.feature.auth.domain.RegisterResult
import com.auracode.hiposim.feature.auth.domain.RegisterUserUseCase
import com.auracode.hiposim.feature.auth.domain.RegistrationError
import com.auracode.hiposim.feature.auth.domain.RegistrationField
import com.auracode.hiposim.feature.auth.domain.RegistrationForm
import com.auracode.hiposim.feature.auth.domain.ValidateRegistrationFormUseCase
import com.auracode.hiposim.feature.simulation.domain.ObserveCurrentSimulationUseCase
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
        private val observeCurrentSimulation: ObserveCurrentSimulationUseCase,
        private val validateForm: ValidateRegistrationFormUseCase,
        private val registerUser: RegisterUserUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(RegisterUiState())
        val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                observeCurrentSimulation().collect { simulation ->
                    val quote =
                        simulation?.let {
                            QuoteSummaryUi(
                                monthlyPayment = PeruFormat.soles(it.monthlyPayment),
                                tcea = PeruFormat.percent(it.tcea),
                                propertyPrice = PeruFormat.soles(it.propertyPrice),
                            )
                        }
                    _uiState.update { it.copy(quote = quote) }
                }
            }
        }

        fun onFullNameChange(value: String) = updateForm(RegistrationField.FullName) { it.copy(fullName = value) }

        fun onEmailChange(value: String) = updateForm(RegistrationField.Email) { it.copy(email = value) }

        fun onPhoneChange(value: String) =
            updateForm(RegistrationField.Phone) {
                it.copy(phone = value.filter { char -> char.isDigit() }.take(MAX_PHONE_DIGITS))
            }

        fun onPasswordChange(value: String) =
            updateForm(RegistrationField.Password) { it.copy(password = value.take(MAX_PASSWORD_LENGTH)) }

        fun onTermsChange(accepted: Boolean) = updateForm(RegistrationField.Terms) { it.copy(termsAccepted = accepted) }

        fun onConsentChange(accepted: Boolean) =
            updateForm(RegistrationField.Consent) { it.copy(consentAccepted = accepted) }

        fun onTogglePasswordVisibility() {
            _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
        }

        /** Validates the form and, if it is valid, creates the account. */
        fun onSubmit() {
            val state = _uiState.value
            if (state.isSubmitting) return

            val validation = validateForm(state.form)
            _uiState.update { it.copy(errors = validation.errors, serverErrors = emptyMap(), failure = null) }
            if (!validation.isValid) return

            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                val result = registerUser(state.form)
                _uiState.update { it.copy(isSubmitting = false).with(result) }
            }
        }

        private fun RegisterUiState.with(result: RegisterResult): RegisterUiState =
            when (result) {
                is RegisterResult.Success -> copy(isRegistered = true)
                RegisterResult.EmailAlreadyRegistered -> copy(errors = errors + EMAIL_TAKEN_ERROR)
                is RegisterResult.Rejected ->
                    if (result.fieldMessages.isEmpty()) {
                        // The API rejected something the form does not show: there is no field to point at.
                        copy(failure = AuthFailure.Unexpected)
                    } else {
                        copy(serverErrors = result.fieldMessages)
                    }
                is RegisterResult.Failed -> copy(failure = result.reason)
            }

        private fun updateForm(
            edited: RegistrationField,
            transform: (RegistrationForm) -> RegistrationForm,
        ) {
            _uiState.update { state ->
                val form = transform(state.form)
                val errors = if (state.errors.isEmpty()) state.errors else revalidate(state, form)
                state.copy(form = form, errors = errors, serverErrors = state.serverErrors - edited, failure = null)
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
            /** A Peruvian mobile number has 9 digits: the field accepts nothing else. */
            const val MAX_PHONE_DIGITS = 9

            /** Upper limit of the API for a password. */
            const val MAX_PASSWORD_LENGTH = 128

            val EMAIL_TAKEN_ERROR = RegistrationField.Email to RegistrationError.EmailAlreadyRegistered
        }
    }
