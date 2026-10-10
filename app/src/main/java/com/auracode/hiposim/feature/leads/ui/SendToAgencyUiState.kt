package com.auracode.hiposim.feature.leads.ui

import androidx.compose.runtime.Immutable
import com.auracode.hiposim.feature.leads.domain.LeadFailure

@Immutable
data class SendToAgencyUiState(
    val isLoadingAgencies: Boolean = true,
    val agencies: List<AgencyUi> = emptyList(),
    /** Why the agencies could not be listed. The buyer can retry. */
    val agenciesFailure: LeadFailure? = null,
    val selectedAgencyId: String? = null,
    /** Explicit consent to share the simulation and the contact details. It never starts checked. */
    val consentAccepted: Boolean = false,
    /** Shown after trying to send without an agency or without the consent. */
    val showAgencyError: Boolean = false,
    val showConsentError: Boolean = false,
    val isSending: Boolean = false,
    /** Why the last attempt to send failed. */
    val sendFailure: LeadFailure? = null,
    /** Name of the agency that received the simulation. Not null means it was sent. */
    val sentToAgency: String? = null,
) {
    val isSent: Boolean get() = sentToAgency != null

    /** The session ended while sending: the buyer has to sign in again. */
    val isSessionExpired: Boolean get() = sendFailure == LeadFailure.SessionExpired
}

@Immutable
data class AgencyUi(
    val id: String,
    val name: String,
)
