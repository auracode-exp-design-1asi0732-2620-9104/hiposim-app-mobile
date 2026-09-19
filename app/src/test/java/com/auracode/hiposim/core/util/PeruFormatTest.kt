package com.auracode.hiposim.core.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PeruFormatTest {
    @Test
    fun soles_withoutDecimals_groupsThousands() {
        assertEquals("S/ 280,000", PeruFormat.soles(BigDecimal("280000")))
        assertEquals("S/ 1,909", PeruFormat.soles(BigDecimal("1909")))
    }

    @Test
    fun soles_withCents_usesDotDecimalSeparator() {
        assertEquals("S/ 1,803.20", PeruFormat.soles(BigDecimal("1803.2"), fractionDigits = 2))
        assertEquals("S/ 219,414.51", PeruFormat.soles(BigDecimal("219414.51"), fractionDigits = 2))
    }

    @Test
    fun soles_roundsHalfUp() {
        assertEquals("S/ 1,910", PeruFormat.soles(BigDecimal("1909.50")))
        assertEquals("S/ 0.13", PeruFormat.soles(BigDecimal("0.125"), fractionDigits = 2))
    }

    @Test
    fun percent_convertsFractionToPercentage() {
        assertEquals("8.85%", PeruFormat.percent(BigDecimal("0.0885")))
        assertEquals("15%", PeruFormat.percent(BigDecimal("0.15"), fractionDigits = 0))
    }
}
