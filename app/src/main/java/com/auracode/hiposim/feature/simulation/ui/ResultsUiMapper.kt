package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.simulation.domain.Installment
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

private const val CENTS = 2
private const val PERCENT = 100

/** Rows shown in the "initial schedule" strip. The full schedule belongs to its own screen (US02). */
private const val INITIAL_SCHEDULE_ROWS = 4

fun LoanSimulation.toResultsSummaryUi(): ResultsSummaryUi =
    ResultsSummaryUi(
        propertyPrice = PeruFormat.soles(propertyPrice),
        downPayment = PeruFormat.soles(downPayment),
        downPaymentPercent = PeruFormat.percent(downPaymentRate, fractionDigits = 0),
        bonusStatus = bonusStatus,
        goodPayerBonus = PeruFormat.soles(goodPayerBonus),
        netLoan = PeruFormat.soles(netLoan),
        termYears = termYears,
        termMonths = termMonths,
        monthlyPayment = PeruFormat.soles(monthlyPayment),
        monthlyInsurance = PeruFormat.soles(monthlyInsurance, fractionDigits = CENTS),
        totalPaid = PeruFormat.soles(totalPaid),
        totalInterest = PeruFormat.soles(totalInterest),
        tcea = PeruFormat.percent(tcea),
        npv = PeruFormat.soles(npv),
        npvDiscountRate = PeruFormat.percent(assumptions.annualDiscountRate),
        irr = PeruFormat.percent(irr),
        composition = schedule.firstOrNull().toComposition(),
        schedule = schedule.take(INITIAL_SCHEDULE_ROWS).map { it.toUi() },
    )

private fun Installment.toUi() =
    InstallmentUi(
        number = number,
        payment = PeruFormat.soles(payment, CENTS),
        principal = PeruFormat.soles(principal, CENTS),
        interest = PeruFormat.soles(interest, CENTS),
        insurance = PeruFormat.soles(insurance, CENTS),
        remainingBalance = PeruFormat.soles(remainingBalance, CENTS),
    )

/** Shares of the total payment of the month. All zero when there is nothing to split. */
private fun Installment?.toComposition(): PaymentCompositionUi {
    if (this == null || payment.signum() <= 0) return PaymentCompositionUi(0, 0, 0)
    val interestPercent = interest.percentOf(payment)
    val insurancePercent = insurance.percentOf(payment)
    return PaymentCompositionUi(
        interestPercent = interestPercent,
        principalPercent = PERCENT - interestPercent - insurancePercent,
        insurancePercent = insurancePercent,
    )
}

private fun BigDecimal.percentOf(total: BigDecimal): Int =
    multiply(BigDecimal(PERCENT))
        .divide(total, MathContext.DECIMAL64)
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()
