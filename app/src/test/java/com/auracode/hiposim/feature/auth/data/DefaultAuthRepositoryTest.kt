package com.auracode.hiposim.feature.auth.data

import com.auracode.hiposim.core.network.AuthInterceptor
import com.auracode.hiposim.core.network.TokenStore
import com.auracode.hiposim.di.NetworkModule
import com.auracode.hiposim.feature.auth.domain.AuthFailure
import com.auracode.hiposim.feature.auth.domain.RegisterResult
import com.auracode.hiposim.feature.auth.domain.RegistrationField
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.auth.domain.SignInResult
import com.auracode.hiposim.testing.contractResource
import com.auracode.hiposim.testing.validRegistrationForm
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** The repository against a local server that answers with the examples of the contract (openapi.v0.yaml). */
class DefaultAuthRepositoryTest {
    private val server = MockWebServer()
    private val tokenStore = TokenStore()
    private lateinit var repository: DefaultAuthRepository

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenStore)).build()
        val service =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
                .build()
                .create(AuthService::class.java)
        repository = DefaultAuthRepository(service, tokenStore)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun enqueue(
        code: Int,
        body: String = "",
        contentType: String = "application/json",
    ) = server.enqueue(
        MockResponse
            .Builder()
            .code(code)
            .addHeader("Content-Type", contentType)
            .body(body)
            .build(),
    )

    private fun enqueueProblem(
        code: Int,
        resource: String,
    ) = enqueue(code, contractResource(resource), contentType = "application/problem+json")

    private fun assertGuestWithoutToken() {
        assertEquals(SessionState.Guest, repository.session.value)
        assertNull(tokenStore.token)
    }

    @Test
    fun session_startsAsGuest() {
        assertGuestWithoutToken()
    }

    @Test
    fun register_postsTheFormOfTheContract() =
        runTest {
            enqueue(201, AUTH_RESPONSE)

            repository.register(
                validRegistrationForm.copy(fullName = "  Carlos Mendoza ", email = " carlos@ejemplo.com "),
            )

            val request = server.takeRequest()
            assertEquals("POST", request.method)
            assertEquals("/api/auth/register", request.url.encodedPath)
            val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals(
                setOf("fullName", "email", "phone", "password", "acceptedTerms", "acceptedDataProcessing"),
                body.keys,
            )
            assertEquals("Carlos Mendoza", body.getValue("fullName").jsonPrimitive.content)
            assertEquals("carlos@ejemplo.com", body.getValue("email").jsonPrimitive.content)
            assertEquals("987654321", body.getValue("phone").jsonPrimitive.content)
            assertTrue(body.getValue("acceptedTerms").jsonPrimitive.boolean)
            assertTrue(body.getValue("acceptedDataProcessing").jsonPrimitive.boolean)
        }

    @Test
    fun register_created_startsTheSessionAndKeepsTheToken() =
        runTest {
            enqueue(201, AUTH_RESPONSE)

            val result = repository.register(validRegistrationForm)

            val user = (result as RegisterResult.Success).user
            assertEquals("7c1d0f52-4b3e-4a8d-9a1e-2f6b8c3d5e10", user.id)
            assertEquals("Ana Torres", user.fullName)
            assertEquals("ana.torres@example.com", user.email)
            assertTrue(user.isBuyer)
            assertEquals(SessionState.Authenticated(user), repository.session.value)
            assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example.signature", tokenStore.token)
        }

    @Test
    fun afterRegistering_theNextRequestCarriesTheBearerToken() =
        runTest {
            enqueue(201, AUTH_RESPONSE)
            enqueue(200, AUTH_RESPONSE)
            repository.register(validRegistrationForm)
            server.takeRequest()

            repository.signIn("ana.torres@example.com", "S3gura-2026")

            assertEquals(
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example.signature",
                server.takeRequest().headers[AuthInterceptor.AUTHORIZATION],
            )
        }

    @Test
    fun register_conflict_isEmailAlreadyRegistered() =
        runTest {
            enqueueProblem(409, "problem-email-already-registered.json")

            assertEquals(RegisterResult.EmailAlreadyRegistered, repository.register(validRegistrationForm))
            assertGuestWithoutToken()
        }

    @Test
    fun register_validationProblem_returnsTheMessageOfEachField() =
        runTest {
            enqueueProblem(400, "problem-registration-validation.json")

            val result = repository.register(validRegistrationForm)

            assertEquals(
                RegisterResult.Rejected(
                    mapOf(
                        RegistrationField.Email to "A valid email address is required.",
                        RegistrationField.Terms to "Terms and Conditions must be accepted.",
                        RegistrationField.Consent to "Personal data processing must be accepted.",
                    ),
                ),
                result,
            )
            assertGuestWithoutToken()
        }

    @Test
    fun register_serverErrorAndNoConnection_areFailures() =
        runTest {
            enqueue(500)
            assertEquals(
                RegisterResult.Failed(AuthFailure.ServiceUnavailable),
                repository.register(validRegistrationForm),
            )

            server.close()
            assertEquals(RegisterResult.Failed(AuthFailure.NoConnection), repository.register(validRegistrationForm))
            assertGuestWithoutToken()
        }

    @Test
    fun signIn_postsTheCredentials_andStartsTheSession() =
        runTest {
            enqueue(200, AUTH_RESPONSE)

            val result = repository.signIn(" ana.torres@example.com ", "S3gura-2026")

            val request = server.takeRequest()
            assertEquals("/api/auth/login", request.url.encodedPath)
            val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
            assertEquals(setOf("email", "password"), body.keys)
            assertEquals("ana.torres@example.com", body.getValue("email").jsonPrimitive.content)

            val user = (result as SignInResult.Success).user
            assertEquals(SessionState.Authenticated(user), repository.session.value)
            assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.example.signature", tokenStore.token)
        }

    @Test
    fun signIn_unauthorized_isInvalidCredentials() =
        runTest {
            enqueueProblem(401, "problem-invalid-credentials.json")

            assertEquals(SignInResult.InvalidCredentials, repository.signIn("ana.torres@example.com", "wrong"))
            assertGuestWithoutToken()
        }

    @Test
    fun signIn_validationProblem_isAnsweredLikeInvalidCredentials() =
        runTest {
            enqueueProblem(400, "problem-registration-validation.json")

            assertEquals(SignInResult.InvalidCredentials, repository.signIn("not-an-email", "x"))
        }

    @Test
    fun signIn_asAnAdvisor_startsNoSession() =
        runTest {
            enqueue(200, AUTH_RESPONSE.replace("\"buyer\"", "\"advisor\""))

            assertEquals(SignInResult.NotABuyer, repository.signIn("advisor@example.com", "S3gura-2026"))
            assertGuestWithoutToken()
        }

    @Test
    fun signIn_serverErrorNoConnectionAndBadBody_areFailures() =
        runTest {
            enqueue(503)
            assertEquals(SignInResult.Failed(AuthFailure.ServiceUnavailable), repository.signIn("a@b.co", "secret123"))

            enqueue(200, """{"accessToken":"x"}""")
            assertEquals(SignInResult.Failed(AuthFailure.Unexpected), repository.signIn("a@b.co", "secret123"))

            server.close()
            assertEquals(SignInResult.Failed(AuthFailure.NoConnection), repository.signIn("a@b.co", "secret123"))
            assertGuestWithoutToken()
        }

    @Test
    fun signOut_forgetsTheSessionAndTheToken() =
        runTest {
            enqueue(200, AUTH_RESPONSE)
            repository.signIn("ana.torres@example.com", "S3gura-2026")

            repository.signOut()

            assertGuestWithoutToken()
        }

    private companion object {
        val AUTH_RESPONSE = contractResource("auth-response.json")
    }
}
