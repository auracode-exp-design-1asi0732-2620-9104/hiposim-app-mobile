package com.auracode.hiposim.core.network

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

class SafeApiCallTest {
    private fun httpException(
        code: Int,
        body: String = "",
    ) = HttpException(Response.error<Unit>(code, body.toResponseBody()))

    private suspend fun failWith(
        code: Int,
        body: String = "",
    ): ApiResult<Int> = safeApiCall { throw httpException(code, body) }

    @Test
    fun success_wrapsTheValue() =
        runTest {
            assertEquals(ApiResult.Success(42), safeApiCall { 42 })
        }

    @Test
    fun ioException_isNoConnection() =
        runTest {
            assertEquals(ApiResult.Failure(ApiError.NoConnection), safeApiCall<Int> { throw IOException("offline") })
            assertEquals(
                ApiResult.Failure(ApiError.NoConnection),
                safeApiCall<Int> { throw SocketTimeoutException("timeout") },
            )
        }

    @Test
    fun http400_carriesTheFieldErrorsOfTheProblemDocument() =
        runTest {
            val body =
                """
                {"title":"One or more validation errors occurred.","status":400,
                 "errors":{"propertyPrice":["The propertyPrice field is required."],
                           "termInMonths":["The termInMonths field is required.","Second message."]}}
                """.trimIndent()

            val expected =
                ApiError.Validation(
                    fieldErrors =
                        mapOf(
                            "propertyPrice" to listOf("The propertyPrice field is required."),
                            "termInMonths" to listOf("The termInMonths field is required.", "Second message."),
                        ),
                    title = "One or more validation errors occurred.",
                )
            assertEquals(ApiResult.Failure(expected), failWith(400, body))
        }

    @Test
    fun http400_pascalCaseKeysAreReadAsCamelCase() =
        runTest {
            val body = """{"title":"Invalid.","status":400,"errors":{"DownPayment":["Too low."]}}"""

            assertEquals(
                ApiResult.Failure(ApiError.Validation(mapOf("downPayment" to listOf("Too low.")), "Invalid.")),
                failWith(400, body),
            )
        }

    @Test
    fun http400_withoutAProblemDocument_hasNoFieldErrors() =
        runTest {
            assertEquals(ApiResult.Failure(ApiError.Validation(emptyMap())), failWith(400))
            assertEquals(ApiResult.Failure(ApiError.Validation(emptyMap())), failWith(400, "<html>Bad Request</html>"))
        }

    @Test
    fun http401_isUnauthorizedWithItsTitle() =
        runTest {
            assertEquals(
                ApiResult.Failure(ApiError.Unauthorized("Invalid credentials.")),
                failWith(401, """{"title":"Invalid credentials.","status":401}"""),
            )
            assertEquals(ApiResult.Failure(ApiError.Unauthorized()), failWith(401))
        }

    @Test
    fun http409_isConflictWithItsTitle() =
        runTest {
            assertEquals(
                ApiResult.Failure(ApiError.Conflict("Email already registered.")),
                failWith(409, """{"title":"Email already registered.","status":409}"""),
            )
        }

    @Test
    fun other4xx_isClientErrorWithItsCode() =
        runTest {
            assertEquals(ApiResult.Failure(ApiError.Client(404)), failWith(404))
            assertEquals(
                ApiResult.Failure(ApiError.Client(403, "Forbidden.")),
                failWith(403, """{"title":"Forbidden.","status":403}"""),
            )
        }

    @Test
    fun http5xx_isServerErrorWithItsCode() =
        runTest {
            listOf(500, 503).forEach { code ->
                assertEquals(ApiResult.Failure(ApiError.Server(code)), failWith(code))
            }
        }

    @Test
    fun malformedBody_isUnexpected() =
        runTest {
            val cause = SerializationException("missing field")

            val result = safeApiCall<Int> { throw cause }

            assertEquals(ApiResult.Failure(ApiError.Unexpected(cause)), result)
        }

    @Test
    fun cancellation_isNotSwallowed() =
        runTest {
            try {
                safeApiCall<Int> { throw CancellationException("cancelled") }
                fail("CancellationException must propagate")
            } catch (e: CancellationException) {
                assertEquals("cancelled", e.message)
            }
        }

    @Test
    fun map_transformsSuccessAndKeepsFailure() {
        assertEquals(ApiResult.Success("2"), ApiResult.Success(2).map { it.toString() })

        val failure: ApiResult<Int> = ApiResult.Failure(ApiError.Server(500))
        assertEquals(failure, failure.map { it.toString() })
    }
}
