package com.auracode.hiposim.feature.auth.domain

data class AuthenticatedUser(
    val id: String,
    val fullName: String,
    val email: String,
    val phone: String,
)

/** Whether someone is signed in. Guests can simulate, but not reach real estate agencies. */
sealed interface SessionState {
    data object Guest : SessionState

    data class Authenticated(
        val user: AuthenticatedUser,
    ) : SessionState
}

sealed interface RegisterResult {
    data class Success(
        val user: AuthenticatedUser,
    ) : RegisterResult

    data object EmailAlreadyRegistered : RegisterResult
}
