package com.auracode.hiposim.feature.auth.ui

import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.feature.auth.data.DefaultAuthRepository
import com.auracode.hiposim.feature.auth.domain.AuthFailure
import com.auracode.hiposim.feature.auth.domain.RegisterUserUseCase
import com.auracode.hiposim.feature.auth.domain.RegistrationError
import com.auracode.hiposim.feature.auth.domain.RegistrationField
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.auth.domain.ValidateRegistrationFormUseCase
import com.auracode.hiposim.feature.simulation.domain.ObserveCurrentSimulationUseCase
import com.auracode.hiposim.testing.MainDispatcherRule
import com.auracode.hiposim.testing.StubAuthService
import com.auracode.hiposim.testing.httpException
import com.auracode.hiposim.testing.simulationRepositoryWithContractExample
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException

class RegisterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var service: StubAuthService
    private lateinit var authRepository: DefaultAuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        service = StubAuthService()
        authRepository = DefaultAuthRepository(service, TokenStore())
        viewModel =
            RegisterViewModel(
                observeCurrentSimulation =
                    ObserveCurrentSimulationUseCase(runBlocking { simulationRepositoryWithContractExample() }),
                validateForm = ValidateRegistrationFormUseCase(),
                registerUser = RegisterUserUseCase(authRepository),
            )
    }

    private val state get() = viewModel.uiState.value

    private fun fillValidForm(acceptBoth: Boolean = true) {
        viewModel.onFullNameChange("Carlos Mendoza")
        viewModel.onEmailChange("carlos@ejemplo.com")
        viewModel.onPhoneChange("987 654 321")
        viewModel.onPasswordChange("secret123")
        if (acceptBoth) {
            viewModel.onTermsChange(true)
            viewModel.onConsentChange(true)
        }
    }

    /** Leaves an account with the form's email and no session. */
    private suspend fun registerTheEmailBeforehand() {
        authRepository.register(validRegistrationForm)
        authRepository.signOut()
        service.registrations.clear()
    }

    @Test
    fun init_showsQuoteSummary() {
        val quote = requireNotNull(state.quote)

        assertEquals("S/ 1,864", quote.monthlyPayment)
        assertEquals("8.50%", quote.tcea)
        assertEquals("S/ 280,000", quote.propertyPrice)
    }

    @Test
    fun bothAcceptances_startUnchecked() {
        assertFalse(state.form.termsAccepted)
        assertFalse(state.form.consentAccepted)
    }

    @Test
    fun submitWithoutTheAcceptances_showsBothErrorsAndDoesNotCallTheApi() {
        fillValidForm(acceptBoth = false)

        viewModel.onSubmit()

        assertEquals(RegistrationError.TermsRequired, state.errors[RegistrationField.Terms])
        assertEquals(RegistrationError.ConsentRequired, state.errors[RegistrationField.Consent])
        assertFalse(state.isRegistered)
        assertTrue(service.registrations.isEmpty())
        assertEquals(SessionState.Guest, authRepository.session.value)
    }

    @Test
    fun submitWithOnlyOneAcceptance_isNotEnough() {
        fillValidForm(acceptBoth = false)
        viewModel.onConsentChange(true)

        viewModel.onSubmit()

        assertEquals(setOf(RegistrationField.Terms), state.errors.keys)
        assertTrue(service.registrations.isEmpty())
    }

    @Test
    fun submitValidForm_createsTheAccountAndSignsIn() {
        fillValidForm()

        viewModel.onSubmit()

        assertTrue(state.errors.isEmpty())
        assertTrue(state.isRegistered)
        assertFalse(state.isSubmitting)
        assertTrue(authRepository.session.value is SessionState.Authenticated)
        val request = service.registrations.single()
        assertEquals("987654321", request.phone)
        assertTrue(request.acceptedTerms && request.acceptedDataProcessing)
    }

    @Test
    fun whileSubmitting_aSecondSubmitIsIgnored() {
        val gate = CompletableDeferred<Unit>()
        val register = service.onRegister
        service.onRegister = {
            gate.await()
            register(it)
        }
        fillValidForm()

        viewModel.onSubmit()
        assertTrue(state.isSubmitting)
        viewModel.onSubmit()

        gate.complete(Unit)
        assertFalse(state.isSubmitting)
        assertEquals(1, service.registrations.size)
    }

    @Test
    fun submitWithTakenEmail_showsEmailErrorAndDoesNotRegister() =
        runTest {
            registerTheEmailBeforehand()
            fillValidForm()

            viewModel.onSubmit()

            assertEquals(RegistrationError.EmailAlreadyRegistered, state.errors[RegistrationField.Email])
            assertFalse(state.isRegistered)
            assertFalse(state.isSubmitting)
            assertEquals(SessionState.Guest, authRepository.session.value)
        }

    @Test
    fun takenEmailError_staysWhenEditingOtherFieldsAndClearsWhenEmailChanges() =
        runTest {
            registerTheEmailBeforehand()
            fillValidForm()
            viewModel.onSubmit()

            viewModel.onFullNameChange("Carlos M.")
            assertEquals(RegistrationError.EmailAlreadyRegistered, state.errors[RegistrationField.Email])

            viewModel.onEmailChange("otro@ejemplo.com")
            assertFalse(state.errors.containsKey(RegistrationField.Email))
        }

    @Test
    fun serverValidationError_isShownNextToItsField_andClearsWhenThatFieldIsEdited() {
        service.onRegister = {
            throw httpException(
                400,
                """
                {"title":"One or more validation errors occurred.","status":400,
                 "errors":{"phone":["Phone is required and cannot exceed 20 characters."],
                           "password":["Password must have between 8 and 128 characters."]}}
                """.trimIndent(),
            )
        }
        fillValidForm()

        viewModel.onSubmit()

        assertEquals(
            mapOf(
                RegistrationField.Phone to "Phone is required and cannot exceed 20 characters.",
                RegistrationField.Password to "Password must have between 8 and 128 characters.",
            ),
            state.serverErrors,
        )
        assertNull(state.failure)
        assertFalse(state.isRegistered)

        viewModel.onPhoneChange("912345678")
        assertEquals(setOf(RegistrationField.Password), state.serverErrors.keys)
    }

    @Test
    fun serverValidationErrorOutsideTheForm_isAGeneralFailure() {
        service.onRegister = {
            throw httpException(400, """{"title":"Invalid.","status":400,"errors":{"request":["Malformed."]}}""")
        }
        fillValidForm()

        viewModel.onSubmit()

        assertTrue(state.serverErrors.isEmpty())
        assertEquals(AuthFailure.Unexpected, state.failure)
    }

    @Test
    fun noConnectionAndServerFailure_showAGeneralFailure_thatClearsOnTheNextEdit() {
        fillValidForm()

        service.onRegister = { throw IOException("offline") }
        viewModel.onSubmit()
        assertEquals(AuthFailure.NoConnection, state.failure)
        assertFalse(state.isRegistered)

        service.onRegister = { throw httpException(503) }
        viewModel.onSubmit()
        assertEquals(AuthFailure.ServiceUnavailable, state.failure)

        viewModel.onFullNameChange("Carlos M.")
        assertNull(state.failure)
    }

    @Test
    fun errors_areHiddenUntilSubmitAndThenFollowEdits() {
        assertTrue(state.errors.isEmpty())

        viewModel.onSubmit()
        assertEquals(RegistrationField.entries.toSet(), state.errors.keys)

        viewModel.onConsentChange(true)
        assertFalse(state.errors.containsKey(RegistrationField.Consent))
        viewModel.onTermsChange(true)
        assertFalse(state.errors.containsKey(RegistrationField.Terms))
    }

    @Test
    fun phoneInput_keepsOnlyDigitsAndSpaces() {
        viewModel.onPhoneChange("9a8-7 6543219999")

        assertEquals("987 6543219", state.form.phone)
    }

    @Test
    fun togglePasswordVisibility_flipsTheFlag() {
        viewModel.onTogglePasswordVisibility()
        assertTrue(state.isPasswordVisible)

        viewModel.onTogglePasswordVisibility()
        assertFalse(state.isPasswordVisible)
    }
}
