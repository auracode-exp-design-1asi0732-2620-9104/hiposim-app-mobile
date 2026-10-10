package com.auracode.hiposim.feature.auth.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.feature.auth.domain.LoginField
import com.auracode.hiposim.feature.auth.domain.LoginForm
import com.auracode.hiposim.feature.auth.domain.LoginFormError

@Immutable
data class LoginUiState(
    val form: LoginForm = LoginForm(),
    val isPasswordVisible: Boolean = false,
    /** Errors are hidden until the user tries to submit, then they follow the edits. */
    val errors: Map<LoginField, LoginFormError> = emptyMap(),
    /** Why the last attempt did not start a session. Cleared on the next edit. */
    val failure: LoginFailure? = null,
    val isSubmitting: Boolean = false,
    /** The session started, so the screen should close. */
    val isSignedIn: Boolean = false,
)

enum class LoginFailure {
    /** Wrong email or password. The message never says which one. */
    InvalidCredentials,
    NotABuyer,
    NoConnection,
    ServiceUnavailable,
    Unexpected,
}
