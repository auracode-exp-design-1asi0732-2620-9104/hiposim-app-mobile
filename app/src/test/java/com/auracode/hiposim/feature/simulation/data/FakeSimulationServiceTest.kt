package com.auracode.hiposim.feature.simulation.data

import com.auracode.hiposim.core.network.ApiError
import com.auracode.hiposim.core.network.ApiResult
import com.auracode.hiposim.core.network.safeApiCall
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The fake must give the numbers of the contract examples (openapi.v0.yaml), which are real results of the
 * backend engine. If one of these fails, the fake no longer represents the API.
 */
class FakeSimulationServiceTest {
    private val service = FakeSimulationService(today = { LocalDate.parse("2026-10-01") })

    private fun request(
        propertyPrice: Double = 280_000.0,
        downPayment: Double = 42_000.0,
        rate: Double = 0.085,
        termInMonths: Int = 240,
        applyBonus: Boolean = true,
    ) = SimulationRequestDto(
        propertyPrice = propertyPrice,
        downPayment = downPayment,
        annualEffectiveRate = rate,
        termInMonths = termInMonths,
        applyGoodPayerBonus = applyBonus,
    )

    private suspend fun fieldErrorsOf(request: SimulationRequestDto): Map<String, List<String>> {
        val result = safeApiCall { service.createSimulation(request) }
        val error = (result as ApiResult.Failure).error
        return (error as ApiError.Validation).fieldErrors
    }

    @Test
    fun withBonusExample_matchesTheContractIndicators() =
        runTest {
            val response = service.createSimulation(request())

            assertEquals(219_800.0, response.financedAmount, 0.0)
            assertEquals(1_863.99, response.monthlyInstallment, 0.0)
            assertEquals(447_357.6, response.totalPaid, CENT)
            assertEquals(227_557.97, response.totalInterest, CENT)
            assertEquals(0.0, response.additionalCosts, 0.0)
            assertEquals(0.085, response.tcea, 0.0)
            assertEquals(0.085, response.irr, 0.0)
            assertEquals(-20_791.75, response.npv, CENT)
            assertEquals(BenefitDto("Good Payer Bonus", eligible = true, appliedAmount = 18_200.0), response.benefit)
            assertEquals(AssumptionsDto(0.0, 0.0, 0.0, annualDiscountRate = 0.1), response.assumptions)
        }

    @Test
    fun withBonusExample_matchesTheContractSchedule() =
        runTest {
            val schedule = service.createSimulation(request()).amortizationSchedule

            assertEquals(240, schedule.size)
            assertEquals(
                AmortizationEntryDto(
                    period = 1,
                    paymentDate = "2026-11-01",
                    openingBalance = 219_800.0,
                    installment = 1_863.99,
                    interest = 1_499.36,
                    amortization = 364.63,
                    remainingBalance = 219_435.37,
                    propertyInsurance = 0.0,
                    creditLifeInsurance = 0.0,
                    totalPayment = 1_863.99,
                    isGracePeriod = false,
                ),
                schedule[0],
            )
            assertEquals(219_435.37, schedule[1].openingBalance, 0.0)
            assertEquals(1_496.88, schedule[1].interest, 0.0)
            assertEquals(367.11, schedule[1].amortization, 0.0)
            assertEquals(219_068.26, schedule[1].remainingBalance, 0.0)

            val last = schedule.last()
            assertEquals(240, last.period)
            assertEquals("2046-10-01", last.paymentDate)
            assertEquals(1_851.36, last.openingBalance, CENT)
            assertEquals(12.63, last.interest, CENT)
            assertEquals(0.0, last.remainingBalance, 0.0)
        }

    @Test
    fun outOfRangeExample_answersOkWithoutBonus() =
        runTest {
            val response = service.createSimulation(request(propertyPrice = 450_000.0, downPayment = 90_000.0))

            assertEquals(BenefitDto("Good Payer Bonus", eligible = false, appliedAmount = 0.0), response.benefit)
            assertEquals(360_000.0, response.financedAmount, 0.0)
            assertEquals(3_052.94, response.monthlyInstallment, 0.0)
            assertEquals(732_705.6, response.totalPaid, CENT)
            assertEquals(-34_053.91, response.npv, CENT)
            assertEquals(0.085, response.tcea, 0.0)
        }

    @Test
    fun bonusNotRequested_isEligibleButNotApplied() =
        runTest {
            val response = service.createSimulation(request(applyBonus = false))

            assertTrue(response.benefit.eligible)
            assertEquals(0.0, response.benefit.appliedAmount, 0.0)
            assertEquals(238_000.0, response.financedAmount, 0.0)
        }

    @Test
    fun bonusTable_followsTheReferenceTiers() =
        runTest {
            suspend fun bonusFor(price: Double) =
                service
                    .createSimulation(
                        request(propertyPrice = price, downPayment = price * 0.2),
                    ).benefit.appliedAmount

            assertEquals(26_400.0, bonusFor(200_000.0), 0.0)
            assertEquals(18_200.0, bonusFor(300_000.0), 0.0)
            assertEquals(10_000.0, bonusFor(400_000.0), 0.0)
            assertEquals(0.0, bonusFor(400_001.0), 0.0)
        }

    @Test
    fun financedAmount_isPriceMinusDownPaymentMinusBonus() =
        runTest {
            val response = service.createSimulation(request(propertyPrice = 350_000.0, downPayment = 70_000.0))

            assertEquals(350_000.0 - 70_000.0 - response.benefit.appliedAmount, response.financedAmount, 0.0)
            assertEquals(response.financedAmount, response.amortizationSchedule.first().openingBalance, 0.0)
        }

    @Test
    fun downPaymentBelowTenPercent_failsLikeTheServer() =
        runTest {
            assertEquals(
                mapOf("downPayment" to listOf("The down payment must be at least 10% of the property price.")),
                fieldErrorsOf(request(downPayment = 27_999.0)),
            )
            assertFalse(fieldErrorsOf(request(downPayment = 300_000.0)).isEmpty())
        }

    @Test
    fun termAndRateOutOfRange_areReportedTogether() =
        runTest {
            val errors = fieldErrorsOf(request(termInMonths = 59, rate = 8.5))

            assertEquals(setOf("termInMonths", "annualEffectiveRate"), errors.keys)
            assertTrue(fieldErrorsOf(request(termInMonths = 361)).containsKey("termInMonths"))
        }

    private companion object {
        /** Sums of 240 rounded rows may differ from the engine in the last cent. */
        const val CENT = 0.011
    }
}
