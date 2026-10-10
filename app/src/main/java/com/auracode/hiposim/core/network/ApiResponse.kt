package com.auracode.hiposim.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import retrofit2.Response
import java.io.IOException

/** `application/problem+json` body the API returns with every 4xx error (RFC 9457). */
@Serializable
data class ProblemDetailsDto(
    val title: String? = null,
    val status: Int? = null,
    /** Failing request properties (camelCase) with their messages. Empty for errors that are not about a field. */
    val errors: Map<String, List<String>> = emptyMap(),
)

/** What happened to one API call, before any feature turns it into its own domain result. */
sealed interface ApiResponse<out T> {
    data class Success<T>(
        val body: T,
    ) : ApiResponse<T>

    /** The server answered with a non-2xx [code]. [problem] is its parsed error body, when it had one. */
    data class HttpError(
        val code: Int,
        val problem: ProblemDetailsDto?,
    ) : ApiResponse<Nothing>

    /** The server could not be reached: offline, DNS failure, refused connection or timeout. */
    data class NetworkError(
        val cause: IOException,
    ) : ApiResponse<Nothing>

    /** The server answered 2xx but the body was empty or did not match the contract. */
    data class UnreadableResponse(
        val cause: Throwable,
    ) : ApiResponse<Nothing>
}

/** Runs a Retrofit call declared as `Response<T>` and sorts the outcome into an [ApiResponse]. */
suspend fun <T : Any> apiCall(call: suspend () -> Response<T>): ApiResponse<T> =
    try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResponse.Success(body)
            } else {
                ApiResponse.UnreadableResponse(IllegalStateException("Empty response body"))
            }
        } else {
            ApiResponse.HttpError(response.code(), response.errorBody()?.string()?.let(::parseProblem))
        }
    } catch (error: IOException) {
        ApiResponse.NetworkError(error)
    } catch (error: SerializationException) {
        ApiResponse.UnreadableResponse(error)
    }

private fun parseProblem(body: String): ProblemDetailsDto? =
    try {
        NetworkJson.decodeFromString<ProblemDetailsDto>(body)
    } catch (_: SerializationException) {
        null
    }
