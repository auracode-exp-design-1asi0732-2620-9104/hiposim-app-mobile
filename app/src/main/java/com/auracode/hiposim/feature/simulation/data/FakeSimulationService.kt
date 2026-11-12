package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.core.network.validationProblemException
import kotlinx.coroutines.delay
import kotlinx.serialization.json.put
import retrofit2.HttpException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.math.pow

/**
 * Stand-in for the API while its host does not exist (`BuildConfig.USE_FAKE_API`).
 *
 * It answers with the exact shape of contract v0.1.0 and follows the rules of the backend engine
 * (hiposim-backEnd, `HipoSim.FinancialEngine`): French method, 30/360 monthly rate, the reference bonus table,
 * lender-side NPV at a 10% discount rate, no insurance, and the grace periods of US03 (partial pays only the
 * interest; total pays nothing and capitalizes it, with the regular installment calculated on that balance). An invalid request fails like the server does,
 * with a 400 `application/problem+json`, so the error path is the same as with the real API.
 *
 * It is a development aid, not the financial engine: the server is the source of truth.
 */
class FakeSimulationService(
    private val latencyMillis: Long = 0,
    private val today: () -> LocalDate = { LocalDate.now(LIMA) },
) : SimulationService {
    override suspend fun createSimulation(request: SimulationRequestDto): SimulationResponseDto {
        if (latencyMillis > 0) delay(latencyMillis)

        val errors = validate(request)
        if (errors.isNotEmpty()) throw validationError(errors)

        val eligibleBonus = BONUS_TIERS.firstOrNull { request.propertyPrice <= it.first }?.second
        val appliedBonus = if (request.applyGoodPayerBonus) eligibleBonus ?: 0.0 else 0.0
        val financedAmount = money(request.propertyPrice - request.downPayment - appliedBonus)
        if (financedAmount <= 0) {
            throw validationError(
                mapOf("downPayment" to listOf("The down payment and the benefit leave nothing to finance.")),
            )
        }

        val monthlyRate = (1 + request.annualEffectiveRate).pow(1.0 / MONTHS_PER_YEAR) - 1
        val startDate = request.startDate?.let(LocalDate::parse) ?: today()
        val grace = Grace(GraceKind.from(request.graceType), request.gracePeriodMonths ?: 0)
        val schedule = buildSchedule(financedAmount, monthlyRate, request.termInMonths, grace, startDate)
        val regularInstallment = schedule.first { !it.isGracePeriod }.installment
        val payments = schedule.map { it.totalPayment }
        val monthlyIrr = irr(financedAmount, payments)
        val annualIrr = rate((1 + monthlyIrr).pow(MONTHS_PER_YEAR) - 1)

        val response =
            SimulationResponseDto(
                simulationId = UUID.randomUUID().toString(),
                financedAmount = financedAmount,
                monthlyInstallment = money(regularInstallment),
                totalPaid = money(payments.sum()),
                totalInterest = money(schedule.sumOf { it.interest }),
                additionalCosts = 0.0,
                tcea = annualIrr,
                irr = annualIrr,
                npv = money(npv(financedAmount, payments, (1 + ANNUAL_DISCOUNT_RATE).pow(1.0 / MONTHS_PER_YEAR) - 1)),
                benefit = BenefitDto(name = BONUS_NAME, eligible = eligibleBonus != null, appliedAmount = appliedBonus),
                assumptions =
                    AssumptionsDto(
                        monthlyPropertyInsurance = 0.0,
                        monthlyCreditLifeInsurance = 0.0,
                        administrativeExpenses = 0.0,
                        annualDiscountRate = ANNUAL_DISCOUNT_RATE,
                    ),
                amortizationSchedule = schedule,
            )
        FakeSimulationStore.put(request, response)
        return response
    }

    private fun validate(request: SimulationRequestDto): Map<String, List<String>> =
        buildMap {
            if (request.propertyPrice <= 0) {
                put("propertyPrice", listOf("The propertyPrice must be greater than 0."))
            } else if (request.downPayment > request.propertyPrice) {
                put("downPayment", listOf("The downPayment cannot be greater than the propertyPrice."))
            } else if (request.downPayment * MIN_DOWN_PAYMENT_DIVISOR < request.propertyPrice) {
                put("downPayment", listOf("The down payment must be at least 10% of the property price."))
            }
            if (request.annualEffectiveRate <= 0 || request.annualEffectiveRate > 1) {
                put(
                    "annualEffectiveRate",
                    listOf(
                        "The annualEffectiveRate must be a fraction greater than 0 and at most 1 (0.085 means 8.5%).",
                    ),
                )
            }
            if (request.termInMonths !in MIN_TERM_MONTHS..MAX_TERM_MONTHS) {
                put("termInMonths", listOf("The termInMonths must be between $MIN_TERM_MONTHS and $MAX_TERM_MONTHS."))
            }
            validateGrace(request)?.let { (field, message) -> put(field, listOf(message)) }
        }

    /** Same rules as the backend validator: the type is known, it is "none" exactly when there are no months. */
    private fun validateGrace(request: SimulationRequestDto): Pair<String, String>? {
        val months = request.gracePeriodMonths ?: 0
        val type = request.graceType ?: GraceKind.NONE
        return when {
            type !in GraceKind.API_VALUES -> "graceType" to "The graceType must be none, total or partial."
            months < 0 -> "gracePeriodMonths" to "The gracePeriodMonths cannot be negative."
            months >= request.termInMonths ->
                "gracePeriodMonths" to
                    "The gracePeriodMonths must be lower than the termInMonths."
            (type == GraceKind.NONE) != (months == 0) ->
                "graceType" to "The graceType must be none if and only if the gracePeriodMonths is 0."
            else -> null
        }
    }

    private fun validationError(errors: Map<String, List<String>>): HttpException = validationProblemException(errors)

    private fun frenchInstallment(
        capital: Double,
        monthlyRate: Double,
        months: Int,
    ): Double {
        val factor = (1 + monthlyRate).pow(months)
        return capital * (monthlyRate * factor) / (factor - 1)
    }

    private fun buildSchedule(
        financedAmount: Double,
        monthlyRate: Double,
        months: Int,
        grace: Grace,
        startDate: LocalDate,
    ): List<AmortizationEntryDto> {
        var balance = financedAmount
        val graceRows =
            (1..grace.months).map { period ->
                val openingBalance = balance
                val interest = balance * monthlyRate
                // Total grace pays nothing and capitalizes the interest; partial grace pays exactly the interest.
                val installment = if (grace.kind == GraceKind.Total) 0.0 else interest
                if (grace.kind == GraceKind.Total) balance += interest
                entry(period, startDate, openingBalance, installment, interest, amortization = 0.0, balance, true)
            }

        val paymentMonths = months - grace.months
        // After a total grace the installment is calculated on the capitalized balance, like the backend engine.
        val regularInstallment = frenchInstallment(balance, monthlyRate, paymentMonths)
        val regularRows =
            (1..paymentMonths).map { k ->
                val openingBalance = balance
                val interest = balance * monthlyRate
                var amortization = regularInstallment - interest
                var installment = regularInstallment
                balance -= amortization
                if (k == paymentMonths) {
                    // The last installment settles whatever balance the rounding left.
                    amortization += balance
                    installment += money(balance)
                    balance = 0.0
                }
                entry(grace.months + k, startDate, openingBalance, installment, interest, amortization, balance, false)
            }
        return graceRows + regularRows
    }

    private fun entry(
        period: Int,
        startDate: LocalDate,
        openingBalance: Double,
        installment: Double,
        interest: Double,
        amortization: Double,
        remainingBalance: Double,
        isGracePeriod: Boolean,
    ) = AmortizationEntryDto(
        period = period,
        paymentDate = startDate.plusMonths(period.toLong()).toString(),
        openingBalance = money(openingBalance),
        installment = money(installment),
        interest = money(interest),
        amortization = money(amortization),
        remainingBalance = money(remainingBalance),
        propertyInsurance = 0.0,
        creditLifeInsurance = 0.0,
        totalPayment = money(installment),
        isGracePeriod = isGracePeriod,
    )

    private data class Grace(
        val kind: GraceKind,
        val months: Int,
    )

    private enum class GraceKind {
        None,
        Partial,
        Total,
        ;

        companion object {
            const val NONE = "none"
            val API_VALUES = setOf(NONE, "partial", "total")

            fun from(value: String?): GraceKind =
                when (value) {
                    "partial" -> Partial
                    "total" -> Total
                    else -> None
                }
        }
    }

    /** Lender's view: the disbursement leaves at month 0 and every payment comes back. */
    private fun npv(
        financedAmount: Double,
        payments: List<Double>,
        monthlyRate: Double,
    ): Double {
        var discount = 1.0
        var total = -financedAmount
        for (payment in payments) {
            discount *= 1 + monthlyRate
            total += payment / discount
        }
        return total
    }

    /** Monthly rate that makes the NPV zero. The NPV falls as the rate grows, so bisection is enough. */
    private fun irr(
        financedAmount: Double,
        payments: List<Double>,
    ): Double {
        var low = 0.0
        var high = 1.0
        repeat(IRR_ITERATIONS) {
            val mid = (low + high) / 2
            if (npv(financedAmount, payments, mid) > 0) low = mid else high = mid
        }
        return (low + high) / 2
    }

    // Banker's rounding, like the engine. "+ 0.0" turns a negative zero into a positive one.
    private fun money(value: Double): Double = BigDecimal(value).setScale(2, RoundingMode.HALF_EVEN).toDouble() + 0.0

    private fun rate(value: Double): Double =
        BigDecimal(value).setScale(RATE_SCALE, RoundingMode.HALF_EVEN).toDouble() + 0.0

    companion object {
        /** Simulated network delay of the debug app, so the loading state can be seen. */
        const val DEFAULT_LATENCY_MILLIS = 400L

        private const val BONUS_NAME = "Good Payer Bonus"

        /**
         * Maximum property price to bonus amount. Reference table of the Landing Page, the same one the
         * backend uses (`BenefitsEngine.ReferenceTiers`). It is NOT an official Fondo Mivivienda table.
         */
        private val BONUS_TIERS = listOf(200_000.0 to 26_400.0, 300_000.0 to 18_200.0, 400_000.0 to 10_000.0)

        private const val ANNUAL_DISCOUNT_RATE = 0.10
        private const val MONTHS_PER_YEAR = 12
        private const val MIN_TERM_MONTHS = 60
        private const val MAX_TERM_MONTHS = 360
        private const val MIN_DOWN_PAYMENT_DIVISOR = 10
        private const val RATE_SCALE = 6
        private const val IRR_ITERATIONS = 100
        private val LIMA: ZoneId = ZoneId.of("America/Lima")
    }
}
