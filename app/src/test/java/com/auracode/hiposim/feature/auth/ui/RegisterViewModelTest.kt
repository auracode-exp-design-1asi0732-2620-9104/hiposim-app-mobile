package com.auracode.hiposim.feature.auth.ui

import com.auracode.hiposim.feature.auth.data.FakeAuthRepository
import com.auracode.hiposim.feature.auth.domain.RegisterUserUseCase
import com.auracode.hiposim.feature.auth.domain.RegistrationError
import com.auracode.hiposim.feature.auth.domain.RegistrationField
import com.auracode.hiposim.feature.auth.domain.RegistrationForm
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.auth.domain.ValidateRegistrationFormUseCase
import com.auracode.hiposim.feature.simulation.data.FakeSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.GetLoanSimulationUseCase
import com.auracode.hiposim.testing.MainDispatcherRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        authRepository = FakeAuthRepository()
        viewModel =
            RegisterViewModel(
                getLoanSimulation = GetLoanSimulationUseCase(FakeSimulationRepository()),
                validateForm = ValidateRegistrationFormUseCase(),
                registerUser = RegisterUserUseCase(authRepository),
            )
    }

    private fun fillValidForm() {
        viewModel.onFullNameChange("Carlos Mendoza")
        viewModel.onEmailChange("carlos@ejemplo.com")
        viewModel.onPhoneChange("987 654 321")
        viewModel.onPasswordChange("secret123")
    }

    @Test
    fun init_showsQuoteSummary() {
        val quote = requireNotNull(viewModel.uiState.value.quote)

        assertEquals("S/ 1,909", quote.monthlyPayment)
        assertEquals("8.85%", quote.tcea)
        assertEquals("S/ 280,000", quote.propertyPrice)
    }

    @Test
    fun consent_startsUnchecked() {
        assertFalse(viewModel.uiState.value.form.consentAccepted)
    }

    @Test
    fun submitWithoutConsent_showsConsentErrorAndDoesNotContinue() {
        fillValidForm()

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertEquals(RegistrationError.ConsentRequired, state.errors[RegistrationField.Consent])
        assertFalse(state.isRegistered)
        assertEquals(SessionState.Guest, authRepository.session.value)
    }

    @Test
    fun submitWithConsent_createsTheAccountAndSignsIn() {
        fillValidForm()
        viewModel.onConsentChange(true)

        viewModel.onSubmit()

        val state = viewModel.uiState.value
        assertTrue(state.errors.isEmpty())
        assertTrue(state.isRegistered)
        assertFalse(state.isSubmitting)
        assertTrue(authRepository.session.value is SessionState.Authenticated)
    }

    @Test
    fun submitWithTakenEmail_showsEmailErrorAndDoesNotRegister() =
        runTest {
            authRepository.register(
                RegistrationForm(fullName = "Ana", email = "carlos@ejemplo.com", phone = "987654321", password = "x"),
            )
            authRepository.signOut()
            fillValidForm()
            viewModel.onConsentChange(true)

            viewModel.onSubmit()

            val state = viewModel.uiState.value
            assertEquals(RegistrationError.EmailAlreadyRegistered, state.errors[RegistrationField.Email])
            assertFalse(state.isRegistered)
            assertFalse(state.isSubmitting)
            assertEquals(SessionState.Guest, authRepository.session.value)
        }

    @Test
    fun takenEmailError_staysWhenEditingOtherFieldsAndClearsWhenEmailChanges() =
        runTest {
            authRepository.register(
                RegistrationForm(fullName = "Ana", email = "carlos@ejemplo.com", phone = "987654321", password = "x"),
            )
            authRepository.signOut()
            fillValidForm()
            viewModel.onConsentChange(true)
            viewModel.onSubmit()

            viewModel.onFullNameChange("Carlos M.")
            assertEquals(
                RegistrationError.EmailAlreadyRegistered,
                viewModel.uiState.value.errors[RegistrationField.Email],
            )

            viewModel.onEmailChange("otro@ejemplo.com")
            assertFalse(
                viewModel.uiState.value.errors
                    .containsKey(RegistrationField.Email),
            )
        }

    @Test
    fun errors_areHiddenUntilSubmitAndThenFollowEdits() {
        assertTrue(
            viewModel.uiState.value.errors
                .isEmpty(),
        )

        viewModel.onSubmit()
        assertEquals(5, viewModel.uiState.value.errors.size)

        viewModel.onConsentChange(true)
        assertFalse(
            viewModel.uiState.value.errors
                .containsKey(RegistrationField.Consent),
        )
    }

    @Test
    fun phoneInput_keepsOnlyDigitsAndSpaces() {
        viewModel.onPhoneChange("9a8-7 6543219999")

        assertEquals("987 6543219", viewModel.uiState.value.form.phone)
    }

    @Test
    fun togglePasswordVisibility_flipsTheFlag() {
        viewModel.onTogglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)

        viewModel.onTogglePasswordVisibility()
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }
}
