package com.auracode.hiposim.feature.scenarios.data

import kotlinx.serialization.Serializable

// POST and GET /api/scenarios (US06), like the backend contract (hiposim-backEnd, openapi.v0.yaml).

@Serializable
data class CreateScenarioRequestDto(
    val name: String,
    val simulationId: String,
)

/** `ScenarioResponse` of the backend. A figure is null when the stored simulation does not carry it. */
@Serializable
data class ScenarioDto(
    val id: String,
    val name: String,
    val simulationId: String,
    val createdAt: String,
    val propertyPrice: Double? = null,
    val downPayment: Double? = null,
    val annualEffectiveRate: Double? = null,
    val termInMonths: Int? = null,
    val gracePeriodMonths: Int? = null,
    val graceType: String? = null,
    val applyGoodPayerBonus: Boolean? = null,
    val goodPayerBonus: Double? = null,
    val financedAmount: Double? = null,
    val monthlyInstallment: Double? = null,
    val totalPaid: Double? = null,
    val totalInterest: Double? = null,
    val tcea: Double? = null,
    val npv: Double? = null,
    val irr: Double? = null,
)
