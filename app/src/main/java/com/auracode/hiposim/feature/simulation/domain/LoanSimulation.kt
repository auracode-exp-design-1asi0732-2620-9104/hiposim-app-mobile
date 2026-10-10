package com.auracode.hiposim.feature.simulation.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Result of a mortgage simulation for a first home purchase. Amounts are in soles (PEN), rates are fractions.
 *
 * The first block echoes what the buyer asked for; the rest comes from the API (`POST /api/simulations`).
 */
data class LoanSimulation(
    /** `simulationId` of the API, used later to send the simulation to a real estate agency. */
    val id: String,
    val propertyPrice: BigDecimal,
    val downPayment: BigDecimal,
    val termMonths: Int,
    /** Effective annual rate used, `0.085` for 8.5%. */
    val annualEffectiveRate: BigDecimal,
    /** The buyer asked for the Bono del Buen Pagador. */
    val bonusRequested: Boolean,
    /** The property price is inside a bonus range, even when the bonus was not requested. */
    val bonusEligible: Boolean,
    /** Bonus subtracted from the loan. Zero when it was not requested or the property does not qualify. */
    val goodPayerBonus: BigDecimal,
    /** Financed amount: [propertyPrice] minus [downPayment] minus [goodPayerBonus]. */
    val netLoan: BigDecimal,
    /** Regular monthly installment: principal plus interest, WITHOUT insurance. */
    val monthlyPayment: BigDecimal,
    /** Sum of every installment, insurance included. */
    val totalPaid: BigDecimal,
    val totalInterest: BigDecimal,
    /** Insurance paid during the whole term plus administrative expenses. */
    val additionalCosts: BigDecimal,
    /** Annual effective total cost rate as a fraction, `0.0885` for 8.85%. */
    val tcea: BigDecimal,
    /** Net present value from the lender's side, so it is usually negative. See [SimulationAssumptions]. */
    val npv: BigDecimal,
    /** Annual internal rate of return as a fraction. */
    val irr: BigDecimal,
    val assumptions: SimulationAssumptions,
    /** Full amortization schedule, one row per month. */
    val schedule: List<Installment>,
) {
    /** Down payment as a fraction of [propertyPrice], `0.15` for 15%. */
    val downPaymentRate: BigDecimal
        get() = downPayment.divide(propertyPrice, RATE_SCALE, RoundingMode.HALF_UP)

    val termYears: Int get() = termMonths / SimulationConfig.MONTHS_PER_YEAR

    /** Property plus credit life insurance charged every month, on top of [monthlyPayment]. */
    val monthlyInsurance: BigDecimal
        get() = assumptions.monthlyPropertyInsurance + assumptions.monthlyCreditLifeInsurance

    val bonusStatus: BonusStatus
        get() =
            when {
                !bonusRequested -> BonusStatus.NotRequested
                goodPayerBonus.signum() > 0 -> BonusStatus.Applied
                else -> BonusStatus.NotEligible
            }

    private companion object {
        const val RATE_SCALE = 6
    }
}

/** What happened with the Bono del Buen Pagador. [NotEligible] is a normal result, not an error. */
enum class BonusStatus { NotRequested, Applied, NotEligible }

/** Server-side values used in the calculation. */
data class SimulationAssumptions(
    val monthlyPropertyInsurance: BigDecimal,
    val monthlyCreditLifeInsurance: BigDecimal,
    val administrativeExpenses: BigDecimal,
    /** Annual rate used to discount the NPV, as a fraction. */
    val annualDiscountRate: BigDecimal,
)

/** One row of the amortization schedule. */
data class Installment(
    val number: Int,
    val paymentDate: LocalDate,
    val openingBalance: BigDecimal,
    /** Principal plus interest, without insurance. */
    val installment: BigDecimal,
    val principal: BigDecimal,
    val interest: BigDecimal,
    val propertyInsurance: BigDecimal,
    val creditLifeInsurance: BigDecimal,
    /** Total paid in the month: [installment] plus [insurance]. */
    val payment: BigDecimal,
    val remainingBalance: BigDecimal,
    val isGracePeriod: Boolean,
) {
    val insurance: BigDecimal get() = propertyInsurance + creditLifeInsurance
}
