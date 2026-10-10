package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.feature.simulation.domain.Installment
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation
import com.auracode.hiposim.feature.simulation.domain.SimulationAssumptions
import com.auracode.hiposim.feature.simulation.domain.SimulationParams
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private const val MONEY_SCALE = 2
private const val RATE_SCALE = 6

fun SimulationParams.toRequestDto(): SimulationRequestDto =
    SimulationRequestDto(
        propertyPrice = propertyPrice.toDouble(),
        downPayment = downPayment.toDouble(),
        annualEffectiveRate = annualEffectiveRate.toDouble(),
        termInMonths = termMonths,
        applyGoodPayerBonus = applyGoodPayerBonus,
    )

/**
 * The response does not echo the request, so the price, down payment and term come from [params].
 *
 * Contract name to model name: `financedAmount` is [LoanSimulation.netLoan], `monthlyInstallment` is
 * [LoanSimulation.monthlyPayment], `benefit.appliedAmount` is [LoanSimulation.goodPayerBonus]; in each row
 * `amortization` is [Installment.principal] and `totalPayment` is [Installment.payment].
 *
 * Returns null when the API sent no installments.
 */
fun SimulationResponseDto.toDomainOrNull(params: SimulationParams): LoanSimulation? {
    // A valid simulation always has installments: without them the answer is not usable.
    if (amortizationSchedule.isEmpty()) return null
    return LoanSimulation(
        id = simulationId,
        propertyPrice = params.propertyPrice,
        downPayment = params.downPayment,
        termMonths = params.termMonths,
        annualEffectiveRate = params.annualEffectiveRate,
        bonusRequested = params.applyGoodPayerBonus,
        bonusEligible = benefit.eligible,
        goodPayerBonus = benefit.appliedAmount.toMoney(),
        netLoan = financedAmount.toMoney(),
        monthlyPayment = monthlyInstallment.toMoney(),
        totalPaid = totalPaid.toMoney(),
        totalInterest = totalInterest.toMoney(),
        additionalCosts = additionalCosts.toMoney(),
        tcea = tcea.toRate(),
        npv = npv.toMoney(),
        irr = irr.toRate(),
        assumptions = assumptions.toDomain(),
        schedule = amortizationSchedule.map { it.toDomain() },
    )
}

private fun AssumptionsDto.toDomain() =
    SimulationAssumptions(
        monthlyPropertyInsurance = monthlyPropertyInsurance.toMoney(),
        monthlyCreditLifeInsurance = monthlyCreditLifeInsurance.toMoney(),
        administrativeExpenses = administrativeExpenses.toMoney(),
        annualDiscountRate = annualDiscountRate.toRate(),
    )

private fun AmortizationEntryDto.toDomain() =
    Installment(
        number = period,
        paymentDate = LocalDate.parse(paymentDate),
        openingBalance = openingBalance.toMoney(),
        installment = installment.toMoney(),
        principal = amortization.toMoney(),
        interest = interest.toMoney(),
        propertyInsurance = propertyInsurance.toMoney(),
        creditLifeInsurance = creditLifeInsurance.toMoney(),
        payment = totalPayment.toMoney(),
        remainingBalance = remainingBalance.toMoney(),
        isGracePeriod = isGracePeriod,
    )

// BigDecimal.valueOf uses the shortest decimal text of the double, so 1863.99 stays 1863.99.
private fun Double.toMoney(): BigDecimal = BigDecimal.valueOf(this).setScale(MONEY_SCALE, RoundingMode.HALF_UP)

private fun Double.toRate(): BigDecimal = BigDecimal.valueOf(this).setScale(RATE_SCALE, RoundingMode.HALF_UP)
