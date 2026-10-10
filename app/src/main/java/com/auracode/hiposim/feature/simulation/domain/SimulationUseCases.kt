package com.auracode.hiposim.feature.simulation.domain

import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class SimulateLoanUseCase
    @Inject
    constructor(
        private val repository: SimulationRepository,
    ) {
        suspend operator fun invoke(params: SimulationParams): SimulationResult = repository.simulate(params)
    }

class ObserveCurrentSimulationUseCase
    @Inject
    constructor(
        private val repository: SimulationRepository,
    ) {
        operator fun invoke(): StateFlow<LoanSimulation?> = repository.currentSimulation
    }
