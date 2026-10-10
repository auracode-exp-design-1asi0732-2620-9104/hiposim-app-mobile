package com.auracode.hiposim.core.designsystem.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Outlined text field with a leading icon. [errorText] replaces [supportingText] while there is an error.
 */
@Composable
fun HipoSimTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    prefix: String? = null,
    suffix: String? = null,
    supportingText: String? = null,
    errorText: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        prefix = prefix?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
        suffix = suffix?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
        leadingIcon = { Icon(leadingIcon, contentDescription = null) },
        trailingIcon = trailingIcon,
        supportingText = (errorText ?: supportingText)?.let { { Text(it) } },
        isError = errorText != null,
        singleLine = true,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        shape = MaterialTheme.shapes.small,
    )
}

/** Shows a string of digits grouped in thousands (`280000` as `280,000`). The value itself keeps only digits. */
object ThousandsSeparatorTransformation : VisualTransformation {
    private const val GROUP_SIZE = 3
    private const val SEPARATOR = ','

    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val grouped = group(digits)
        val mapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int {
                    val digitsToTheRight = digits.length - offset
                    val separatorsToTheRight = if (digitsToTheRight == 0) 0 else (digitsToTheRight - 1) / GROUP_SIZE
                    return (offset + separatorCount(digits.length) - separatorsToTheRight).coerceIn(0, grouped.length)
                }

                override fun transformedToOriginal(offset: Int): Int {
                    val end = offset.coerceIn(0, grouped.length)
                    return (end - grouped.take(end).count { it == SEPARATOR }).coerceIn(0, digits.length)
                }
            }
        return TransformedText(AnnotatedString(grouped), mapping)
    }

    fun group(digits: String): String =
        digits
            .reversed()
            .chunked(GROUP_SIZE)
            .joinToString(SEPARATOR.toString())
            .reversed()

    private fun separatorCount(length: Int): Int = if (length == 0) 0 else (length - 1) / GROUP_SIZE
}
