package com.auracode.hiposim.feature.simulation.domain

import com.auracode.hiposim.core.network.ApiResult
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class SimulateLoanUseCase
    @Inject
    constructor(
        private val repository: SimulationRepository,
    ) {
        suspend operator fun invoke(params: SimulationParams): ApiResult<LoanSimulation> = repository.simulate(params)
    }

class ObserveCurrentSimulationUseCase
    @Inject
    constructor(
        private val repository: SimulationRepository,
    ) {
        operator fun invoke(): StateFlow<LoanSimulation?> = repository.currentSimulation
    }
