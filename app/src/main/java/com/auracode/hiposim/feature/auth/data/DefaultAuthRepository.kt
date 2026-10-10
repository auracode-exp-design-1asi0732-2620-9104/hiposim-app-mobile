package com.auracode.hiposim.feature.auth.data

import com.auracode.hiposim.core.network.ApiError
import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.core.network.safeApiCall
import com.auracode.hiposim.feature.auth.domain.AuthFailure
import com.auracode.hiposim.feature.auth.domain.AuthRepository
import com.auracode.hiposim.feature.auth.domain.AuthenticatedUser
import com.auracode.hiposim.feature.auth.domain.RegisterResult
import com.auracode.hiposim.feature.auth.domain.RegistrationField
import com.auracode.hiposim.feature.auth.domain.RegistrationForm
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.auth.domain.SignInResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only [AuthRepository]. Real or in-memory accounts are decided by the [AuthService] that Hilt provides.
 *
 * The session lives in memory: the user and, in [TokenStore], the JWT that the auth interceptor attaches to
 * every later request. Both are lost when the process dies, so the buyer signs in again.
 */
@Singleton
class DefaultAuthRepository
    @Inject
    constructor(
        private val service: AuthService,
        private val tokenStore: TokenStore,
    ) : AuthRepository {
        private val _session = MutableStateFlow<SessionState>(SessionState.Guest)
        override val session: StateFlow<SessionState> = _session.asStateFlow()

        override suspend fun register(form: RegistrationForm): RegisterResult =
            when (val response = safeApiCall { service.register(form.toRequestDto()) }) {
                is ApiResult.Success -> RegisterResult.Success(startSession(response.data))
                is ApiResult.Failure ->
                    when (val error = response.error) {
                        is ApiError.Conflict -> RegisterResult.EmailAlreadyRegistered
                        is ApiError.Validation -> RegisterResult.Rejected(error.toFieldMessages())
                        else -> RegisterResult.Failed(error.toAuthFailure())
                    }
            }

        override suspend fun signIn(
            email: String,
            password: String,
        ): SignInResult =
            when (val response = safeApiCall { service.login(LoginRequestDto(email.trim(), password)) }) {
                is ApiResult.Success ->
                    if (response.data.user
                            .toDomain()
                            .isBuyer
                    ) {
                        SignInResult.Success(startSession(response.data))
                    } else {
                        SignInResult.NotABuyer
                    }

                is ApiResult.Failure ->
                    when (val error = response.error) {
                        // A 400 here is a malformed email or an empty password: it is answered like wrong
                        // credentials so the message never points at one of the two values.
                        is ApiError.Unauthorized, is ApiError.Validation -> SignInResult.InvalidCredentials
                        else -> SignInResult.Failed(error.toAuthFailure())
                    }
            }

        override suspend fun signOut() {
            tokenStore.clear()
            _session.value = SessionState.Guest
        }

        private fun startSession(response: AuthResponseDto): AuthenticatedUser {
            val user = response.user.toDomain()
            tokenStore.token = response.accessToken
            _session.value = SessionState.Authenticated(user)
            return user
        }

        private fun RegistrationForm.toRequestDto() =
            RegisterRequestDto(
                fullName = fullName.trim(),
                email = email.trim(),
                phone = phone.filterNot { it.isWhitespace() || it == '-' },
                password = password,
                acceptedTerms = termsAccepted,
                acceptedDataProcessing = consentAccepted,
            )

        private fun UserDto.toDomain() = AuthenticatedUser(id = id, fullName = fullName, email = email, role = role)

        /** First message of each form field the API named. */
        private fun ApiError.Validation.toFieldMessages(): Map<RegistrationField, String> =
            buildMap {
                fieldErrors.forEach { (property, messages) ->
                    val field = RegistrationField.fromRequestProperty(property)
                    val message = messages.firstOrNull()
                    if (field != null && message != null) put(field, message)
                }
            }

        private fun ApiError.toAuthFailure(): AuthFailure =
            when (this) {
                ApiError.NoConnection -> AuthFailure.NoConnection
                is ApiError.Server -> AuthFailure.ServiceUnavailable
                else -> AuthFailure.Unexpected
            }
    }
