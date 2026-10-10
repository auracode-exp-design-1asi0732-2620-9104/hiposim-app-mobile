package com.auracode.hiposim.feature.simulation.data

import kotlinx.serialization.Serializable

// DTOs of POST /api/simulations, named exactly like contract v0.1.0 (openapi.v0.yaml).
// Amounts are PEN with 2 decimals; rates are fractions with 6 decimals (0.085 means 8.5%).

@Serializable
data class SimulationRequestDto(
    val propertyPrice: Double,
    val downPayment: Double,
    val annualEffectiveRate: Double,
    val termInMonths: Int,
    val applyGoodPayerBonus: Boolean,
    // Optional in the contract and not used by the app in this delivery (US03). Null fields are not sent.
    val gracePeriodMonths: Int? = null,
    val graceType: String? = null,
    val startDate: String? = null,
)

@Serializable
data class SimulationResponseDto(
    val simulationId: String,
    val financedAmount: Double,
    val monthlyInstallment: Double,
    val totalPaid: Double,
    val totalInterest: Double,
    val additionalCosts: Double,
    val tcea: Double,
    val irr: Double,
    val npv: Double,
    val benefit: BenefitDto,
    val assumptions: AssumptionsDto,
    val amortizationSchedule: List<AmortizationEntryDto>,
)

@Serializable
data class BenefitDto(
    val name: String,
    val eligible: Boolean,
    val appliedAmount: Double,
)

@Serializable
data class AssumptionsDto(
    val monthlyPropertyInsurance: Double,
    val monthlyCreditLifeInsurance: Double,
    val administrativeExpenses: Double,
    val annualDiscountRate: Double,
)

@Serializable
data class AmortizationEntryDto(
    val period: Int,
    val paymentDate: String,
    val openingBalance: Double,
    val installment: Double,
    val interest: Double,
    val amortization: Double,
    val remainingBalance: Double,
    val propertyInsurance: Double,
    val creditLifeInsurance: Double,
    val totalPayment: Double,
    val isGracePeriod: Boolean,
)
