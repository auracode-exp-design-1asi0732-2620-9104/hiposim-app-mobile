package com.auracode.hiposim.feature.simulation.domain

import java.math.BigDecimal
import javax.inject.Inject

/**
 * Client-side validation with the ranges of [SimulationConfig].
 *
 * It only saves the buyer a round trip: the API validates again and its answer wins.
 */
class ValidateSimulationFormUseCase
    @Inject
    constructor() {
        operator fun invoke(form: SimulationForm): SimulationFormValidation {
            val price = form.propertyPrice.toAmountOrNull()
            val downPayment = form.downPayment.toAmountOrNull()
            val termYears = form.termYears.trim().toIntOrNull()
            val graceMonths = if (form.graceType == GraceType.None) 0 else form.graceMonths.trim().toIntOrNull()

            val errors =
                buildMap {
                    propertyPriceError(price)?.let { put(SimulationField.PropertyPrice, it) }
                    downPaymentError(downPayment, price)?.let { put(SimulationField.DownPayment, it) }
                    termError(termYears)?.let { put(SimulationField.TermYears, it) }
                    graceError(form.graceType, graceMonths)?.let { put(SimulationField.GraceMonths, it) }
                }

            val params =
                if (errors.isEmpty() &&
                    price != null &&
                    downPayment != null &&
                    termYears != null &&
                    graceMonths != null
                ) {
                    SimulationParams(
                        propertyPrice = price,
                        downPayment = downPayment,
                        termYears = termYears,
                        applyGoodPayerBonus = form.applyGoodPayerBonus,
                        graceType = form.graceType,
                        graceMonths = graceMonths,
                    )
                } else {
                    null
                }
            return SimulationFormValidation(errors, params)
        }

        private fun propertyPriceError(price: BigDecimal?): SimulationFormError? =
            when {
                price == null -> SimulationFormError.PropertyPriceRequired
                price < SimulationConfig.MIN_PROPERTY_PRICE || price > SimulationConfig.MAX_PROPERTY_PRICE ->
                    SimulationFormError.PropertyPriceOutOfRange
                else -> null
            }

        /** Compared against the price only when the price itself was typed. */
        private fun downPaymentError(
            downPayment: BigDecimal?,
            price: BigDecimal?,
        ): SimulationFormError? =
            when {
                downPayment == null -> SimulationFormError.DownPaymentRequired
                price == null -> null
                downPayment >= price -> SimulationFormError.DownPaymentTooHigh
                downPayment < price.multiply(SimulationConfig.MIN_DOWN_PAYMENT_RATE) ->
                    SimulationFormError.DownPaymentBelowMinimum
                else -> null
            }

        private fun termError(termYears: Int?): SimulationFormError? =
            when {
                termYears == null -> SimulationFormError.TermRequired
                termYears < SimulationConfig.MIN_TERM_YEARS || termYears > SimulationConfig.MAX_TERM_YEARS ->
                    SimulationFormError.TermOutOfRange
                else -> null
            }

        /**
         * Only checked when the buyer chose a grace period. The maximum grace (12 months) is always below the
         * minimum term (60 months), so the contract rule "grace lower than the term" cannot fail here.
         */
        private fun graceError(
            graceType: GraceType,
            graceMonths: Int?,
        ): SimulationFormError? =
            when {
                graceType == GraceType.None -> null
                graceMonths == null -> SimulationFormError.GraceMonthsRequired
                graceMonths < SimulationConfig.MIN_GRACE_MONTHS || graceMonths > SimulationConfig.MAX_GRACE_MONTHS ->
                    SimulationFormError.GraceMonthsOutOfRange
                else -> null
            }

        private fun String.toAmountOrNull(): BigDecimal? = trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()
    }
