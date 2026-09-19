package com.auracode.hiposim.feature.simulation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ResultsUiState())
        val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                val summary = getLoanSimulation().toResultsSummaryUi()
                _uiState.update { it.copy(isLoading = false, summary = summary) }
            }
        }

        /** Guests must sign up before sending the quote, so this opens the unlock sheet. */
        fun onSendQuoteClick() = showUnlockSheet()

        /** Realtors, History and Profile need an account. */
        fun onLockedDestinationClick() = showUnlockSheet()

        fun onUnlockSheetDismiss() {
            _uiState.update { it.copy(isUnlockSheetVisible = false) }
        }

        private fun showUnlockSheet() {
            _uiState.update { it.copy(isUnlockSheetVisible = true) }
        }
    }
