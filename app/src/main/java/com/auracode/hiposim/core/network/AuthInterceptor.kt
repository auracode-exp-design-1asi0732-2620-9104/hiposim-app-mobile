package com.auracode.hiposim.core.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Adds `Authorization: Bearer <token>` when there is a session. Guests go through untouched. */
class AuthInterceptor
    @Inject
    constructor(
        private val tokenStore: TokenStore,
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = tokenStore.token
            val request =
                if (token.isNullOrBlank()) {
                    chain.request()
                } else {
                    chain
                        .request()
                        .newBuilder()
                        .header(AUTHORIZATION, "$BEARER $token")
                        .build()
                }
            return chain.proceed(request)
        }

        companion object {
            const val AUTHORIZATION = "Authorization"
            private const val BEARER = "Bearer"
        }
    }
