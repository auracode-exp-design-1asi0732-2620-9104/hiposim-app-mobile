package com.auracode.hiposim.feature.simulation.domain

import java.math.BigDecimal

/** Result of a mortgage simulation for a first home purchase. Amounts are in soles (PEN). */
data class LoanSimulation(
    val propertyPrice: BigDecimal,
    val downPayment: BigDecimal,
    /** Down payment as a fraction of [propertyPrice], `0.15` for 15%. */
    val downPaymentRate: BigDecimal,
    /** Bono del Buen Pagador applied to the purchase. */
    val goodPayerBonus: BigDecimal,
    val netLoan: BigDecimal,
    val termYears: Int,
    val monthlyPayment: BigDecimal,
    val monthlyInsurance: BigDecimal,
    /** Annual effective total cost rate as a fraction, `0.0885` for 8.85%. */
    val tcea: BigDecimal,
    /** Net present value of the loan. */
    val npv: BigDecimal,
    /** Internal rate of return as a fraction. */
    val irr: BigDecimal,
    val initialSchedule: List<Installment>,
) {
    val termMonths: Int get() = termYears * MONTHS_PER_YEAR

    private companion object {
        const val MONTHS_PER_YEAR = 12
    }
}

/** One row of the amortization schedule. [payment] includes [insurance]. */
data class Installment(
    val number: Int,
    val payment: BigDecimal,
    val principal: BigDecimal,
    val interest: BigDecimal,
    val insurance: BigDecimal,
    val remainingBalance: BigDecimal,
)
