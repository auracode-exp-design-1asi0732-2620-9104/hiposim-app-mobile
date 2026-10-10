package com.auracode.hiposim.core.network

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response

private val PROBLEM_JSON = "application/problem+json".toMediaType()

/**
 * The exception Retrofit throws for an `application/problem+json` answer of the API.
 *
 * The in-memory services use it to fail exactly like the server, so their errors go through the same
 * [safeApiCall] path as the real ones. [errors] is the `errors` map of a validation problem (400).
 */
fun problemException(
    status: Int,
    title: String,
    errors: Map<String, List<String>>? = null,
): HttpException {
    val body =
        buildJsonObject {
            put("title", title)
            put("status", status)
            if (errors != null) {
                putJsonObject("errors") {
                    errors.forEach { (field, messages) -> put(field, JsonArray(messages.map(::JsonPrimitive))) }
                }
            }
        }.toString()
    return HttpException(Response.error<Unit>(status, body.toResponseBody(PROBLEM_JSON)))
}

/** A 400 with the title the API uses for validation problems. */
fun validationProblemException(errors: Map<String, List<String>>): HttpException =
    problemException(HTTP_BAD_REQUEST, "One or more validation errors occurred.", errors)

private const val HTTP_BAD_REQUEST = 400
