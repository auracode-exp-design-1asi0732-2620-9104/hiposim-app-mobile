package com.auracode.hiposim.feature.simulation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.auracode.hiposim.feature.simulation.domain.GetLoanSimulationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel
@Inject
constructor(
    private val getLoanSimulation: GetLoanSimulationUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ScheduleUiState())
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val simulation = getLoanSimulation()
            _uiState.value =
                ScheduleUiState(
                    isLoading = false,
                    schedule = simulation.initialSchedule.toDetailedScheduleUi(simulation.netLoan),
                )
        }
    }
}

@androidx.compose.runtime.Immutable
data class ScheduleUiState(
    val isLoading: Boolean = true,
    val schedule: List<ScheduleInstallmentUi> = emptyList(),
)
