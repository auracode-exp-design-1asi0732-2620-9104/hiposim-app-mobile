package com.auracode.hiposim.core.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Body of an `application/problem+json` error (RFC 9457), with the `errors` map of validation problems. */
@Serializable
internal data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null,
    val traceId: String? = null,
    val errors: Map<String, List<String>>? = null,
)

private val problemJson = Json { ignoreUnknownKeys = true }

/** Returns null when [body] is empty or is not a problem document, so the caller falls back to the status code. */
internal fun parseProblemDetails(body: String?): ProblemDetailsDto? {
    if (body.isNullOrBlank()) return null
    return try {
        problemJson.decodeFromString<ProblemDetailsDto>(body)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}

/**
 * Field errors keyed like the request DTO.
 *
 * The contract asks for camelCase keys, but ASP.NET Core emits PascalCase (`DownPayment`) unless configured,
 * so the first letter is lowered here and both spellings reach the UI as `downPayment`.
 */
internal fun ProblemDetailsDto.fieldErrors(): Map<String, List<String>> =
    errors
        .orEmpty()
        .filterValues { it.isNotEmpty() }
        .mapKeys { (key, _) -> key.replaceFirstChar { it.lowercaseChar() } }
