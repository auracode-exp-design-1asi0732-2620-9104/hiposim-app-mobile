package com.auracode.hiposim.core.designsystem.component

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Test

class ThousandsSeparatorTransformationTest {
    private fun transform(digits: String) = ThousandsSeparatorTransformation.filter(AnnotatedString(digits))

    @Test
    fun groupsDigitsInThousands() {
        assertEquals("", transform("").text.text)
        assertEquals("280", transform("280").text.text)
        assertEquals("1,000", transform("1000").text.text)
        assertEquals("280,000", transform("280000").text.text)
        assertEquals("1,234,567", transform("1234567").text.text)
    }

    @Test
    fun cursorOffsetsMapBothWays() {
        val mapping = transform("280000").offsetMapping

        // 280000 -> 280,000
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(2, mapping.originalToTransformed(2))
        assertEquals(4, mapping.originalToTransformed(3))
        assertEquals(7, mapping.originalToTransformed(6))

        assertEquals(0, mapping.transformedToOriginal(0))
        assertEquals(3, mapping.transformedToOriginal(3))
        assertEquals(3, mapping.transformedToOriginal(4))
        assertEquals(6, mapping.transformedToOriginal(7))
    }

    @Test
    fun everyOffsetRoundTripsInsideTheText() {
        listOf("", "5", "42000", "280000", "1234567").forEach { digits ->
            val transformed = transform(digits)
            for (offset in 0..digits.length) {
                val there = transformed.offsetMapping.originalToTransformed(offset)
                assertEquals(digits, offset, transformed.offsetMapping.transformedToOriginal(there))
            }
        }
    }
}
