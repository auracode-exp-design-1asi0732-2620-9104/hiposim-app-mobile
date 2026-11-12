package com.auracode.hiposim.feature.scenarios.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.core.navigation.Route
import com.auracode.hiposim.feature.scenarios.domain.CompareScenariosResult
import com.auracode.hiposim.feature.scenarios.domain.CompareScenariosUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Builds the side by side comparison of the scenarios the buyer picked (US07), which arrive in the route. */
@HiltViewModel
class CompareScenariosViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val compareScenarios: CompareScenariosUseCase,
    ) : ViewModel() {
        private val ids: List<String> =
            Route.CompareScenarios.splitIds(
                savedStateHandle.get<String>(Route.CompareScenarios.ARG_IDS),
            )

        private val _uiState = MutableStateFlow(CompareScenariosUiState())
        val uiState: StateFlow<CompareScenariosUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun onRetry() = load()

        private fun load() {
            _uiState.value = CompareScenariosUiState(isLoading = true)
            viewModelScope.launch {
                _uiState.value =
                    when (val result = compareScenarios(ids)) {
                        is CompareScenariosResult.Success ->
                            CompareScenariosUiState(isLoading = false, comparison = result.comparison.toUi())

                        is CompareScenariosResult.Failure ->
                            CompareScenariosUiState(isLoading = false, failure = result.reason)
                    }
            }
        }
    }
