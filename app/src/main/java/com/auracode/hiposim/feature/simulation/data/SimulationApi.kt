package com.auracode.hiposim.feature.simulation.data

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/** Simulation endpoints of the HipoSim API (docs/api-contract/openapi.v0.yaml in the backend repository). */
interface SimulationApi {
    /** `POST /api/simulations`. Authentication is optional. */
    @POST("api/simulations")
    suspend fun simulate(
        @Body request: SimulationRequestDto,
    ): Response<SimulationResponseDto>
}

/** Body of `POST /api/simulations`. The rate is a fraction: `0.0885` means 8.85%. */
@Serializable
data class SimulationRequestDto(
    val propertyPrice: Double,
    val downPayment: Double,
    val annualEffectiveRate: Double,
    val termInMonths: Int,
    val applyGoodPayerBonus: Boolean,
)

/** Only the response fields the app uses. The API sends more (totals, assumptions); they are ignored. */
@Serializable
data class SimulationResponseDto(
    val simulationId: String,
    val financedAmount: Double,
    val monthlyInstallment: Double,
    val tcea: Double,
    val irr: Double,
    val npv: Double,
    val benefit: BenefitDto,
    val amortizationSchedule: List<AmortizationEntryDto>,
)

@Serializable
data class BenefitDto(
    val name: String,
    val eligible: Boolean,
    val appliedAmount: Double,
)

@Serializable
data class AmortizationEntryDto(
    val period: Int,
    val interest: Double,
    val amortization: Double,
    val remainingBalance: Double,
    val propertyInsurance: Double,
    val creditLifeInsurance: Double,
    /** Installment plus both insurances: what the buyer pays that month. */
    val totalPayment: Double,
)
