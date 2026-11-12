package com.auracode.hiposim.feature.simulation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.core.analytics.UsageEvents
import com.auracode.hiposim.core.analytics.UsageTracker
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
        private val usageTracker: UsageTracker = UsageTracker.None,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ScheduleUiState())
        val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

        init {
            observeCurrentSimulation().value?.let { simulation ->
                usageTracker.track(
                    UsageEvents.SCHEDULE_OPENED,
                    mapOf("termYears" to simulation.termYears, "graceType" to simulation.graceType.apiValue),
                )
            }
            viewModelScope.launch {
                observeCurrentSimulation().collect { simulation ->
                    _uiState.value =
                        simulation?.toScheduleUiState()
                            ?: ScheduleUiState(isLoading = false, isSimulationMissing = true)
                }
            }
        }
    }
