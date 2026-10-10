package com.auracode.hiposim.core.network

import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

private const val BAD_REQUEST = 400
private const val UNAUTHORIZED = 401
private const val CONFLICT = 409
private const val SERVER_ERROR_FIRST = 500

/**
 * Runs a Retrofit call and turns its exceptions into an [ApiResult].
 *
 * Every API-backed repository should wrap its service calls with this, so error handling stays in one place.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> =
    try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        ApiResult.Failure(e.toApiError())
    } catch (e: IOException) {
        ApiResult.Failure(ApiError.NoConnection)
    } catch (e: Exception) {
        ApiResult.Failure(ApiError.Unexpected(e))
    }

private fun HttpException.toApiError(): ApiError {
    val code = code()
    if (code >= SERVER_ERROR_FIRST) return ApiError.Server(code)
    if (code < BAD_REQUEST) return ApiError.Unexpected(this)

    val problem = parseProblemDetails(errorBodyOrNull())
    val title = problem?.title
    return when (code) {
        BAD_REQUEST -> ApiError.Validation(fieldErrors = problem?.fieldErrors().orEmpty(), title = title)
        UNAUTHORIZED -> ApiError.Unauthorized(title)
        CONFLICT -> ApiError.Conflict(title)
        else -> ApiError.Client(code, title)
    }
}

private fun HttpException.errorBodyOrNull(): String? =
    try {
        response()?.errorBody()?.string()
    } catch (_: IOException) {
        null
    }
