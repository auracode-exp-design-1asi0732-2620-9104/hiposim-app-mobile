package com.auracode.hiposim.feature.auth.ui

import com.auracode.hiposim.feature.auth.domain.RegistrationError
import com.auracode.hiposim.feature.auth.domain.RegistrationField
import com.auracode.hiposim.feature.auth.domain.ValidateRegistrationFormUseCase
import com.auracode.hiposim.feature.simulation.data.FakeSimulationRepository
import com.auracode.hiposim.feature.simulation.domain.GetLoanSimulationUseCase
import com.auracode.hiposim.testing.MainDispatcherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RegisterViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        viewModel =
            RegisterViewModel(
                getLoanSimulation = GetLoanSimulationUseCase(FakeSimulationRepository()),
                validateForm = ValidateRegistrationFormUseCase(),
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
        assertFalse(state.showComingSoon)
    }

    @Test
    fun submitWithConsent_continues() {
        fillValidForm()
        viewModel.onConsentChange(true)

        viewModel.onSubmit()

        assertTrue(
            viewModel.uiState.value.errors
                .isEmpty(),
        )
        assertTrue(viewModel.uiState.value.showComingSoon)

        viewModel.onComingSoonShown()
        assertFalse(viewModel.uiState.value.showComingSoon)
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
