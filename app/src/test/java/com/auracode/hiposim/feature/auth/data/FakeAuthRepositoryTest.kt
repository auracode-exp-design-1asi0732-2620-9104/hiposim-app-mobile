package com.auracode.hiposim.feature.auth.data

import com.auracode.hiposim.feature.auth.domain.RegisterResult
import com.auracode.hiposim.feature.auth.domain.RegistrationForm
import com.auracode.hiposim.feature.auth.domain.SessionState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAuthRepositoryTest {
    private val repository = FakeAuthRepository()

    private val form =
        RegistrationForm(
            fullName = "  Carlos Mendoza ",
            email = "Carlos@Ejemplo.com ",
            phone = "987 654 321",
            password = "secret123",
            consentAccepted = true,
        )

    @Test
    fun session_startsAsGuest() {
        assertEquals(SessionState.Guest, repository.session.value)
    }

    @Test
    fun register_signsTheUserInWithNormalizedData() =
        runTest {
            val result = repository.register(form)

            val user = (result as RegisterResult.Success).user
            assertEquals("Carlos Mendoza", user.fullName)
            assertEquals("carlos@ejemplo.com", user.email)
            assertEquals("987654321", user.phone)
            assertEquals(SessionState.Authenticated(user), repository.session.value)
        }

    @Test
    fun register_rejectsTheSameEmailIgnoringCase() =
        runTest {
            repository.register(form)
            repository.signOut()

            val result = repository.register(form.copy(email = "CARLOS@ejemplo.com"))

            assertEquals(RegisterResult.EmailAlreadyRegistered, result)
            assertEquals(SessionState.Guest, repository.session.value)
        }

    @Test
    fun signOut_returnsToGuest() =
        runTest {
            repository.register(form)
            assertTrue(repository.session.value is SessionState.Authenticated)

            repository.signOut()

            assertEquals(SessionState.Guest, repository.session.value)
        }
}
