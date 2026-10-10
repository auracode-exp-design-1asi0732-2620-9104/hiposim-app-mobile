package com.auracode.hiposim.testing

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.AuthResponseDto
import com.auracode.hiposim.feature.auth.data.AuthService
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.data.FakeAuthService
import com.auracode.hiposim.feature.auth.data.LoginRequestDto
import com.auracode.hiposim.feature.auth.data.RegisterRequestDto
import com.auracode.hiposim.feature.auth.domain.RegistrationForm

/** A registration the API accepts: every field valid and both acceptances given. */
val validRegistrationForm =
    RegistrationForm(
        fullName = "Carlos Mendoza",
        email = "carlos@ejemplo.com",
        phone = "987 654 321",
        password = "secret123",
        termsAccepted = true,
        consentAccepted = true,
    )

/** A repository backed by the in-memory accounts, without latency. */
fun fakeBackedAuthRepository(tokenStore: TokenStore = TokenStore()) =
    DefaultAuthRepository(FakeAuthService(), tokenStore)

/** A service whose answers each test decides, to reach the failure paths. It behaves like the fake by default. */
class StubAuthService : AuthService {
    private val fake = FakeAuthService()

    var onRegister: suspend (RegisterRequestDto) -> AuthResponseDto = { fake.register(it) }
    var onLogin: suspend (LoginRequestDto) -> AuthResponseDto = { fake.login(it) }
    val registrations = mutableListOf<RegisterRequestDto>()
    val logins = mutableListOf<LoginRequestDto>()

    override suspend fun register(request: RegisterRequestDto): AuthResponseDto {
        registrations += request
        return onRegister(request)
    }

    override suspend fun login(request: LoginRequestDto): AuthResponseDto {
        logins += request
        return onLogin(request)
    }
}
