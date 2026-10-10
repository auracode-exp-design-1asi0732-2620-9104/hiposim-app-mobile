package com.auracode.hiposim.feature.auth.data

import com.auracode.hiposim.core.network.problemException
import com.auracode.hiposim.core.network.validationProblemException
import com.auracode.hiposim.feature.auth.domain.UserRole
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Stand-in for the account endpoints (`BuildConfig.USE_FAKE_API`).
 *
 * Accounts live in memory and are lost when the process dies. It answers with the shape of contract v0.1.0
 * and fails like the server: 400 with field errors, 409 for a repeated email, 401 for wrong credentials.
 * The token is not a real JWT and no real API would accept it.
 */
class FakeAuthService(
    private val latencyMillis: Long = 0,
) : AuthService {
    private data class Account(
        val user: UserDto,
        val password: String,
    )

    private val accounts = ConcurrentHashMap<String, Account>()

    override suspend fun register(request: RegisterRequestDto): AuthResponseDto {
        if (latencyMillis > 0) delay(latencyMillis)

        val errors = validate(request)
        if (errors.isNotEmpty()) throw validationProblemException(errors)

        val email = request.email.trim()
        val user =
            UserDto(
                id = UUID.randomUUID().toString(),
                fullName = request.fullName.trim(),
                email = email,
                role = UserRole.BUYER,
            )
        val existing = accounts.putIfAbsent(email.lowercase(), Account(user, request.password))
        if (existing != null) throw problemException(CONFLICT, "Email already registered.")
        return session(user)
    }

    override suspend fun login(request: LoginRequestDto): AuthResponseDto {
        if (latencyMillis > 0) delay(latencyMillis)

        val errors =
            buildMap {
                if (!request.email.looksLikeAnEmail()) put("email", listOf(INVALID_EMAIL))
                if (request.password.isEmpty()) put("password", listOf("Password is required."))
            }
        if (errors.isNotEmpty()) throw validationProblemException(errors)

        val account = accounts[request.email.trim().lowercase()]
        if (account == null || account.password != request.password) {
            throw problemException(UNAUTHORIZED, "Invalid credentials.")
        }
        return session(account.user)
    }

    private fun validate(request: RegisterRequestDto): Map<String, List<String>> =
        buildMap {
            if (request.fullName.isBlank() || request.fullName.trim().length > MAX_NAME_LENGTH) {
                put("fullName", listOf("Full name is required and cannot exceed 150 characters."))
            }
            if (!request.email.looksLikeAnEmail()) put("email", listOf(INVALID_EMAIL))
            if (request.phone.isBlank() || request.phone.trim().length > MAX_PHONE_LENGTH) {
                put("phone", listOf("Phone is required and cannot exceed 20 characters."))
            }
            if (request.password.length !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH) {
                put("password", listOf("Password must have between 8 and 128 characters."))
            }
            if (!request.acceptedTerms) put("acceptedTerms", listOf("Terms and Conditions must be accepted."))
            if (!request.acceptedDataProcessing) {
                put("acceptedDataProcessing", listOf("Personal data processing must be accepted."))
            }
        }

    private fun session(user: UserDto) =
        AuthResponseDto(
            accessToken = "fake.${user.id}.token",
            tokenType = "Bearer",
            expiresIn = TOKEN_LIFETIME_SECONDS,
            user = user,
        )

    private fun String.looksLikeAnEmail(): Boolean = EMAIL_REGEX.matches(trim())

    companion object {
        /** Simulated network delay of the app, so the loading state can be seen. */
        const val DEFAULT_LATENCY_MILLIS = 400L

        private const val INVALID_EMAIL = "A valid email address is required."
        private const val MAX_NAME_LENGTH = 150
        private const val MAX_PHONE_LENGTH = 20
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 128
        private const val TOKEN_LIFETIME_SECONDS = 28_800
        private const val UNAUTHORIZED = 401
        private const val CONFLICT = 409
        private val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}
