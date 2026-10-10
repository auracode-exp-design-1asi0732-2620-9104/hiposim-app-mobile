package com.auracode.hiposim.feature.auth.domain

data class RegistrationForm(
    val fullName: String = "",
    val email: String = "",
    /** National number typed by the user, without the +51 prefix. Spaces are allowed. */
    val phone: String = "",
    val password: String = "",
    /** Acceptance of the Terms and Conditions. It must never start checked. */
    val termsAccepted: Boolean = false,
    /** Explicit consent to process personal data under Ley N° 29733. It must never start checked. */
    val consentAccepted: Boolean = false,
)

/** Form fields that can show an error. [requestProperty] is the name the API uses in its validation errors. */
enum class RegistrationField(
    val requestProperty: String,
) {
    FullName("fullName"),
    Email("email"),
    Phone("phone"),
    Password("password"),
    Terms("acceptedTerms"),
    Consent("acceptedDataProcessing"),
    ;

    companion object {
        fun fromRequestProperty(name: String): RegistrationField? = entries.firstOrNull { it.requestProperty == name }
    }
}

enum class RegistrationError {
    NameRequired,
    EmailInvalid,
    PhoneInvalid,
    PasswordTooShort,
    TermsRequired,
    ConsentRequired,

    /** Reported by the repository after submitting, not by local validation. */
    EmailAlreadyRegistered,
}

data class RegistrationValidation(
    val errors: Map<RegistrationField, RegistrationError>,
) {
    val isValid: Boolean get() = errors.isEmpty()
}
