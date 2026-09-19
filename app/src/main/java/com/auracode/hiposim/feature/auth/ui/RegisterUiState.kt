package com.auracode.hiposim.feature.auth.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.feature.auth.domain.RegistrationError
import com.auracode.hiposim.feature.auth.domain.RegistrationField
import com.auracode.hiposim.feature.auth.domain.RegistrationForm

@Immutable
data class RegisterUiState(
    val form: RegistrationForm = RegistrationForm(),
    val isPasswordVisible: Boolean = false,
    /** Errors are hidden until the user tries to submit, then they follow the edits. */
    val errors: Map<RegistrationField, RegistrationError> = emptyMap(),
    val quote: QuoteSummaryUi? = null,
    /** One-off message request, cleared by [RegisterViewModel.onComingSoonShown]. */
    val showComingSoon: Boolean = false,
)

/** The quote the user is about to send, shown at the top of the form. */
@Immutable
data class QuoteSummaryUi(
    val monthlyPayment: String,
    val tcea: String,
    val propertyPrice: String,
)
