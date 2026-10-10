package com.auracode.hiposim.feature.simulation.domain

import javax.inject.Inject

class SimulateLoanUseCase
    @Inject
    constructor(
        private val repository: SimulationRepository,
    ) {
        suspend operator fun invoke(request: SimulationRequest): SimulationResult = repository.simulate(request)
    }
