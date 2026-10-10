package com.auracode.hiposim.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.feature.auth.domain.AuthFailure
import com.auracode.hiposim.feature.auth.domain.LoginForm
import com.auracode.hiposim.feature.auth.domain.SignInResult
import com.auracode.hiposim.feature.auth.domain.SignInUseCase
import com.auracode.hiposim.feature.auth.domain.ValidateLoginFormUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel
    @Inject
    constructor(
        private val validateForm: ValidateLoginFormUseCase,
        private val signIn: SignInUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(LoginUiState())
        val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

        fun onEmailChange(value: String) = updateForm { it.copy(email = value) }

        fun onPasswordChange(value: String) = updateForm { it.copy(password = value) }

        fun onTogglePasswordVisibility() {
            _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
        }

        /** Validates the form and, if it is usable, asks the API for a session. */
        fun onSubmit() {
            val state = _uiState.value
            if (state.isSubmitting) return

            val errors = validateForm(state.form)
            _uiState.update { it.copy(errors = errors, failure = null) }
            if (errors.isNotEmpty()) return

            _uiState.update { it.copy(isSubmitting = true) }
            viewModelScope.launch {
                val result = signIn(state.form)
                _uiState.update { it.copy(isSubmitting = false).with(result) }
            }
        }

        private fun LoginUiState.with(result: SignInResult): LoginUiState =
            when (result) {
                is SignInResult.Success -> copy(isSignedIn = true)
                // The password is cleared so a retry starts clean; the email stays to be corrected.
                SignInResult.InvalidCredentials ->
                    copy(failure = LoginFailure.InvalidCredentials, form = form.copy(password = ""))
                SignInResult.NotABuyer -> copy(failure = LoginFailure.NotABuyer, form = form.copy(password = ""))
                is SignInResult.Failed ->
                    copy(
                        failure =
                            when (result.reason) {
                                AuthFailure.NoConnection -> LoginFailure.NoConnection
                                AuthFailure.ServiceUnavailable -> LoginFailure.ServiceUnavailable
                                AuthFailure.Unexpected -> LoginFailure.Unexpected
                            },
                    )
            }

        private fun updateForm(transform: (LoginForm) -> LoginForm) {
            _uiState.update { state ->
                val form = transform(state.form)
                val errors = if (state.errors.isEmpty()) state.errors else validateForm(form)
                state.copy(form = form, errors = errors, failure = null)
            }
        }
    }
