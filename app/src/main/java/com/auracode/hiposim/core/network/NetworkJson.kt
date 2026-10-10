package com.auracode.hiposim.core.network

import kotlinx.serialization.json.Json

/**
 * JSON settings shared by every API call. Unknown fields are ignored so a newer API version does not break the app,
 * and null fields are left out of requests (the API treats a missing optional field as "not sent").
 */
val NetworkJson: Json =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }
