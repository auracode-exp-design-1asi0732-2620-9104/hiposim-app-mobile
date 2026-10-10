package com.auracode.hiposim.core.designsystem.component

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThousandsVisualTransformationTest {
    private fun transform(digits: String) = ThousandsVisualTransformation.filter(AnnotatedString(digits))

    @Test
    fun groupsDigitsByThousands() {
        assertEquals("", transform("").text.text)
        assertEquals("7", transform("7").text.text)
        assertEquals("999", transform("999").text.text)
        assertEquals("1,000", transform("1000").text.text)
        assertEquals("42,000", transform("42000").text.text)
        assertEquals("280,000", transform("280000").text.text)
        assertEquals("1,234,567", transform("1234567").text.text)
        assertEquals("999,999,999", transform("999999999").text.text)
    }

    @Test
    fun cursorAtTheEnd_staysAtTheEnd() {
        listOf("", "5", "280000", "1234567").forEach { digits ->
            val transformed = transform(digits)

            assertEquals(digits.length, transformed.offsetMapping.transformedToOriginal(transformed.text.length))
            assertEquals(transformed.text.length, transformed.offsetMapping.originalToTransformed(digits.length))
        }
    }

    @Test
    fun offsets_roundTripAndStayInsideTheText() {
        (0..9).forEach { length ->
            val digits = "1234567890".take(length)
            val transformed = transform(digits)
            var previous = -1

            (0..length).forEach { offset ->
                val inTransformed = transformed.offsetMapping.originalToTransformed(offset)

                assertTrue("offset $offset of $digits", inTransformed in 0..transformed.text.length)
                assertTrue("offsets must not go backwards", inTransformed >= previous)
                assertEquals(offset, transformed.offsetMapping.transformedToOriginal(inTransformed))
                previous = inTransformed
            }
        }
    }

    @Test
    fun everyTransformedOffset_mapsBackToAnOriginalOffset() {
        val digits = "280000"
        val transformed = transform(digits)

        (0..transformed.text.length).forEach { offset ->
            assertTrue(transformed.offsetMapping.transformedToOriginal(offset) in 0..digits.length)
        }
    }

    @Test
    fun cursorAfterTheSeparator_mapsToTheDigitThatFollowsIt() {
        val transformed = transform("280000")

        // "280,000": the cursor after the comma (offset 4) sits before the first digit of the last group (original 3).
        assertEquals(3, transformed.offsetMapping.transformedToOriginal(4))
        assertEquals(3, transformed.offsetMapping.transformedToOriginal(3))
    }
}
