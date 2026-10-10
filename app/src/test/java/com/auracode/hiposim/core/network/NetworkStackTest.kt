package com.auracode.hiposim.core.network

import com.auracode.hiposim.di.NetworkModule
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
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
import retrofit2.http.GET

@Serializable
data class HealthDto(
    val status: String,
)

interface HealthService {
    @GET("health")
    suspend fun health(): HealthDto
}

/** Exercises the real OkHttp + Retrofit + JSON stack against a local server, with `GET /health` of the contract. */
class NetworkStackTest {
    private val server = MockWebServer()
    private val tokenStore = TokenStore()
    private lateinit var service: HealthService

    @Before
    fun setUp() {
        server.start()
        val client = OkHttpClient.Builder().addInterceptor(AuthInterceptor(tokenStore)).build()
        service =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(NetworkModule.provideJson().asConverterFactory("application/json".toMediaType()))
                .build()
                .create(HealthService::class.java)
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
        body: String,
    ) = enqueue(code, body, contentType = "application/problem+json")

    @Test
    fun guest_requestHasNoAuthorizationHeader() =
        runTest {
            enqueue(200, HEALTHY)

            service.health()

            val request = server.takeRequest()
            assertEquals("/health", request.url.encodedPath)
            assertNull(request.headers[AuthInterceptor.AUTHORIZATION])
        }

    @Test
    fun signedIn_requestCarriesTheBearerToken_untilItIsCleared() =
        runTest {
            enqueue(200, HEALTHY)
            enqueue(200, HEALTHY)

            tokenStore.token = "abc.def.ghi"
            service.health()
            assertEquals("Bearer abc.def.ghi", server.takeRequest().headers[AuthInterceptor.AUTHORIZATION])

            tokenStore.clear()
            service.health()
            assertNull(server.takeRequest().headers[AuthInterceptor.AUTHORIZATION])
        }

    @Test
    fun unknownFieldsInTheResponseAreIgnored() =
        runTest {
            enqueue(200, """{"status":"Healthy","addedLater":true}""")

            assertEquals(ApiResult.Success(HealthDto("Healthy")), safeApiCall { service.health() })
        }

    @Test
    fun validationProblem_becomesFieldErrors() =
        runTest {
            enqueueProblem(
                400,
                """
                {"title":"One or more validation errors occurred.","status":400,
                 "errors":{"downPayment":["The down payment must be at least 10% of the property price."]}}
                """.trimIndent(),
            )

            val expected =
                ApiError.Validation(
                    fieldErrors =
                        mapOf("downPayment" to listOf("The down payment must be at least 10% of the property price.")),
                    title = "One or more validation errors occurred.",
                )
            assertEquals(ApiResult.Failure(expected), safeApiCall { service.health() })
        }

    @Test
    fun unauthorizedAndConflictProblems_keepTheirTitle() =
        runTest {
            enqueueProblem(401, """{"title":"Invalid credentials.","status":401}""")
            enqueueProblem(409, """{"title":"Email already registered.","status":409}""")

            assertEquals(
                ApiResult.Failure(ApiError.Unauthorized("Invalid credentials.")),
                safeApiCall { service.health() },
            )
            assertEquals(
                ApiResult.Failure(ApiError.Conflict("Email already registered.")),
                safeApiCall { service.health() },
            )
        }

    @Test
    fun otherHttpErrorsAndBadBodiesBecomeFailures() =
        runTest {
            enqueue(404)
            enqueue(500)
            enqueue(200, """{"unexpected":1}""")

            assertEquals(ApiResult.Failure(ApiError.Client(404)), safeApiCall { service.health() })
            assertEquals(ApiResult.Failure(ApiError.Server(500)), safeApiCall { service.health() })
            val malformed = safeApiCall { service.health() }
            assertTrue(malformed is ApiResult.Failure && malformed.error is ApiError.Unexpected)
        }

    @Test
    fun unreachableServer_isNoConnection() =
        runTest {
            server.close()

            assertEquals(ApiResult.Failure(ApiError.NoConnection), safeApiCall { service.health() })
        }

    private companion object {
        const val HEALTHY = """{"status":"Healthy"}"""
    }
}
