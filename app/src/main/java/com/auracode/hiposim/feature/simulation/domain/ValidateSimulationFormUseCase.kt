package com.auracode.hiposim.feature.simulation.domain

import java.math.BigDecimal
import javax.inject.Inject

/** Applies the same rules as `POST /api/simulations`, so the buyer sees the error before any request is sent. */
class ValidateSimulationFormUseCase
    @Inject
    constructor() {
        operator fun invoke(form: SimulationForm): SimulationFormValidation {
            val price = form.propertyPrice.toAmountOrNull()
            val downPayment = form.downPayment.toAmountOrNull()
            val errors =
                buildMap {
                    if (price == null || price.signum() <= 0) {
                        put(SimulationField.PropertyPrice, SimulationFormError.PropertyPriceRequired)
                    }
                    if (downPayment == null) {
                        put(SimulationField.DownPayment, SimulationFormError.DownPaymentRequired)
                    } else if (price != null && price.signum() > 0) {
                        when {
                            downPayment >= price ->
                                put(SimulationField.DownPayment, SimulationFormError.DownPaymentTooHigh)

                            isBelowMinimum(downPayment, price) ->
                                put(SimulationField.DownPayment, SimulationFormError.DownPaymentTooLow)
                        }
                    }
                    if (form.termYears !in MIN_TERM_YEARS..MAX_TERM_YEARS) {
                        put(SimulationField.Term, SimulationFormError.TermOutOfRange)
                    }
                }
            if (errors.isNotEmpty() || price == null || downPayment == null) {
                return SimulationFormValidation(errors, request = null)
            }
            return SimulationFormValidation(
                errors = errors,
                request =
                    SimulationRequest(
                        propertyPrice = price,
                        downPayment = downPayment,
                        termYears = form.termYears,
                    ),
            )
        }

        /** Compares `downPayment / price` with the minimum share without dividing, so there is no rounding. */
        private fun isBelowMinimum(
            downPayment: BigDecimal,
            price: BigDecimal,
        ): Boolean = downPayment.multiply(BigDecimal(PERCENT)) < price.multiply(BigDecimal(MIN_DOWN_PAYMENT_PERCENT))

        companion object {
            /** The API accepts terms from 60 to 360 months. */
            const val MIN_TERM_YEARS = 5
            const val MAX_TERM_YEARS = 30

            /** The down payment must be at least this share of the price (US01). */
            const val MIN_DOWN_PAYMENT_PERCENT = 10

            private const val PERCENT = 100
        }
    }

private fun String.toAmountOrNull(): BigDecimal? = filter { it.isDigit() }.takeIf { it.isNotEmpty() }?.toBigDecimal()
