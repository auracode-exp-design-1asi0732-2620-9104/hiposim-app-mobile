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
        propertyPrice: String = "280000",
        downPayment: String = "42000",
        termYears: String = "20",
        applyBonus: Boolean = false,
    ) = SimulationForm(propertyPrice, downPayment, termYears, applyBonus)

    private fun errorOf(
        form: SimulationForm,
        field: SimulationField,
    ) = validate(form).errors[field]

    @Test
    fun validForm_returnsTheParams() {
        val validation = validate(form(applyBonus = true))

        assertTrue(validation.isValid)
        assertTrue(validation.errors.isEmpty())
        assertEquals(
            SimulationParams(
                propertyPrice = BigDecimal("280000"),
                downPayment = BigDecimal("42000"),
                termYears = 20,
                applyGoodPayerBonus = true,
            ),
            validation.params,
        )
    }

    @Test
    fun params_carryTheConfiguredRateAsAFractionAndTheTermInMonths() {
        val params = requireNotNull(validate(form()).params)

        assertEquals(SimulationConfig.ANNUAL_EFFECTIVE_RATE, params.annualEffectiveRate)
        assertTrue(params.annualEffectiveRate < BigDecimal.ONE)
        assertEquals(240, params.termMonths)
    }

    @Test
    fun emptyForm_reportsEveryFieldAndHasNoParams() {
        val validation = validate(SimulationForm())

        assertFalse(validation.isValid)
        assertNull(validation.params)
        assertEquals(
            mapOf(
                SimulationField.PropertyPrice to SimulationFormError.PropertyPriceRequired,
                SimulationField.DownPayment to SimulationFormError.DownPaymentRequired,
                SimulationField.TermYears to SimulationFormError.TermRequired,
            ),
            validation.errors,
        )
    }

    @Test
    fun propertyPrice_mustBeBetween120000And800000() {
        val field = SimulationField.PropertyPrice
        val outOfRange = SimulationFormError.PropertyPriceOutOfRange

        assertEquals(outOfRange, errorOf(form(propertyPrice = "119999", downPayment = "20000"), field))
        assertNull(errorOf(form(propertyPrice = "120000", downPayment = "20000"), field))
        assertNull(errorOf(form(propertyPrice = "800000", downPayment = "80000"), field))
        assertEquals(outOfRange, errorOf(form(propertyPrice = "800001", downPayment = "90000"), field))
    }

    @Test
    fun downPayment_mustBeAtLeastTenPercent() {
        val field = SimulationField.DownPayment

        assertEquals(SimulationFormError.DownPaymentBelowMinimum, errorOf(form(downPayment = "27999"), field))
        assertNull(errorOf(form(downPayment = "28000"), field))
    }

    @Test
    fun downPayment_mustBeLowerThanThePrice() {
        val field = SimulationField.DownPayment

        assertEquals(SimulationFormError.DownPaymentTooHigh, errorOf(form(downPayment = "280000"), field))
        assertEquals(SimulationFormError.DownPaymentTooHigh, errorOf(form(downPayment = "300000"), field))
        assertNull(errorOf(form(downPayment = "279999"), field))
    }

    @Test
    fun downPayment_isNotComparedWhileThePriceIsMissing() {
        val validation = validate(form(propertyPrice = ""))

        assertEquals(setOf(SimulationField.PropertyPrice), validation.errors.keys)
    }

    @Test
    fun term_mustBeBetween5And30Years() {
        val field = SimulationField.TermYears

        assertEquals(SimulationFormError.TermOutOfRange, errorOf(form(termYears = "4"), field))
        assertNull(errorOf(form(termYears = "5"), field))
        assertNull(errorOf(form(termYears = "30"), field))
        assertEquals(SimulationFormError.TermOutOfRange, errorOf(form(termYears = "31"), field))
    }

    @Test
    fun fields_knowTheirRequestProperty() {
        assertEquals(SimulationField.PropertyPrice, SimulationField.fromRequestProperty("propertyPrice"))
        assertEquals(SimulationField.DownPayment, SimulationField.fromRequestProperty("downPayment"))
        assertEquals(SimulationField.TermYears, SimulationField.fromRequestProperty("termInMonths"))
        assertNull(SimulationField.fromRequestProperty("annualEffectiveRate"))
    }
}
