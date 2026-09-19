package com.auracode.hiposim.feature.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateRegistrationFormUseCaseTest {
    private val validate = ValidateRegistrationFormUseCase()

    private val validForm =
        RegistrationForm(
            fullName = "Carlos Mendoza",
            email = "carlos@ejemplo.com",
            phone = "987 654 321",
            password = "secret123",
            consentAccepted = true,
        )

    private fun errorOf(
        form: RegistrationForm,
        field: RegistrationField,
    ) = validate(form).errors[field]

    @Test
    fun validForm_hasNoErrors() {
        assertTrue(validate(validForm).isValid)
    }

    @Test
    fun emptyForm_reportsEveryField() {
        val errors = validate(RegistrationForm()).errors

        assertEquals(RegistrationField.entries.toSet(), errors.keys)
    }

    @Test
    fun withoutConsent_isInvalidEvenIfEverythingElseIsValid() {
        val result = validate(validForm.copy(consentAccepted = false))

        assertFalse(result.isValid)
        assertEquals(mapOf(RegistrationField.Consent to RegistrationError.ConsentRequired), result.errors)
    }

    @Test
    fun email_needsAtSignAndDomain() {
        assertEquals(RegistrationError.EmailInvalid, errorOf(validForm.copy(email = "carlos"), RegistrationField.Email))
        assertEquals(
            RegistrationError.EmailInvalid,
            errorOf(validForm.copy(email = "carlos@ejemplo"), RegistrationField.Email),
        )
    }

    @Test
    fun phone_needsNineDigitsStartingWithNine() {
        assertTrue(validate(validForm.copy(phone = "987654321")).isValid)
        assertEquals(
            RegistrationError.PhoneInvalid,
            errorOf(validForm.copy(phone = "87654321"), RegistrationField.Phone),
        )
        assertEquals(
            RegistrationError.PhoneInvalid,
            errorOf(validForm.copy(phone = "887654321"), RegistrationField.Phone),
        )
    }

    @Test
    fun password_needsEightCharacters() {
        assertEquals(
            RegistrationError.PasswordTooShort,
            errorOf(validForm.copy(password = "1234567"), RegistrationField.Password),
        )
        assertTrue(validate(validForm.copy(password = "12345678")).isValid)
    }

    @Test
    fun name_cannotBeBlank() {
        assertEquals(
            RegistrationError.NameRequired,
            errorOf(validForm.copy(fullName = "   "), RegistrationField.FullName),
        )
    }
}
