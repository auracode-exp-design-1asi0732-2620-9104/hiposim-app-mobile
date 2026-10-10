package com.auracode.hiposim.feature.auth.domain

import javax.inject.Inject

data class LoginForm(
    val email: String = "",
    val password: String = "",
)

enum class LoginField { Email, Password }

enum class LoginFormError { EmailInvalid, PasswordRequired }

/** Only checks that both values were typed with a usable shape. Whether they are right is up to the API. */
class ValidateLoginFormUseCase
    @Inject
    constructor() {
        operator fun invoke(form: LoginForm): Map<LoginField, LoginFormError> =
            buildMap {
                if (!EmailFormat.isValid(form.email)) put(LoginField.Email, LoginFormError.EmailInvalid)
                if (form.password.isEmpty()) put(LoginField.Password, LoginFormError.PasswordRequired)
            }
    }
