package com.auracode.hiposim.feature.scenarios.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.feature.scenarios.domain.GetScenariosUseCase
import com.auracode.hiposim.feature.scenarios.domain.MAX_COMPARED_SCENARIOS
import com.auracode.hiposim.feature.scenarios.domain.ScenariosResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Reads the buyer's saved scenarios when the screen opens, and again on demand. It also keeps the scenarios the
 * buyer picked to compare (US07), so the choice survives rotating the phone or reading the list again.
 */
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

        /** Picks or drops a scenario to compare. At most [MAX_COMPARED_SCENARIOS] can be picked at once. */
        fun onToggleSelection(id: String) {
            _uiState.update { state ->
                val picked = state.selectedIds
                when {
                    id in picked -> state.copy(selectedIds = picked - id)
                    picked.size >= MAX_COMPARED_SCENARIOS -> state
                    state.scenarios.none { it.id == id } -> state
                    else -> state.copy(selectedIds = picked + id)
                }
            }
        }

        private fun load() {
            val previouslyPicked = _uiState.value.selectedIds
            _uiState.value = ScenariosUiState(isLoading = true)
            viewModelScope.launch {
                _uiState.value =
                    when (val result = getScenarios()) {
                        is ScenariosResult.Success -> {
                            val scenarios = result.scenarios.map { it.toUi() }
                            ScenariosUiState(
                                isLoading = false,
                                scenarios = scenarios,
                                // A scenario that is no longer in the list cannot stay picked.
                                selectedIds = previouslyPicked.filter { id -> scenarios.any { it.id == id } },
                            )
                        }

                        is ScenariosResult.Failure -> ScenariosUiState(isLoading = false, failure = result.reason)
                    }
            }
        }
    }
