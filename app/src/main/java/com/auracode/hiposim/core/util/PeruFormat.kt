package com.auracode.hiposim.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Formatting for Peruvian soles and percentages (es-PE): `S/ 1,909`, `S/ 1,803.20`, `-S/ 20,792`, `8.85%`. */
object PeruFormat {
    private const val PERCENT_FACTOR = 100

    // Separators are fixed on purpose so the output does not depend on the JDK/ICU locale data.
    private val symbols =
        DecimalFormatSymbols(Locale.forLanguageTag("es-PE")).apply {
            decimalSeparator = '.'
            groupingSeparator = ','
        }

    fun soles(
        amount: BigDecimal,
        fractionDigits: Int = 0,
    ): String {
        val formatted = decimal(amount.abs(), fractionDigits)
        // The sign goes before the currency symbol, and a value that rounds to zero has none.
        val isNegative = amount.signum() < 0 && formatted.any { it in '1'..'9' }
        return if (isNegative) "-S/ $formatted" else "S/ $formatted"
    }

    /** [fraction] is a rate expressed as a fraction, so `0.0885` becomes `8.85%`. */
    fun percent(
        fraction: BigDecimal,
        fractionDigits: Int = 2,
    ): String = "${decimal(fraction.multiply(BigDecimal(PERCENT_FACTOR)), fractionDigits)}%"

    private fun decimal(
        value: BigDecimal,
        fractionDigits: Int,
    ): String {
        val pattern = if (fractionDigits > 0) "#,##0.${"0".repeat(fractionDigits)}" else "#,##0"
        return DecimalFormat(pattern, symbols).apply { roundingMode = RoundingMode.HALF_UP }.format(value)
    }
}
