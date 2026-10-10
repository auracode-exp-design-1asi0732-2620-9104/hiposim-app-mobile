package com.auracode.hiposim.feature.auth.ui

import androidx.annotation.StringRes
import com.auracode.hiposim.R
import com.auracode.hiposim.feature.auth.domain.AuthFailure

/** Message for a failure that is not about what the buyer typed. */
@StringRes
internal fun AuthFailure.messageRes(): Int =
    when (this) {
        AuthFailure.NoConnection -> R.string.error_no_connection
        AuthFailure.ServiceUnavailable -> R.string.error_service_unavailable
        AuthFailure.Unexpected -> R.string.error_unexpected
    }
