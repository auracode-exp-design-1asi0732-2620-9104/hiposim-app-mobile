package com.auracode.hiposim.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auracode.hiposim.core.designsystem.theme.HipoSimTheme
import com.auracode.hiposim.core.designsystem.theme.Spacing

private val ButtonContentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm)
private val ButtonIconSize = 20.dp

/** Main call to action, amber fill. [supportingText] is a second, smaller line. */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    supportingText: String? = null,
) {
    val extra = HipoSimTheme.extraColors
    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Spacing.minTouchTarget),
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = extra.accent, contentColor = extra.onAccent),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp),
        contentPadding = ButtonContentPadding,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
                    Spacer(Modifier.width(Spacing.sm))
                }
                Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
            }
            if (supportingText != null) {
                Text(text = supportingText, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Filled brand button for forms. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.defaultMinSize(minHeight = Spacing.minTouchTarget + Spacing.xs),
        shape = MaterialTheme.shapes.small,
        contentPadding = ButtonContentPadding,
    ) {
        Text(text = text, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        if (trailingIcon != null) {
            Spacer(Modifier.width(Spacing.sm))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
        }
    }
}

/** Secondary action with a brand outline. */
@Composable
fun OutlinedActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = Spacing.minTouchTarget),
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        contentPadding = ButtonContentPadding,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text = text, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
    }
}
