package com.auracode.hiposim.feature.auth.data

import com.auracode.hiposim.core.network.ApiError
import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.safeApiCall
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The in-memory accounts must answer and fail with the shapes of the contract. */
class FakeAuthServiceTest {
    private val service = FakeAuthService()

    private val request =
        RegisterRequestDto(
            fullName = " Ana Torres ",
            email = "Ana.Torres@example.com",
            phone = "987654321",
            password = "S3gura-2026",
            acceptedTerms = true,
            acceptedDataProcessing = true,
        )

    private suspend fun errorOf(call: suspend () -> AuthResponseDto): ApiError =
        (safeApiCall { call() } as ApiResult.Failure).error

    @Test
    fun register_answersLikeTheContract_withABuyerAndABearerToken() =
        runTest {
            val response = service.register(request)

            assertEquals("Bearer", response.tokenType)
            assertTrue(response.accessToken.isNotBlank())
            assertTrue(response.expiresIn > 0)
            assertEquals("Ana Torres", response.user.fullName)
            assertEquals("Ana.Torres@example.com", response.user.email)
            assertEquals("buyer", response.user.role)
        }

    @Test
    fun register_sameEmailIgnoringCase_isAConflict() =
        runTest {
            service.register(request)

            val error = errorOf { service.register(request.copy(email = "ana.torres@EXAMPLE.com")) }

            assertEquals(ApiError.Conflict("Email already registered."), error)
        }

    @Test
    fun register_withoutTheAcceptances_isRejectedByField() =
        runTest {
            val error =
                errorOf { service.register(request.copy(acceptedTerms = false, acceptedDataProcessing = false)) }

            assertEquals(
                setOf("acceptedTerms", "acceptedDataProcessing"),
                (error as ApiError.Validation).fieldErrors.keys,
            )
        }

    @Test
    fun register_withInvalidFields_reportsEachOne() =
        runTest {
            val invalid = request.copy(fullName = " ", email = "not-an-email", phone = "", password = "short")

            val error = errorOf { service.register(invalid) }

            assertEquals(
                setOf("fullName", "email", "phone", "password"),
                (error as ApiError.Validation).fieldErrors.keys,
            )
        }

    @Test
    fun login_withTheRegisteredCredentials_returnsTheSameUser() =
        runTest {
            val registered = service.register(request).user

            val response = service.login(LoginRequestDto(" ANA.torres@example.com ", "S3gura-2026"))

            assertEquals(registered, response.user)
        }

    @Test
    fun login_wrongPasswordAndUnknownEmail_failTheSameWay() =
        runTest {
            service.register(request)

            val wrongPassword = errorOf { service.login(LoginRequestDto("ana.torres@example.com", "other-password")) }
            val unknownEmail = errorOf { service.login(LoginRequestDto("nobody@example.com", "S3gura-2026")) }

            assertEquals(ApiError.Unauthorized("Invalid credentials."), wrongPassword)
            assertEquals(wrongPassword, unknownEmail)
        }

    @Test
    fun login_malformedRequest_isAValidationProblem() =
        runTest {
            val error = errorOf { service.login(LoginRequestDto("not-an-email", "")) }

            assertEquals(setOf("email", "password"), (error as ApiError.Validation).fieldErrors.keys)
        }
}
