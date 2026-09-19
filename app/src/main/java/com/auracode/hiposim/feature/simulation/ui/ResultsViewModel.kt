package com.auracode.hiposim.feature.simulation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.core.navigation.MainDestination
import com.auracode.hiposim.feature.auth.domain.ObserveSessionUseCase
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.auth.domain.SignOutUseCase
import com.auracode.hiposim.feature.auth.ui.AccountUi
import com.auracode.hiposim.feature.simulation.domain.GetLoanSimulationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResultsViewModel
    @Inject
    constructor(
        private val getLoanSimulation: GetLoanSimulationUseCase,
        private val observeSession: ObserveSessionUseCase,
        private val signOut: SignOutUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ResultsUiState())
        val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val summary = getLoanSimulation().toResultsSummaryUi()
                _uiState.update { it.copy(isLoading = false, summary = summary) }
            }
            viewModelScope.launch {
                observeSession().collect { session ->
                    val account =
                        (session as? SessionState.Authenticated)?.user?.let {
                            AccountUi(
                                it.fullName,
                                it.email,
                            )
                        }
                    _uiState.update { it.copy(account = account, isUnlockSheetVisible = false) }
                }
            }
        }

        /** Guests must sign up first. Signed in users get "coming soon" until the agencies feature exists. */
        fun onSendQuoteClick() = requireAccount { showComingSoon() }

        /** The PDF summary is not generated yet. */
        fun onDownloadPdfClick() = showComingSoon()

        fun onDestinationClick(destination: MainDestination) {
            when (destination) {
                MainDestination.Simulate -> Unit
                MainDestination.Profile -> requireAccount { showAccountSheet() }
                MainDestination.Realtors, MainDestination.History -> requireAccount { showComingSoon() }
            }
        }

        fun onUnlockSheetDismiss() {
            _uiState.update { it.copy(isUnlockSheetVisible = false) }
        }

        fun onAccountSheetDismiss() {
            _uiState.update { it.copy(isAccountSheetVisible = false) }
        }

        fun onSignOutClick() {
            _uiState.update { it.copy(isAccountSheetVisible = false) }
            viewModelScope.launch { signOut() }
        }

        fun onComingSoonShown() {
            _uiState.update { it.copy(showComingSoon = false) }
        }

        private fun requireAccount(whenSignedIn: () -> Unit) {
            if (_uiState.value.isAuthenticated) {
                whenSignedIn()
            } else {
                _uiState.update {
                    it.copy(
                        isUnlockSheetVisible = true,
                    )
                }
            }
        }

        private fun showComingSoon() {
            _uiState.update { it.copy(showComingSoon = true) }
        }

        private fun showAccountSheet() {
            _uiState.update { it.copy(isAccountSheetVisible = true) }
        }
    }
