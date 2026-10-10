package com.auracode.hiposim.feature.auth.data

import kotlinx.serialization.Serializable

// DTOs of POST /api/auth/register and POST /api/auth/login, named exactly like contract v0.1.0 (openapi.v0.yaml).

@Serializable
data class RegisterRequestDto(
    val fullName: String,
    val email: String,
    val phone: String,
    val password: String,
    /** Must be true: Terms and Conditions. */
    val acceptedTerms: Boolean,
    /** Must be true: authorization to process personal data (Ley N° 29733). */
    val acceptedDataProcessing: Boolean,
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String,
)

/** Answer of both endpoints (`LoginResponse` in the contract). */
@Serializable
data class AuthResponseDto(
    /** JWT sent later as `Authorization: Bearer <accessToken>`. */
    val accessToken: String,
    val tokenType: String,
    /** Token lifetime in seconds. */
    val expiresIn: Int,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: String,
    val fullName: String,
    val email: String,
    /** `buyer` or `advisor`. */
    val role: String,
)
