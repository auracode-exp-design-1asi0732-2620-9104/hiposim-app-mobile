package com.auracode.hiposim.feature.simulation.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class ValidateSimulationFormUseCaseTest {
    private val validate = ValidateSimulationFormUseCase()

    private fun form(
        price: String = "280000",
        downPayment: String = "42000",
        termYears: Int = 20,
    ) = SimulationForm(propertyPrice = price, downPayment = downPayment, termYears = termYears)

    @Test
    fun defaultForm_isValidAndBuildsTheRequestWithTheReferenceRate() {
        val validation = validate(SimulationForm())

        assertTrue(validation.isValid)
        assertTrue(validation.errors.isEmpty())
        val request = requireNotNull(validation.request)
        assertEquals(0, BigDecimal("280000").compareTo(request.propertyPrice))
        assertEquals(0, BigDecimal("42000").compareTo(request.downPayment))
        assertEquals(20, request.termYears)
        assertEquals(240, request.termMonths)
        assertEquals(0, BigDecimal("0.0885").compareTo(request.annualEffectiveRate))
        assertFalse(request.applyGoodPayerBonus)
    }

    @Test
    fun downPayment_ofExactlyTenPercent_isAccepted() {
        assertTrue(validate(form(downPayment = "28000")).isValid)
    }

    @Test
    fun downPayment_belowTenPercent_isTooLow() {
        val validation = validate(form(downPayment = "27999"))

        assertEquals(SimulationFormError.DownPaymentTooLow, validation.errors[SimulationField.DownPayment])
        assertNull(validation.request)
    }

    @Test
    fun downPayment_equalToOrAbovePrice_isTooHigh() {
        assertEquals(
            SimulationFormError.DownPaymentTooHigh,
            validate(form(downPayment = "280000")).errors[SimulationField.DownPayment],
        )
        assertEquals(
            SimulationFormError.DownPaymentTooHigh,
            validate(form(downPayment = "300000")).errors[SimulationField.DownPayment],
        )
    }

    @Test
    fun downPayment_justBelowPrice_isAccepted() {
        assertTrue(validate(form(downPayment = "279999")).isValid)
    }

    @Test
    fun emptyOrZeroPrice_isRequired() {
        listOf("", "0", "000").forEach { price ->
            val validation = validate(form(price = price))

            assertEquals(
                price,
                SimulationFormError.PropertyPriceRequired,
                validation.errors[SimulationField.PropertyPrice],
            )
            assertNull(validation.request)
        }
    }

    @Test
    fun withoutPrice_theDownPaymentIsNotJudgedAgainstIt() {
        val validation = validate(form(price = "", downPayment = "5"))

        assertEquals(setOf(SimulationField.PropertyPrice), validation.errors.keys)
    }

    @Test
    fun emptyDownPayment_isRequired() {
        val validation = validate(form(downPayment = ""))

        assertEquals(SimulationFormError.DownPaymentRequired, validation.errors[SimulationField.DownPayment])
    }

    @Test
    fun term_acceptsFiveToThirtyYears() {
        assertTrue(validate(form(termYears = 5)).isValid)
        assertTrue(validate(form(termYears = 30)).isValid)
    }

    @Test
    fun term_outsideTheRange_isRejected() {
        listOf(0, 4, 31, 40).forEach { years ->
            assertEquals(
                SimulationFormError.TermOutOfRange,
                validate(form(termYears = years)).errors[SimulationField.Term],
            )
        }
    }

    @Test
    fun severalProblems_areReportedTogether() {
        val validation = validate(form(price = "", downPayment = "", termYears = 3))

        assertEquals(
            setOf(SimulationField.PropertyPrice, SimulationField.DownPayment, SimulationField.Term),
            validation.errors.keys,
        )
    }

    @Test
    fun separatorsAndLetters_areIgnored() {
        val validation = validate(form(price = "280,000", downPayment = "S/ 42,000"))

        val request = requireNotNull(validation.request)
        assertEquals(0, BigDecimal("280000").compareTo(request.propertyPrice))
        assertEquals(0, BigDecimal("42000").compareTo(request.downPayment))
    }
}
