package com.auracode.hiposim.feature.simulation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.core.navigation.MainDestination
import com.auracode.hiposim.feature.auth.domain.ObserveSessionUseCase
import com.auracode.hiposim.feature.auth.domain.SessionState
import com.auracode.hiposim.feature.auth.domain.SignOutUseCase
import com.auracode.hiposim.feature.auth.ui.AccountUi
import com.auracode.hiposim.feature.report.domain.ExportReportResult
import com.auracode.hiposim.feature.report.domain.ExportSimulationReportUseCase
import com.auracode.hiposim.feature.scenarios.domain.MAX_SCENARIO_NAME_LENGTH
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioResult
import com.auracode.hiposim.feature.scenarios.domain.SaveScenarioUseCase
import com.auracode.hiposim.feature.simulation.domain.ObserveCurrentSimulationUseCase
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
        private val observeCurrentSimulation: ObserveCurrentSimulationUseCase,
        private val observeSession: ObserveSessionUseCase,
        private val signOut: SignOutUseCase,
        private val saveScenario: SaveScenarioUseCase,
        private val exportReport: ExportSimulationReportUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ResultsUiState())
        val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                observeCurrentSimulation().collect { simulation ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            summary = simulation?.toResultsSummaryUi(),
                            isSimulationMissing = simulation == null,
                        )
                    }
                }
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

        /** Guests must sign up or sign in first. Signed in buyers choose an agency and authorize the sending. */
        fun onSendQuoteClick() = requireAccount { showSendSheet() }

        /** Downloads the PDF report of the simulation (US08). Guests must sign up or sign in first. */
        fun onDownloadPdfClick() = requireAccount { exportPdf() }

        fun onDestinationClick(destination: MainDestination) {
            when (destination) {
                MainDestination.Simulate -> Unit
                MainDestination.Profile -> requireAccount { showAccountSheet() }
                MainDestination.Realtors -> requireAccount { showSendSheet() }
                MainDestination.History -> requireAccount { openScenarios() }
            }
        }

        fun onUnlockSheetDismiss() {
            _uiState.update { it.copy(isUnlockSheetVisible = false) }
        }

        fun onSendSheetDismiss() {
            _uiState.update { it.copy(isSendSheetVisible = false) }
        }

        fun onAccountSheetDismiss() {
            _uiState.update { it.copy(isAccountSheetVisible = false) }
        }

        fun onSignOutClick() {
            _uiState.update { it.copy(isAccountSheetVisible = false) }
            viewModelScope.launch { signOut() }
        }

        /** Opens the save dialog with [suggestedName] (US06). Guests must sign up or sign in first. */
        fun onSaveScenarioClick(suggestedName: String) =
            requireAccount {
                if (_uiState.value.summary != null) {
                    _uiState.update {
                        it.copy(
                            saveScenario = SaveScenarioUi(name = suggestedName.take(MAX_SCENARIO_NAME_LENGTH)),
                        )
                    }
                }
            }

        fun onScenarioNameChange(name: String) {
            _uiState.update { state ->
                state.copy(
                    saveScenario = state.saveScenario?.copy(name = name.take(MAX_SCENARIO_NAME_LENGTH), failure = null),
                )
            }
        }

        fun onSaveScenarioDismiss() {
            if (_uiState.value.saveScenario?.isSaving == true) return
            _uiState.update { it.copy(saveScenario = null) }
        }

        fun onSaveScenarioConfirm() {
            val dialog = _uiState.value.saveScenario ?: return
            if (dialog.isSaving) return
            _uiState.update { it.copy(saveScenario = dialog.copy(isSaving = true, failure = null)) }
            viewModelScope.launch {
                when (val result = saveScenario(dialog.name)) {
                    is SaveScenarioResult.Success ->
                        _uiState.update { it.copy(saveScenario = null, scenarioSaved = true) }

                    is SaveScenarioResult.Failure ->
                        _uiState.update { state ->
                            state.copy(
                                saveScenario = state.saveScenario?.copy(isSaving = false, failure = result.reason),
                            )
                        }
                }
            }
        }

        fun onScenarioSavedShown() {
            _uiState.update { it.copy(scenarioSaved = false) }
        }

        /** From the confirmation after saving. */
        fun onViewScenariosClick() = requireAccount { openScenarios() }

        fun onScenariosNavigated() {
            _uiState.update { it.copy(navigateToScenarios = false) }
        }

        fun onPdfReportHandled() {
            _uiState.update { it.copy(pdfReport = null) }
        }

        fun onPdfFailureShown() {
            _uiState.update { it.copy(pdfFailure = null) }
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

        private fun openScenarios() {
            _uiState.update { it.copy(navigateToScenarios = true) }
        }

        /** Nothing is downloaded without a simulation on screen, and a second tap while one is under way is ignored. */
        private fun exportPdf() {
            val state = _uiState.value
            if (state.summary == null || state.isExportingPdf) return
            _uiState.update { it.copy(isExportingPdf = true, pdfFailure = null) }
            viewModelScope.launch {
                when (val result = exportReport()) {
                    is ExportReportResult.Success ->
                        _uiState.update { it.copy(isExportingPdf = false, pdfReport = result.file) }

                    is ExportReportResult.Failure ->
                        _uiState.update { it.copy(isExportingPdf = false, pdfFailure = result.reason) }
                }
            }
        }

        /** Nothing can be sent without a simulation on screen. */
        private fun showSendSheet() {
            if (_uiState.value.summary != null) _uiState.update { it.copy(isSendSheetVisible = true) }
        }

        private fun showAccountSheet() {
            _uiState.update { it.copy(isAccountSheetVisible = true) }
        }
    }
