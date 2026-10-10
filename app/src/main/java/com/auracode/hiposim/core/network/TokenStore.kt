package com.auracode.hiposim.core.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the JWT of the signed in user, in memory only.
 *
 * Nothing sets it yet: the sign in and registration repositories will store the token here when the API exists,
 * and clear it on sign out. [AuthInterceptor] reads it on every request.
 */
@Singleton
class TokenStore
    @Inject
    constructor() {
        @Volatile
        var token: String? = null

        fun clear() {
            token = null
        }
    }
