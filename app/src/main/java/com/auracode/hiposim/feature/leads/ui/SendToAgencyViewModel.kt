package com.auracode.hiposim.feature.leads.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.feature.leads.domain.AgenciesResult
import com.auracode.hiposim.feature.leads.domain.GetAgenciesUseCase
import com.auracode.hiposim.feature.leads.domain.SendLeadResult
import com.auracode.hiposim.feature.leads.domain.SendSimulationToAgencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives the sheet where a signed in buyer picks an agency and authorizes sending the simulation (US21). */
@HiltViewModel
class SendToAgencyViewModel
    @Inject
    constructor(
        private val getAgencies: GetAgenciesUseCase,
        private val sendSimulation: SendSimulationToAgencyUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(SendToAgencyUiState())
        val uiState: StateFlow<SendToAgencyUiState> = _uiState.asStateFlow()

        /** Every time the sheet opens it starts clean: a consent given before is never reused. */
        fun onOpen() {
            _uiState.value = SendToAgencyUiState()
            loadAgencies()
        }

        fun onRetryAgencies() {
            _uiState.update { it.copy(isLoadingAgencies = true, agenciesFailure = null) }
            loadAgencies()
        }

        fun onAgencySelected(agencyId: String) {
            _uiState.update { it.copy(selectedAgencyId = agencyId, showAgencyError = false, sendFailure = null) }
        }

        fun onConsentChange(accepted: Boolean) {
            _uiState.update { it.copy(consentAccepted = accepted, showConsentError = false, sendFailure = null) }
        }

        /** Sends only with an agency chosen and the consent given. Otherwise it says what is missing. */
        fun onSend() {
            val state = _uiState.value
            if (state.isSending || state.isSent) return

            val agency = state.agencies.firstOrNull { it.id == state.selectedAgencyId }
            _uiState.update {
                it.copy(showAgencyError = agency == null, showConsentError = !state.consentAccepted, sendFailure = null)
            }
            if (agency == null || !state.consentAccepted) return

            _uiState.update { it.copy(isSending = true) }
            viewModelScope.launch {
                when (val result = sendSimulation(agency.id, consentAccepted = true)) {
                    is SendLeadResult.Success ->
                        _uiState.update { it.copy(isSending = false, sentToAgency = agency.name) }

                    is SendLeadResult.Failure ->
                        _uiState.update { it.copy(isSending = false, sendFailure = result.reason) }
                }
            }
        }

        private fun loadAgencies() {
            viewModelScope.launch {
                when (val result = getAgencies()) {
                    is AgenciesResult.Success -> {
                        val agencies = result.agencies.map { AgencyUi(it.id, it.name) }
                        _uiState.update {
                            it.copy(
                                isLoadingAgencies = false,
                                agencies = agencies,
                                // With a single agency there is nothing to choose. The consent is still required.
                                selectedAgencyId = agencies.singleOrNull()?.id,
                            )
                        }
                    }

                    is AgenciesResult.Failure ->
                        _uiState.update { it.copy(isLoadingAgencies = false, agenciesFailure = result.reason) }
                }
            }
        }
    }
