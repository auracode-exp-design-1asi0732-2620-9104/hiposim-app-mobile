package com.auracode.hiposim.feature.auth.domain

import javax.inject.Inject

class ValidateRegistrationFormUseCase
    @Inject
    constructor() {
        operator fun invoke(form: RegistrationForm): RegistrationValidation {
            val phoneDigits = form.phone.filterNot { it.isWhitespace() || it == '-' }
            val errors =
                buildMap {
                    if (form.fullName.trim().length < MIN_NAME_LENGTH) {
                        put(RegistrationField.FullName, RegistrationError.NameRequired)
                    }
                    if (!EMAIL_REGEX.matches(form.email.trim())) {
                        put(RegistrationField.Email, RegistrationError.EmailInvalid)
                    }
                    if (!MOBILE_REGEX.matches(phoneDigits)) {
                        put(RegistrationField.Phone, RegistrationError.PhoneInvalid)
                    }
                    if (form.password.length < MIN_PASSWORD_LENGTH) {
                        put(RegistrationField.Password, RegistrationError.PasswordTooShort)
                    }
                    if (!form.consentAccepted) {
                        put(RegistrationField.Consent, RegistrationError.ConsentRequired)
                    }
                }
            return RegistrationValidation(errors)
        }

        private companion object {
            const val MIN_NAME_LENGTH = 2
            const val MIN_PASSWORD_LENGTH = 8
            val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

            /** Peruvian mobile numbers have 9 digits and start with 9. */
            val MOBILE_REGEX = Regex("^9\\d{8}$")
        }
    }
