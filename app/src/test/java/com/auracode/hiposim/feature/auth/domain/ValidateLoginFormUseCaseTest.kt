package com.auracode.hiposim.feature.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateLoginFormUseCaseTest {
    private val validate = ValidateLoginFormUseCase()

    @Test
    fun emailAndPassword_areEnough() {
        assertTrue(validate(LoginForm(email = " ana.torres@example.com ", password = "x")).isEmpty())
    }

    @Test
    fun emptyForm_reportsBothFields() {
        assertEquals(
            mapOf(
                LoginField.Email to LoginFormError.EmailInvalid,
                LoginField.Password to LoginFormError.PasswordRequired,
            ),
            validate(LoginForm()),
        )
    }

    @Test
    fun malformedEmail_isReported() {
        listOf("ana", "ana@", "ana@example", "ana torres@example.com").forEach { email ->
            assertEquals(email, LoginFormError.EmailInvalid, validate(LoginForm(email, "secret123"))[LoginField.Email])
        }
    }

    @Test
    fun passwordLength_isNotCheckedHere() {
        assertTrue(validate(LoginForm("ana.torres@example.com", "1")).isEmpty())
    }
}
