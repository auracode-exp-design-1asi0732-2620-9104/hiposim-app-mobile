package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.feature.simulation.domain.Installment
import com.auracode.hiposim.feature.simulation.domain.LoanSimulation
import com.auracode.hiposim.feature.simulation.domain.SimulationRepository
import java.math.BigDecimal
import javax.inject.Inject

/**
 * Fixed data with the landing page figures, until the API and the financial engine exist.
 *
 * Price 280,000 - down payment 42,000 (15%) - bonus 18,200 = net loan 219,800, 20 years, payment 1,909.
 * The schedule rows are a French-method run at 0.645% monthly with 105.80 of insurance, rounded to cents.
 */
class FakeSimulationRepository
    @Inject
    constructor() : SimulationRepository {
        override suspend fun getCurrentSimulation(): LoanSimulation = SIMULATION

        private companion object {
            private val INSURANCE = BigDecimal("105.80")
            private val PAYMENT = BigDecimal("1909.00")

            val SIMULATION =
                LoanSimulation(
                    propertyPrice = BigDecimal("280000"),
                    downPayment = BigDecimal("42000"),
                    downPaymentRate = BigDecimal("0.15"),
                    goodPayerBonus = BigDecimal("18200"),
                    netLoan = BigDecimal("219800"),
                    termYears = 20,
                    monthlyPayment = PAYMENT,
                    monthlyInsurance = INSURANCE,
                    tcea = BigDecimal("0.0885"),
                    // TODO: replace with the NPV computed by the financial engine. Example value.
                    npv = BigDecimal("14200"),
                    // TODO: replace with the IRR computed by the financial engine. Example value.
                    irr = BigDecimal("0.0810"),
                    initialSchedule =
                        listOf(
                            installment(1, principal = "385.49", interest = "1417.71", balance = "219414.51"),
                            installment(2, principal = "387.98", interest = "1415.22", balance = "219026.53"),
                            installment(3, principal = "390.48", interest = "1412.72", balance = "218636.05"),
                            installment(4, principal = "393.00", interest = "1410.20", balance = "218243.05"),
                        ),
                )

            private fun installment(
                number: Int,
                principal: String,
                interest: String,
                balance: String,
            ) = Installment(
                number = number,
                payment = PAYMENT,
                principal = BigDecimal(principal),
                interest = BigDecimal(interest),
                insurance = INSURANCE,
                remainingBalance = BigDecimal(balance),
            )
        }
    }
