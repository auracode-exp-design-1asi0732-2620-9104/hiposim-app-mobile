package com.auracode.hiposim.feature.auth.domain

data class RegistrationForm(
    val fullName: String = "",
    val email: String = "",
    /** National number typed by the user, without the +51 prefix. Spaces are allowed. */
    val phone: String = "",
    val password: String = "",
    /** Explicit consent under Ley N° 29733. It must never start checked. */
    val consentAccepted: Boolean = false,
)

enum class RegistrationField { FullName, Email, Phone, Password, Consent }

enum class RegistrationError {
    NameRequired,
    EmailInvalid,
    PhoneInvalid,
    PasswordTooShort,
    ConsentRequired,
}

data class RegistrationValidation(
    val errors: Map<RegistrationField, RegistrationError>,
) {
    val isValid: Boolean get() = errors.isEmpty()
}
