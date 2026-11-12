package com.auracode.hiposim.feature.scenarios.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.feature.scenarios.domain.GetScenariosUseCase
import com.auracode.hiposim.feature.scenarios.domain.ScenariosResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Reads the buyer's saved scenarios when the screen opens, and again on demand. */
@HiltViewModel
class ScenariosViewModel
    @Inject
    constructor(
        private val getScenarios: GetScenariosUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ScenariosUiState())
        val uiState: StateFlow<ScenariosUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun onRetry() = load()

        private fun load() {
            _uiState.value = ScenariosUiState(isLoading = true)
            viewModelScope.launch {
                _uiState.value =
                    when (val result = getScenarios()) {
                        is ScenariosResult.Success ->
                            ScenariosUiState(isLoading = false, scenarios = result.scenarios.map { it.toUi() })

                        is ScenariosResult.Failure -> ScenariosUiState(isLoading = false, failure = result.reason)
                    }
            }
        }
    }
