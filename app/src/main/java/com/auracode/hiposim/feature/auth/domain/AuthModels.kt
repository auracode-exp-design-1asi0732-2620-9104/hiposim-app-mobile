package com.auracode.hiposim.feature.auth.domain

/** The signed in account, as the API describes it (`user` of the auth response). */
data class AuthenticatedUser(
    val id: String,
    val fullName: String,
    val email: String,
    val role: String = UserRole.BUYER,
) {
    val isBuyer: Boolean get() = role == UserRole.BUYER
}

/** Roles of the API. This app is the buyer's product: advisors use the web app. */
object UserRole {
    const val BUYER = "buyer"
    const val ADVISOR = "advisor"
}

/** Whether someone is signed in. Guests can simulate, but not reach real estate agencies. */
sealed interface SessionState {
    data object Guest : SessionState

    data class Authenticated(
        val user: AuthenticatedUser,
    ) : SessionState
}

/** Why an account request could not be answered, when it is not about what the buyer typed. */
enum class AuthFailure { NoConnection, ServiceUnavailable, Unexpected }

sealed interface RegisterResult {
    data class Success(
        val user: AuthenticatedUser,
    ) : RegisterResult

    /** The API answered 409: there is already an account with that email. */
    data object EmailAlreadyRegistered : RegisterResult

    /** The API rejected the form (400). [fieldMessages] has its message for each field it named. */
    data class Rejected(
        val fieldMessages: Map<RegistrationField, String>,
    ) : RegisterResult

    data class Failed(
        val reason: AuthFailure,
    ) : RegisterResult
}

sealed interface SignInResult {
    data class Success(
        val user: AuthenticatedUser,
    ) : SignInResult

    /**
     * The API refused the credentials. It never says which of the two is wrong, and neither does the app.
     */
    data object InvalidCredentials : SignInResult

    /** The credentials are right but the account is not a buyer account, so no session is started. */
    data object NotABuyer : SignInResult

    data class Failed(
        val reason: AuthFailure,
    ) : SignInResult
}
