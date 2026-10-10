package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.feature.simulation.domain.Installment
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation
import com.auracode.hiposim.feature.simulation.domain.SimulationRequest
import java.math.BigDecimal
import java.math.RoundingMode

/** The Results screen shows the first year of the schedule. The API returns every installment. */
internal const val INITIAL_SCHEDULE_ROWS = 12

private const val RATE_SCALE = 6

internal fun SimulationRequest.toDto(): SimulationRequestDto =
    SimulationRequestDto(
        propertyPrice = propertyPrice.toDouble(),
        downPayment = downPayment.toDouble(),
        annualEffectiveRate = annualEffectiveRate.toDouble(),
        termInMonths = termMonths,
        applyGoodPayerBonus = applyGoodPayerBonus,
    )

/** Returns null when the API sent no installments, which a valid simulation always has. */
internal fun SimulationResponseDto.toDomainOrNull(request: SimulationRequest): LoanSimulation? {
    val first = amortizationSchedule.firstOrNull() ?: return null
    return LoanSimulation(
        propertyPrice = request.propertyPrice,
        downPayment = request.downPayment,
        downPaymentRate = request.downPayment.divide(request.propertyPrice, RATE_SCALE, RoundingMode.HALF_UP),
        goodPayerBonus = benefit.appliedAmount.toDecimal(),
        netLoan = financedAmount.toDecimal(),
        termYears = request.termYears,
        monthlyPayment = first.totalPayment.toDecimal(),
        monthlyInsurance = first.insurance(),
        tcea = tcea.toDecimal(),
        npv = npv.toDecimal(),
        irr = irr.toDecimal(),
        initialSchedule = amortizationSchedule.take(INITIAL_SCHEDULE_ROWS).map { it.toDomain() },
        simulationId = simulationId,
    )
}

private fun AmortizationEntryDto.toDomain(): Installment =
    Installment(
        number = period,
        payment = totalPayment.toDecimal(),
        principal = amortization.toDecimal(),
        interest = interest.toDecimal(),
        insurance = insurance(),
        remainingBalance = remainingBalance.toDecimal(),
    )

/** `BigDecimal.valueOf` keeps the shortest decimal text of the double (1863.99 stays 1863.99). */
private fun Double.toDecimal(): BigDecimal = BigDecimal.valueOf(this)

/** Both insurances are added as decimals, not doubles, so 20.1 + 35.2 stays 55.3. */
private fun AmortizationEntryDto.insurance(): BigDecimal =
    propertyInsurance.toDecimal().add(creditLifeInsurance.toDecimal())
