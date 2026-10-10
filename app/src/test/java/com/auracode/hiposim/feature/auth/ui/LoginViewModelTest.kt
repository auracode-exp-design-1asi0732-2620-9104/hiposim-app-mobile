package com.auracode.hiposim.feature.auth.ui

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.domain.LoginField
import com.auracode.hiposim.feature.auth.domain.LoginFormError
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.auth.domain.SignInUseCase
import com.auracode.hiposim.feature.auth.domain.ValidateLoginFormUseCase
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.StubAuthService
import com.auracode.hiposim.testing.contractResource
import com.auracode.hiposim.testing.httpException
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class LoginViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var service: StubAuthService
    private lateinit var authRepository: DefaultAuthRepository
    private lateinit var tokenStore: TokenStore
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        service = StubAuthService()
        tokenStore = TokenStore()
        authRepository = DefaultAuthRepository(service, tokenStore)
        // An existing account, with no session.
        runBlocking {
            authRepository.register(validRegistrationForm)
            authRepository.signOut()
        }
        viewModel = LoginViewModel(ValidateLoginFormUseCase(), SignInUseCase(authRepository))
    }

    private val state get() = viewModel.uiState.value

    private fun type(
        email: String = validRegistrationForm.email,
        password: String = validRegistrationForm.password,
    ) {
        viewModel.onEmailChange(email)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun validCredentials_startTheSession() {
        type()

        viewModel.onSubmit()

        assertTrue(state.isSignedIn)
        assertFalse(state.isSubmitting)
        assertNull(state.failure)
        assertTrue(authRepository.session.value is SessionState.Authenticated)
        assertTrue(!tokenStore.token.isNullOrBlank())
    }

    @Test
    fun emptyForm_showsErrorsAndDoesNotCallTheApi() {
        viewModel.onSubmit()

        assertEquals(
            mapOf(
                LoginField.Email to LoginFormError.EmailInvalid,
                LoginField.Password to LoginFormError.PasswordRequired,
            ),
            state.errors,
        )
        assertTrue(service.logins.isEmpty())

        type()
        assertTrue(state.errors.isEmpty())
    }

    @Test
    fun wrongPasswordAndUnknownEmail_showTheSameGenericFailure() {
        type(password = "another-password")
        viewModel.onSubmit()
        val wrongPassword = state.failure

        type(email = "nobody@ejemplo.com")
        viewModel.onSubmit()
        val unknownEmail = state.failure

        assertEquals(LoginFailure.InvalidCredentials, wrongPassword)
        assertEquals(wrongPassword, unknownEmail)
        // Nothing points at one of the two fields.
        assertTrue(state.errors.isEmpty())
        assertFalse(state.isSignedIn)
        assertEquals(SessionState.Guest, authRepository.session.value)
        assertNull(tokenStore.token)
    }

    @Test
    fun invalidCredentials_clearThePasswordAndKeepTheEmail_andTheFailureClearsOnTheNextEdit() {
        type(password = "another-password")

        viewModel.onSubmit()

        assertEquals(validRegistrationForm.email, state.form.email)
        assertEquals("", state.form.password)

        viewModel.onPasswordChange("s")
        assertNull(state.failure)
    }

    @Test
    fun advisorAccount_isToldToUseTheWebApp_andGetsNoSession() {
        service.onLogin =
            { service.onRegister(ADVISOR_REGISTRATION).let { it.copy(user = it.user.copy(role = "advisor")) } }
        type(email = "asesor@ejemplo.com")

        viewModel.onSubmit()

        assertEquals(LoginFailure.NotABuyer, state.failure)
        assertFalse(state.isSignedIn)
        assertNull(tokenStore.token)
    }

    @Test
    fun noConnectionServerFailureAndBadAnswer_haveTheirOwnMessage() {
        type()

        service.onLogin = { throw IOException("offline") }
        viewModel.onSubmit()
        assertEquals(LoginFailure.NoConnection, state.failure)

        service.onLogin = { throw httpException(500) }
        viewModel.onSubmit()
        assertEquals(LoginFailure.ServiceUnavailable, state.failure)

        service.onLogin = { throw httpException(404) }
        viewModel.onSubmit()
        assertEquals(LoginFailure.Unexpected, state.failure)
        // Only wrong credentials clear the password.
        assertEquals(validRegistrationForm.password, state.form.password)
    }

    @Test
    fun unauthorizedFromTheApi_isInvalidCredentials() {
        service.onLogin = { throw httpException(401, contractResource("problem-invalid-credentials.json")) }
        type()

        viewModel.onSubmit()

        assertEquals(LoginFailure.InvalidCredentials, state.failure)
    }

    @Test
    fun whileSubmitting_aSecondSubmitIsIgnored() {
        val gate = CompletableDeferred<Unit>()
        val login = service.onLogin
        service.onLogin = {
            gate.await()
            login(it)
        }
        type()

        viewModel.onSubmit()
        assertTrue(state.isSubmitting)
        viewModel.onSubmit()

        gate.complete(Unit)
        assertFalse(state.isSubmitting)
        assertEquals(1, service.logins.size)
    }

    @Test
    fun togglePasswordVisibility_flipsTheFlag() {
        viewModel.onTogglePasswordVisibility()
        assertTrue(state.isPasswordVisible)

        viewModel.onTogglePasswordVisibility()
        assertFalse(state.isPasswordVisible)
    }

    private companion object {
        val ADVISOR_REGISTRATION =
            com.auracode.hiposim.feature.auth.data.RegisterRequestDto(
                fullName = "Demo Advisor",
                email = "asesor@ejemplo.com",
                phone = "900000000",
                password = "secret123",
                acceptedTerms = true,
                acceptedDataProcessing = true,
            )
    }
}
