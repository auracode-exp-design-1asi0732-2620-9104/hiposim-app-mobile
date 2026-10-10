package com.auracode.hiposim.core.network

/** Outcome of a call to the HipoSim API. Repositories return this instead of throwing. */
sealed interface ApiResult<out T> {
    data class Success<T>(
        val data: T,
    ) : ApiResult<T>

    data class Failure(
        val error: ApiError,
    ) : ApiResult<Nothing>
}

/**
 * Why a call failed, already classified so the UI can pick a message without knowing about HTTP.
 *
 * The API answers errors with `application/problem+json` (contract v0). [title] is the server text, in English
 * for now, so prefer a string resource chosen from the error type and keep [title] for logs or as a fallback.
 */
sealed interface ApiError {
    /** The request never got an answer: no network, timeout or unreachable server. */
    data object NoConnection : ApiError

    /**
     * 400: the server rejected the request body.
     *
     * [fieldErrors] maps each request property, in camelCase like the DTO (`downPayment`), to its messages.
     * It is empty when the server sent no `errors` object.
     */
    data class Validation(
        val fieldErrors: Map<String, List<String>>,
        val title: String? = null,
    ) : ApiError

    /** 401: wrong credentials, or a missing or expired token. */
    data class Unauthorized(
        val title: String? = null,
    ) : ApiError

    /** 409: the request conflicts with existing data, for example an email that is already registered. */
    data class Conflict(
        val title: String? = null,
    ) : ApiError

    /** Any other 4xx. */
    data class Client(
        val code: Int,
        val title: String? = null,
    ) : ApiError

    /** The server failed (5xx). */
    data class Server(
        val code: Int,
    ) : ApiError

    /** Anything else, such as a response body that does not match the expected DTO. */
    data class Unexpected(
        val cause: Throwable? = null,
    ) : ApiError
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> =
    when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(data))
        is ApiResult.Failure -> this
    }
