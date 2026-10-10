package com.auracode.hiposim.core.designsystem.component

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

private const val GROUP_SIZE = 3
private const val SEPARATOR = ','

/** Shows digits grouped by thousands (`280000` as `280,000`) while the field keeps holding the plain digits. */
object ThousandsVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val formatted = groupThousands(digits)
        val mapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val position = offset.coerceIn(0, digits.length)
                    return position + (1 until position).count { isSeparatorBefore(digits.length, it) }
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val position = offset.coerceIn(0, formatted.length)
                    return position - formatted.take(position).count { it == SEPARATOR }
                }
            }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}

/** `"1234567"` becomes `"1,234,567"`. Text that is not made of digits is grouped the same way, character by character. */
internal fun groupThousands(digits: String): String =
    buildString {
        digits.forEachIndexed { index, char ->
            if (isSeparatorBefore(digits.length, index)) append(SEPARATOR)
            append(char)
        }
    }

/** A separator goes before the character at [index] when a whole number of groups of three remains after it. */
private fun isSeparatorBefore(
    length: Int,
    index: Int,
): Boolean = index > 0 && (length - index) % GROUP_SIZE == 0
