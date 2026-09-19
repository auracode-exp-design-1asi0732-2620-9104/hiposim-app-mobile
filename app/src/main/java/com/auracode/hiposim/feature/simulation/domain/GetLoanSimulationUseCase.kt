package com.auracode.hiposim.feature.simulation.domain

import javax.inject.Inject

class GetLoanSimulationUseCase
    @Inject
    constructor(
        private val repository: SimulationRepository,
    ) {
        suspend operator fun invoke(): LoanSimulation = repository.getCurrentSimulation()
    }
