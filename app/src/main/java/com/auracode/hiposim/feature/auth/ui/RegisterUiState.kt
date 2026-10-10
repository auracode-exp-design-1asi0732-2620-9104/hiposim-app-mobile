package com.auracode.hiposim.feature.auth.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.feature.auth.domain.AuthFailure
import com.auracode.hiposim.feature.auth.domain.RegistrationError
import com.auracode.hiposim.feature.auth.domain.RegistrationField
import com.auracode.hiposim.feature.auth.domain.RegistrationForm

@Immutable
data class RegisterUiState(
    val form: RegistrationForm = RegistrationForm(),
    val isPasswordVisible: Boolean = false,
    /** Errors are hidden until the user tries to submit, then they follow the edits. */
    val errors: Map<RegistrationField, RegistrationError> = emptyMap(),
    /** Messages the API attached to a field (400). Each one is cleared when its field is edited. */
    val serverErrors: Map<RegistrationField, String> = emptyMap(),
    /** A failure that does not belong to a single field. */
    val failure: AuthFailure? = null,
    val quote: QuoteSummaryUi? = null,
    val isSubmitting: Boolean = false,
    /** The account was created and the user is signed in, so the screen should close. */
    val isRegistered: Boolean = false,
)

/** The quote the user is about to send, shown at the top of the form. */
@Immutable
data class QuoteSummaryUi(
    val monthlyPayment: String,
    val tcea: String,
    val propertyPrice: String,
)
