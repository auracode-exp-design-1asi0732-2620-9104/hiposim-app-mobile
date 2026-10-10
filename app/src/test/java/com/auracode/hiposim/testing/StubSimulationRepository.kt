package com.auracode.hiposim.testing

import com.auracode.hiposim.feature.simulation.domain.Installment
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import com.auracode.hiposim.feature.simulation.domain.SimulationRequest
import com.auracode.hiposim.feature.simulation.domain.SimulationResult
import kotlinx.coroutines.CompletableDeferred
import java.math.BigDecimal

/** Repository whose answers are set by each test. [gate] holds `simulate` until the test completes it. */
class StubSimulationRepository(
    var simulateResult: SimulationResult = SimulationResult.Success(sampleSimulation()),
    var current: LoanSimulation? = sampleSimulation(),
) : SimulationRepository {
    val requests = mutableListOf<SimulationRequest>()
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun simulate(request: SimulationRequest): SimulationResult {
        requests += request
        gate?.await()
        return simulateResult
    }

    override suspend fun getCurrentSimulation(): LoanSimulation? = current
}

/** Same figures as the contract example: S/ 280,000, 15% down payment, 20 years, with the 18,200 bonus. */
fun sampleSimulation(goodPayerBonus: BigDecimal = BigDecimal("18200")): LoanSimulation =
    LoanSimulation(
        propertyPrice = BigDecimal("280000"),
        downPayment = BigDecimal("42000"),
        downPaymentRate = BigDecimal("0.15"),
        goodPayerBonus = goodPayerBonus,
        netLoan = BigDecimal("238000").subtract(goodPayerBonus),
        termYears = 20,
        monthlyPayment = BigDecimal("1863.99"),
        monthlyInsurance = BigDecimal.ZERO,
        tcea = BigDecimal("0.085"),
        npv = BigDecimal("-20791.75"),
        irr = BigDecimal("0.085"),
        initialSchedule =
            listOf(
                Installment(
                    number = 1,
                    payment = BigDecimal("1863.99"),
                    principal = BigDecimal("364.63"),
                    interest = BigDecimal("1499.36"),
                    insurance = BigDecimal.ZERO,
                    remainingBalance = BigDecimal("219435.37"),
                ),
            ),
        simulationId = "3f9b8c2e-6a1d-4c58-9e0f-7d2b5a1c4e90",
    )
