package com.auracode.hiposim.feature.auth.domain

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.testing.fakeBackedAuthRepository
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthUseCasesTest {
    private val tokenStore = TokenStore()
    private val repository = fakeBackedAuthRepository(tokenStore)
    private val observeSession = ObserveSessionUseCase(repository)

    @Test
    fun register_signsIn_thenSignOutAndSignInAgain() =
        runTest {
            assertEquals(SessionState.Guest, observeSession().value)

            val registered = RegisterUserUseCase(repository)(validRegistrationForm)
            val user = (registered as RegisterResult.Success).user
            assertEquals(SessionState.Authenticated(user), observeSession().value)
            assertNotNull(tokenStore.token)

            SignOutUseCase(repository)()
            assertEquals(SessionState.Guest, observeSession().value)
            assertNull(tokenStore.token)

            val signedIn =
                SignInUseCase(repository)(LoginForm(validRegistrationForm.email, validRegistrationForm.password))
            assertEquals(SignInResult.Success(user), signedIn)
            assertTrue(observeSession().value is SessionState.Authenticated)
        }

    @Test
    fun signIn_withTheWrongPassword_isInvalidCredentials() =
        runTest {
            RegisterUserUseCase(repository)(validRegistrationForm)
            SignOutUseCase(repository)()

            val result = SignInUseCase(repository)(LoginForm(validRegistrationForm.email, "another-password"))

            assertEquals(SignInResult.InvalidCredentials, result)
            assertEquals(SessionState.Guest, observeSession().value)
        }
}
