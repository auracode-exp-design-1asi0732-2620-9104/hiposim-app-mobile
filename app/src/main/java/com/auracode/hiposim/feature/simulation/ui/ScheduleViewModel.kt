package com.auracode.hiposim.feature.simulation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.feature.simulation.domain.ObserveCurrentSimulationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shows the schedule of the simulation that is already in memory: it makes no request of its own. */
@HiltViewModel
class ScheduleViewModel
    @Inject
    constructor(
        private val observeCurrentSimulation: ObserveCurrentSimulationUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ScheduleUiState())
        val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                observeCurrentSimulation().collect { simulation ->
                    _uiState.value =
                        simulation?.toScheduleUiState()
                            ?: ScheduleUiState(isLoading = false, isSimulationMissing = true)
                }
            }
        }
    }
