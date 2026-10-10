package com.auracode.hiposim.feature.auth.data

import retrofit2.http.Body
import retrofit2.http.POST

/** Account endpoints of the HipoSim API. [FakeAuthService] stands in when `BuildConfig.USE_FAKE_API` is on. */
interface AuthService {
    /**
     * Creates a buyer account and signs it in (201). Answers 400 with field errors when the form is invalid
     * or an acceptance is missing, and 409 when the email already has an account.
     */
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequestDto,
    ): AuthResponseDto

    /** Answers 401 for wrong credentials, without saying which of the two values is wrong. */
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequestDto,
    ): AuthResponseDto
}
