package com.auracode.hiposim.feature.simulation.ui

import com.auracode.hiposim.core.util.PeruFormat
import com.auracode.hiposim.feature.simulation.domain.Installment
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

private const val CENTS = 2
private const val PERCENT = 100

fun LoanSimulation.toResultsSummaryUi(): ResultsSummaryUi =
    ResultsSummaryUi(
        propertyPrice = PeruFormat.soles(propertyPrice),
        downPayment = PeruFormat.soles(downPayment),
        downPaymentPercent = PeruFormat.percent(downPaymentRate, fractionDigits = 0),
        goodPayerBonus = PeruFormat.soles(goodPayerBonus),
        netLoan = PeruFormat.soles(netLoan),
        termYears = termYears,
        termMonths = termMonths,
        monthlyPayment = PeruFormat.soles(monthlyPayment),
        monthlyInsurance = PeruFormat.soles(monthlyInsurance, fractionDigits = CENTS),
        tcea = PeruFormat.percent(tcea),
        npv = PeruFormat.soles(npv),
        irr = PeruFormat.percent(irr),
        composition = initialSchedule.first().toComposition(),
        schedule = initialSchedule.map { it.toUi() },
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

private fun Installment.toComposition(): PaymentCompositionUi {
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
