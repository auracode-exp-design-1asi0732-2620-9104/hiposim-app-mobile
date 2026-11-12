package com.auracode.hiposim.feature.simulation.domain

import com.auracode.hiposim.core.analytics.UsageEvents
import com.auracode.hiposim.core.analytics.UsageTracker
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class SimulateLoanUseCase
    @Inject
    constructor(
        private val repository: SimulationRepository,
        private val usageTracker: UsageTracker = UsageTracker.None,
    ) {
        suspend operator fun invoke(params: SimulationParams): SimulationResult {
            val result = repository.simulate(params)
            if (result is SimulationResult.Success) {
                usageTracker.track(UsageEvents.SIMULATION_COMPLETED, params.usageProperties())
            }
            return result
        }
    }

/** What the experiments need to know about a simulation, without amounts that could identify the buyer. */
internal fun SimulationParams.usageProperties(): Map<String, Any> =
    mapOf(
        "termYears" to termYears,
        "graceType" to graceType.apiValue,
        "graceMonths" to graceMonths,
        "goodPayerBonus" to applyGoodPayerBonus,
        "downPaymentPercent" to downPaymentPercentRounded(),
    )

/** Down payment as a whole percentage of the price, 0 when the price is not positive. */
private fun SimulationParams.downPaymentPercentRounded(): Int =
    if (propertyPrice.signum() <= 0) {
        0
    } else {
        downPayment
            .multiply(java.math.BigDecimal(100))
            .divide(propertyPrice, 0, java.math.RoundingMode.HALF_UP)
            .toInt()
    }

class ObserveCurrentSimulationUseCase
    @Inject
    constructor(
        private val repository: SimulationRepository,
    ) {
        operator fun invoke(): StateFlow<LoanSimulation?> = repository.currentSimulation
    }
